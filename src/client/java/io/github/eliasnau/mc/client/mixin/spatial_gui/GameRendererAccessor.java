package io.github.eliasnau.mc.client.mixin.spatial_gui;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Accessor("featureRenderDispatcher")
	FeatureRenderDispatcher mc$featureRenderDispatcher();
}
