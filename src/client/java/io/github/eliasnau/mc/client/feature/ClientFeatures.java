package io.github.eliasnau.mc.client.feature;

import io.github.eliasnau.mc.client.core.ClientFeature;
import io.github.eliasnau.mc.client.feature.spatial_gui.SpatialGuiClientFeature;
import java.util.List;

/** Client side of each feature lives in {@code client.feature.<id>}; its mixins in {@code client.mixin.<id>}. */
public final class ClientFeatures {
	public static final List<ClientFeature> ALL = List.of(
		new SpatialGuiClientFeature()
	);

	private ClientFeatures() {
	}
}
