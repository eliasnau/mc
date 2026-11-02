package io.github.eliasnau.mc.client.core.screen;

import io.github.eliasnau.mc.Mc;
import io.github.eliasnau.mc.client.core.McConfig;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A two-column settings screen built from a list of options stored in the feature's config section.
 * Labels use the translation key {@code gui.mc.<feature id>.<option key>}.
 */
public final class FeatureSettingsScreen extends McMenuScreen {
	private static final int COLUMN_WIDTH = 150;
	private static final int GAP = 10;

	private final String featureId;
	private final List<Option> options;

	public FeatureSettingsScreen(final Component title, final @Nullable Screen parent, final String featureId, final List<Option> options) {
		super(title, parent);
		this.featureId = featureId;
		this.options = options;
	}

	@Override
	protected void init() {
		int rows = (this.options.size() + 1) / 2;
		int left = this.width / 2 - COLUMN_WIDTH - GAP / 2;
		int right = this.width / 2 + GAP / 2;
		int y = this.addTitle(Math.max(8, this.height / 2 - (rows * ROW_HEIGHT + 60) / 2));

		for (int i = 0; i < this.options.size(); i++) {
			int x = i % 2 == 0 ? left : right;
			int rowY = y + i / 2 * ROW_HEIGHT;
			switch (this.options.get(i)) {
				case Toggle toggle -> this.addRenderableWidget(CycleButton.onOffBuilder(McConfig.getBoolean(this.featureId, toggle.key(), toggle.fallback()))
					.create(x, rowY, COLUMN_WIDTH, 20, this.label(toggle.key()), (button, value) -> McConfig.setBoolean(this.featureId, toggle.key(), value)));
				case Slider slider -> this.addRenderableWidget(new ConfigSlider(
					x, rowY, COLUMN_WIDTH, this.label(slider.key()), slider.min(), slider.max(),
					McConfig.getFloat(this.featureId, slider.key(), slider.fallback()), slider.format(),
					value -> McConfig.setFloat(this.featureId, slider.key(), value)
				));
			}
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(this.width / 2 - 100, y + rows * ROW_HEIGHT + 8, 200, 20)
			.build());
	}

	private Component label(final String key) {
		return Component.translatable("gui." + Mc.MOD_ID + "." + this.featureId + "." + key);
	}

	public static Component percent(final float value) {
		return Component.literal(Math.round(value * 100.0F) + "%");
	}

	public static Component times(final float value) {
		return Component.literal(String.format(java.util.Locale.ROOT, "%.1fx", value));
	}

	public sealed interface Option permits Toggle, Slider {
	}

	public record Toggle(String key, boolean fallback) implements Option {
	}

	public record Slider(String key, float min, float max, float fallback, Function<Float, Component> format) implements Option {
	}
}
