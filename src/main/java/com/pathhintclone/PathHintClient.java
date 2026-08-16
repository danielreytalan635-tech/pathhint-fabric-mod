package com.pathhintclone;

import com.pathhintclone.gui.PathHintScreen;
import com.pathhintclone.render.HudOverlay;
import com.pathhintclone.render.PathRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathHintClient implements ClientModInitializer {
	public static final String MOD_ID = "pathhintclone";
	public static final Logger LOGGER = LoggerFactory.getLogger("PathHint");

	private static KeyBinding openMenuKey;

	/** Ticks between automatic path recalculations while a target is active. */
	private static final int RECALC_INTERVAL = 20; // once a second
	private int tickCounter = 0;
	private float lastHealth = -1;

	@Override
	public void onInitializeClient() {
		WaypointManager.load();

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.pathhintclone.open_menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				"category.pathhintclone.main"
		));

		WorldRenderEvents.AFTER_TRANSLUCENT.register(PathRenderer::render);
		HudRenderCallback.EVENT.register(HudOverlay::render);

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

		LOGGER.info("[PathHint] Client initialized. Press H in-game to open the menu.");
	}

	private void onTick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			return;
		}

		while (openMenuKey.wasPressed()) {
			if (client.currentScreen == null) {
				client.setScreen(new PathHintScreen());
			}
		}

		// Death detection (client-side): watch for a health drop to zero.
		float health = client.player.getHealth();
		if (lastHealth > 0 && health <= 0) {
			BlockPos deathPos = client.player.getBlockPos();
			WaypointManager.addDeathMarker(new Waypoint("Death", deathPos));
			client.player.sendMessage(Text.literal("§c[PathHint] Death location marked."), false);
		}
		lastHealth = health;

		// Breadcrumb trail.
		PathHintState.recordBreadcrumb(client.player.getBlockPos());

		// Periodically recompute the active path so it follows the player and
		// reacts to the world changing (blocks broken/placed, etc).
		if (PathHintState.activeTarget != null) {
			tickCounter++;
			if (tickCounter >= RECALC_INTERVAL) {
				tickCounter = 0;
				recalculate(client);
			}
		}
	}

	public static void recalculate(MinecraftClient client) {
		if (client.player == null || client.world == null || PathHintState.activeTarget == null) {
			return;
		}
		BlockPos start = client.player.getBlockPos();
		BlockPos goal = PathHintState.activeTarget;
		PathfindingEngine.Result result = PathfindingEngine.findPath(client.world, start, goal);
		if (result.path().isEmpty()) {
			client.player.sendMessage(Text.literal("§c[PathHint] No path found to target."), false);
			return;
		}
		PathHintState.setPath(result, goal, PathHintState.activeTargetLabel);
	}
}
