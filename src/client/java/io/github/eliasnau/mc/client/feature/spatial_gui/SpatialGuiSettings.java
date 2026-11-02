package io.github.eliasnau.mc.client.feature.spatial_gui;

import io.github.eliasnau.mc.client.core.McConfig;

/** Typed access to the {@code spatial_gui} section of the config. */
final class SpatialGuiSettings {
	static final String ID = "spatial_gui";
	static final String CONTAINERS = "containers";
	static final String CHAT = "chat";
	static final String MENUS = "menus";
	static final String BOOKS_AND_SIGNS = "books_and_signs";
	static final String FACE_PANEL = "face_panel";
	static final String ARM_REACH = "arm_reach";
	static final String FLOATING_ITEM = "floating_item";
	static final String SHOULDER_SWAP = "shoulder_swap";
	static final String BACKGROUND_BLUR = "background_blur";
	static final String PANEL_SCALE = "panel_scale";
	static final String CAMERA_DISTANCE = "camera_distance";

	private SpatialGuiSettings() {
	}

	static boolean enabled() {
		return McConfig.isEnabled(ID);
	}

	/** Toggles default to on, except the ones that change the look of the whole screen. */
	static boolean on(final String key) {
		return McConfig.getBoolean(ID, key, !key.equals(BACKGROUND_BLUR));
	}

	static float panelScale() {
		return McConfig.getFloat(ID, PANEL_SCALE, 1.0F);
	}

	static float cameraDistance() {
		return McConfig.getFloat(ID, CAMERA_DISTANCE, 1.0F);
	}
}
