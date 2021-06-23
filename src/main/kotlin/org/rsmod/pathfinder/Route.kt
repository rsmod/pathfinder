package org.rsmod.pathfinder

public data class Route(
    public val coords: List<RouteCoordinates>,
    public val alternative: Boolean,
    public val success: Boolean
) : List<RouteCoordinates> by coords {

    public val failed: Boolean
        get() = !success
}

public inline class RouteCoordinates(private val packed: Int) {

    public val x: Int
        get() = packed and 0xFFFF

    public val y: Int
        get() = (packed shr 16) and 0xFFFF

    public constructor(x: Int, y: Int) : this(
        (x and 0xFFFF) or ((y and 0xFFFF) shl 16)
    )

    public fun translate(xOffset: Int, yOffset: Int): RouteCoordinates {
        return RouteCoordinates(
            x = x + xOffset,
            y = y + yOffset
        )
    }

    public fun translateX(offset: Int): RouteCoordinates = translate(offset, 0)

    public fun translateY(offset: Int): RouteCoordinates = translate(0, offset)

    override fun toString(): String {
        return "${javaClass.simpleName}{x=$x, y=$y}"
    }

    public companion object {

        @JvmStatic
        public fun getX(coords: RouteCoordinates): Int {
            return coords.x
        }

        @JvmStatic
        public fun getY(coords: RouteCoordinates): Int {
            return coords.y
        }

        @JvmStatic
        public fun translate(coords: RouteCoordinates, xOffset: Int, yOffset: Int): RouteCoordinates {
            return coords.translate(xOffset, yOffset)
        }

        @JvmStatic
        public fun translateX(coords: RouteCoordinates, offset: Int): RouteCoordinates {
            return coords.translateX(offset)
        }

        @JvmStatic
        public fun translateY(coords: RouteCoordinates, offset: Int): RouteCoordinates {
            return coords.translateY(offset)
        }
    }
}
