package com.pathhintclone;

import net.minecraft.util.math.BlockPos;

/**
 * A single saved location the player can navigate to.
 */
public class Waypoint {
	public String name;
	public int x;
	public int y;
	public int z;

	public Waypoint() {
		// needed for Gson deserialization
	}

	public Waypoint(String name, BlockPos pos) {
		this.name = name;
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
	}

	public BlockPos toBlockPos() {
		return new BlockPos(x, y, z);
	}

	@Override
	public String toString() {
		return name + " (" + x + ", " + y + ", " + z + ")";
	}
}
