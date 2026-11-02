package io.github.eliasnau.mc.client.mixin.spatial_gui;

import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
	/** The fullscreen dim would turn the floating panel into a dark rectangle, and the world should stay visible. */
	@Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
	private void mc$skipDimWhenSpatial(final GuiGraphicsExtractor graphics, final CallbackInfo ci) {
		if (SpatialGui.isCapturing()) {
			ci.cancel();
		}
	}

	/** Menus like the pause screen cover the window with a dark texture, which has no place on a floating panel. */
	@Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), cancellable = true)
	private void mc$skipMenuBackgroundWhenSpatial(final GuiGraphicsExtractor graphics, final CallbackInfo ci) {
		if (SpatialGui.isCapturing()) {
			ci.cancel();
		}
	}
}
