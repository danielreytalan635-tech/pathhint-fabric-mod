package com.pathhintclone.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pathhintclone.PathHintState;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws the active path, block break/place hints, breadcrumb trail and
 * target beacon into the world. Renders through walls so the player can
 * always see where they're going (like the original PathHint's beacon).
 */
public final class PathRenderer {
	private PathRenderer() {
	}

	public static void render(WorldRenderContext context) {
		List<BlockPos> path = PathHintState.currentPath;
		if (path.isEmpty() && PathHintState.breadcrumbs.isEmpty()) {
			return;
		}

		VertexConsumerProvider.Immediate consumers = (VertexConsumerProvider.Immediate) context.consumers();
		if (consumers == null) return;

		MatrixStack matrices = context.matrixStack();
		if (matrices == null) return;

		Vec3d cam = context.camera().getPos();

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);

		matrices.push();
		matrices.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f matrix = matrices.peek().getPositionMatrix();

		// 1. The glowing path line.
		if (path.size() >= 2) {
			VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLineStrip());
			for (BlockPos pos : path) {
				addLineVertex(buffer, matrix, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 60, 220, 255, 235);
			}
		}

		// 2. Breadcrumb trail: small crosses at each recorded position.
		VertexConsumer crumbBuffer = consumers.getBuffer(RenderLayer.getLines());
		for (BlockPos pos : PathHintState.breadcrumbs) {
			drawCross(crumbBuffer, matrix, pos, 255, 200, 60, 160);
		}

		// 3. Break/place hint outlines.
		VertexConsumer hintBuffer = consumers.getBuffer(RenderLayer.getLines());
		for (BlockPos pos : PathHintState.breakHints) {
			drawBoxOutline(hintBuffer, matrix, pos, 255, 80, 80, 255);
		}
		for (BlockPos pos : PathHintState.placeHints) {
			drawBoxOutline(hintBuffer, matrix, pos, 80, 255, 120, 255);
		}

		// 4. Target beacon: a vertical shaft above the destination.
		if (PathHintState.activeTarget != null) {
			VertexConsumer beacon = consumers.getBuffer(RenderLayer.getLineStrip());
			BlockPos t = PathHintState.activeTarget;
			double baseY = t.getY() + 0.1;
			for (int i = 0; i <= 40; i++) {
				addLineVertex(beacon, matrix, t.getX() + 0.5, baseY + i * 4.0, t.getZ() + 0.5, 255, 235, 60, 200);
			}
		}

		consumers.draw();
		matrices.pop();

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}

	private static void addLineVertex(VertexConsumer buffer, Matrix4f matrix, double x, double y, double z,
			int r, int g, int b, int a) {
		buffer.vertex(matrix, (float) x, (float) y, (float) z).color(r, g, b, a);
	}

	private static void drawCross(VertexConsumer buffer, Matrix4f matrix, BlockPos pos, int r, int g, int b, int a) {
		double x = pos.getX() + 0.5, y = pos.getY() + 0.15, z = pos.getZ() + 0.5;
		double s = 0.15;
		buffer.vertex(matrix, (float) (x - s), (float) y, (float) z).color(r, g, b, a);
		buffer.vertex(matrix, (float) (x + s), (float) y, (float) z).color(r, g, b, a);
		buffer.vertex(matrix, (float) x, (float) y, (float) (z - s)).color(r, g, b, a);
		buffer.vertex(matrix, (float) x, (float) y, (float) (z + s)).color(r, g, b, a);
	}

	private static void drawBoxOutline(VertexConsumer buffer, Matrix4f matrix, BlockPos pos, int r, int g, int b, int a) {
		double x0 = pos.getX(), y0 = pos.getY(), z0 = pos.getZ();
		double x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
		double[][] edges = {
				{x0, y0, z0, x1, y0, z0}, {x1, y0, z0, x1, y0, z1}, {x1, y0, z1, x0, y0, z1}, {x0, y0, z1, x0, y0, z0},
				{x0, y1, z0, x1, y1, z0}, {x1, y1, z0, x1, y1, z1}, {x1, y1, z1, x0, y1, z1}, {x0, y1, z1, x0, y1, z0},
				{x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x1, y0, z1, x1, y1, z1}, {x0, y0, z1, x0, y1, z1}
		};
		for (double[] e : edges) {
			buffer.vertex(matrix, (float) e[0], (float) e[1], (float) e[2]).color(r, g, b, a);
			buffer.vertex(matrix, (float) e[3], (float) e[4], (float) e[5]).color(r, g, b, a);
		}
	}
}
