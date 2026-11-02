package io.github.eliasnau.mc.client.feature.spatial_gui;

import io.github.eliasnau.mc.client.mixin.spatial_gui.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.jspecify.annotations.Nullable;

/**
 * Where a screen's content sits on the GUI canvas and where that content floats in camera space.
 * Camera space: +X right, +Y up, camera looks down -Z. Units are blocks.
 *
 * @param focusX      left edge of the screen's content, in GUI pixels
 * @param focusY      top edge of the screen's content, in GUI pixels
 * @param focusWidth  content width, in GUI pixels
 * @param focusHeight content height, in GUI pixels
 * @param worldHeight how tall the content appears in the world
 * @param anchorX     camera-space position of the content's center
 * @param anchorY     camera-space position of the content's center
 * @param anchorZ     camera-space position of the content's center
 * @param yaw         panel rotation around Y in degrees; negative turns its right edge towards the camera
 * @param pitch       panel rotation around X in degrees; negative leans its top away from the camera
 * @param curveRadius radius in blocks of the cylinder the panel is bent around, or {@code 0} for a flat panel
 * @param openPitch   extra pitch the panel starts at when it opens, so it can flip up into place
 * @param margin      extra canvas around the focus area that still gets drawn, so tooltips and recipe books aren't cut off
 * @param clip        clips the screen's content to the focus area, for screens that draw outside their own bounds
 */
public record PanelLayout(
	float focusX,
	float focusY,
	float focusWidth,
	float focusHeight,
	float worldHeight,
	float anchorX,
	float anchorY,
	float anchorZ,
	float yaw,
	float pitch,
	float curveRadius,
	float openPitch,
	float margin,
	boolean clip
) {
	private static final float WIDE_MARGIN = 140.0F;
	/** Same scale for every container, so a chest's slots match the inventory's instead of being squeezed. */
	private static final float CONTAINER_PIXEL_SIZE = 0.78F / 166.0F;
	/** Keeps very tall modded containers from filling the whole view. */
	private static final float MAX_CONTAINER_HEIGHT = 1.2F;
	private static final float MENU_PADDING = 10.0F;
	/** Chat's input box spans the whole window, so chat is clipped to its message column. */
	private static final float CHAT_MARGIN = 4.0F;
	private static final float CHAT_PIXEL_SIZE = 0.0045F;

	/** Returns how {@code screen} should be laid out in the world, or {@code null} to render it normally. */
	public static @Nullable PanelLayout forScreen(final Screen screen, final int guiWidth, final int guiHeight) {
		if (screen instanceof AbstractContainerScreen<?> container && SpatialGuiSettings.on(SpatialGuiSettings.CONTAINERS)) {
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor)container;
			float worldHeight = Math.min(accessor.mc$imageHeight() * CONTAINER_PIXEL_SIZE, MAX_CONTAINER_HEIGHT);
			return new PanelLayout(
				accessor.mc$leftPos(), accessor.mc$topPos(), accessor.mc$imageWidth(), accessor.mc$imageHeight(),
				worldHeight, 0.44F, 0.04F, -1.25F, -24.0F, 0.0F, 0.0F, 0.0F, WIDE_MARGIN, false
			);
		}

		if (screen instanceof ChatScreen && SpatialGuiSettings.on(SpatialGuiSettings.CHAT)) {
			Options options = Minecraft.getInstance().options;
			double chatScale = options.chatScale().get();
			// Message column (plus its left padding and scrollbar) stacked on top of the input box.
			float width = Math.min(guiWidth, (float)(ChatComponent.getWidth(options.chatWidth().get()) * chatScale) + 12.0F);
			float height = Math.min(guiHeight, (float)(ChatComponent.getHeight(options.chatHeightFocused().get()) * chatScale) + 20.0F);
			return new PanelLayout(
				0.0F, guiHeight - height, width, height, height * CHAT_PIXEL_SIZE, 0.42F, -0.04F, -1.30F, -20.0F,
				0.0F, 0.0F, 0.0F, CHAT_MARGIN, true
			);
		}

		if (screen instanceof PauseScreen pause && pause.showsPauseMenu() && SpatialGuiSettings.on(SpatialGuiSettings.MENUS)) {
			ScreenRectangle widgets = widgetBounds(screen);
			if (widgets == null) {
				return null;
			}

			// The pause grid only knows its own size once laid out, so the panel wraps whatever widgets it ended up with.
			float x = widgets.left() - MENU_PADDING;
			float y = widgets.top() - MENU_PADDING;
			float width = widgets.width() + MENU_PADDING * 2.0F;
			float height = widgets.height() + MENU_PADDING * 2.0F;
			return new PanelLayout(
				x, y, width, height, height * CONTAINER_PIXEL_SIZE * 1.1F, 0.40F, 0.06F, -1.25F, -20.0F,
				0.0F, 0.0F, 0.0F, MENU_PADDING, false
			);
		}

		boolean bookOrSign = screen instanceof BookViewScreen || screen instanceof BookEditScreen || screen instanceof BookSignScreen
			|| screen instanceof AbstractSignEditScreen;
		if (bookOrSign && SpatialGuiSettings.on(SpatialGuiSettings.BOOKS_AND_SIGNS)) {
			float width = Math.min(guiWidth, 200);
			float height = Math.min(guiHeight, 200);
			return new PanelLayout((guiWidth - width) / 2.0F, 0.0F, width, height, 0.85F, 0.42F, 0.02F, -1.25F, -22.0F, 0.0F, 0.0F, 0.0F, WIDE_MARGIN, false);
		}

		return null;
	}

	private static @Nullable ScreenRectangle widgetBounds(final Screen screen) {
		int left = Integer.MAX_VALUE;
		int top = Integer.MAX_VALUE;
		int right = Integer.MIN_VALUE;
		int bottom = Integer.MIN_VALUE;
		for (GuiEventListener child : screen.children()) {
			if (child instanceof LayoutElement element) {
				ScreenRectangle rectangle = element.getRectangle();
				left = Math.min(left, rectangle.left());
				top = Math.min(top, rectangle.top());
				right = Math.max(right, rectangle.right());
				bottom = Math.max(bottom, rectangle.bottom());
			}
		}

		return right > left && bottom > top ? new ScreenRectangle(left, top, right - left, bottom - top) : null;
	}

	public float centerX() {
		return this.focusX + this.focusWidth / 2.0F;
	}

	public float centerY() {
		return this.focusY + this.focusHeight / 2.0F;
	}
}
