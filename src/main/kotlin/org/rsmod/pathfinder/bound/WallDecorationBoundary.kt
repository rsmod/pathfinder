package org.rsmod.pathfinder.bound

import org.rsmod.pathfinder.collision.CollisionFlagMap

internal fun reachWallDeco(
    flags: CollisionFlagMap,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    srcSize: Int,
    shape: Int,
    rot: Int
): Boolean = when {
    srcSize == 1 && currX == destX && destY == currY -> true
    srcSize != 1 && destX >= currX && srcSize + currX + -1 >= destX && srcSize + destY + -1 >= destY -> true
    srcSize == 1 -> reachWallDeco1(flags, currX, currY, destX, destY, shape, rot)
    else -> reachWallDecoN(flags, currX, currY, destX, destY, srcSize, shape, rot)
}

private fun reachWallDeco1(
    flags: CollisionFlagMap,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    shape: Int,
    rot: Int
): Boolean {
    if (shape in 6..7) {
        when (rot.alteredRotation(shape)) {
            0 -> {
                if (currX == destX + 1 && currY == destY &&
                    (flag(flags, currX, currY) and 0x80) == 0
                ) return true
                if (currX == destX && currY == destY - 1 &&
                    (flag(flags, currX, currY) and 0x2) == 0
                ) return true
            }
            1 -> {
                if (currX == destX - 1 && currY == destY &&
                    (flag(flags, currX, currY) and 0x8) == 0
                ) return true
                if (currX == destX && currY == destY - 1 &&
                    (flag(flags, currX, currY) and 0x2) == 0
                ) return true
            }
            2 -> {
                if (currX == destX - 1 && currY == destY &&
                    (flag(flags, currX, currY) and 0x8) == 0
                ) return true
                if (currX == destX && currY == destY + 1 &&
                    (flag(flags, currX, currY) and 0x20) == 0
                ) return true
            }
            3 -> {
                if (currX == destX + 1 && currY == destY &&
                    (flag(flags, currX, currY) and 0x80) == 0
                ) return true
                if (currX == destX && currY == destY + 1 &&
                    (flag(flags, currX, currY) and 0x20) == 0
                ) return true
            }
        }
    } else if (shape == 8) {
        if (currX == destX && currY == destY + 1 &&
            (flag(flags, currX, currY) and 0x20) == 0
        ) return true
        if (currX == destX && currY == destY - 1 &&
            (flag(flags, currX, currY) and 0x2) == 0
        ) return true
        if (currX == destX - 1 && currY == destY &&
            (flag(flags, currX, currY) and 0x8) == 0
        ) return true

        return currX == destX + 1 && currY == destY &&
            (flag(flags, currX, currY) and 0x80) == 0
    }
    return false
}

private fun reachWallDecoN(
    flags: CollisionFlagMap,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    srcSize: Int,
    shape: Int,
    rot: Int
): Boolean {
    val east = currX + srcSize - 1
    val north = currY + srcSize - 1
    if (shape in 6..7) {
        when (rot.alteredRotation(shape)) {
            0 -> {
                if (currX == destX + 1 && currY <= destY && north >= destY &&
                    (flag(flags, currX, destY) and 0x80) == 0
                ) return true
                if (currX <= destX && currY == destY - srcSize && east >= destX &&
                    (flag(flags, destX, north) and 0x2) == 0
                ) return true
            }
            1 -> {
                if (currX == destX - srcSize && currY <= destY && north >= destY &&
                    (flag(flags, east, destY) and 0x8) == 0
                ) return true
                if (currX <= destX && currY == destY - srcSize && east >= destX &&
                    (flag(flags, destX, north) and 0x2) == 0
                ) return true
            }
            2 -> {
                if (currX == destX - srcSize && currY <= destY && north >= destY &&
                    (flag(flags, east, destY) and 0x8) == 0
                ) return true
                if (currX <= destX && currY == destY + 1 && east >= destX &&
                    (flag(flags, destX, currY) and 0x20) == 0
                ) return true
            }
            3 -> {
                if (currX == destX + 1 && currY <= destY && north >= destY &&
                    (flag(flags, currX, destY) and 0x80) == 0
                ) return true
                if (currX <= destX && currY == destY + 1 && east >= destX &&
                    (flag(flags, destX, currY) and 0x20) == 0
                ) return true
            }
        }
    } else if (shape == 8) {
        if (currX <= destX && currY == destY + 1 && east >= destX &&
            (flag(flags, destX, currY) and 0x20) == 0
        ) return true
        if (currX <= destX && currY == destY - srcSize && east >= destX &&
            (flag(flags, destX, north) and 0x2) == 0
        ) return true
        if (currX == destX - srcSize && currY <= destY && north >= destY &&
            (flag(flags, east, destY) and 0x8) == 0
        ) return true

        return currX == destX + 1 && currY <= destY && north >= destY &&
            (flag(flags, currX, destY) and 0x80) == 0
    }
    return false
}

private fun Int.alteredRotation(shape: Int): Int {
    return if (shape == 7) (this + 2) and 0x3 else this
}

private fun flag(flags: CollisionFlagMap, x: Int, y: Int): Int {
    return flags[x, y]
}
