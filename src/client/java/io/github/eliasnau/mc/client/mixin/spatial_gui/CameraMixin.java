package io.github.eliasnau.mc.client.mixin.spatial_gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
abstract class CameraMixin {
	/** Replaces the third-person pull-back with the over-the-shoulder offset while a spatial screen is up. */
	@WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V", ordinal = 0))
	private void mc$shoulderCamera(final Camera camera, final float forwards, final float up, final float right, final Operation<Void> original) {
		float[] offset = SpatialGui.shoulderOffset(camera.position(), camera.rotation(), forwards, up, right);
		if (offset != null) {
			original.call(camera, offset[0], offset[1], offset[2]);
		} else {
			original.call(camera, forwards, up, right);
		}
	}
}
