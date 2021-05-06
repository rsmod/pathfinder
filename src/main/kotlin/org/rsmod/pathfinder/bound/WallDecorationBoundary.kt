package org.rsmod.pathfinder.bound

internal fun reachWallDeco(
    flags: IntArray,
    mapSize: Int,
    localSrcX: Int,
    localSrcY: Int,
    localDestX: Int,
    localDestY: Int,
    srcSize: Int,
    shape: Int,
    rot: Int
): Boolean = when {
    srcSize == 1 && localSrcX == localDestX && localDestY == localSrcY -> true
    srcSize != 1 && localDestX >= localSrcX && srcSize + localSrcX + -1 >= localDestX &&
        srcSize + localDestY + -1 >= localDestY -> true
    srcSize == 1 -> reachWallDeco1(flags, mapSize, localSrcX, localSrcY, localDestX, localDestY, shape, rot)
    else -> reachWallDecoN(flags, mapSize, localSrcX, localSrcY, localDestX, localDestY, srcSize, shape, rot)
}

private fun reachWallDeco1(
    flags: IntArray,
    mapSize: Int,
    localSrcX: Int,
    localSrcY: Int,
    localDestX: Int,
    localDestY: Int,
    shape: Int,
    rot: Int
): Boolean {
    if (shape in 6..7) {
        when (rot.alteredRotation(shape)) {
            0 -> {
                if (localSrcX == localDestX + 1 && localSrcY == localDestY &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x80) == 0
                ) return true
                if (localSrcX == localDestX && localSrcY == localDestY - 1 &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x2) == 0
                ) return true
            }
            1 -> {
                if (localSrcX == localDestX - 1 && localSrcY == localDestY &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x8) == 0
                ) return true
                if (localSrcX == localDestX && localSrcY == localDestY - 1 &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x2) == 0
                ) return true
            }
            2 -> {
                if (localSrcX == localDestX - 1 && localSrcY == localDestY &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x8) == 0
                ) return true
                if (localSrcX == localDestX && localSrcY == localDestY + 1 &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x20) == 0
                ) return true
            }
            3 -> {
                if (localSrcX == localDestX + 1 && localSrcY == localDestY &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x80) == 0
                ) return true
                if (localSrcX == localDestX && localSrcY == localDestY + 1 &&
                    (flag(flags, mapSize, localSrcX, localSrcY) and 0x20) == 0
                ) return true
            }
        }
    } else if (shape == 8) {
        if (localSrcX == localDestX && localSrcY == localDestY + 1 &&
            (flag(flags, mapSize, localSrcX, localSrcY) and 0x20) == 0
        ) return true
        if (localSrcX == localDestX && localSrcY == localDestY - 1 &&
            (flag(flags, mapSize, localSrcX, localSrcY) and 0x2) == 0
        ) return true
        if (localSrcX == localDestX - 1 && localSrcY == localDestY &&
            (flag(flags, mapSize, localSrcX, localSrcY) and 0x8) == 0
        ) return true

        return localSrcX == localDestX + 1 && localSrcY == localDestY &&
            (flag(flags, mapSize, localSrcX, localSrcY) and 0x80) == 0
    }
    return false
}

private fun reachWallDecoN(
    flags: IntArray,
    mapSize: Int,
    localSrcX: Int,
    localSrcY: Int,
    localDestX: Int,
    localDestY: Int,
    srcSize: Int,
    shape: Int,
    rot: Int
): Boolean {
    val east = localSrcX + srcSize - 1
    val north = localSrcY + srcSize - 1
    if (shape in 6..7) {
        when (rot.alteredRotation(shape)) {
            0 -> {
                if (localSrcX == localDestX + 1 && localSrcY <= localDestY && north >= localDestY &&
                    (flag(flags, mapSize, localSrcX, localDestY) and 0x80) == 0
                ) return true
                if (localSrcX <= localDestX && localSrcY == localDestY - srcSize && east >= localDestX &&
                    (flag(flags, mapSize, localDestX, north) and 0x2) == 0
                ) return true
            }
            1 -> {
                if (localSrcX == localDestX - srcSize && localSrcY <= localDestY && north >= localDestY &&
                    (flag(flags, mapSize, east, localDestY) and 0x8) == 0
                ) return true
                if (localSrcX <= localDestX && localSrcY == localDestY - srcSize && east >= localDestX &&
                    (flag(flags, mapSize, localDestX, north) and 0x2) == 0
                ) return true
            }
            2 -> {
                if (localSrcX == localDestX - srcSize && localSrcY <= localDestY && north >= localDestY &&
                    (flag(flags, mapSize, east, localDestY) and 0x8) == 0
                ) return true
                if (localSrcX <= localDestX && localSrcY == localDestY + 1 && east >= localDestX &&
                    (flag(flags, mapSize, localDestX, localSrcY) and 0x20) == 0
                ) return true
            }
            3 -> {
                if (localSrcX == localDestX + 1 && localSrcY <= localDestY && north >= localDestY &&
                    (flag(flags, mapSize, localSrcX, localDestY) and 0x80) == 0
                ) return true
                if (localSrcX <= localDestX && localSrcY == localDestY + 1 && east >= localDestX &&
                    (flag(flags, mapSize, localDestX, localSrcY) and 0x20) == 0
                ) return true
            }
        }
    } else if (shape == 8) {
        if (localSrcX <= localDestX && localSrcY == localDestY + 1 && east >= localDestX &&
            (flag(flags, mapSize, localDestX, localSrcY) and 0x20) == 0
        ) return true
        if (localSrcX <= localDestX && localSrcY == localDestY - srcSize && east >= localDestX &&
            (flag(flags, mapSize, localDestX, north) and 0x2) == 0
        ) return true
        if (localSrcX == localDestX - srcSize && localSrcY <= localDestY && north >= localDestY &&
            (flag(flags, mapSize, east, localDestY) and 0x8) == 0
        ) return true

        return localSrcX == localDestX + 1 && localSrcY <= localDestY && north >= localDestY &&
            (flag(flags, mapSize, localSrcX, localDestY) and 0x80) == 0
    }
    return false
}

private fun Int.alteredRotation(shape: Int): Int {
    return if (shape == 7) (this + 2) and 0x3 else this
}

private fun flag(flags: IntArray, width: Int, x: Int, y: Int): Int {
    return flags[(y * width) + x]
}
