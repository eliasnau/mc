package io.github.eliasnau.mc.client.feature.spatial_gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.Nullable;

/**
 * A mipmapped copy of the offscreen GUI. The panel is seen at an angle, so parts of it are always minified;
 * sampling a mip chain with trilinear filtering keeps tilted straight edges smooth instead of stair-stepped.
 */
final class PanelMipmaps implements AutoCloseable {
	private static final int MAX_LEVELS = 5;
	private static final int USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
		| GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;

	private @Nullable GpuTexture texture;
	private @Nullable GpuTextureView view;
	private GpuTextureView[] levelViews = new GpuTextureView[0];

	/** Copies {@code source} into level 0 and box-filters it down into every smaller level. */
	void update(final GpuTexture source) {
		int width = source.getWidth(0);
		int height = source.getHeight(0);
		this.ensureSize(width, height);

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.copyTextureToTexture(source, this.texture, 0, 0, 0, 0, 0, width, height);

		// Each level is exactly half the previous one, so one bilinear tap per pixel averages a 2x2 block.
		GpuSampler downsample = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		for (int level = 1; level < this.levelViews.length; level++) {
			try (RenderPass pass = encoder.createRenderPass(() -> "Spatial GUI mipmap", this.levelViews[level], Optional.empty())) {
				pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.TRACY_BLIT));
				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("InSampler", this.levelViews[level - 1], downsample);
				pass.draw(3, 1, 0, 0);
			}
		}
	}

	@Nullable GpuTextureView view() {
		return this.view;
	}

	static GpuSampler sampler() {
		return RenderSystem.getSamplerCache()
			.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, true);
	}

	private void ensureSize(final int width, final int height) {
		if (this.texture != null && this.texture.getWidth(0) == width && this.texture.getHeight(0) == height) {
			return;
		}

		this.close();
		int levels = Math.min(MAX_LEVELS, 32 - Integer.numberOfLeadingZeros(Math.max(width, height)));
		GpuDevice device = RenderSystem.getDevice();
		this.texture = device.createTexture(() -> "Spatial GUI panel", USAGE, GpuFormat.RGBA8_UNORM, width, height, 1, levels);
		this.view = device.createTextureView(this.texture);
		this.levelViews = new GpuTextureView[levels];
		for (int level = 0; level < levels; level++) {
			this.levelViews[level] = device.createTextureView(this.texture, level, 1);
		}
	}

	@Override
	public void close() {
		for (GpuTextureView levelView : this.levelViews) {
			levelView.close();
		}
		this.levelViews = new GpuTextureView[0];

		if (this.view != null) {
			this.view.close();
			this.view = null;
		}

		if (this.texture != null) {
			this.texture.close();
			this.texture = null;
		}
	}
}
