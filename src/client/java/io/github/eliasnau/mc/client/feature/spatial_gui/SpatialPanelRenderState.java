package io.github.eliasnau.mc.client.feature.spatial_gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.ARGB;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * The offscreen GUI texture drawn as a perspective-projected panel.
 * The GUI pipeline only interpolates UVs affinely, so the panel is split into a grid of small quads
 * which keeps the texture from warping across the two triangles of one big quad.
 */
public record SpatialPanelRenderState(
	TextureSetup textureSetup,
	float[] positions,
	float[] uvs,
	int quadCount,
	int color,
	@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
	/** Vertices closer than this to the camera plane are dropped instead of being projected behind the viewer. */
	private static final float MIN_DEPTH = 0.06F;

	static SpatialPanelRenderState create(
		final TextureSetup textureSetup,
		final PanelSurface surface,
		final Matrix4fc projection,
		final float x0,
		final float y0,
		final float x1,
		final float y1,
		final int canvasWidth,
		final int canvasHeight,
		final int guiWidth,
		final int guiHeight,
		final float alpha,
		final int grid
	) {
		int side = grid + 1;
		float[] projected = new float[side * side * 2];
		boolean[] visible = new boolean[side * side];
		Vector3f point = new Vector3f();
		Vector4f vertex = new Vector4f();
		for (int j = 0; j < side; j++) {
			for (int i = 0; i < side; i++) {
				int index = j * side + i;
				float x = x0 + (x1 - x0) * i / grid;
				float y = y0 + (y1 - y0) * j / grid;
				surface.toView(x, y, point);
				vertex.set(point, 1.0F);
				if (vertex.z > -MIN_DEPTH) {
					continue;
				}

				projection.transform(vertex);
				visible[index] = true;
				projected[index * 2] = (vertex.x / vertex.w * 0.5F + 0.5F) * guiWidth;
				projected[index * 2 + 1] = (0.5F - vertex.y / vertex.w * 0.5F) * guiHeight;
			}
		}

		float[] positions = new float[grid * grid * 8];
		float[] uvs = new float[grid * grid * 8];
		int quads = 0;
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (int j = 0; j < grid; j++) {
			for (int i = 0; i < grid; i++) {
				// Corner order matches vanilla blits: top-left, bottom-left, bottom-right, top-right.
				int[] corners = {j * side + i, (j + 1) * side + i, (j + 1) * side + i + 1, j * side + i + 1};
				if (!visible[corners[0]] || !visible[corners[1]] || !visible[corners[2]] || !visible[corners[3]]) {
					continue;
				}

				for (int c = 0; c < 4; c++) {
					int index = corners[c];
					float px = projected[index * 2];
					float py = projected[index * 2 + 1];
					positions[quads * 8 + c * 2] = px;
					positions[quads * 8 + c * 2 + 1] = py;
					float gx = x0 + (x1 - x0) * (index % side) / grid;
					float gy = y0 + (y1 - y0) * (index / side) / grid;
					// Render targets are stored bottom-up, so V is flipped.
					uvs[quads * 8 + c * 2] = gx / canvasWidth;
					uvs[quads * 8 + c * 2 + 1] = 1.0F - gy / canvasHeight;
					minX = Math.min(minX, px);
					minY = Math.min(minY, py);
					maxX = Math.max(maxX, px);
					maxY = Math.max(maxY, py);
				}
				quads++;
			}
		}

		ScreenRectangle bounds = null;
		if (quads > 0) {
			int left = Math.max(0, (int)Math.floor(minX));
			int top = Math.max(0, (int)Math.floor(minY));
			int right = Math.min(guiWidth, (int)Math.ceil(maxX));
			int bottom = Math.min(guiHeight, (int)Math.ceil(maxY));
			if (right > left && bottom > top) {
				bounds = new ScreenRectangle(left, top, right - left, bottom - top);
			}
		}

		// The texture holds premultiplied alpha, so fading has to scale every channel.
		int channel = Math.round(alpha * 255.0F);
		return new SpatialPanelRenderState(textureSetup, positions, uvs, quads, ARGB.color(channel, channel, channel, channel), bounds);
	}

	@Override
	public void buildVertices(final VertexConsumer vertexConsumer) {
		for (int i = 0; i < this.quadCount * 4; i++) {
			vertexConsumer.addVertex(this.positions[i * 2], this.positions[i * 2 + 1], 0.0F)
				.setUv(this.uvs[i * 2], this.uvs[i * 2 + 1])
				.setColor(this.color);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return null;
	}
}
