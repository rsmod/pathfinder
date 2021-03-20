package org.rsmod.pathfinder

import org.rsmod.pathfinder.bound.reachRectangle
import org.rsmod.pathfinder.bound.reachWall
import org.rsmod.pathfinder.bound.reachWallDeco
import org.rsmod.pathfinder.collision.CollisionFlagMap
import org.rsmod.pathfinder.collision.CollisionLocalFlagMap
import org.rsmod.pathfinder.collision.CollisionStrategies
import org.rsmod.pathfinder.collision.CollisionStrategy
import org.rsmod.pathfinder.flag.CollisionFlag
import org.rsmod.pathfinder.flag.DirectionFlag
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

private const val WALL_STRATEGY = 0
private const val WALL_DECO_STRATEGY = 1
private const val RECTANGLE_STRATEGY = 2
private const val NO_STRATEGY = 3

/*
 * For optimization, we use this value to separate each section
 * where the list of route coordinates made a turn in any direction.
 */
private val TURN_COORDS = RouteCoordinates(0)

@Suppress("MemberVisibilityCanBePrivate")
public class SmartPathFinder(
    private val resetOnSearch: Boolean = DEFAULT_RESET_ON_SEARCH,
    public val searchMapSize: Int = DEFAULT_SEARCH_MAP_SIZE,
    private val ringBufferSize: Int = DEFAULT_RING_BUFFER_SIZE,
    private val directions: IntArray = IntArray(searchMapSize * searchMapSize),
    private val distances: IntArray = IntArray(searchMapSize * searchMapSize),
    private val ringBufferX: IntArray = IntArray(ringBufferSize),
    private val ringBufferY: IntArray = IntArray(ringBufferSize),
    private var bufReaderIndex: Int = 0,
    private var bufWriterIndex: Int = 0,
    private var currentX: Int = 0,
    private var currentY: Int = 0
) {

    @Deprecated("Use findPath(CollisionFlagMap...) instead")
    public fun findPath(
        clipFlags: IntArray,
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
        collision: CollisionStrategy = CollisionStrategies.Normal
    ): Route {
        val localFlags = CollisionLocalFlagMap(srcX, srcY, searchMapSize, clipFlags)
        return findPath(
            localFlags,
            srcX,
            srcY,
            destX,
            destY,
            srcSize,
            destWidth,
            destHeight,
            objRot,
            objShape,
            moveNear,
            accessBitMask,
            maxTurns,
            collision
        )
    }

    public fun findPath(
        flags: CollisionFlagMap,
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
        collision: CollisionStrategy = CollisionStrategies.Normal
    ): Route {
        if (resetOnSearch) {
            reset()
        }
        val baseX = srcX - (searchMapSize / 2)
        val baseY = srcY - (searchMapSize / 2)
        val localSrcX = srcX - baseX
        val localSrcY = srcY - baseY
        val localDestX = destX - baseX
        val localDestY = destY - baseY
        directions[localSrcX, localSrcY] = DEFAULT_SRC_DIRECTION_VALUE
        distances[localSrcX, localSrcY] = 0
        setAndIncrementWriterBuf(localSrcX, localSrcY)
        val pathFound = when (srcSize) {
            1 -> findPath1(
                flags,
                baseX,
                baseY,
                localDestX,
                localDestY,
                destWidth,
                destHeight,
                srcSize,
                objRot,
                objShape,
                accessBitMask,
                collision
            )
            2 -> findPath2(
                flags,
                baseX,
                baseY,
                localDestX,
                localDestY,
                destWidth,
                destHeight,
                srcSize,
                objRot,
                objShape,
                accessBitMask,
                collision
            )
            else -> findPathN(
                flags,
                baseX,
                baseY,
                localDestX,
                localDestY,
                destWidth,
                destHeight,
                srcSize,
                objRot,
                objShape,
                accessBitMask,
                collision
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
        var nextDir = directions[currentX, currentY]
        var currDir = -1
        var turns = 0
        for (i in 0 until searchMapSize * searchMapSize) {
            if (currentX == localSrcX && currentY == localSrcY) break
            val coords = RouteCoordinates(currentX + baseX, currentY + baseY)
            if (currDir != nextDir) {
                turns++
                coordinates.add(0, TURN_COORDS)
                currDir = nextDir
            }
            coordinates.add(0, coords)
            if ((currDir and DirectionFlag.EAST) != 0) {
                currentX++
            } else if ((currDir and DirectionFlag.WEST) != 0) {
                currentX--
            }
            if ((currDir and DirectionFlag.NORTH) != 0) {
                currentY++
            } else if ((currDir and DirectionFlag.SOUTH) != 0) {
                currentY--
            }
            nextDir = directions[currentX, currentY]
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
        flags: CollisionFlagMap,
        baseX: Int,
        baseY: Int,
        localDestX: Int,
        localDestY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        objRot: Int,
        objShape: Int,
        accessBitMask: Int,
        collision: CollisionStrategy
    ): Boolean {
        var localX: Int
        var localY: Int
        var clipFlag: Int
        var dirFlag: Int
        val relativeSearchSize = searchMapSize - 1
        while (bufWriterIndex != bufReaderIndex) {
            currentX = ringBufferX[bufReaderIndex]
            currentY = ringBufferY[bufReaderIndex]
            bufReaderIndex = (bufReaderIndex + 1) and (ringBufferSize - 1)

            if (reached(
                    flags,
                    baseX + currentX,
                    baseY + currentY,
                    baseX + localDestX,
                    baseY + localDestY,
                    destWidth,
                    destHeight,
                    srcSize,
                    objRot,
                    objShape,
                    accessBitMask
                )
            ) {
                return true
            }

            val nextDistance = distances[currentX, currentY] + 1

            /* east to west */
            localX = currentX - 1
            localY = currentY
            clipFlag = CollisionFlag.BLOCK_WEST
            dirFlag = DirectionFlag.EAST
            if (currentX > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], clipFlag)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* west to east */
            localX = currentX + 1
            localY = currentY
            clipFlag = CollisionFlag.BLOCK_EAST
            dirFlag = DirectionFlag.WEST
            if (currentX < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], clipFlag)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* north to south  */
            localX = currentX
            localY = currentY - 1
            clipFlag = CollisionFlag.BLOCK_SOUTH
            dirFlag = DirectionFlag.NORTH
            if (currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], clipFlag)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* south to north */
            localX = currentX
            localY = currentY + 1
            clipFlag = CollisionFlag.BLOCK_NORTH
            dirFlag = DirectionFlag.SOUTH
            if (currentY < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], clipFlag)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* north-east to south-west */
            localX = currentX - 1
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH_EAST
            if (currentX > 0 && currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY], CollisionFlag.BLOCK_WEST) &&
                collision.canMove(flags[baseX + currentX, baseY + localY], CollisionFlag.BLOCK_SOUTH)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* north-west to south-east */
            localX = currentX + 1
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH_WEST
            if (currentX < relativeSearchSize && currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY], CollisionFlag.BLOCK_EAST) &&
                collision.canMove(flags[baseX + currentX, baseY + localY], CollisionFlag.BLOCK_SOUTH)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* south-east to north-west */
            localX = currentX - 1
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH_EAST
            if (currentX > 0 && currentY < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY], CollisionFlag.BLOCK_WEST) &&
                collision.canMove(flags[baseX + currentX, baseY + localY], CollisionFlag.BLOCK_NORTH)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* south-west to north-east */
            localX = currentX + 1
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH_WEST
            if (currentX < relativeSearchSize && currentY < relativeSearchSize &&
                directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_NORTH_EAST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY], CollisionFlag.BLOCK_EAST) &&
                collision.canMove(flags[baseX + currentX, baseY + localY], CollisionFlag.BLOCK_NORTH)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }
        }
        return false
    }

    private fun findPath2(
        flags: CollisionFlagMap,
        baseX: Int,
        baseY: Int,
        localDestX: Int,
        localDestY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        objRot: Int,
        objShape: Int,
        accessBitMask: Int,
        collision: CollisionStrategy
    ): Boolean {
        var localX: Int
        var localY: Int
        var dirFlag: Int
        val relativeSearchSize = searchMapSize - 2
        while (bufWriterIndex != bufReaderIndex) {
            currentX = ringBufferX[bufReaderIndex]
            currentY = ringBufferY[bufReaderIndex]
            bufReaderIndex = (bufReaderIndex + 1) and (ringBufferSize - 1)

            if (reached(
                    flags,
                    baseX + currentX,
                    baseY + currentY,
                    baseX + localDestX,
                    baseY + localDestY,
                    destWidth,
                    destHeight,
                    srcSize,
                    objRot,
                    objShape,
                    accessBitMask
                )
            ) {
                return true
            }

            val nextDistance = distances[currentX, currentY] + 1

            /* east to west */
            localX = currentX - 1
            localY = currentY
            dirFlag = DirectionFlag.EAST
            if (currentX > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY + 1], CollisionFlag.BLOCK_NORTH_WEST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* west to east */
            localX = currentX + 1
            localY = currentY
            dirFlag = DirectionFlag.WEST
            if (currentX < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + currentX + 2, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[baseX + currentX + 2, baseY + currentY + 1], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* north to south  */
            localX = currentX
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH
            if (currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + currentX + 1, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* south to north */
            localX = currentX
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH
            if (currentY < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + currentY + 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[baseX + currentX + 1, baseY + currentY + 2], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* north-east to south-west */
            localX = currentX - 1
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH_EAST
            if (currentX > 0 && currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + currentY], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + currentX, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* north-west to south-east */
            localX = currentX + 1
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH_WEST
            if (currentX < relativeSearchSize && currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + currentX + 2, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[baseX + currentX + 2, baseY + currentY], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* south-east to north-west */
            localX = currentX - 1
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH_EAST
            if (currentX > 0 && currentY < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY + 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[baseX + currentX, baseY + currentY + 2], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }

            /* south-west to north-east */
            localX = currentX + 1
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH_WEST
            if (currentX < relativeSearchSize && currentY < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + currentY + 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[baseX + currentX + 2, baseY + currentY + 2], CollisionFlag.BLOCK_NORTH_EAST) &&
                collision.canMove(flags[baseX + currentX + 2, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                setAndIncrementWriterBuf(localX, localY)
                directions[localX, localY] = dirFlag
                distances[localX, localY] = nextDistance
            }
        }
        return false
    }

    private fun findPathN(
        flags: CollisionFlagMap,
        baseX: Int,
        baseY: Int,
        localDestX: Int,
        localDestY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        objRot: Int,
        objShape: Int,
        accessBitMask: Int,
        collision: CollisionStrategy
    ): Boolean {
        var localX: Int
        var localY: Int
        var dirFlag: Int
        val relativeSearchSize = searchMapSize - srcSize
        while (bufWriterIndex != bufReaderIndex) {
            currentX = ringBufferX[bufReaderIndex]
            currentY = ringBufferY[bufReaderIndex]
            bufReaderIndex = (bufReaderIndex + 1) and (ringBufferSize - 1)

            if (reached(
                    flags,
                    baseX + currentX,
                    baseY + currentY,
                    baseX + localDestX,
                    baseY + localDestY,
                    destWidth,
                    destHeight,
                    srcSize,
                    objRot,
                    objShape,
                    accessBitMask
                )
            ) {
                return true
            }

            val nextDistance = distances[currentX, currentY] + 1

            /* east to west */
            localX = currentX - 1
            localY = currentY
            dirFlag = DirectionFlag.EAST
            if (currentX > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY + srcSize - 1], CollisionFlag.BLOCK_NORTH_WEST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_AND_SOUTH_EAST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[baseX + localX, baseY + currentY + it], clipFlag)
                }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
                }
            }

            /* west to east */
            localX = currentX + 1
            localY = currentY
            dirFlag = DirectionFlag.WEST
            if (currentX < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + currentX + srcSize, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(
                        flags[baseX + currentX + srcSize, baseY + currentY + srcSize - 1],
                        CollisionFlag.BLOCK_NORTH_EAST
                    )
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                val blocked =
                    (1 until srcSize - 1).any {
                        !collision.canMove(
                            flags[baseX + currentX + srcSize, baseY + currentY + it],
                            clipFlag
                        )
                    }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
                }
            }

            /* north to south  */
            localX = currentX
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH
            if (currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + currentX + srcSize - 1, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[baseX + currentX + it, baseY + localY], clipFlag)
                }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
                }
            }

            /* south to north */
            localX = currentX
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH
            if (currentY < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + currentY + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(
                        flags[baseX + currentX + srcSize - 1, baseY + currentY + srcSize],
                        CollisionFlag.BLOCK_NORTH_EAST
                    )
            ) {
                val clipFlag = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val blocked =
                    (1 until srcSize - 1).any {
                        !collision.canMove(flags[baseX + localX + it, baseY + currentY + srcSize], clipFlag)
                    }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
                }
            }

            /* north-east to south-west */
            localX = currentX - 1
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH_EAST
            if (currentX > 0 && currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(
                        flags[baseX + localX, baseY + currentY + srcSize - 2],
                        CollisionFlag.BLOCK_NORTH_WEST
                    ) &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + currentX + srcSize - 2, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[baseX + localX, baseY + currentY + it - 1], clipFlag1) ||
                        !collision.canMove(flags[baseX + currentX + it - 1, baseY + localY], clipFlag2)
                }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
                }
            }

            /* north-west to south-east */
            localX = currentX + 1
            localY = currentY - 1
            dirFlag = DirectionFlag.NORTH_WEST
            if (currentX < relativeSearchSize && currentY > 0 && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + currentX + srcSize, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(
                        flags[baseX + currentX + srcSize, baseY + currentY + srcSize - 2],
                        CollisionFlag.BLOCK_NORTH_EAST
                    )
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[baseX + currentX + srcSize, baseY + currentY + it - 1], clipFlag1) ||
                        !collision.canMove(flags[baseX + currentX + it + 1, baseY + localY], clipFlag2)
                }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
                }
            }

            /* south-east to north-west */
            localX = currentX - 1
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH_EAST
            if (currentX > 0 && currentY < relativeSearchSize && directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + localY], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[baseX + localX, baseY + currentY + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[baseX + currentX, baseY + currentY + srcSize], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_EAST
                val clipFlag2 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[baseX + localX, baseY + currentY + it + 1], clipFlag1) ||
                        !collision.canMove(flags[baseX + currentX + it - 1, baseY + currentY + srcSize], clipFlag2)
                }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
                }
            }

            /* south-west to north-east */
            localX = currentX + 1
            localY = currentY + 1
            dirFlag = DirectionFlag.SOUTH_WEST
            if (currentX < relativeSearchSize && currentY < relativeSearchSize &&
                directions[localX, localY] == 0 &&
                collision.canMove(flags[baseX + localX, baseY + currentY + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(
                        flags[baseX + currentX + srcSize, baseY + currentY + srcSize],
                        CollisionFlag.BLOCK_NORTH_EAST
                    ) &&
                collision.canMove(flags[baseX + currentX + srcSize, baseY + localY], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                val blocked = (1 until srcSize - 1).any {
                    !collision.canMove(flags[baseX + currentX + it + 1, baseY + currentY + srcSize], clipFlag1) ||
                        !collision.canMove(flags[baseX + currentX + srcSize, baseY + currentY + it + 1], clipFlag2)
                }
                if (!blocked) {
                    setAndIncrementWriterBuf(localX, localY)
                    directions[localX, localY] = dirFlag
                    distances[localX, localY] = nextDistance
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
                    currentX = x
                    currentY = y
                    lowestCost = cost
                    maxAlternativePath = distances[x, y]
                }
            }
        }
        return !(lowestCost == MAX_ALTERNATIVE_ROUTE_LOWEST_COST || (srcX == currentX && srcY == currentY))
    }

    private fun reached(
        flags: CollisionFlagMap,
        currX: Int,
        currY: Int,
        destX: Int,
        destY: Int,
        destWidth: Int,
        destHeight: Int,
        srcSize: Int,
        rotation: Int,
        shape: Int,
        accessBitMask: Int
    ): Boolean {
        if (currX == destX && currY == destY) {
            return true
        }
        return when (shape.exitStrategy) {
            WALL_STRATEGY -> reachWall(
                flags,
                currX,
                currY,
                destX,
                destY,
                srcSize,
                shape,
                rotation
            )
            WALL_DECO_STRATEGY -> reachWallDeco(
                flags,
                currX,
                currY,
                destX,
                destY,
                srcSize,
                shape,
                rotation
            )
            RECTANGLE_STRATEGY -> reachRectangle(
                flags,
                accessBitMask,
                currX,
                currY,
                destX,
                destY,
                srcSize,
                destWidth,
                destHeight
            )
            else -> false
        }
    }

    private fun reset() {
        java.util.Arrays.setAll(directions) { 0 }
        java.util.Arrays.setAll(distances) { DEFAULT_DISTANCE_VALUE }
        bufReaderIndex = 0
        bufWriterIndex = 0
    }

    private fun setAndIncrementWriterBuf(x: Int, y: Int) {
        ringBufferX[bufWriterIndex] = x
        ringBufferY[bufWriterIndex] = y
        bufWriterIndex = (bufWriterIndex + 1) and (ringBufferSize - 1)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun IntArray.get(x: Int, y: Int): Int {
        val index = (y * searchMapSize) + x
        return this[index]
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun IntArray.set(x: Int, y: Int, value: Int) {
        val index = (y * searchMapSize) + x
        this[index] = value
    }

    private val Int.exitStrategy: Int
        get() = when {
            this == -1 -> NO_STRATEGY
            this in 0..3 || this == 9 -> WALL_STRATEGY
            this < 9 -> WALL_DECO_STRATEGY
            this in 10..11 || this == 22 -> RECTANGLE_STRATEGY
            else -> NO_STRATEGY
        }
}
