package io.github.eliasnau.mc.client.core;

import io.github.eliasnau.mc.Mc;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Client-only half of a feature (rendering, keybinds, screens, HUD).
 * Should use the same {@link #id()} as its common {@link io.github.eliasnau.mc.core.Feature}, if it has one.
 * Register implementations in {@link io.github.eliasnau.mc.client.feature.ClientFeatures#ALL}.
 */
public interface ClientFeature {
	String id();

	void initClient();

	/** Shown in the F7 features menu; translation key {@code feature.mc.<id>}. */
	default Component name() {
		return Component.translatable("feature." + Mc.MOD_ID + "." + this.id());
	}

	/** Features stay registered when disabled, so they should check this before doing anything visible. */
	default boolean isEnabled() {
		return McConfig.isEnabled(this.id());
	}

	/** Returns the feature's own settings screen, or {@code null} if it has none. */
	default @Nullable Screen createSettingsScreen(final Screen parent) {
		return null;
	}
}
