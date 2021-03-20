package org.rsmod.pathfinder.bound

import org.rsmod.pathfinder.collision.CollisionFlagMap
import kotlin.math.min

internal fun reachRectangle(
    flags: CollisionFlagMap,
    accessBitMask: Int,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    srcSize: Int,
    destWidth: Int,
    destHeight: Int
): Boolean = when {
    srcSize > 1 -> {
        collides(currX, currY, destX, destY, srcSize, srcSize, destWidth, destHeight) ||
            reachRectangleN(
                flags,
                accessBitMask,
                currX,
                currY,
                destX,
                destY,
                srcSize,
                srcSize,
                destWidth,
                destHeight
            )
    }
    else -> reachRectangle1(flags, accessBitMask, currX, currY, destX, destY, destWidth, destHeight)
}

private fun collides(
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    srcWidth: Int,
    srcHeight: Int,
    destWidth: Int,
    destHeight: Int
): Boolean = if (currX >= destX + destWidth || currX + srcWidth <= destX) {
    false
} else {
    currY < destY + destHeight && destY < srcHeight + currY
}

private fun reachRectangle1(
    flags: CollisionFlagMap,
    accessBitMask: Int,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    destWidth: Int,
    destHeight: Int
): Boolean {
    val east = destX + destWidth - 1
    val north = destY + destHeight - 1

    if (currX in destX..east && currY >= destY && currY <= north)
        return true

    if (currX == destX - 1 && currY >= destY && currY <= north &&
        (flag(flags, currX, currY) and 0x8) == 0 &&
        (accessBitMask and 0x8) == 0
    ) return true

    if (currX == east + 1 && currY >= destY && currY <= north &&
        (flag(flags, currX, currY) and 0x80) == 0 &&
        (accessBitMask and 0x2) == 0
    ) return true

    if (currY + 1 == destY && currX >= destX && currX <= east &&
        (flag(flags, currX, currY) and 0x2) == 0 &&
        (accessBitMask and 0x4) == 0

    ) return true

    return currY == north + 1 && currX >= destX && currX <= east &&
        (flag(flags, currX, currY) and 0x20) == 0 &&
        (accessBitMask and 0x1) == 0
}

private fun reachRectangleN(
    flags: CollisionFlagMap,
    accessBitMask: Int,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    srcWidth: Int,
    srcHeight: Int,
    destWidth: Int,
    destHeight: Int
): Boolean {
    val srcEast = currX + srcWidth
    val srcNorth = srcHeight + currY
    val destEast = destWidth + destX
    val destNorth = destHeight + destY
    if (currX in destX until destEast) {
        if (destY == srcNorth && (accessBitMask and 0x4) == 0) {
            val minEast = min(srcEast, destEast)
            for (x in currX until minEast) {
                if ((flag(flags, x, srcNorth - 1) and 0x2) == 0) {
                    return true
                }
            }
        } else if (destNorth == currY && (accessBitMask and 0x1) == 0) {
            val minEastX = min(srcEast, destEast)
            for (x in currX until minEastX) {
                if ((flag(flags, x, currY) and 0x20) == 0) {
                    return true
                }
            }
        }
    } else if (srcEast in (destX + 1)..destEast) {
        if (destY == srcNorth && (accessBitMask and 0x4) == 0) {
            for (x in destX until srcEast) {
                if ((flag(flags, x, srcNorth - 1) and 0x2) == 0) {
                    return true
                }
            }
        } else if (currY == destNorth && (accessBitMask and 0x1) == 0) {
            for (x in destX until srcEast) {
                if ((flag(flags, x, currY) and 0x2) == 0) {
                    return true
                }
            }
        }
    } else if (currY in destY until destNorth) {
        if (srcEast == destX && (accessBitMask and 0x8) == 0) {
            val minNorthY = min(srcNorth, destNorth)
            for (y in currY until minNorthY) {
                if ((flag(flags, srcEast - 1, y) and 0x8) == 0) {
                    return true
                }
            }
        } else if (destEast == currX && (accessBitMask and 0x2) == 0) {
            val minNorthY = min(srcNorth, destNorth)
            for (y in currY until minNorthY) {
                if ((flag(flags, currX, y) and 0x80) == 0) {
                    return true
                }
            }
        }
    } else if (srcNorth in (destY + 1)..destNorth) {
        if (destX == srcEast && (accessBitMask and 0x8) == 0) {
            for (y in destY until srcNorth) {
                if ((flag(flags, srcEast - 1, y) and 0x8) == 0) {
                    return true
                }
            }
        } else if (destEast == currX && (accessBitMask and 0x2) == 0) {
            for (y in destY until srcNorth) {
                if ((flag(flags, currX, y) and 0x80) == 0) {
                    return true
                }
            }
        }
    }
    return false
}

private fun flag(flags: CollisionFlagMap, x: Int, y: Int): Int {
    return flags[x, y]
}
