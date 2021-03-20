package org.rsmod.pathfinder.collision

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.rsmod.pathfinder.DEFAULT_SEARCH_MAP_SIZE
import org.rsmod.pathfinder.DumbPathFinder
import org.rsmod.pathfinder.RouteCoordinates
import org.rsmod.pathfinder.SmartPathFinder
import org.rsmod.pathfinder.flag.CollisionFlag

private const val BLOCK_FLAG = CollisionFlag.FLOOR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollisionStrategyTest {

    @Test
    fun smartInverseFlagPass() {
        val pf = SmartPathFinder()
        val (src, dest, flags) = validPath()
        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y, collision = InverseBlockFlagCollision(BLOCK_FLAG))
        Assertions.assertTrue(route.success)
        Assertions.assertTrue(route.isNotEmpty())
        Assertions.assertEquals(dest.x, route.last().x)
        Assertions.assertEquals(dest.y, route.last().y)
    }

    @Test
    fun smartInverseFlagBlock() {
        val pf = SmartPathFinder()
        val (src, dest, flags) = invalidPath()
        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y, collision = InverseBlockFlagCollision(BLOCK_FLAG))
        Assertions.assertTrue(route.failed)
        Assertions.assertTrue(route.isEmpty())
    }

    @Test
    fun dumbInverseFlagPass() {
        val pf = DumbPathFinder()
        val (src, dest, flags) = validPath()
        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y, collision = InverseBlockFlagCollision(BLOCK_FLAG))
        Assertions.assertTrue(route.success)
        Assertions.assertTrue(route.isNotEmpty())
        Assertions.assertEquals(dest.x, route.last().x)
        Assertions.assertEquals(dest.y, route.last().y)
    }

    @Test
    fun dumbInverseFlagBlock() {
        val pf = DumbPathFinder()
        val (src, dest, flags) = invalidPath()
        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y, collision = InverseBlockFlagCollision(BLOCK_FLAG))
        Assertions.assertTrue(route.failed)
        Assertions.assertTrue(route.isEmpty())
    }

    private fun validPath(): CollisionParameters {
        val flags = IntArray(DEFAULT_SEARCH_MAP_SIZE * DEFAULT_SEARCH_MAP_SIZE)
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(1, 0)
        val srcFlagX = DEFAULT_SEARCH_MAP_SIZE / 2
        val srcFlagY = DEFAULT_SEARCH_MAP_SIZE / 2
        val destFlagX = (dest.x - src.x) + (DEFAULT_SEARCH_MAP_SIZE / 2)
        val destFlagY = (dest.y - src.y) + (DEFAULT_SEARCH_MAP_SIZE / 2)
        flags[(srcFlagY * DEFAULT_SEARCH_MAP_SIZE) + srcFlagX] = BLOCK_FLAG
        flags[(destFlagY * DEFAULT_SEARCH_MAP_SIZE) + destFlagX] = BLOCK_FLAG
        return CollisionParameters(src, dest, flags)
    }

    private fun invalidPath(): CollisionParameters {
        val flags = IntArray(DEFAULT_SEARCH_MAP_SIZE * DEFAULT_SEARCH_MAP_SIZE)
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(1, 0)
        val flag = CollisionFlag.FLOOR
        val srcFlagX = DEFAULT_SEARCH_MAP_SIZE / 2
        val srcFlagY = DEFAULT_SEARCH_MAP_SIZE / 2
        flags[(srcFlagY * DEFAULT_SEARCH_MAP_SIZE) + srcFlagX] = flag
        return CollisionParameters(src, dest, flags)
    }
}

private data class CollisionParameters(
    val src: RouteCoordinates,
    val dest: RouteCoordinates,
    val clipFlags: IntArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CollisionParameters

        if (src != other.src) return false
        if (dest != other.dest) return false
        if (!clipFlags.contentEquals(other.clipFlags)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = src.hashCode()
        result = 31 * result + dest.hashCode()
        result = 31 * result + clipFlags.contentHashCode()
        return result
    }
}
