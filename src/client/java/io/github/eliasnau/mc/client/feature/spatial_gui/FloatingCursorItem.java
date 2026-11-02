package io.github.eliasnau.mc.client.feature.spatial_gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * The stack held on the cursor, lifted off the panel towards the camera instead of being baked into its texture.
 * It bobs gently and swings in the direction it's dragged.
 */
final class FloatingCursorItem {
	/** Blocks between the panel and the item. */
	private static final float LIFT = 0.06F;
	private static final float BOB = 0.008F;
	private static final float MAX_TILT = 0.45F;
	private static final float SCALE = 1.15F;

	private static @Nullable ItemStack stack;
	private static @Nullable String countText;
	private static float guiX;
	private static float guiY;
	private static float tilt;
	private static float previousX = Float.NaN;
	private static long lastNanos;

	private FloatingCursorItem() {
	}

	static void clear() {
		stack = null;
	}

	/** Takes over drawing the carried stack at top-left {@code x, y}. Returns {@code false} to let vanilla draw it. */
	static boolean capture(final ItemStack carried, final int x, final int y, final @Nullable String count) {
		if (!SpatialGui.isCapturing() || !SpatialGuiSettings.on(SpatialGuiSettings.FLOATING_ITEM)) {
			return false;
		}

		stack = carried;
		countText = count;
		guiX = x + 8.0F;
		guiY = y + 8.0F;
		return true;
	}

	static void extract(final GuiGraphicsExtractor graphics, final PanelSurface surface, final Matrix4fc projection, final float alpha) {
		long now = Util.getNanos();
		float dt = lastNanos == 0L ? 0.0F : Math.min((now - lastNanos) / 1.0E9F, 0.1F);
		lastNanos = now;
		if (stack == null || alpha < 0.5F) {
			previousX = Float.NaN;
			tilt = 0.0F;
			return;
		}

		// Swing like a held object: dragging right tips the item left and vice versa.
		float velocity = Float.isNaN(previousX) || dt <= 0.0F ? 0.0F : (guiX - previousX) / dt;
		previousX = guiX;
		float smoothing = 1.0F - (float)Math.exp(-dt * 10.0F);
		tilt += (Mth.clamp(-velocity * 0.0012F, -MAX_TILT, MAX_TILT) - tilt) * smoothing;

		float lift = LIFT + BOB * Mth.sin((float)(now % 6_283_185_307L) / 1.0E9F * 3.0F);
		Vector3f normal = surface.normal(new Vector3f()).mul(lift);
		Vector3f center = surface.toView(guiX, guiY, new Vector3f()).add(normal);
		Vector3f edge = surface.toView(guiX + 16.0F, guiY, new Vector3f()).add(normal);
		float[] screenCenter = project(projection, center, graphics.guiWidth(), graphics.guiHeight());
		float[] screenEdge = project(projection, edge, graphics.guiWidth(), graphics.guiHeight());
		if (screenCenter == null || screenEdge == null) {
			return;
		}

		float scale = (float)Math.hypot(screenEdge[0] - screenCenter[0], screenEdge[1] - screenCenter[1]) / 16.0F * SCALE;
		graphics.nextStratum();
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(screenCenter[0], screenCenter[1]);
		pose.rotate(tilt);
		pose.scale(scale, scale);
		graphics.item(stack, -8, -8);
		graphics.itemDecorations(Minecraft.getInstance().font, stack, -8, -8, countText);
		pose.popMatrix();
	}

	private static float @Nullable [] project(final Matrix4fc projection, final Vector3f view, final int guiWidth, final int guiHeight) {
		if (view.z > -0.06F) {
			return null;
		}

		Vector4f clip = projection.transform(new Vector4f(view, 1.0F));
		return new float[]{(clip.x / clip.w * 0.5F + 0.5F) * guiWidth, (0.5F - clip.y / clip.w * 0.5F) * guiHeight};
	}
}
