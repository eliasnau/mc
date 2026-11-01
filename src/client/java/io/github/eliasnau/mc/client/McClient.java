package io.github.eliasnau.mc.client;

import io.github.eliasnau.mc.Mc;
import io.github.eliasnau.mc.client.core.ClientFeature;
import io.github.eliasnau.mc.client.feature.ClientFeatures;
import net.fabricmc.api.ClientModInitializer;

public class McClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (ClientFeature feature : ClientFeatures.ALL) {
			Mc.LOGGER.debug("Initializing client feature {}", feature.id());
			feature.initClient();
		}
	}
}
