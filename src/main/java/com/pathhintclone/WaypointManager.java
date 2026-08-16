package com.pathhintclone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and saves waypoints + death markers to
 * .minecraft/config/pathhintclone/waypoints.json
 */
public class WaypointManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("pathhintclone");
	private static final Path FILE = CONFIG_DIR.resolve("waypoints.json");

	public static class SaveData {
		public List<Waypoint> waypoints = new ArrayList<>();
		public List<Waypoint> deathMarkers = new ArrayList<>();
	}

	private static SaveData data;

	public static SaveData get() {
		if (data == null) {
			load();
		}
		return data;
	}

	public static void load() {
		data = new SaveData();
		if (!Files.exists(FILE)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
			Type type = new TypeToken<SaveData>() {}.getType();
			SaveData loaded = GSON.fromJson(reader, type);
			if (loaded != null) {
				data = loaded;
				if (data.waypoints == null) data.waypoints = new ArrayList<>();
				if (data.deathMarkers == null) data.deathMarkers = new ArrayList<>();
			}
		} catch (IOException e) {
			PathHintClient.LOGGER.warn("[PathHint] Failed to load waypoints.json", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_DIR);
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(get(), writer);
			}
		} catch (IOException e) {
			PathHintClient.LOGGER.warn("[PathHint] Failed to save waypoints.json", e);
		}
	}

	public static void addWaypoint(Waypoint waypoint) {
		get().waypoints.add(waypoint);
		save();
	}

	public static void removeWaypoint(Waypoint waypoint) {
		get().waypoints.remove(waypoint);
		save();
	}

	public static void addDeathMarker(Waypoint marker) {
		// Keep the most recent 10 death markers only.
		get().deathMarkers.add(0, marker);
		while (get().deathMarkers.size() > 10) {
			get().deathMarkers.remove(get().deathMarkers.size() - 1);
		}
		save();
	}
}
