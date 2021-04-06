package org.rsmod.pathfinder.reach

public interface ReachStrategy {

    public fun reached(
        flags: IntArray,
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        rotation: Int,
        shape: Int,
        accessBitMask: Int,
        searchMapSize: Int
    ): Boolean
}
