package io.github.eliasnau.mc.client.mixin.spatial_gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
	/** The carried stack is drawn floating above the panel instead of flat inside it. */
	@WrapOperation(
		method = "extractCarriedItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractFloatingItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"
		)
	)
	private void mc$floatCarriedItem(
		final AbstractContainerScreen<?> screen,
		final GuiGraphicsExtractor graphics,
		final ItemStack carried,
		final int x,
		final int y,
		final String itemCount,
		final Operation<Void> original
	) {
		if (!SpatialGui.captureCarriedItem(carried, x, y, itemCount)) {
			original.call(screen, graphics, carried, x, y, itemCount);
		}
	}
}
