package io.github.eliasnau.mc.client.core.screen;

import io.github.eliasnau.mc.client.core.McConfig;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Base for the mod's own menus: returns to its parent on close and saves the config. */
public abstract class McMenuScreen extends Screen {
	protected static final int ROW_HEIGHT = 24;
	protected final @Nullable Screen parent;

	protected McMenuScreen(final Component title, final @Nullable Screen parent) {
		super(title);
		this.parent = parent;
	}

	/** Adds the centered title and returns the y of the first row below it. */
	protected int addTitle(final int y) {
		int textWidth = this.font.width(this.title);
		this.addRenderableWidget(new StringWidget(this.width / 2 - textWidth / 2, y, textWidth, 9, this.title, this.font));
		return y + 9 + 12;
	}

	@Override
	public void onClose() {
		McConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
