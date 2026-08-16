package com.pathhintclone;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

/**
 * A basic A* pathfinder over the block grid. It is not a perfect
 * recreation of a AAA-quality nav mesh, but it handles the common
 * cases: walking, stepping up/down, falling, swimming, climbing
 * ladders/vines, and (as a last resort) routes that require breaking
 * or placing a block, which get reported back as hints instead of
 * being performed automatically.
 */
public class PathfindingEngine {

	private static final int MAX_NODES = 15000;
	private static final int MAX_RANGE = 200; // don't search absurdly far

	public record Result(boolean success, List<BlockPos> path, List<BlockPos> breakHints, List<BlockPos> placeHints) {
		public static Result failure() {
			return new Result(false, List.of(), List.of(), List.of());
		}
	}

	private record Move(BlockPos to, double cost, BlockPos breakHint, BlockPos placeHint) {
	}

	private static final class Node {
		final BlockPos pos;
		double g = Double.MAX_VALUE;
		double f = Double.MAX_VALUE;
		Node parent;
		BlockPos breakHint;
		BlockPos placeHint;

		Node(BlockPos pos) {
			this.pos = pos;
		}
	}

	public static Result findPath(World world, BlockPos start, BlockPos goal) {
		if (start.getSquaredDistance(goal) > (double) MAX_RANGE * MAX_RANGE) {
			return Result.failure();
		}

		Map<BlockPos, Node> nodes = new HashMap<>();
		PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
		Set<BlockPos> closed = new HashSet<>();

		Node startNode = new Node(start);
		startNode.g = 0;
		startNode.f = heuristic(start, goal);
		nodes.put(start, startNode);
		open.add(startNode);

		int explored = 0;
		Node best = startNode; // fallback: closest node found, in case of no exact path

		while (!open.isEmpty() && explored < MAX_NODES) {
			Node current = open.poll();
			if (closed.contains(current.pos)) continue;
			closed.add(current.pos);
			explored++;

			if (heuristic(current.pos, goal) < heuristic(best.pos, goal)) {
				best = current;
			}

			if (current.pos.equals(goal) || (current.pos.getX() == goal.getX() && current.pos.getZ() == goal.getZ()
					&& Math.abs(current.pos.getY() - goal.getY()) <= 1)) {
				return buildResult(current, true);
			}

			for (Move move : neighbors(world, current.pos)) {
				if (closed.contains(move.to())) continue;
				double tentativeG = current.g + move.cost();
				Node neighbor = nodes.computeIfAbsent(move.to(), Node::new);
				if (tentativeG < neighbor.g) {
					neighbor.g = tentativeG;
					neighbor.f = tentativeG + heuristic(move.to(), goal);
					neighbor.parent = current;
					neighbor.breakHint = move.breakHint();
					neighbor.placeHint = move.placeHint();
					open.add(neighbor);
				}
			}
		}

		// No full path found within the budget; return the partial best-effort route
		// so the player still gets *some* useful guidance instead of nothing.
		if (best != startNode) {
			return buildResult(best, false);
		}
		return Result.failure();
	}

	private static Result buildResult(Node end, boolean success) {
		List<BlockPos> path = new ArrayList<>();
		List<BlockPos> breakHints = new ArrayList<>();
		List<BlockPos> placeHints = new ArrayList<>();
		Node n = end;
		while (n != null) {
			path.add(n.pos);
			if (n.breakHint != null) breakHints.add(n.breakHint);
			if (n.placeHint != null) placeHints.add(n.placeHint);
			n = n.parent;
		}
		Collections.reverse(path);
		Collections.reverse(breakHints);
		Collections.reverse(placeHints);
		return new Result(success, path, breakHints, placeHints);
	}

	private static double heuristic(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dy = a.getY() - b.getY();
		double dz = a.getZ() - b.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static final Direction[] CARDINAL = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

	private static List<Move> neighbors(World world, BlockPos from) {
		List<Move> moves = new ArrayList<>();

		for (Direction dir : CARDINAL) {
			BlockPos side = from.offset(dir);

			// 1. Flat walk
			if (isStandable(world, side)) {
				double cost = isWater(world, side) ? 1.6 : 1.0;
				moves.add(new Move(side, cost, null, null));
			}
			// 2. Step / jump up one block
			else if (isStandable(world, side.up()) && isPassable(world, from.up().up())) {
				moves.add(new Move(side.up(), 1.5, null, null));
			}
			// 3. Step down / fall (up to 3 blocks)
			else {
				BlockPos fallPos = findFallLanding(world, side);
				if (fallPos != null) {
					int dropDistance = from.getY() - fallPos.getY();
					moves.add(new Move(fallPos, 1.0 + dropDistance * 0.3, null, null));
				} else if (isBreakable(world, side) && isStandable(world, side.down())) {
					// Blocked horizontally by a breakable block at foot level with solid footing beyond it.
					moves.add(new Move(side, 3.0, side, null));
				} else if (!isSupported(world, side) && !isHazard(world, side.down())) {
					// Gap with nothing to land on close by: suggest bridging with a placed block.
					BlockPos landing = side.down();
					if (isPassable(world, side) && isPassable(world, side.up())) {
						moves.add(new Move(side, 2.5, null, landing));
					}
				}
			}
		}

		// Ladders / vines: climb straight up or down.
		if (isClimbable(world, from) || isClimbable(world, from.up())) {
			if (isPassable(world, from.up()) || isClimbable(world, from.up())) {
				moves.add(new Move(from.up(), 1.1, null, null));
			}
		}
		if (isClimbable(world, from.down())) {
			moves.add(new Move(from.down(), 1.1, null, null));
		}

		return moves;
	}

	/** A position the player's feet + head can occupy, and where has proper footing (solid, water, or a ladder). */
	private static boolean isStandable(World world, BlockPos pos) {
		return isPassable(world, pos) && isPassable(world, pos.up()) && isSupported(world, pos);
	}

	private static boolean isSupported(World world, BlockPos pos) {
		return isSolid(world, pos.down()) || isClimbable(world, pos) || isWater(world, pos);
	}

	private static boolean isPassable(World world, BlockPos pos) {
		if (isHazard(world, pos)) return false;
		BlockState state = world.getBlockState(pos);
		return state.getCollisionShape(world, pos).isEmpty() || isWater(world, pos) || isClimbable(world, pos);
	}

	private static boolean isSolid(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return !state.getCollisionShape(world, pos).isEmpty();
	}

	private static boolean isWater(World world, BlockPos pos) {
		return world.getFluidState(pos).isIn(FluidTags.WATER);
	}

	private static boolean isHazard(World world, BlockPos pos) {
		return world.getFluidState(pos).isIn(FluidTags.LAVA);
	}

	private static boolean isClimbable(World world, BlockPos pos) {
		return world.getBlockState(pos).isIn(BlockTags.CLIMBABLE);
	}

	private static boolean isBreakable(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) return false;
		if (isHazard(world, pos)) return false;
		float hardness = state.getHardness(world, pos);
		return hardness >= 0; // negative hardness (bedrock, barriers, etc.) = unbreakable
	}

	/** Looks straight down up to 3 blocks for a place the player could land standing up. */
	private static BlockPos findFallLanding(World world, BlockPos side) {
		for (int drop = 0; drop <= 3; drop++) {
			BlockPos candidate = side.down(drop);
			if (isStandable(world, candidate)) {
				return candidate;
			}
			if (isHazard(world, candidate)) {
				return null; // don't path into lava
			}
		}
		return null;
	}
}
