package org.rsmod.pathfinder.bound

import org.rsmod.pathfinder.collision.CollisionFlagMap

internal fun reachWall(
    flags: CollisionFlagMap,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    srcSize: Int,
    shape: Int,
    rot: Int
): Boolean = when {
    srcSize == 1 && currX == destX && currY == destY -> true
    srcSize != 1 && destX >= currX && srcSize + currX - 1 >= destX && srcSize + destY - 1 >= destY -> true
    srcSize == 1 -> reachWall1(flags, currX, currY, destX, destY, shape, rot)
    else -> reachWallN(flags, currX, currY, destX, destY, srcSize, shape, rot)
}

private fun reachWall1(
    flags: CollisionFlagMap,
    currX: Int,
    currY: Int,
    destX: Int,
    destY: Int,
    shape: Int,
    rot: Int
): Boolean {
    when (shape) {
        0 -> {
            when (rot) {
                0 -> {
                    if (currX == destX - 1 && currY == destY)
                        return true
                    if (currX == destX && currY == destY + 1 &&
                        (flag(flags, currX, currY) and 0x12c0120) == 0
                    ) return true
                    if (currX == destX && currY == destY - 1 &&
                        (flag(flags, currX, currY) and 0x12c0102) == 0
                    ) return true
                }
                1 -> {
                    if (currX == destX && currY == destY + 1)
                        return true
                    if (currX == destX - 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0108) == 0
                    ) return true
                    if (currX == destX + 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0180) == 0
                    ) return true
                }
                2 -> {
                    if (currX == destX + 1 && currY == destY)
                        return true
                    if (currX == destX && currY == destY + 1 &&
                        (flag(flags, currX, currY) and 0x12c0120) == 0
                    ) return true
                    if (currX == destX && currY == destY - 1 &&
                        (flag(flags, currX, currY) and 0x12c0102) == 0
                    ) return true
                }
                3 -> {
                    if (currX == destX && currY == destY - 1)
                        return true
                    if (currX == destX - 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0108) == 0
                    ) return true
                    if (currX == destX + 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0180) == 0
                    ) return true
                }
            }
        }
        2 -> {
            when (rot) {
                0 -> {
                    if (currX == destX - 1 && currY == destY)
                        return true
                    if (currX == destX && currY == destY + 1)
                        return true
                    if (currX == destX + 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0180) == 0
                    ) return true
                    if (currX == destX && currY == destY - 1 &&
                        (flag(flags, currX, currY) and 0x12c0102) == 0
                    ) return true
                }
                1 -> {
                    if (currX == destX - 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0108) == 0
                    ) return true
                    if (currX == destX && currY == destY + 1)
                        return true
                    if (currX == destX + 1 && currY == destY)
                        return true
                    if (currX == destX && currY == destY - 1 &&
                        (flag(flags, currX, currY) and 0x12c0102) == 0
                    ) return true
                }
                2 -> {
                    if (currX == destX - 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0108) == 0
                    ) return true
                    if (currX == destX && currY == destY + 1 &&
                        (flag(flags, currX, currY) and 0x12c0120) == 0
                    ) return true
                    if (currX == destX + 1 && currY == destY)
                        return true
                    if (currX == destX && currY == destY - 1)
                        return true
                }
                3 -> {
                    if (currX == destX - 1 && currY == destY)
                        return true
                    if (currX == destX && currY == destY + 1 &&
                        (flag(flags, currX, currY) and 0x12c0120) == 0
                    ) return true
                    if (currX == destX + 1 && currY == destY &&
                        (flag(flags, currX, currY) and 0x12c0180) == 0
                    ) return true
                    if (currX == destX && currY == destY - 1)
                        return true
                }
            }
        }
        9 -> {
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
    }
    return false
}

private fun reachWallN(
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
    when (shape) {
        0 -> {
            when (rot) {
                0 -> {
                    if (currX == destX - srcSize && currY <= destY && north >= destY)
                        return true
                    if (destX in currX..east && currY == destY + 1 &&
                        (flag(flags, destX, currY) and 0x12c0120) == 0
                    ) return true
                    if (destX in currX..east && currY == destY - srcSize &&
                        (flag(flags, destX, north) and 0x12c0102) == 0
                    ) return true
                }
                1 -> {
                    if (destX in currX..east && currY == destY + 1)
                        return true
                    if (currX == destX - srcSize && currY <= destY && north >= destY &&
                        (flag(flags, east, destY) and 0x12c0108) == 0
                    ) return true
                    if (currX == destX + 1 && currY <= destY && north >= destY &&
                        (flag(flags, currX, destY) and 0x12c0180) == 0
                    ) return true
                }
                2 -> {
                    if (currX == destX + 1 && currY <= destY && north >= destY)
                        return true
                    if (destX in currX..east && currY == destY + 1 &&
                        (flag(flags, destX, currY) and 0x12c0120) == 0
                    ) return true
                    if (destX in currX..east && currY == destY - srcSize &&
                        (flag(flags, destX, north) and 0x12c0102) == 0
                    ) return true
                }
                3 -> {
                    if (destX in currX..east && currY == destY - srcSize)
                        return true
                    if (currX == destX - srcSize && currY <= destY && north >= destY &&
                        (flag(flags, east, destY) and 0x12c0108) == 0
                    ) return true
                    if (currX == destX + 1 && currY <= destY && north >= destY &&
                        (flag(flags, currX, destY) and 0x12c0180) == 0
                    ) return true
                }
            }
        }
        2 -> {
            when (rot) {
                0 -> {
                    if (currX == destX - srcSize && currY <= destY && north >= destY)
                        return true
                    if (destX in currX..east && currY == destY + 1)
                        return true
                    if (currX == destX + 1 && currY <= destY && north >= destY &&
                        (flag(flags, currX, destY) and 0x12c0180) == 0
                    ) return true
                    if (destX in currX..east && currY == destY - srcSize &&
                        (flag(flags, destX, north) and 0x12c0102) == 0
                    ) return true
                }
                1 -> {
                    if (currX == destX - srcSize && currY <= destY && north >= destY &&
                        (flag(flags, east, destY) and 0x12c0108) == 0
                    ) return true
                    if (destX in currX..east && currY == destY + 1)
                        return true
                    if (currX == destX + 1 && currY <= destY && north >= destY)
                        return true
                    if (destX in currX..east && currY == destY - srcSize &&
                        (flag(flags, destX, north) and 0x12c0102) == 0
                    ) return true
                }
                2 -> {
                    if (currX == destX - srcSize && currY <= destY && north >= destY &&
                        (flag(flags, east, destY) and 0x12c0108) == 0
                    ) return true
                    if (destX in currX..east && currY == destY + 1 &&
                        (flag(flags, destX, currY) and 0x12c0120) == 0
                    ) return true
                    if (currX == destX + 1 && currY <= destY && north >= destY)
                        return true
                    if (destX in currX..east && currY == destY - srcSize)
                        return true
                }
                3 -> {
                    if (currX == destX - srcSize && currY <= destY && north >= destY)
                        return true
                    if (destX in currX..east && currY == destY + 1 &&
                        (flag(flags, destX, currY) and 0x12c0120) == 0
                    ) return true
                    if (currX == destX + 1 && currY <= destY && north >= destY &&
                        (flag(flags, currX, destY) and 0x12c0180) == 0
                    ) return true
                    if (destX in currX..east && currY == destY - srcSize)
                        return true
                }
            }
        }
        9 -> {
            if (destX in currX..east && currY == destY + 1 &&
                (flag(flags, destX, currY) and 0x12c0120) == 0
            ) return true
            if (destX in currX..east && currY == destY - srcSize &&
                (flag(flags, destX, north) and 0x12c0102) == 0
            ) return true
            if (currX == destX - srcSize && currY <= destY && north >= destY &&
                (flag(flags, east, destY) and 0x12c0108) == 0
            ) return true

            return currX == destX + 1 && currY <= destY && north >= destY &&
                (flag(flags, currX, destY) and 0x12c0180) == 0
        }
    }
    return false
}

private fun flag(flags: CollisionFlagMap, x: Int, y: Int): Int {
    return flags[x, y]
}
