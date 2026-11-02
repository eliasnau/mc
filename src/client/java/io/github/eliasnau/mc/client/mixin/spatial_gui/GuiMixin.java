package io.github.eliasnau.mc.client.mixin.spatial_gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
abstract class GuiMixin {
	@WrapOperation(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
		)
	)
	private void mc$extractScreenOffscreen(
		final Screen screen,
		final GuiGraphicsExtractor graphics,
		final int mouseX,
		final int mouseY,
		final float partialTicks,
		final Operation<Void> original
	) {
		SpatialGui.extractScreen(screen, graphics, mouseX, mouseY, partialTicks, original);
	}

	/** Runs right after the screen (or where it would be), so the panel sits above the HUD but below toasts. */
	@WrapOperation(
		method = "extractRenderState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
	)
	private void mc$extractSpatialPanel(
		final Hud hud, final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker, final Operation<Void> original
	) {
		SpatialGui.extractPanel(graphics);
		original.call(hud, graphics, deltaTracker);
	}
}
