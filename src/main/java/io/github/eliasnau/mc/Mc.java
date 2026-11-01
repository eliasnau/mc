package io.github.eliasnau.mc;

import io.github.eliasnau.mc.core.Feature;
import io.github.eliasnau.mc.feature.Features;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mc implements ModInitializer {
	public static final String MOD_ID = "mc";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		for (Feature feature : Features.ALL) {
			LOGGER.debug("Initializing feature {}", feature.id());
			feature.init();
		}
		LOGGER.info("Initialized {} feature(s)", Features.ALL.size());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
