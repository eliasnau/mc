package io.github.eliasnau.mc.feature;

import io.github.eliasnau.mc.core.Feature;
import java.util.List;

/**
 * Every feature lives in its own package: {@code feature.<id>}.
 * Its mixins go in {@code mixin.<id>}; its client side in {@code client.feature.<id>}.
 */
public final class Features {
	public static final List<Feature> ALL = List.of(
	);

	private Features() {
	}
}
