package org.rsmod.pathfinder.collision

public interface CollisionFlagMap {

    public operator fun get(x: Int, y: Int): Int
}

public class CollisionLocalFlagMap(
    private val centerX: Int,
    private val centerY: Int,
    private val width: Int,
    private val flags: IntArray = IntArray(width * width)
) : CollisionFlagMap {

    private val baseX: Int
        get() = centerX - (width / 2)

    private val baseY: Int
        get() = centerY - (width / 2)

    override fun get(x: Int, y: Int): Int {
        val localX = x - baseX
        val localY = y - baseY
        val index = localX + (localY * width)
        return flags[index]
    }
}
