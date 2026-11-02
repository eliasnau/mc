package io.github.eliasnau.mc.client.core.screen;

import io.github.eliasnau.mc.client.core.ClientFeature;
import io.github.eliasnau.mc.client.core.McConfig;
import io.github.eliasnau.mc.client.feature.ClientFeatures;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Lists every feature with an on/off switch and a button into its own settings. Opened with F7. */
public final class FeaturesScreen extends McMenuScreen {
	private static final int TOGGLE_WIDTH = 200;
	private static final int SETTINGS_WIDTH = 100;
	private static final int GAP = 4;

	public FeaturesScreen(final @Nullable Screen parent) {
		super(Component.translatable("gui.mc.features"), parent);
	}

	@Override
	protected void init() {
		int left = this.width / 2 - (TOGGLE_WIDTH + GAP + SETTINGS_WIDTH) / 2;
		int y = this.addTitle(this.height / 4);
		for (ClientFeature feature : ClientFeatures.ALL) {
			this.addRenderableWidget(CycleButton.onOffBuilder(feature.isEnabled())
				.create(left, y, TOGGLE_WIDTH, 20, feature.name(), (button, enabled) -> McConfig.setEnabled(feature.id(), enabled)));
			Button settings = Button.builder(
					Component.translatable("gui.mc.features.settings"),
					button -> {
						Screen screen = feature.createSettingsScreen(this);
						if (screen != null) {
							this.minecraft.gui.setScreen(screen);
						}
					}
				)
				.bounds(left + TOGGLE_WIDTH + GAP, y, SETTINGS_WIDTH, 20)
				.build();
			settings.active = feature.createSettingsScreen(this) != null;
			this.addRenderableWidget(settings);
			y += ROW_HEIGHT;
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(this.width / 2 - 100, y + 8, 200, 20)
			.build());
	}
}
