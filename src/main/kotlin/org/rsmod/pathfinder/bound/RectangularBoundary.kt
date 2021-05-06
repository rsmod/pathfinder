package org.rsmod.pathfinder.bound

import kotlin.math.min

internal fun reachRectangle(
    flags: IntArray,
    mapSize: Int,
    accessBitMask: Int,
    localSrcX: Int,
    localSrcY: Int,
    localDestX: Int,
    localDestY: Int,
    srcSize: Int,
    destWidth: Int,
    destHeight: Int
): Boolean = when {
    srcSize > 1 -> {
        collides(localSrcX, localSrcY, localDestX, localDestY, srcSize, srcSize, destWidth, destHeight) ||
            reachRectangleN(
                flags,
                mapSize,
                accessBitMask,
                localSrcX,
                localSrcY,
                localDestX,
                localDestY,
                srcSize,
                srcSize,
                destWidth,
                destHeight
            )
    }
    else -> reachRectangle1(
        flags,
        mapSize,
        accessBitMask,
        localSrcX,
        localSrcY,
        localDestX,
        localDestY,
        destWidth,
        destHeight
    )
}

private fun collides(
    localSrcX: Int,
    localSrcY: Int,
    localDestX: Int,
    localDestY: Int,
    srcWidth: Int,
    srcHeight: Int,
    destWidth: Int,
    destHeight: Int
): Boolean = if (localSrcX >= localDestX + destWidth || localSrcX + srcWidth <= localDestX) {
    false
} else {
    localSrcY < localDestY + destHeight && localDestY < srcHeight + localSrcY
}

private fun reachRectangle1(
    flags: IntArray,
    mapSize: Int,
    accessBitMask: Int,
    localSrcX: Int,
    localSrcY: Int,
    localDestX: Int,
    localDestY: Int,
    destWidth: Int,
    destHeight: Int
): Boolean {
    val east = localDestX + destWidth - 1
    val north = localDestY + destHeight - 1

    if (localSrcX in localDestX..east && localSrcY >= localDestY && localSrcY <= north)
        return true

    if (localSrcX == localDestX - 1 && localSrcY >= localDestY && localSrcY <= north &&
        (flag(flags, mapSize, localSrcX, localSrcY) and 0x8) == 0 &&
        (accessBitMask and 0x8) == 0
    ) return true

    if (localSrcX == east + 1 && localSrcY >= localDestY && localSrcY <= north &&
        (flag(flags, mapSize, localSrcX, localSrcY) and 0x80) == 0 &&
        (accessBitMask and 0x2) == 0
    ) return true

    if (localSrcY + 1 == localDestY && localSrcX >= localDestX && localSrcX <= east &&
        (flag(flags, mapSize, localSrcX, localSrcY) and 0x2) == 0 &&
        (accessBitMask and 0x4) == 0

    ) return true

    return localSrcY == north + 1 && localSrcX >= localDestX && localSrcX <= east &&
        (flag(flags, mapSize, localSrcX, localSrcY) and 0x20) == 0 &&
        (accessBitMask and 0x1) == 0
}

private fun reachRectangleN(
    flags: IntArray,
    mapSize: Int,
    accessBitMask: Int,
    localSrcX: Int,
    localSrcY: Int,
    localDestX: Int,
    localDestY: Int,
    srcWidth: Int,
    srcHeight: Int,
    destWidth: Int,
    destHeight: Int
): Boolean {
    val srcEast = localSrcX + srcWidth
    val srcNorth = srcHeight + localSrcY
    val destEast = destWidth + localDestX
    val destNorth = destHeight + localDestY
    if (localSrcX in localDestX until destEast) {
        if (localDestY == srcNorth && (accessBitMask and 0x4) == 0) {
            val minEast = min(srcEast, destEast)
            for (x in localSrcX until minEast) {
                if ((flag(flags, mapSize, x, srcNorth - 1) and 0x2) == 0) {
                    return true
                }
            }
        } else if (destNorth == localSrcY && (accessBitMask and 0x1) == 0) {
            val minEastX = min(srcEast, destEast)
            for (x in localSrcX until minEastX) {
                if ((flag(flags, mapSize, x, localSrcY) and 0x20) == 0) {
                    return true
                }
            }
        }
    } else if (srcEast in (localDestX + 1)..destEast) {
        if (localDestY == srcNorth && (accessBitMask and 0x4) == 0) {
            for (x in localDestX until srcEast) {
                if ((flag(flags, mapSize, x, srcNorth - 1) and 0x2) == 0) {
                    return true
                }
            }
        } else if (localSrcY == destNorth && (accessBitMask and 0x1) == 0) {
            for (x in localDestX until srcEast) {
                if ((flag(flags, mapSize, x, localSrcY) and 0x2) == 0) {
                    return true
                }
            }
        }
    } else if (localSrcY in localDestY until destNorth) {
        if (srcEast == localDestX && (accessBitMask and 0x8) == 0) {
            val minNorthY = min(srcNorth, destNorth)
            for (y in localSrcY until minNorthY) {
                if ((flag(flags, mapSize, srcEast - 1, y) and 0x8) == 0) {
                    return true
                }
            }
        } else if (destEast == localSrcX && (accessBitMask and 0x2) == 0) {
            val minNorthY = min(srcNorth, destNorth)
            for (y in localSrcY until minNorthY) {
                if ((flag(flags, mapSize, localSrcX, y) and 0x80) == 0) {
                    return true
                }
            }
        }
    } else if (srcNorth in (localDestY + 1)..destNorth) {
        if (localDestX == srcEast && (accessBitMask and 0x8) == 0) {
            for (y in localDestY until srcNorth) {
                if ((flag(flags, mapSize, srcEast - 1, y) and 0x8) == 0) {
                    return true
                }
            }
        } else if (destEast == localSrcX && (accessBitMask and 0x2) == 0) {
            for (y in localDestY until srcNorth) {
                if ((flag(flags, mapSize, localSrcX, y) and 0x80) == 0) {
                    return true
                }
            }
        }
    }
    return false
}

private fun flag(flags: IntArray, width: Int, x: Int, y: Int): Int {
    return flags[(y * width) + x]
}
