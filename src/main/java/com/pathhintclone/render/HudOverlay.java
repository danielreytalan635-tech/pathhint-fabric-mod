package com.pathhintclone.render;

import com.pathhintclone.PathHintState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class HudOverlay {
	private HudOverlay() {
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}

		if (PathHintState.activeTarget == null) {
			return;
		}

		BlockPos playerPos = client.player.getBlockPos();
		BlockPos target = PathHintState.activeTarget;

		double dx = target.getX() - playerPos.getX();
		double dy = target.getY() - playerPos.getY();
		double dz = target.getZ() - playerPos.getZ();
		double flatDistance = Math.sqrt(dx * dx + dz * dz);

		String label = PathHintState.activeTargetLabel.isEmpty() ? "Target" : PathHintState.activeTargetLabel;
		Text line1 = Text.literal(String.format("%s: %.0fm", label, flatDistance));
		Text line2 = Text.literal(String.format("%s%.0f blocks", dy >= 0 ? "+" : "", dy));

		int x = 8;
		int y = 8;
		context.drawTextWithShadow(client.textRenderer, line1, x, y, 0x55DDFF);
		context.drawTextWithShadow(client.textRenderer, line2, x, y + 10, 0xCCCCCC);
	}
}
