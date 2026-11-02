package io.github.eliasnau.mc.client.mixin.spatial_gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int mc$leftPos();

	@Accessor("topPos")
	int mc$topPos();

	@Accessor("imageWidth")
	int mc$imageWidth();

	@Accessor("imageHeight")
	int mc$imageHeight();
}
