package org.rsmod.pathfinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.rsmod.pathfinder.flag.CollisionFlag
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class DumbPathFinderTest {

    private val pf = DumbPathFinder()

    private val flags = IntArray(pf.searchMapSize * pf.searchMapSize)

    private val halfMap: Int
        get() = pf.searchMapSize / 2

    @Test
    fun reachEmptyTile() {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(1, 0)
        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y)
        Assertions.assertEquals(1, route.size)
        Assertions.assertEquals(dest.x, route.last().x)
        Assertions.assertEquals(dest.y, route.last().y)
    }

    @Test
    fun failOccupiedTile() {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(1, 0)

        /* set flag mask to block path */
        val flagX = halfMap + 1
        val flagY = halfMap
        flags[(flagY * DEFAULT_SEARCH_MAP_SIZE) + flagX] = CollisionFlag.FLOOR

        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y)
        Assertions.assertTrue(route.failed)
        Assertions.assertTrue(route.isEmpty())
    }

    @Test
    fun fullyBlockedByObject() {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(2, 0)

        /* set flag mask to block path */
        val flagX = halfMap + 1
        val flagY = halfMap
        flags[(flagY * DEFAULT_SEARCH_MAP_SIZE) + flagX] = CollisionFlag.OBJECT

        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y)
        Assertions.assertTrue(route.failed)
        Assertions.assertTrue(route.isEmpty())
    }

    @Test
    fun partiallyBlockedByObject() {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(3, 0)

        /* set flag mask to block path */
        val flagX = halfMap + 2
        val flagY = halfMap
        flags[(flagY * DEFAULT_SEARCH_MAP_SIZE) + flagX] = CollisionFlag.OBJECT

        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y)
        Assertions.assertTrue(route.failed)
        Assertions.assertTrue(route.isNotEmpty())
        for (i in route.indices) {
            Assertions.assertEquals((src.x + 1) + i, route[i].x)
        }
        Assertions.assertNotEquals(dest.x, route.last().x)
    }

    @ParameterizedTest
    @ArgumentsSource(DimensionParameterProvider::class)
    fun reachRectObjectSuccessfully(width: Int, height: Int) {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(3 + width, 0) /* ensure destination is further than width */

        val flagX = halfMap + 3 + width
        val flagY = halfMap

        /* mark tiles with object */
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = ((flagY + y) * DEFAULT_SEARCH_MAP_SIZE) + (flagX + x)
                flags[index] = CollisionFlag.OBJECT
            }
        }

        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y, destWidth = width, destHeight = height)
        Assertions.assertTrue(route.success)
        Assertions.assertFalse(route.alternative)
    }

    private object DimensionParameterProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(1, 1),
                Arguments.of(2, 2),
                Arguments.of(3, 3),
                Arguments.of(1, 2),
                Arguments.of(2, 1),
            )
        }
    }
}
