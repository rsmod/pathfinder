/*
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.rsmod.pathfinder

import org.rsmod.pathfinder.collision.CollisionFlagMap
import org.rsmod.pathfinder.collision.CollisionLocalFlagMap
import org.rsmod.pathfinder.flag.CollisionFlag.OBJECT_PROJECTILE_BLOCKER
import org.rsmod.pathfinder.flag.CollisionFlag.WALL_EAST_PROJECTILE_BLOCKER
import org.rsmod.pathfinder.flag.CollisionFlag.WALL_NORTH_PROJECTILE_BLOCKER
import org.rsmod.pathfinder.flag.CollisionFlag.WALL_SOUTH_PROJECTILE_BLOCKER
import org.rsmod.pathfinder.flag.CollisionFlag.WALL_WEST_PROJECTILE_BLOCKER
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
public class ProjectileValidator(public val searchMapSize: Int) {

    public fun isValid(
        flags: IntArray,
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
        srcSize: Int = 1,
        destWidth: Int = 0,
        destHeight: Int = 0
    ): Boolean {
        val localFlags = CollisionLocalFlagMap(srcX, srcY, searchMapSize, flags)
        return isValid(localFlags, srcX, srcY, destX, destY, srcSize, destWidth, destHeight)
    }

    public fun isValid(
        flags: CollisionFlagMap,
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
        srcSize: Int = 1,
        destWidth: Int = 0,
        destHeight: Int = 0
    ): Boolean {
        val route = rayCast(flags, srcX, srcY, destX, destY, srcSize, destWidth, destHeight)
        return route.success
    }

    public fun rayCast(
        flags: CollisionFlagMap,
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
        srcSize: Int = 1,
        destWidth: Int = 0,
        destHeight: Int = 0
    ): Route {
        val halfMap = searchMapSize / 2
        val baseX = srcX - halfMap
        val baseY = srcY - halfMap
        val localSrcX = srcX - baseX
        val localSrcY = srcY - baseY
        val localDestX = destX - baseX
        val localDestY = destY - baseY

        val startX = coordinate(localSrcX, localDestX, srcSize)
        val startY = coordinate(localSrcY, localDestY, srcSize)

        val endX = coordinate(localDestX, localSrcX, destWidth)
        val endY = coordinate(localDestY, localSrcY, destHeight)

        val deltaX = endX - startX
        val deltaY = endY - startY

        val travelEast = deltaX >= 0
        val travelNorth = deltaY >= 0

        val xFlags = if (travelEast) BLOCKED_WEST else BLOCKED_EAST
        val yFlags = if (travelNorth) BLOCKED_SOUTH else BLOCKED_NORTH

        val coords = mutableListOf<RouteCoordinates>()

        if (abs(deltaX) > abs(deltaY)) {
            val offsetX = if (travelEast) 1 else -1
            val offsetY = if (travelNorth) 0 else -1

            var scaledY = scaleUp(startY) + HALF_TILE + offsetY
            val tangent = scaleUp(deltaY) / abs(deltaX)

            var currX = startX
            while (currX != endX) {
                currX += offsetX
                val currY = scaleDown(scaledY)

                if (flags.isFlagged(baseX + currX, baseY + currY, xFlags)) {
                    return Route(coords, alternative = false, success = false)
                }

                scaledY += tangent

                val nextY = scaleDown(scaledY)
                if (nextY != currY && flags.isFlagged(baseX + currX, baseY + nextY, yFlags)) {
                    return Route(coords, alternative = false, success = false)
                }
            }
        } else {
            val offsetX = if (travelEast) 0 else -1
            val offsetY = if (travelNorth) 1 else -1

            var scaledX = scaleUp(startX) + HALF_TILE + offsetX
            val tangent = scaleUp(deltaX) / abs(deltaY)

            var currY = startY
            while (currY != endY) {
                currY += offsetY
                val currX = scaleDown(scaledX)

                if (flags.isFlagged(baseX + currX, baseY + currY, yFlags)) {
                    return Route(coords, alternative = false, success = false)
                }

                scaledX += tangent

                val nextX = scaleDown(scaledX)
                if (nextX != currX && flags.isFlagged(baseX + nextX, baseY + currY, xFlags)) {
                    return Route(coords, alternative = false, success = false)
                }
            }
        }
        return Route(coords, alternative = false, success = true)
    }

    private fun coordinate(a: Int, b: Int, size: Int): Int {
        return when {
            a >= b -> a
            a + size - 1 <= b -> a + size - 1
            else -> b
        }
    }

    private fun CollisionFlagMap.isFlagged(x: Int, y: Int, flags: Int): Boolean {
        return (this[x, y] and flags) != 0
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun IntArray.get(x: Int, y: Int): Int {
        val index = (y * searchMapSize) + x
        return this[index]
    }

    private companion object {

        private const val BLOCKED_NORTH = OBJECT_PROJECTILE_BLOCKER or WALL_NORTH_PROJECTILE_BLOCKER
        private const val BLOCKED_EAST = OBJECT_PROJECTILE_BLOCKER or WALL_EAST_PROJECTILE_BLOCKER
        private const val BLOCKED_SOUTH = OBJECT_PROJECTILE_BLOCKER or WALL_SOUTH_PROJECTILE_BLOCKER
        private const val BLOCKED_WEST = OBJECT_PROJECTILE_BLOCKER or WALL_WEST_PROJECTILE_BLOCKER

        private const val SCALE = 16
        private val HALF_TILE = scaleUp(tiles = 1) / 2

        private fun scaleUp(tiles: Int) = tiles shl SCALE

        private fun scaleDown(tiles: Int) = tiles ushr SCALE
    }
}
