package io.github.eliasnau.mc.core;

/**
 * A self-contained piece of functionality. Runs on both client and dedicated server.
 * Register implementations in {@link io.github.eliasnau.mc.feature.Features#ALL}.
 */
public interface Feature {
	/** Unique snake_case id, also used as the feature's package name. */
	String id();

	/** Register blocks, items, events, networking payloads, etc. */
	void init();
}
