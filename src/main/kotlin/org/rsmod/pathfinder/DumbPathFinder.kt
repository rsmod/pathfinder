package org.rsmod.pathfinder

import org.rsmod.pathfinder.bound.reachRectangle
import org.rsmod.pathfinder.collision.CollisionStrategy
import org.rsmod.pathfinder.flag.CollisionFlag
import org.rsmod.pathfinder.reach.ReachStrategy

public class DumbPathFinder(public val searchMapSize: Int = DEFAULT_SEARCH_MAP_SIZE) : PathFinder {

    @Suppress("UNUSED_PARAMETER")
    public override fun findPath(
        flags: IntArray,
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
        srcSize: Int,
        destWidth: Int,
        destHeight: Int,
        objRot: Int,
        objShape: Int,
        moveNear: Boolean,
        accessBitMask: Int,
        maxTurns: Int,
        collision: CollisionStrategy,
        reachStrategy: ReachStrategy
    ): Route {
        val baseX = srcX - (searchMapSize / 2)
        val baseY = srcY - (searchMapSize / 2)
        val localSrcX = srcX - baseX
        val localSrcY = srcY - baseY
        val localDestX = destX - baseX
        val localDestY = destY - baseY
        var currX = localSrcX
        var currY = localSrcY
        val coords = mutableListOf<RouteCoordinates>()
        var success = false
        for (i in 0 until searchMapSize * searchMapSize) {
            if (reached(flags, currX, currY, localDestX, localDestY, srcSize, destWidth, destHeight)) {
                success = true
                break
            }
            val startX = currX
            val startY = currY
            val dir = getDirection(currX, currY, localDestX, localDestY) ?: break
            val blocked = dir.isBlocked(flags, currX, currY, srcSize, collision)
            if (dir == South && !blocked) {
                currY--
            } else if (dir == North && !blocked) {
                currY++
            } else if (dir == West && !blocked) {
                currX--
            } else if (dir == East && !blocked) {
                currX++
            } else if (dir == SouthWest) {
                if (blocked) {
                    if (!South.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currY--
                    } else if (!West.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currX--
                    }
                } else {
                    currX--
                    currY--
                }
            } else if (dir == NorthWest) {
                if (blocked) {
                    if (!North.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currY++
                    } else if (!West.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currX--
                    }
                } else {
                    currX--
                    currY++
                }
            } else if (dir == SouthEast) {
                if (blocked) {
                    if (!South.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currY--
                    } else if (!East.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currX++
                    }
                } else {
                    currX++
                    currY--
                }
            } else if (dir == NorthEast) {
                if (blocked) {
                    if (!North.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currY++
                    } else if (!East.isBlocked(flags, currX, currY, srcSize, collision)) {
                        currX++
                    }
                } else {
                    currX++
                    currY++
                }
            }
            if (startX == currX && startY == currY) {
                /* no valid tile found */
                break
            }
            coords.add(RouteCoordinates(baseX + currX, baseY + currY))
        }
        return Route(coords, alternative = false, success = success)
    }

    private fun reached(
        flags: IntArray,
        localSrcX: Int,
        localSrcY: Int,
        localDestX: Int,
        localDestY: Int,
        srcSize: Int = 1,
        destWidth: Int = 0,
        destHeight: Int = 0
    ): Boolean = if (destWidth != 0 || destHeight != 0) {
        reachRectangle(
            flags = flags,
            mapSize = searchMapSize,
            accessBitMask = 0,
            srcX = localSrcX,
            srcY = localSrcY,
            destX = localDestX,
            destY = localDestY,
            srcSize = srcSize,
            destWidth = destWidth,
            destHeight = destHeight
        )
    } else {
        localSrcX == localDestX && localSrcY == localDestY
    }

    private fun Direction.isBlocked(
        flags: IntArray,
        x: Int,
        y: Int,
        srcSize: Int,
        collision: CollisionStrategy
    ): Boolean = when (srcSize) {
        1 -> isBlocked1(flags, x, y, collision)
        2 -> isBlocked2(flags, x, y, collision)
        else -> isBlockedN(flags, x, y, srcSize, collision)
    }

    private fun Direction.isBlocked1(
        flags: IntArray,
        x: Int,
        y: Int,
        collision: CollisionStrategy
    ): Boolean = when (this) {
        South -> !collision.canMove(flags[x, y - 1], CollisionFlag.BLOCK_SOUTH)
        North -> !collision.canMove(flags[x, y + 1], CollisionFlag.BLOCK_NORTH)
        West -> !collision.canMove(flags[x - 1, y], CollisionFlag.BLOCK_WEST)
        East -> !collision.canMove(flags[x + 1, y], CollisionFlag.BLOCK_EAST)
        SouthWest -> !collision.canMove(flags[x - 1, y - 1], CollisionFlag.BLOCK_SOUTH_WEST) ||
            !collision.canMove(flags[x - 1, y], CollisionFlag.BLOCK_WEST) ||
            !collision.canMove(flags[x, y - 1], CollisionFlag.BLOCK_SOUTH)
        NorthWest -> !collision.canMove(flags[x - 1, y + 1], CollisionFlag.BLOCK_NORTH_WEST) ||
            !collision.canMove(flags[x - 1, y], CollisionFlag.BLOCK_WEST) ||
            !collision.canMove(flags[x, y + 1], CollisionFlag.BLOCK_NORTH)
        SouthEast -> !collision.canMove(flags[x + 1, y - 1], CollisionFlag.BLOCK_SOUTH_EAST) ||
            !collision.canMove(flags[x + 1, y], CollisionFlag.BLOCK_EAST) ||
            !collision.canMove(flags[x, y - 1], CollisionFlag.BLOCK_SOUTH)
        NorthEast -> !collision.canMove(flags[x + 1, y + 1], CollisionFlag.BLOCK_NORTH_EAST) ||
            !collision.canMove(flags[x + 1, y], CollisionFlag.BLOCK_EAST) ||
            !collision.canMove(flags[x, y + 1], CollisionFlag.BLOCK_NORTH)
    }

    private fun Direction.isBlocked2(
        flags: IntArray,
        x: Int,
        y: Int,
        collision: CollisionStrategy
    ): Boolean = when (this) {
        South -> !collision.canMove(flags[x, y - 1], CollisionFlag.BLOCK_SOUTH_WEST) ||
            !collision.canMove(flags[x + 1, y - 1], CollisionFlag.BLOCK_SOUTH_EAST)
        North -> !collision.canMove(flags[x, y + 2], CollisionFlag.BLOCK_NORTH_WEST) ||
            !collision.canMove(flags[x + 1, y + 2], CollisionFlag.BLOCK_NORTH_EAST)
        West -> !collision.canMove(flags[x - 1, y], CollisionFlag.BLOCK_SOUTH_WEST) ||
            !collision.canMove(flags[x - 1, y + 1], CollisionFlag.BLOCK_NORTH_WEST)
        East -> !collision.canMove(flags[x + 2, y], CollisionFlag.BLOCK_SOUTH_EAST) ||
            !collision.canMove(flags[x + 2, y + 1], CollisionFlag.BLOCK_NORTH_EAST)
        SouthWest -> !collision.canMove(flags[x - 1, y], CollisionFlag.BLOCK_NORTH_WEST) ||
            !collision.canMove(flags[x - 1, y - 1], CollisionFlag.BLOCK_SOUTH_WEST) ||
            !collision.canMove(flags[x, y - 1], CollisionFlag.BLOCK_SOUTH_EAST)
        NorthWest -> !collision.canMove(flags[x - 1, y + 1], CollisionFlag.BLOCK_SOUTH_WEST) ||
            !collision.canMove(flags[x - 1, y + 2], CollisionFlag.BLOCK_NORTH_WEST) ||
            !collision.canMove(flags[x, y + 2], CollisionFlag.BLOCK_NORTH_EAST)
        SouthEast -> !collision.canMove(flags[x + 1, y - 1], CollisionFlag.BLOCK_SOUTH_WEST) ||
            !collision.canMove(flags[x + 2, y - 1], CollisionFlag.BLOCK_SOUTH_EAST) ||
            !collision.canMove(flags[x + 2, y], CollisionFlag.BLOCK_NORTH_EAST)
        NorthEast -> !collision.canMove(flags[x + 1, y + 2], CollisionFlag.BLOCK_NORTH_WEST) ||
            !collision.canMove(flags[x + 2, y + 2], CollisionFlag.BLOCK_NORTH_EAST) ||
            !collision.canMove(flags[x + 2, y + 1], CollisionFlag.BLOCK_SOUTH_EAST)
    }

    private fun Direction.isBlockedN(
        flags: IntArray,
        x: Int,
        y: Int,
        srcSize: Int,
        collision: CollisionStrategy
    ): Boolean = when (this) {
        South -> {
            if (collision.canMove(flags[x, y - 1], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x + srcSize - 1, y - 1], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                (1 until srcSize - 1).any { !collision.canMove(flags[x + it, y], clipFlag) }
            } else true
        }
        North -> {
            if (collision.canMove(flags[x, y + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[x + srcSize - 1, y + srcSize], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                (1 until srcSize - 1).any { !collision.canMove(flags[x + it, y], clipFlag) }
            } else true
        }
        West -> {
            if (collision.canMove(flags[x - 1, y], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x - 1, y + srcSize - 1], CollisionFlag.BLOCK_NORTH_WEST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_AND_SOUTH_EAST
                (1 until srcSize - 1).any { !collision.canMove(flags[x + it, y], clipFlag) }
            } else true
        }
        East -> {
            if (collision.canMove(flags[x + srcSize, y], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[x + srcSize, y + srcSize - 1], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                (1 until srcSize - 1).any { !collision.canMove(flags[x + it, y], clipFlag) }
            } else true
        }
        SouthWest -> {
            if (collision.canMove(flags[x - 1, y + srcSize - 2], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[x - 1, y - 1], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x + srcSize - 2, y - 1], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                (1 until srcSize - 1).any {
                    !collision.canMove(flags[x - 1, y + it - 1], clipFlag1) ||
                        !collision.canMove(flags[x + it - 1, y - 1], clipFlag2)
                }
            } else true
        }
        NorthWest -> {
            if (collision.canMove(flags[x - 1, y + 1], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x - 1, y + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[x, y + srcSize], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_EAST
                val clipFlag2 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                (1 until srcSize - 1).any {
                    !collision.canMove(flags[x - 1, y + it - 1], clipFlag1) ||
                        !collision.canMove(flags[x + it - 1, y + srcSize], clipFlag2)
                }
            } else true
        }
        SouthEast -> {
            if (collision.canMove(flags[x + 1, y - 1], CollisionFlag.BLOCK_SOUTH_WEST) &&
                collision.canMove(flags[x + srcSize, y - 1], CollisionFlag.BLOCK_SOUTH_EAST) &&
                collision.canMove(flags[x + srcSize, y + srcSize - 2], CollisionFlag.BLOCK_NORTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_EAST_AND_WEST
                (1 until srcSize - 1).any {
                    !collision.canMove(flags[x + srcSize, y + it - 1], clipFlag1) ||
                        !collision.canMove(flags[x + it + 1, y - 1], clipFlag2)
                }
            } else true
        }
        NorthEast -> {
            if (collision.canMove(flags[x + 1, y + srcSize], CollisionFlag.BLOCK_NORTH_WEST) &&
                collision.canMove(flags[x + srcSize, y + srcSize], CollisionFlag.BLOCK_NORTH_EAST) &&
                collision.canMove(flags[x + srcSize, y + 1], CollisionFlag.BLOCK_SOUTH_EAST)
            ) {
                val clipFlag1 = CollisionFlag.BLOCK_SOUTH_EAST_AND_WEST
                val clipFlag2 = CollisionFlag.BLOCK_NORTH_AND_SOUTH_WEST
                (1 until srcSize - 1).any {
                    !collision.canMove(flags[x + it + 1, y + srcSize], clipFlag1) ||
                        !collision.canMove(flags[x + srcSize, y + it + 1], clipFlag2)
                }
            } else true
        }
    }

    private fun getDirection(srcX: Int, srcY: Int, destX: Int, destY: Int): Direction? {
        if (srcX == destX) {
            if (srcY > destY) {
                return South
            } else if (srcY < destY) {
                return North
            }
        } else return if (srcY == destY) {
            if (srcX > destX) {
                West
            } else {
                East
            }
        } else {
            if (srcX < destX && srcY < destY) {
                NorthEast
            } else if (srcX < destX) {
                SouthEast
            } else if (srcY < destY) {
                NorthWest
            } else {
                SouthWest
            }
        }
        return null
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun IntArray.get(x: Int, y: Int): Int {
        val index = (y * searchMapSize) + x
        return this[index]
    }
}
