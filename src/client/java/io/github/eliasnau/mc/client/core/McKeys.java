package io.github.eliasnau.mc.client.core;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.eliasnau.mc.Mc;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/** All of the mod's key mappings share one category in the Controls screen. */
public final class McKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Mc.id("main"));

	private McKeys() {
	}

	/** Registers {@code key.mc.<name>}, bound to {@code defaultKey} (an {@link InputConstants} keyboard code). */
	public static KeyMapping register(final String name, final int defaultKey) {
		return register(name, InputConstants.Type.KEYBOARD, defaultKey);
	}

	/** Registers {@code key.mc.<name>} with a keyboard or mouse default, like {@link InputConstants#MOUSE_BUTTON_MIDDLE}. */
	public static KeyMapping register(final String name, final InputConstants.Type type, final int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key." + Mc.MOD_ID + "." + name, type, defaultKey, CATEGORY));
	}
}
