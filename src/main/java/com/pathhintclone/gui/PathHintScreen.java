package com.pathhintclone.gui;

import com.pathhintclone.PathHintState;
import com.pathhintclone.PathfindingEngine;
import com.pathhintclone.Waypoint;
import com.pathhintclone.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class PathHintScreen extends Screen {
	private TextFieldWidget xField;
	private TextFieldWidget yField;
	private TextFieldWidget zField;
	private TextFieldWidget nameField;

	private static final int PANEL_WIDTH = 260;

	public PathHintScreen() {
		super(Text.literal("PathHint"));
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int top = this.height / 2 - 110;
		int fieldWidth = 60;

		xField = new TextFieldWidget(this.textRenderer, centerX - PANEL_WIDTH / 2, top, fieldWidth, 20, Text.literal("X"));
		yField = new TextFieldWidget(this.textRenderer, centerX - PANEL_WIDTH / 2 + fieldWidth + 5, top, fieldWidth, 20, Text.literal("Y"));
		zField = new TextFieldWidget(this.textRenderer, centerX - PANEL_WIDTH / 2 + (fieldWidth + 5) * 2, top, fieldWidth, 20, Text.literal("Z"));
		xField.setPlaceholder(Text.literal("X"));
		yField.setPlaceholder(Text.literal("Y"));
		zField.setPlaceholder(Text.literal("Z"));

		if (this.client != null && this.client.player != null) {
			BlockPos p = this.client.player.getBlockPos();
			xField.setText(String.valueOf(p.getX()));
			yField.setText(String.valueOf(p.getY()));
			zField.setText(String.valueOf(p.getZ()));
		}

		addDrawableChild(xField);
		addDrawableChild(yField);
		addDrawableChild(zField);

		addDrawableChild(ButtonWidget.builder(Text.literal("Go to Coordinates"), b -> goToCoordinates())
				.dimensions(centerX - PANEL_WIDTH / 2, top + 25, PANEL_WIDTH, 20).build());

		nameField = new TextFieldWidget(this.textRenderer, centerX - PANEL_WIDTH / 2, top + 55, PANEL_WIDTH - 90, 20, Text.literal("Name"));
		nameField.setPlaceholder(Text.literal("Waypoint name"));
		addDrawableChild(nameField);

		addDrawableChild(ButtonWidget.builder(Text.literal("Save Here"), b -> saveWaypointHere())
				.dimensions(centerX - PANEL_WIDTH / 2 + PANEL_WIDTH - 85, top + 55, 85, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Cave Escape"), b -> caveEscape())
				.dimensions(centerX - PANEL_WIDTH / 2, top + 80, 125, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Clear Path"), b -> clearPath())
				.dimensions(centerX - PANEL_WIDTH / 2 + 135, top + 80, 125, 20).build());

		int listTop = top + 110;
		List<Waypoint> waypoints = WaypointManager.get().waypoints;
		int row = 0;
		for (Waypoint wp : waypoints) {
			int y = listTop + row * 22;
			if (y > this.height - 40) break; // don't overflow the screen
			addDrawableChild(ButtonWidget.builder(Text.literal(wp.name), b -> goToWaypoint(wp))
					.dimensions(centerX - PANEL_WIDTH / 2, y, PANEL_WIDTH - 60, 20).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> deleteWaypoint(wp))
					.dimensions(centerX - PANEL_WIDTH / 2 + PANEL_WIDTH - 55, y, 55, 20).build());
			row++;
		}

		List<Waypoint> deaths = WaypointManager.get().deathMarkers;
		if (!deaths.isEmpty()) {
			int y = listTop + row * 22 + 10;
			if (y <= this.height - 30) {
				Waypoint lastDeath = deaths.get(0);
				addDrawableChild(ButtonWidget.builder(Text.literal("Go to Last Death"), b -> goToWaypoint(lastDeath))
						.dimensions(centerX - PANEL_WIDTH / 2, y, PANEL_WIDTH, 20).build());
			}
		}
	}

	private void goToCoordinates() {
		try {
			int x = Integer.parseInt(xField.getText().trim());
			int y = Integer.parseInt(yField.getText().trim());
			int z = Integer.parseInt(zField.getText().trim());
			startPathTo(new BlockPos(x, y, z), "Waypoint");
		} catch (NumberFormatException e) {
			if (this.client != null && this.client.player != null) {
				this.client.player.sendMessage(Text.literal("§c[PathHint] Enter valid whole-number coordinates."), false);
			}
		}
	}

	private void saveWaypointHere() {
		if (this.client == null || this.client.player == null) return;
		String name = nameField.getText().trim();
		if (name.isEmpty()) name = "Waypoint " + (WaypointManager.get().waypoints.size() + 1);
		WaypointManager.addWaypoint(new Waypoint(name, this.client.player.getBlockPos()));
		this.client.setScreen(new PathHintScreen()); // refresh list
	}

	private void deleteWaypoint(Waypoint wp) {
		WaypointManager.removeWaypoint(wp);
		if (this.client != null) this.client.setScreen(new PathHintScreen());
	}

	private void goToWaypoint(Waypoint wp) {
		startPathTo(wp.toBlockPos(), wp.name);
	}

	private void caveEscape() {
		if (this.client == null || this.client.player == null || this.client.world == null) return;
		BlockPos playerPos = this.client.player.getBlockPos();
		BlockPos surface = playerPos;
		for (int y = playerPos.getY(); y <= this.client.world.getTopYInclusive(); y++) {
			BlockPos check = new BlockPos(playerPos.getX(), y, playerPos.getZ());
			if (this.client.world.isSkyVisible(check)) {
				surface = check;
				break;
			}
		}
		startPathTo(surface, "Surface");
	}

	private void clearPath() {
		PathHintState.clearPath();
		if (this.client != null) this.client.setScreen(null);
	}

	private void startPathTo(BlockPos target, String label) {
		if (this.client == null || this.client.player == null || this.client.world == null) return;
		PathfindingEngine.Result result = PathfindingEngine.findPath(this.client.world, this.client.player.getBlockPos(), target);
		if (result.path().isEmpty()) {
			this.client.player.sendMessage(Text.literal("§c[PathHint] No path found to " + label + "."), false);
			return;
		}
		if (!result.success()) {
			this.client.player.sendMessage(Text.literal("§e[PathHint] Only a partial route was found — the rest may need manual navigation."), false);
		}
		PathHintState.setPath(result, target, label);
		this.client.setScreen(null);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 130, 0xFFFFFF);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
