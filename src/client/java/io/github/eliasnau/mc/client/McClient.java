package io.github.eliasnau.mc.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.eliasnau.mc.Mc;
import io.github.eliasnau.mc.client.core.ClientFeature;
import io.github.eliasnau.mc.client.core.McKeys;
import io.github.eliasnau.mc.client.core.screen.FeaturesScreen;
import io.github.eliasnau.mc.client.feature.ClientFeatures;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;

public class McClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (ClientFeature feature : ClientFeatures.ALL) {
			Mc.LOGGER.debug("Initializing client feature {}", feature.id());
			feature.initClient();
		}

		KeyMapping openFeatures = McKeys.register("features", InputConstants.KEY_F7);
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (openFeatures.consumeClick()) {
				if (minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new FeaturesScreen(null));
				}
			}
		});
	}
}
