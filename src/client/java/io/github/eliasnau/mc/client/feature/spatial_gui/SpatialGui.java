package io.github.eliasnau.mc.client.feature.spatial_gui;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.eliasnau.mc.client.mixin.spatial_gui.GameRendererAccessor;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * Renders supported screens into an offscreen texture instead of the window, then draws that texture
 * as a perspective panel floating next to the player while the camera moves over their shoulder.
 * Mouse input is ray cast onto the panel and converted back into the screen's GUI coordinates.
 */
public final class SpatialGui {
	private static final float OPEN_SECONDS = 0.28F;
	private static final float CLOSE_SECONDS = 0.22F;
	/** Over-the-shoulder camera offset, applied as {@code Camera.move(forwards, up, right)}. */
	private static final float SHOULDER_FORWARDS = -1.35F;
	private static final float SHOULDER_UP = 0.10F;
	private static final float SHOULDER_RIGHT = 0.70F;
	private static final float MAX_PARALLAX_DEGREES = 3.0F;
	private static final int BACKGROUND_DIM = 70;
	/** How far the body turns towards the panel; the head covers the rest. */
	private static final float MAX_BODY_TURN = 35.0F;
	private static final float MAX_HEAD_TURN = 75.0F;
	/** Keeps the arm slightly bent down from the exact cursor direction, which reads more like reaching. */
	private static final float ARM_DROOP = 0.25F;
	/**
	 * How far along the cursor ray the player looks, in blocks. The panel sits almost beside the player, so looking
	 * at the panel itself would barely turn the head; looking past it makes the head clearly follow the mouse.
	 */
	private static final float LOOK_DISTANCE = 4.0F;
	private static final int GRID = 20;
	/** The panel is usually shown larger than 1:1, so the screen is rendered supersampled to stay sharp. */
	private static final float SUPERSAMPLE = 2.0F;
	private static final int MAX_TARGET_SIZE = 4096;

	private static @Nullable Screen activeScreen;
	private static @Nullable PanelLayout layout;
	private static float progress;
	private static long lastFrameNanos;
	private static float parallaxX;
	private static float parallaxY;
	/** Smoothed cursor position in normalized device coordinates; the player looks along this ray. */
	private static float lookX;
	private static float lookY;
	private static boolean lookValid;
	/** {@code 1} for the right shoulder, {@code -1} for the left; picked whenever a panel opens from closed. */
	private static float side = 1.0F;
	private static @Nullable LivingEntityRenderState reachState;
	private static float reachYaw;
	private static float reachPitch;
	private static float reachProgress;
	private static @Nullable CameraType savedCameraType;

	private static @Nullable TextureTarget target;
	private static @Nullable GuiRenderState renderState;
	private static @Nullable GuiRenderer renderer;
	private static boolean capturing;
	private static boolean extractedThisFrame;
	private static boolean hasContent;
	private static int canvasWidth;
	private static int canvasHeight;

	private static final PanelMipmaps mipmaps = new PanelMipmaps();
	private static final PanelSurface surface = new PanelSurface();
	private static final Matrix4f projection = new Matrix4f();
	private static boolean panelValid;

	private SpatialGui() {
	}

	// --- Frame lifecycle -------------------------------------------------------------------------------------

	/** Called at the start of every frame's extract phase, before the camera is set up. */
	public static void beginFrame() {
		Minecraft minecraft = Minecraft.getInstance();
		long now = Util.getNanos();
		float dt = lastFrameNanos == 0L ? 0.0F : Math.min((now - lastFrameNanos) / 1.0E9F, 0.1F);
		lastFrameNanos = now;

		Screen screen = minecraft.gui.screen();
		Window window = minecraft.getWindow();
		PanelLayout wanted = SpatialGuiSettings.enabled() && minecraft.level != null && minecraft.player != null && screen != null
			? PanelLayout.forScreen(screen, window.getGuiScaledWidth(), window.getGuiScaledHeight())
			: null;

		reachState = null;
		if (wanted != null) {
			if (progress <= 0.0F) {
				side = chooseSide(minecraft);
			}
			if (screen != activeScreen) {
				// Switching straight from one spatial screen to another keeps the panel up, but restarts the slide-in.
				progress = Math.min(progress, 0.35F);
			}
			activeScreen = screen;
			layout = wanted;
			progress = Math.min(1.0F, progress + dt / OPEN_SECONDS);
		} else {
			activeScreen = null;
			progress = minecraft.level == null ? 0.0F : Math.max(0.0F, progress - dt / CLOSE_SECONDS);
		}

		updateCameraType(minecraft);
		updateParallax(minecraft, dt);
		updatePanelTransform(minecraft);
		updateLookTarget(minecraft, dt);
	}

	private static void updateCameraType(final Minecraft minecraft) {
		if (progress > 0.0F && savedCameraType == null) {
			savedCameraType = minecraft.options.getCameraType();
			minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		} else if (progress <= 0.0F && savedCameraType != null) {
			minecraft.options.setCameraType(savedCameraType);
			savedCameraType = null;
		}
	}

	private static void updateParallax(final Minecraft minecraft, final float dt) {
		Window window = minecraft.getWindow();
		float targetX = 0.0F;
		float targetY = 0.0F;
		if (activeScreen != null && window.getScreenWidth() > 0 && window.getScreenHeight() > 0) {
			targetX = Mth.clamp((float)(minecraft.mouseHandler.xpos() / window.getScreenWidth()) * 2.0F - 1.0F, -1.0F, 1.0F);
			targetY = Mth.clamp(1.0F - (float)(minecraft.mouseHandler.ypos() / window.getScreenHeight()) * 2.0F, -1.0F, 1.0F);
		}

		float smoothing = 1.0F - (float)Math.exp(-dt * 6.0F);
		parallaxX += (targetX - parallaxX) * smoothing;
		parallaxY += (targetY - parallaxY) * smoothing;
	}

	private static void updatePanelTransform(final Minecraft minecraft) {
		Window window = minecraft.getWindow();
		if (layout == null || progress <= 0.0F || window.getWidth() <= 0 || window.getHeight() <= 0) {
			panelValid = false;
			return;
		}

		float eased = eased();
		float pixelSize = layout.worldHeight() / Math.max(layout.focusHeight(), 1.0F) * SpatialGuiSettings.panelScale()
			* (0.85F + 0.15F * eased);
		float yaw = (layout.yaw() + (1.0F - eased) * 30.0F) * side - parallaxX * MAX_PARALLAX_DEGREES;
		float pitch = layout.pitch() + (1.0F - eased) * layout.openPitch() + parallaxY * MAX_PARALLAX_DEGREES * 0.6F;

		Matrix4f localToView = new Matrix4f()
			.translate((layout.anchorX() + (1.0F - eased) * 0.35F) * side, layout.anchorY(), layout.anchorZ() - (1.0F - eased) * 0.2F)
			.rotateY(yaw * Mth.DEG_TO_RAD)
			.rotateX(pitch * Mth.DEG_TO_RAD);
		surface.set(localToView, pixelSize, layout.centerX(), layout.centerY(), layout.curveRadius());

		float fov = minecraft.options.fov().get().floatValue();
		projection.setPerspective(fov * Mth.DEG_TO_RAD, (float)window.getWidth() / window.getHeight(), 0.05F, 100.0F);
		panelValid = true;
	}

	/** Uses the left shoulder when a wall leaves clearly more room there than on the right. */
	private static float chooseSide(final Minecraft minecraft) {
		if (!SpatialGuiSettings.on(SpatialGuiSettings.SHOULDER_SWAP) || minecraft.player == null || minecraft.level == null) {
			return 1.0F;
		}

		Vec3 eye = minecraft.player.getEyePosition();
		float yaw = minecraft.player.getYRot() * Mth.DEG_TO_RAD;
		Vec3 back = new Vec3(Mth.sin(yaw), 0.0, -Mth.cos(yaw)).scale(-SHOULDER_FORWARDS * SpatialGuiSettings.cameraDistance());
		Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw)).scale(SHOULDER_RIGHT);
		float rightRoom = freeFraction(minecraft, eye, eye.add(back).add(right));
		float leftRoom = freeFraction(minecraft, eye, eye.add(back).subtract(right));
		return rightRoom < 0.75F && leftRoom > rightRoom + 0.1F ? -1.0F : 1.0F;
	}

	private static float freeFraction(final Minecraft minecraft, final Vec3 from, final Vec3 to) {
		HitResult hit = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
		return hit.getType() == HitResult.Type.MISS ? 1.0F : (float)(hit.getLocation().distanceTo(from) / to.distanceTo(from));
	}

	private static void updateLookTarget(final Minecraft minecraft, final float dt) {
		if (!panelValid) {
			lookValid = false;
			return;
		}

		// Keep the last direction while the panel fades out, so the head doesn't snap away.
		Window window = minecraft.getWindow();
		float targetX = lookX;
		float targetY = lookY;
		if (activeScreen != null && window.getScreenWidth() > 0 && window.getScreenHeight() > 0) {
			targetX = Mth.clamp((float)(minecraft.mouseHandler.xpos() / window.getScreenWidth()) * 2.0F - 1.0F, -1.0F, 1.0F);
			targetY = Mth.clamp(1.0F - (float)(minecraft.mouseHandler.ypos() / window.getScreenHeight()) * 2.0F, -1.0F, 1.0F);
		}

		if (!lookValid) {
			lookX = targetX;
			lookY = targetY;
			lookValid = true;
		}

		float smoothing = 1.0F - (float)Math.exp(-dt * 12.0F);
		lookX += (targetX - lookX) * smoothing;
		lookY += (targetY - lookY) * smoothing;
	}

	/** Camera-space direction of the ray through a point in normalized device coordinates. */
	private static Vector3f rayDirection(final float ndcX, final float ndcY) {
		Vector4f far = projection.invert(new Matrix4f()).transform(new Vector4f(ndcX, ndcY, 1.0F, 1.0F));
		return new Vector3f(far.x / far.w, far.y / far.w, far.z / far.w).normalize();
	}

	private static float eased() {
		float inverse = 1.0F - progress;
		return 1.0F - inverse * inverse * inverse;
	}

	// --- Extraction ------------------------------------------------------------------------------------------

	/** Wraps the vanilla screen extraction, redirecting it into the offscreen GUI state when the screen is spatial. */
	public static void extractScreen(
		final Screen screen,
		final GuiGraphicsExtractor graphics,
		final int mouseX,
		final int mouseY,
		final float partialTicks,
		final Operation<Void> original
	) {
		if (screen != activeScreen || !panelValid) {
			original.call(screen, graphics, mouseX, mouseY, partialTicks);
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Window window = minecraft.getWindow();
		ensureResources(minecraft, window.getWidth(), window.getHeight());
		renderState.reset();
		FloatingCursorItem.clear();
		GuiGraphicsExtractor offscreen = new GuiGraphicsExtractor(minecraft, renderState, mouseX, mouseY);
		boolean clip = layout.clip();
		if (clip) {
			offscreen.enableScissor(
				Math.round(layout.focusX()), Math.round(layout.focusY()),
				Math.round(layout.focusX() + layout.focusWidth()), Math.round(layout.focusY() + layout.focusHeight())
			);
		}

		capturing = true;
		try {
			original.call(screen, offscreen, mouseX, mouseY, partialTicks);
		} finally {
			capturing = false;
			if (clip) {
				offscreen.disableScissor();
			}
		}

		canvasWidth = window.getGuiScaledWidth();
		canvasHeight = window.getGuiScaledHeight();
		extractedThisFrame = true;
		hasContent = true;
	}

	/** Adds the floating panel to the regular HUD layer, for both the open and the fade-out animation. */
	public static void extractPanel(final GuiGraphicsExtractor graphics) {
		if (!panelValid || !hasContent || layout == null || mipmaps.view() == null) {
			return;
		}

		float x0 = Math.max(0.0F, layout.focusX() - layout.margin());
		float y0 = Math.max(0.0F, layout.focusY() - layout.margin());
		float x1 = Math.min(canvasWidth, layout.focusX() + layout.focusWidth() + layout.margin());
		float y1 = Math.min(canvasHeight, layout.focusY() + layout.focusHeight() + layout.margin());
		if (x1 <= x0 || y1 <= y0) {
			return;
		}

		float alpha = Mth.clamp(eased(), 0.0F, 1.0F);
		// Only while the spatial screen is up: no other screen can have blurred this frame then, and vanilla allows one blur.
		if (activeScreen != null && SpatialGuiSettings.on(SpatialGuiSettings.BACKGROUND_BLUR)) {
			graphics.nextStratum();
			if (Minecraft.getInstance().options.getMenuBackgroundBlurriness() >= 1.0F) {
				graphics.blurBeforeThisStratum();
			}
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.color(Math.round(alpha * BACKGROUND_DIM), 0, 0, 0));
		}

		TextureSetup texture = TextureSetup.singleTexture(mipmaps.view(), PanelMipmaps.sampler());
		graphics.nextStratum();
		graphics.guiRenderState.addGuiElement(SpatialPanelRenderState.create(
			texture, surface, projection, x0, y0, x1, y1, canvasWidth, canvasHeight,
			graphics.guiWidth(), graphics.guiHeight(), alpha, GRID
		));
		FloatingCursorItem.extract(graphics, surface, projection, alpha);
	}

	public static boolean captureCarriedItem(final ItemStack carried, final int x, final int y, final @Nullable String count) {
		return FloatingCursorItem.capture(carried, x, y, count);
	}

	public static boolean isCapturing() {
		return capturing;
	}

	// --- Rendering -------------------------------------------------------------------------------------------

	/** Called right before the main GUI is drawn, so the panel texture is up to date when it gets sampled. */
	public static void renderOffscreen() {
		if (!extractedThisFrame || renderer == null || target == null) {
			return;
		}

		extractedThisFrame = false;
		RenderSystem.getDevice()
			.createCommandEncoder()
			.clearColorAndDepthTextures(target.getColorTexture(), GuiRenderer.CLEAR_COLOR, target.getDepthTexture(), 0.0);
		renderer.render();
		renderer.endFrame();
		mipmaps.update(target.getColorTexture());
	}

	public static boolean isOffscreenRenderer(final Object guiRenderer) {
		return guiRenderer == renderer;
	}

	public static @Nullable RenderTarget target() {
		return target;
	}

	/** Scales a rectangle in window pixels to the supersampled offscreen target. Returns {@code {x, y, width, height}}. */
	public static int[] scaleScissor(final int x, final int y, final int width, final int height) {
		Window window = Minecraft.getInstance().getWindow();
		if (target == null || window.getWidth() <= 0 || window.getHeight() <= 0) {
			return new int[]{x, y, width, height};
		}

		float scaleX = (float)target.width / window.getWidth();
		float scaleY = (float)target.height / window.getHeight();
		return new int[]{
			Math.round(x * scaleX), Math.round(y * scaleY), Math.round(width * scaleX), Math.round(height * scaleY)
		};
	}

	private static void ensureResources(final Minecraft minecraft, final int windowWidth, final int windowHeight) {
		float scale = Math.max(1.0F, Math.min(SUPERSAMPLE, (float)MAX_TARGET_SIZE / Math.max(windowWidth, windowHeight)));
		int width = Math.round(windowWidth * scale);
		int height = Math.round(windowHeight * scale);
		if (target == null) {
			target = new TextureTarget("mc spatial gui", width, height, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
		} else if (target.width != width || target.height != height) {
			target.resize(width, height);
		}

		if (renderer == null) {
			renderState = new GuiRenderState();
			renderer = new GuiRenderer(
				renderState,
				((GameRendererAccessor)minecraft.gameRenderer).mc$featureRenderDispatcher(),
				List.of(
					new GuiEntityRenderer(minecraft.getEntityRenderDispatcher()),
					new GuiSkinRenderer(),
					new GuiBookModelRenderer(),
					new GuiBannerResultRenderer(minecraft.getAtlasManager()),
					new GuiProfilerChartRenderer()
				)
			);
		}
	}

	// --- Input -----------------------------------------------------------------------------------------------

	/**
	 * Converts a window cursor position into the active screen's GUI coordinates by ray casting onto the panel.
	 * Returns {@code null} when the cursor should be handled the vanilla way.
	 */
	public static double @Nullable [] mapCursor(final Window window, final double xpos, final double ypos) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!panelValid || activeScreen == null || minecraft.gui.screen() != activeScreen
			|| window.getScreenWidth() <= 0 || window.getScreenHeight() <= 0) {
			return null;
		}

		float ndcX = (float)(xpos / window.getScreenWidth()) * 2.0F - 1.0F;
		float ndcY = 1.0F - (float)(ypos / window.getScreenHeight()) * 2.0F;
		Vector3f direction = rayDirection(ndcX, ndcY);

		return surface.raycast(direction);
	}

	// --- Player pose -----------------------------------------------------------------------------------------

	/**
	 * Turns the local player's head and body to look where the cursor points, so it looks like they're using the panel,
	 * and remembers where the reaching arm should point.
	 */
	public static void facePanel(final LivingEntity entity, final LivingEntityRenderState state, final float partialTicks) {
		Minecraft minecraft = Minecraft.getInstance();
		boolean face = SpatialGuiSettings.on(SpatialGuiSettings.FACE_PANEL);
		boolean reach = SpatialGuiSettings.on(SpatialGuiSettings.ARM_REACH);
		// Screens like the inventory extract the player again for their preview doll; only pose the one in the world.
		if (capturing || entity != minecraft.player || !panelValid || !lookValid || state.isUpsideDown || !face && !reach) {
			return;
		}

		Camera camera = minecraft.gameRenderer.mainCamera();
		Vector3f look = rayDirection(lookX, lookY).mul(LOOK_DISTANCE).rotate(camera.rotation());
		Vec3 target = camera.position().add(look.x, look.y, look.z);
		Vec3 eye = entity.getEyePosition(partialTicks);
		double dx = target.x - eye.x;
		double dy = target.y - eye.y;
		double dz = target.z - eye.z;
		float yaw = (float)Mth.atan2(dz, dx) * Mth.RAD_TO_DEG - 90.0F;
		float pitch = -(float)Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG;

		float t = eased();
		if (face) {
			float headYaw = Mth.rotLerp(t, state.bodyRot + state.yRot, yaw);
			float bodyYaw = state.bodyRot + Mth.clamp(Mth.wrapDegrees(yaw - state.bodyRot), -MAX_BODY_TURN, MAX_BODY_TURN) * t;
			state.bodyRot = bodyYaw;
			state.yRot = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -MAX_HEAD_TURN, MAX_HEAD_TURN);
			state.xRot = Mth.lerp(t, state.xRot, pitch);
		}

		if (reach) {
			reachState = state;
			reachYaw = Mth.clamp(Mth.wrapDegrees(yaw - state.bodyRot), -MAX_HEAD_TURN, MAX_HEAD_TURN) * Mth.DEG_TO_RAD;
			reachPitch = pitch * Mth.DEG_TO_RAD;
			reachProgress = t;
		}
	}

	/** Raises the arm on the panel's side so it points at the cursor, like tapping a touch screen. */
	public static void poseReachingArm(final HumanoidModel<?> model, final HumanoidRenderState state) {
		if (state != reachState) {
			return;
		}

		ModelPart arm = side > 0.0F ? model.rightArm : model.leftArm;
		float t = reachProgress;
		arm.xRot = Mth.lerp(t, arm.xRot, -Mth.HALF_PI + reachPitch + ARM_DROOP);
		arm.yRot = Mth.lerp(t, arm.yRot, reachYaw - 0.1F * side);
		arm.zRot = Mth.lerp(t, arm.zRot, 0.0F);
	}

	// --- Camera ----------------------------------------------------------------------------------------------

	/**
	 * Blends the third-person camera offset towards the over-the-shoulder position.
	 * Returns {@code {forwards, up, right}} for {@code Camera.move}, or {@code null} to keep vanilla behaviour.
	 */
	public static float @Nullable [] shoulderOffset(
		final Vec3 eye, final Quaternionfc rotation, final float forwards, final float up, final float right
	) {
		if (progress <= 0.0F || savedCameraType == null) {
			return null;
		}

		boolean fromFirstPerson = savedCameraType.isFirstPerson();
		float t = eased();
		float f = Mth.lerp(t, fromFirstPerson ? 0.0F : forwards, SHOULDER_FORWARDS * SpatialGuiSettings.cameraDistance());
		float u = Mth.lerp(t, fromFirstPerson ? 0.0F : up, SHOULDER_UP);
		float r = Mth.lerp(t, fromFirstPerson ? 0.0F : right, SHOULDER_RIGHT * side);

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) {
			Vector3f offset = new Vector3f(r, u, -f).rotate(rotation);
			float length = offset.length();
			if (length > 1.0E-4F) {
				Vec3 end = eye.add(offset.x, offset.y, offset.z);
				HitResult hit = minecraft.level.clip(new ClipContext(
					eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.gameRenderer.mainCamera().entity()
				));
				if (hit.getType() != HitResult.Type.MISS) {
					float scale = Math.max(0.0F, (float)hit.getLocation().distanceTo(eye) - 0.2F) / length;
					f *= scale;
					u *= scale;
					r *= scale;
				}
			}
		}

		return new float[]{f, u, r};
	}
}
