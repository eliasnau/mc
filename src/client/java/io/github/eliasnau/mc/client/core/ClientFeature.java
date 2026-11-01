package io.github.eliasnau.mc.client.core;

/**
 * Client-only half of a feature (rendering, keybinds, screens, HUD).
 * Should use the same {@link #id()} as its common {@link io.github.eliasnau.mc.core.Feature}, if it has one.
 * Register implementations in {@link io.github.eliasnau.mc.client.feature.ClientFeatures#ALL}.
 */
public interface ClientFeature {
	String id();

	void initClient();
}
