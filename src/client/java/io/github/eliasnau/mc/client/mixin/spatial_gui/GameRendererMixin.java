package io.github.eliasnau.mc.client.mixin.spatial_gui;

import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Inject(method = "extract", at = @At("HEAD"))
	private void mc$beginSpatialGuiFrame(final DeltaTracker deltaTracker, final boolean advanceGameTime, final CallbackInfo ci) {
		SpatialGui.beginFrame();
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
	private void mc$renderSpatialGui(final CallbackInfo ci) {
		SpatialGui.renderOffscreen();
	}
}
