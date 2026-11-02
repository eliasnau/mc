package io.github.eliasnau.mc.client.mixin.spatial_gui;

import com.mojang.blaze3d.platform.Window;
import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every screen mouse event and hover check reads these, so mapping them covers clicks, scrolling and tooltips. */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@Shadow
	private double xpos;

	@Shadow
	private double ypos;

	@Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("HEAD"), cancellable = true)
	private void mc$mapSpatialX(final Window window, final CallbackInfoReturnable<Double> cir) {
		double[] mapped = SpatialGui.mapCursor(window, this.xpos, this.ypos);
		if (mapped != null) {
			cir.setReturnValue(mapped[0]);
		}
	}

	@Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("HEAD"), cancellable = true)
	private void mc$mapSpatialY(final Window window, final CallbackInfoReturnable<Double> cir) {
		double[] mapped = SpatialGui.mapCursor(window, this.xpos, this.ypos);
		if (mapped != null) {
			cir.setReturnValue(mapped[1]);
		}
	}
}
