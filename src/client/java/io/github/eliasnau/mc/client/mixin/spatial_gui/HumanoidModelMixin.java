package io.github.eliasnau.mc.client.mixin.spatial_gui;

import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
	private void mc$reachForPanel(final HumanoidRenderState state, final CallbackInfo ci) {
		SpatialGui.poseReachingArm((HumanoidModel<?>)(Object)this, state);
	}
}
