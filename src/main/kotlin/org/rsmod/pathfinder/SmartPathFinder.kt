package org.rsmod.pathfinder

import org.rsmod.pathfinder.collision.CollisionStrategies
import org.rsmod.pathfinder.collision.CollisionStrategy
import org.rsmod.pathfinder.flag.CollisionFlag
import org.rsmod.pathfinder.flag.DirectionFlag
import org.rsmod.pathfinder.reach.DefaultReachStrategy
import org.rsmod.pathfinder.reach.ReachStrategy
import java.util.Arrays
import kotlin.collections.ArrayList
import kotlin.math.abs

private const val DEFAULT_RESET_ON_SEARCH = true
internal const val DEFAULT_SEARCH_MAP_SIZE = 128
private const val DEFAULT_RING_BUFFER_SIZE = 4096
private const val DEFAULT_MAX_TURNS = 24

private const val DEFAULT_DISTANCE_VALUE = 99999999
private const val DEFAULT_SRC_DIRECTION_VALUE = 99

private const val MAX_ALTERNATIVE_ROUTE_LOWEST_COST = 1000
private const val MAX_ALTERNATIVE_ROUTE_SEEK_RANGE = 100
private const val MAX_ALTERNATIVE_ROUTE_DISTANCE_FROM_DESTINATION = 10

/*
 * For optimization, we use this value to separate each section
 * where the list of route coordinates made a turn in any direction.
 */
private val TURN_COORDS = RouteCoordinates(0)

public class SmartPathFinder(
    private val resetOnSearch: Boolean = DEFAULT_RESET_ON_SEARCH,
    public val searchMapSize: Int = DEFAULT_SEARCH_MAP_SIZE,
    private val ringBufferSize: Int = DEFAULT_RING_BUFFER_SIZE,
    private val directions: IntArray = IntArray(searchMapSize * searchMapSize),
    private val distances: IntArray = IntArray(searchMapSize * searchMapSize) { DEFAULT_DISTANCE_VALUE },
    private val validLocalX: IntArray = IntArray(ringBufferSize),
    private val validLocalY: IntArray = IntArray(ringBufferSize),
    private var bufReaderIndex: Int = 0,
    private var bufWriterIndex: Int = 0,
    private var currLocalX: Int = 0,
    private var currLocalY: Int = 0
) {

    public fun findPath(
        flags: IntArray,
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
        srcSize: Int = 1,
        destWidth: Int = 0,
        destHeight: Int = 0,
        objRot: Int = 0,
        objShape: Int = -1,
        moveNear: Boolean = true,
        accessBitMask: Int = 0,
        maxTurns: Int = DEFAULT_MAX_TURNS,
        collision: CollisionStrategy = CollisionStrategies.Normal,
        reachStrategy: ReachStrategy = DefaultReachStrategy
    ): Route {
        require(flags.size == directions.size) {
            "Clipping flag size must be same size as [directions] and [distances]"
        }
        if (resetOnSearch) {
            reset()
        }
        val baseX = srcX - (searchMapSize / 2)
        val baseY = srcY - (searchMapSize / 2)
        val localSrcX = srcX - baseX
        val localSrcY = srcY - baseY
        val localDestX = destX - baseX
        val localDestY = destY - baseY
        setNextValidLocalCoords(localSrcX, localSrcY, DEFAULT_SRC_DIRECTION_VALUE, 0)
        val pathFound = when (srcSize) {
            1 -> findPath1(
                flags,
                localDestX,
                localDestY,
                destWidth,
                destHeight,
                srcSize,
                objRot,
                objShape,
                accessBitMask,
                collision,
                reachStrategy
            )
            2 -> findPath2(
                flags,
                localDestX,
                localDestY,
                destWidth,
                destHeight,
                srcSize,
                objRot,
                objShape,
                accessBitMask,
                collision,
                reachStrategy
            )
            else -> findPathN(
                flags,
                localDestX,
                localDestY,
                destWidth,
                destHeight,
                srcSize,
                objRot,
                objShape,
                accessBitMask,
                collision,
                reachStrategy
            )
        }
        if (!pathFound) {
            if (!moveNear) {
                return Route(emptyList(), alternative = false, success = false)
            } else if (!findClosestApproachPoint(localSrcX, localSrcY, localDestX, localDestY)) {
                return Route(emptyList(), alternative = false, success = false)
            }
        }
        val coordinates = ArrayList<RouteCoordinates>(255)
        var nextDir = directions[currLocalX, currLocalY]
        var currDir = -1
        var turns = 0
        for (i in 0 until searchMapSize * searchMapSize) {
            if (currLocalX == localSrcX && currLocalY == localSrcY) break
            val coords = RouteCoordinates(currLocalX + baseX, currLocalY + baseY)
            if (currDir != nextDir) {
                turns++
                coordinates.add(0, TURN_COORDS)
                currDir = nextDir
            }
            coordinates.add(0, coords)
            if ((currDir and DirectionFlag.EAST) != 0) {
                currLocalX++
            } else if ((currDir and DirectionFlag.WEST) != 0) {
                currLocalX--
            }
            if ((currDir and DirectionFlag.NORTH) != 0) {
                currLocalY++
            } else if ((currDir and DirectionFlag.SOUTH) != 0) {
                currLocalY--
            }
            nextDir = directions[currLocalX, currLocalY]
        }
        return if (turns > maxTurns) {
            val filtered = ArrayList<RouteCoordinates>(coordinates.size - turns)
            var currTurns = 0
            for (coords in coordinates) {
                if (currTurns > maxTurns) break
                if (coords == TURN_COORDS) {
                    currTurns++
                    continue
                }
                filtered.add(coords)
            }
            Route(filtered, alternative = !pathFound, success = true)
        } else {
            val filtered = ArrayList<RouteCoordinates>(coordinates.size - turns)
            coordinates.forEach { coords ->
                if (coords == TURN_COORDS) return@forEach
                filtered.add(coords)
            }
            Route(filtered, alternative = !pathFound, success = true)
        }
    }

    private fun findPath1(
        flags: IntArray,
        destX: Int,
        destY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        objRot: Int,
        objShape: Int,
        accessBitMask: Int,
        collision: CollisionStrategy,
        reachStrategy: ReachStrategy
    ): Boolean {
        var x: Int
        var y: Int
        var clipFlag: Int
        var dirFlag: Int
        val relativeSearchSize = searchMapSize - 1
        while (bufWriterIndex != bufReaderIndex) {
            currLocalX = validLocalX[bufReaderIndex]
            currLocalY = validLocalY[bufReaderIndex]
            bufReaderIndex = (bufReaderIndex + 1) and (ringBufferSize - 1)

            if (reachStrategy.reached(
                    flags,
                    currLocalX,
                    currLocalY,
                    destX,
                    destY,
                    destWidth,
                    destHeight,
                    srcSize,
                    objRot,
                    objShape,
                    accessBitMask,
                    searchMapSize
                )
            ) {
                return true
            }

            val nextDistance = distances[currLocalX, currLocalY] + 1

            /* east to west */
            x = currLocalX - 1
            y = currLocalY
            clipFlag = CollisionFlag.BLOCK_WEST
            dirFlag = DirectionFlag.EAST
            if (currLocalX > 0 && directions[x, y] == 0 && collision.canMove(flags[x, y], clipFlag)) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* west to east */
            x = currLocalX + 1
            y = currLocalY
            clipFlag = CollisionFlag.BLOCK_EAST
            dirFlag = DirectionFlag.WEST
            if (currLocalX < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], clipFlag)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* north to south  */
            x = currLocalX
            y = currLocalY - 1
            clipFlag = CollisionFlag.BLOCK_SOUTH
            dirFlag = DirectionFlag.NORTH
            if (currLocalY > 0 && directions[x, y] == 0 && collision.canMove(flags[x, y], clipFlag)) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* south to north */
            x = currLocalX
            y = currLocalY + 1
            clipFlag = CollisionFlag.BLOCK_NORTH
            dirFlag = DirectionFlag.SOUTH
            if (currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], clipFlag)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* north-east to south-west */
            x = currLocalX - 1
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH_EAST
            if (currLocalX > 0 && currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x, currLocalY], CollisionFlag.BLOCK_WEST) &&
                collision.canMove(flags[currLocalX, y], CollisionFlag.BLOCK_SOUTH)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* north-west to south-east */
            x = currLocalX + 1
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH_WEST
            if (currLocalX < relativeSearchSize && currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[x, currLocalY], CollisionFlag.BLOCK_EAST) &&
                collision.canMove(flags[currLocalX, y], CollisionFlag.BLOCK_SOUTH)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* south-east to north-west */
            x = currLocalX - 1
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH_EAST
            if (currLocalX > 0 && currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[x, currLocalY], CollisionFlag.BLOCK_WEST) &&
                collision.canMove(flags[currLocalX, y], CollisionFlag.BLOCK_NORTH)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* south-west to north-east */
            x = currLocalX + 1
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH_WEST
            if (currLocalX < relativeSearchSize && currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_NORTH_EAST) &&
                collision.canMove(flags[x, currLocalY], CollisionFlag.BLOCK_EAST) &&
                collision.canMove(flags[currLocalX, y], CollisionFlag.BLOCK_NORTH)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }
        }
        return false
    }

    private fun findPath2(
        flags: IntArray,
        destX: Int,
        destY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        objRot: Int,
        objShape: Int,
        accessBitMask: Int,
        collision: CollisionStrategy,
        reachStrategy: ReachStrategy
    ): Boolean {
        var x: Int
        var y: Int
        var dirFlag: Int
        val relativeSearchSize = searchMapSize - 2
        while (bufWriterIndex != bufReaderIndex) {
            currLocalX = validLocalX[bufReaderIndex]
            currLocalY = validLocalY[bufReaderIndex]
            bufReaderIndex = (bufReaderIndex + 1) and (ringBufferSize - 1)

            if (reachStrategy.reached(
                    flags,
                    currLocalX,
                    currLocalY,
                    destX,
                    destY,
                    destWidth,
                    destHeight,
                    srcSize,
                    objRot,
                    objShape,
                    accessBitMask,
                    searchMapSize
                )
            ) {
                return true
            }

            val nextDistance = distances[currLocalX, currLocalY] + 1

            /* east to west */
            x = currLocalX - 1
            y = currLocalY
            dirFlag = DirectionFlag.EAST
            if (currLocalX > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x, currLocalY + 1], CollisionFlag.BLOCK_NORTH_WEST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* west to east */
            x = currLocalX + 1
            y = currLocalY
            dirFlag = DirectionFlag.WEST
            if (currLocalX < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[currLocalX + 2, y], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[currLocalX + 2, currLocalY + 1], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* north to south  */
            x = currLocalX
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH
            if (currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[currLocalX + 1, y], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* south to north */
            x = currLocalX
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH
            if (currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, currLocalY + 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[currLocalX + 1, currLocalY + 2], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* north-east to south-west */
            x = currLocalX - 1
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH_EAST
            if (currLocalX > 0 && currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, currLocalY], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[currLocalX, y], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* north-west to south-east */
            x = currLocalX + 1
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH_WEST
            if (currLocalX < relativeSearchSize && currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[currLocalX + 2, y], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[currLocalX + 2, currLocalY], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* south-east to north-west */
            x = currLocalX - 1
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH_EAST
            if (currLocalX > 0 && currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x, currLocalY + 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[currLocalX, currLocalY + 2], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }

            /* south-west to north-east */
            x = currLocalX + 1
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH_WEST
            if (currLocalX < relativeSearchSize && currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, currLocalY + 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[currLocalX + 2, currLocalY + 2], CollisionFlag.BLOCK_NORTH_EAST) &&
                collision.canMove(flags[currLocalX + 2, y], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                setNextValidLocalCoords(x, y, dirFlag, nextDistance)
            }
        }
        return false
    }

    private fun findPathN(
        flags: IntArray,
        destX: Int,
        destY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        objRot: Int,
        objShape: Int,
        accessBitMask: Int,
        collision: CollisionStrategy,
        reachStrategy: ReachStrategy
    ): Boolean {
        var x: Int
        var y: Int
        var dirFlag: Int
        val relativeSearchSize = searchMapSize - srcSize
        while (bufWriterIndex != bufReaderIndex) {
            currLocalX = validLocalX[bufReaderIndex]
            currLocalY = validLocalY[bufReaderIndex]
            bufReaderIndex = (bufReaderIndex + 1) and (ringBufferSize - 1)

            if (reachStrategy.reached(
                    flags,
                    currLocalX,
                    currLocalY,
                    destX,
                    destY,
                    destWidth,
                    destHeight,
                    srcSize,
                    objRot,
                    objShape,
                    accessBitMask,
                    searchMapSize
                )
            ) {
                return true
            }

            val nextDistance = distances[currLocalX, currLocalY] + 1

            /* east to west */
            x = currLocalX - 1
            y = currLocalY
            dirFlag = DirectionFlag.EAST
            if (currLocalX > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x, currLocalY + srcSize - 1], CollisionFlag.BLOCK_NORTH_WEST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_AND_SOUTH_EAST
                val blocked = (1 until srcSize - 1).any { !collision.canMove(flags[x, currLocalY + it], clipFlag) }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }

            /* west to east */
            x = currLocalX + 1
            y = currLocalY
            dirFlag = DirectionFlag.WEST
            if (currLocalX < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[currLocalX + srcSize, y], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[currLocalX + srcSize, currLocalY + srcSize - 1], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[currLocalX + srcSize, currLocalY + it], clipFlag)
                }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }

            /* north to south  */
            x = currLocalX
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH
            if (currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[currLocalX + srcSize - 1, y], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any { !collision.canMove(flags[currLocalX + it, y], clipFlag) }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }

            /* south to north */
            x = currLocalX
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH
            if (currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, currLocalY + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[currLocalX + srcSize - 1, currLocalY + srcSize], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val blocked =
                    (1 until srcSize - 1).any { !collision.canMove(flags[x + it, currLocalY + srcSize], clipFlag) }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }

            /* north-east to south-west */
            x = currLocalX - 1
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH_EAST
            if (currLocalX > 0 && currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, currLocalY + srcSize - 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[currLocalX + srcSize - 2, y], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[x, currLocalY + it - 1], clipFlag1) ||
                        !collision.canMove(flags[currLocalX + it - 1, y], clipFlag2)
                }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }

            /* north-west to south-east */
            x = currLocalX + 1
            y = currLocalY - 1
            dirFlag = DirectionFlag.NORTH_WEST
            if (currLocalX < relativeSearchSize && currLocalY > 0 && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[currLocalX + srcSize, y], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[currLocalX + srcSize, currLocalY + srcSize - 2], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[currLocalX + srcSize, currLocalY + it - 1], clipFlag1) ||
                        !collision.canMove(flags[currLocalX + it + 1, y], clipFlag2)
                }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }

            /* south-east to north-west */
            x = currLocalX - 1
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH_EAST
            if (currLocalX > 0 && currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x, currLocalY + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[currLocalX, currLocalY + srcSize], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_EAST
                val clipFlag2 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[x, currLocalY + it + 1], clipFlag1) ||
                        !collision.canMove(flags[currLocalX + it - 1, currLocalY + srcSize], clipFlag2)
                }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }

            /* south-west to north-east */
            x = currLocalX + 1
            y = currLocalY + 1
            dirFlag = DirectionFlag.SOUTH_WEST
            if (currLocalX < relativeSearchSize && currLocalY < relativeSearchSize && directions[x, y] == 0 &&
                collision.canMove(flags[x, currLocalY + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[currLocalX + srcSize, currLocalY + srcSize], CollisionFlag.BLOCK_NORTH_EAST) &&
                collision.canMove(flags[currLocalX + srcSize, y], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[currLocalX + it + 1, currLocalY + srcSize], clipFlag1) ||
                        !collision.canMove(flags[currLocalX + srcSize, currLocalY + it + 1], clipFlag2)
                }
                if (!blocked) {
                    setNextValidLocalCoords(x, y, dirFlag, nextDistance)
                }
            }
        }
        return false
    }

    private fun findClosestApproachPoint(
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int
    ): Boolean {
        var lowestCost = MAX_ALTERNATIVE_ROUTE_LOWEST_COST
        var maxAlternativePath = MAX_ALTERNATIVE_ROUTE_SEEK_RANGE
        val alternativeRouteRange = MAX_ALTERNATIVE_ROUTE_DISTANCE_FROM_DESTINATION
        val radiusX = destX - alternativeRouteRange..destX + alternativeRouteRange
        val radiusY = destY - alternativeRouteRange..destY + alternativeRouteRange
        for (x in radiusX) {
            for (y in radiusY) {
                if (x !in 0 until searchMapSize ||
                    y !in 0 until searchMapSize ||
                    distances[x, y] >= MAX_ALTERNATIVE_ROUTE_SEEK_RANGE
                ) {
                    continue
                }
                val dx = abs(destX - x)
                val dy = abs(destY - y)
                val cost = dx * dx + dy * dy
                if (cost < lowestCost || (cost == lowestCost && maxAlternativePath > distances[x, y])) {
                    currLocalX = x
                    currLocalY = y
                    lowestCost = cost
                    maxAlternativePath = distances[x, y]
                }
            }
        }
        return !(lowestCost == MAX_ALTERNATIVE_ROUTE_LOWEST_COST || (srcX == currLocalX && srcY == currLocalY))
    }

    private fun reset() {
        Arrays.setAll(directions) { 0 }
        Arrays.setAll(distances) { DEFAULT_DISTANCE_VALUE }
        bufReaderIndex = 0
        bufWriterIndex = 0
    }

    private fun setNextValidLocalCoords(x: Int, y: Int, direction: Int, distance: Int) {
        val pathIndex = (y * searchMapSize) + x
        directions[pathIndex] = direction
        distances[pathIndex] = distance
        validLocalX[bufWriterIndex] = x
        validLocalY[bufWriterIndex] = y
        bufWriterIndex = (bufWriterIndex + 1) and (ringBufferSize - 1)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun IntArray.get(x: Int, y: Int): Int {
        val index = (y * searchMapSize) + x
        return this[index]
    }
}
