package io.github.eliasnau.mc.client.feature.spatial_gui;

import io.github.eliasnau.mc.client.core.ClientFeature;
import io.github.eliasnau.mc.client.core.screen.FeatureSettingsScreen;
import io.github.eliasnau.mc.client.core.screen.FeatureSettingsScreen.Slider;
import io.github.eliasnau.mc.client.core.screen.FeatureSettingsScreen.Toggle;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Inventories, chat, menus, books and signs open as a 3D panel next to the player instead of a flat overlay. */
public final class SpatialGuiClientFeature implements ClientFeature {
	@Override
	public String id() {
		return SpatialGuiSettings.ID;
	}

	@Override
	public void initClient() {
		// Everything runs through the spatial_gui mixins, which check SpatialGuiSettings each frame.
	}

	@Override
	public Screen createSettingsScreen(final Screen parent) {
		return new FeatureSettingsScreen(Component.translatable("gui.mc.spatial_gui.settings"), parent, this.id(), List.of(
			new Toggle(SpatialGuiSettings.CONTAINERS, true),
			new Toggle(SpatialGuiSettings.CHAT, true),
			new Toggle(SpatialGuiSettings.MENUS, true),
			new Toggle(SpatialGuiSettings.BOOKS_AND_SIGNS, true),
			new Toggle(SpatialGuiSettings.FACE_PANEL, true),
			new Toggle(SpatialGuiSettings.ARM_REACH, true),
			new Toggle(SpatialGuiSettings.FLOATING_ITEM, true),
			new Toggle(SpatialGuiSettings.SHOULDER_SWAP, true),
			new Toggle(SpatialGuiSettings.BACKGROUND_BLUR, false),
			new Slider(SpatialGuiSettings.PANEL_SCALE, 0.6F, 1.6F, 1.0F, FeatureSettingsScreen::percent),
			new Slider(SpatialGuiSettings.CAMERA_DISTANCE, 0.6F, 2.0F, 1.0F, FeatureSettingsScreen::percent)
		));
	}
}
