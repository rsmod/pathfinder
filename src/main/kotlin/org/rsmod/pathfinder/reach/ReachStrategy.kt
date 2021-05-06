package org.rsmod.pathfinder.reach

public interface ReachStrategy {

    public fun reached(
        flags: IntArray,
        localSrcX: Int,
        localSrcY: Int,
        localDestX: Int,
        localDestY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        rotation: Int,
        shape: Int,
        accessBitMask: Int,
        searchMapSize: Int
    ): Boolean
}
