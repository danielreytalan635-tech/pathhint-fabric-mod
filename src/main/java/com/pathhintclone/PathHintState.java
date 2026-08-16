package com.pathhintclone;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Holds the live, in-memory state of the mod: the active path, the
 * breadcrumb trail, and the current navigation target. This is
 * intentionally a static holder so the render/HUD/GUI classes can all
 * read it without passing objects around.
 */
public class PathHintState {
	/** The full route from the player to the target, in walking order. */
	public static List<BlockPos> currentPath = new ArrayList<>();

	/** Positions along the path where a block must be broken to proceed. */
	public static List<BlockPos> breakHints = new ArrayList<>();

	/** Positions along the path where a block must be placed to proceed. */
	public static List<BlockPos> placeHints = new ArrayList<>();

	/** Where the player is currently trying to go, or null if no active path. */
	public static BlockPos activeTarget = null;
	public static String activeTargetLabel = "";

	/** Rolling trail of recently visited positions, oldest first. */
	public static final Deque<BlockPos> breadcrumbs = new ArrayDeque<>();
	private static final int MAX_BREADCRUMBS = 200;

	public static void clearPath() {
		currentPath = new ArrayList<>();
		breakHints = new ArrayList<>();
		placeHints = new ArrayList<>();
		activeTarget = null;
		activeTargetLabel = "";
	}

	public static void setPath(PathfindingEngine.Result result, BlockPos target, String label) {
		currentPath = result.path();
		breakHints = result.breakHints();
		placeHints = result.placeHints();
		activeTarget = target;
		activeTargetLabel = label;
	}

	public static void recordBreadcrumb(BlockPos pos) {
		if (!breadcrumbs.isEmpty() && breadcrumbs.peekLast().getSquaredDistance(pos) < 9) {
			return; // don't spam crumbs when standing still / moving less than 3 blocks
		}
		breadcrumbs.addLast(pos.toImmutable());
		while (breadcrumbs.size() > MAX_BREADCRUMBS) {
			breadcrumbs.removeFirst();
		}
	}

	public static void clearBreadcrumbs() {
		breadcrumbs.clear();
	}
}
