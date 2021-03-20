package org.rsmod.pathfinder

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
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

private const val RECT_OBJ_SHAPE = 10

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SmartPathFinderTest {

    private val pf = SmartPathFinder()

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
    fun trimMaxDistanceUpperBound() {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translateX(halfMap)
        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y)
        Assertions.assertEquals(src.x + halfMap - 1, route.last().x)
        Assertions.assertEquals(dest.y, route.last().y)
    }

    @Test
    fun trimMaxDistanceLowerBound() {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translateX(-(halfMap + 1))
        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y)
        Assertions.assertEquals(src.x + -halfMap, route.last().x)
        Assertions.assertEquals(dest.y, route.last().y)
    }

    @ParameterizedTest
    @ArgumentsSource(DirectionProvider::class)
    internal fun failBlockedDirectionPath(dir: Direction) {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(dir.offX * 1, dir.offY * 1)

        /* set flag mask to block path */
        val flagX = halfMap + dir.offX
        val flagY = halfMap + dir.offY
        flags[(flagY * pf.searchMapSize) + flagX] = CollisionFlag.OBJECT

        val route = pf.findPath(flags, src.x, src.y, dest.x, dest.y, moveNear = false)
        Assertions.assertTrue(route.isEmpty())
    }

    @ParameterizedTest
    @ArgumentsSource(ParameterFileNameProvider::class)
    fun reachDestination(resourceFile: String) {
        val params = loadParameters(resourceFile)
        val pf = SmartPathFinder(resetOnSearch = false)
        val route = pf.findPath(params.flags, params.srcX, params.srcY, params.destX, params.destY)
        Assertions.assertEquals(params.expectedX, route.last().x)
        Assertions.assertEquals(params.expectedY, route.last().y)
    }

    @ParameterizedTest
    @ArgumentsSource(DimensionParameterProvider::class)
    fun reachRectObjectSuccessfully(width: Int, height: Int) {
        val src = RouteCoordinates(3200, 3200)
        val dest = src.translate(3 + width, 0) /* ensure destination is further than width */

        val flagX = halfMap
        val flagY = halfMap + 3 + width

        /* mark tiles with object */
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = ((flagY + y) * DEFAULT_SEARCH_MAP_SIZE) + (flagX + x)
                flags[index] = CollisionFlag.OBJECT
            }
        }

        val route = pf.findPath(
            flags,
            src.x,
            src.y,
            dest.x,
            dest.y,
            objShape = RECT_OBJ_SHAPE,
            destWidth = width,
            destHeight = height
        )
        Assertions.assertTrue(route.success)
        Assertions.assertFalse(route.alternative)
    }

    private fun loadParameters(resourceFile: String): PathParameter {
        val mapper = ObjectMapper(JsonFactory())
        val input = Route::class.java.getResourceAsStream(resourceFile)
        return input.use { mapper.readValue(it, PathParameter::class.java) }
    }

    private object ParameterFileNameProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return Stream.of(
                Arguments.of("lumbridge.json"),
                Arguments.of("barb-village.json"),
                Arguments.of("gnome-maze.json") /* stops after 24 turns */
            )
        }
    }

    private object DimensionParameterProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(1, 1),
                Arguments.of(2, 2),
                Arguments.of(3, 3),
                Arguments.of(1, 2),
                Arguments.of(2, 1)
            )
        }
    }

    private object DirectionProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(North),
                Arguments.of(South),
                Arguments.of(East),
                Arguments.of(West),
                Arguments.of(NorthEast),
                Arguments.of(NorthWest),
                Arguments.of(SouthEast),
                Arguments.of(SouthWest)
            )
        }
    }

    private data class PathParameter(
        val srcX: Int,
        val srcY: Int,
        val destX: Int,
        val destY: Int,
        val expectedX: Int,
        val expectedY: Int,
        val flags: IntArray
    ) {

        constructor() : this(0, 0, 0, 0, 0, 0, intArrayOf())

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PathParameter

            if (srcX != other.srcX) return false
            if (srcY != other.srcY) return false
            if (destX != other.destX) return false
            if (destY != other.destY) return false
            if (!flags.contentEquals(other.flags)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = srcX
            result = 31 * result + srcY
            result = 31 * result + destX
            result = 31 * result + destY
            result = 31 * result + flags.contentHashCode()
            return result
        }
    }
}
