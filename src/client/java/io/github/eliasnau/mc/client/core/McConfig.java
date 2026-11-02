package io.github.eliasnau.mc.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.eliasnau.mc.Mc;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client settings in {@code config/mc.json}, one object per feature id. Every feature has an {@code enabled} flag;
 * anything else is up to the feature. Setters only change memory, call {@link #save()} to persist.
 */
public final class McConfig {
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(Mc.MOD_ID + ".json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final JsonObject ROOT = load();

	private McConfig() {
	}

	public static boolean isEnabled(final String featureId) {
		return getBoolean(featureId, "enabled", true);
	}

	public static void setEnabled(final String featureId, final boolean enabled) {
		setBoolean(featureId, "enabled", enabled);
	}

	public static boolean getBoolean(final String featureId, final String key, final boolean fallback) {
		JsonElement value = section(featureId).get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
	}

	public static void setBoolean(final String featureId, final String key, final boolean value) {
		section(featureId).addProperty(key, value);
	}

	public static float getFloat(final String featureId, final String key, final float fallback) {
		JsonElement value = section(featureId).get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsFloat() : fallback;
	}

	public static void setFloat(final String featureId, final String key, final float value) {
		section(featureId).addProperty(key, value);
	}

	public static Set<Integer> getIntSet(final String featureId, final String key) {
		Set<Integer> values = new TreeSet<>();
		if (section(featureId).get(key) instanceof JsonArray array) {
			for (JsonElement element : array) {
				if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
					values.add(element.getAsInt());
				}
			}
		}

		return values;
	}

	public static void setIntSet(final String featureId, final String key, final Set<Integer> values) {
		JsonArray array = new JsonArray();
		new TreeSet<>(values).forEach(array::add);
		section(featureId).add(key, array);
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(ROOT));
		} catch (IOException e) {
			Mc.LOGGER.error("Failed to save {}", PATH, e);
		}
	}

	private static JsonObject section(final String featureId) {
		if (!(ROOT.get(featureId) instanceof JsonObject section)) {
			JsonObject created = new JsonObject();
			ROOT.add(featureId, created);
			return created;
		}

		return section;
	}

	private static JsonObject load() {
		if (Files.exists(PATH)) {
			try {
				if (JsonParser.parseString(Files.readString(PATH)) instanceof JsonObject root) {
					return root;
				}
			} catch (IOException | RuntimeException e) {
				Mc.LOGGER.error("Failed to read {}, using defaults", PATH, e);
			}
		}

		return new JsonObject();
	}
}
