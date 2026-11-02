package io.github.eliasnau.mc.client.mixin.spatial_gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Points the spatial GUI's own renderer at the supersampled offscreen target and keeps it from blurring the world. */
@Mixin(GuiRenderer.class)
abstract class GuiRendererMixin {
	@WrapOperation(
		method = "draw",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
	)
	private RenderTarget mc$useSpatialTarget(final GameRenderer gameRenderer, final Operation<RenderTarget> original) {
		RenderTarget target = SpatialGui.target();
		return target != null && SpatialGui.isOffscreenRenderer(this) ? target : original.call(gameRenderer);
	}

	@WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
	private void mc$skipSpatialBlur(final GameRenderer gameRenderer, final Operation<Void> original) {
		if (!SpatialGui.isOffscreenRenderer(this)) {
			original.call(gameRenderer);
		}
	}

	@WrapOperation(method = "enableScissor", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;enableScissor(IIII)V"))
	private void mc$scaleSpatialScissor(
		final RenderPass renderPass, final int x, final int y, final int width, final int height, final Operation<Void> original
	) {
		if (SpatialGui.isOffscreenRenderer(this)) {
			int[] scaled = SpatialGui.scaleScissor(x, y, width, height);
			original.call(renderPass, scaled[0], scaled[1], scaled[2], scaled[3]);
		} else {
			original.call(renderPass, x, y, width, height);
		}
	}
}
