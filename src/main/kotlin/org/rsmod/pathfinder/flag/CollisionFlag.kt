package org.rsmod.pathfinder.flag

@Suppress("UNUSED", "MemberVisibilityCanBePrivate")
public object CollisionFlag {

    public const val WALL_NORTH_WEST: Int = 0x1
    public const val WALL_NORTH: Int = 0x2
    public const val WALL_NORTH_EAST: Int = 0x4
    public const val WALL_EAST: Int = 0x8
    public const val WALL_SOUTH_EAST: Int = 0x10
    public const val WALL_SOUTH: Int = 0x20
    public const val WALL_SOUTH_WEST: Int = 0x40
    public const val WALL_WEST: Int = 0x80
    public const val OBJECT: Int = 0x100
    public const val WALL_NORTH_WEST_PROJECTILE_BLOCKER: Int = 0x200
    public const val WALL_NORTH_PROJECTILE_BLOCKER: Int = 0x400
    public const val WALL_NORTH_EAST_PROJECTILE_BLOCKER: Int = 0x800
    public const val WALL_EAST_PROJECTILE_BLOCKER: Int = 0x1000
    public const val WALL_SOUTH_EAST_PROJECTILE_BLOCKER: Int = 0x2000
    public const val WALL_SOUTH_PROJECTILE_BLOCKER: Int = 0x4000
    public const val WALL_SOUTH_WEST_PROJECTILE_BLOCKER: Int = 0x8000
    public const val WALL_WEST_PROJECTILE_BLOCKER: Int = 0x10000
    public const val OBJECT_PROJECTILE_BLOCKER: Int = 0x20000
    public const val FLOOR_DECORATION: Int = 0x40000
    public const val UNKNOWN_BIT_20: Int = 0x80000

    /*private public const val NOT_IN_USE_BIT_21: Int = 0x100000 - allegedly for non-water tiles, not in use by intelligent pathfinder*/
    public const val FLOOR: Int = 0x200000
    public const val WALL_NORTH_WEST_ROUTE_BLOCKER: Int = 0x400000
    public const val WALL_NORTH_ROUTE_BLOCKER: Int = 0x800000
    public const val WALL_NORTH_EAST_ROUTE_BLOCKER: Int = 0x1000000
    public const val WALL_EAST_ROUTE_BLOCKER: Int = 0x2000000
    public const val WALL_SOUTH_EAST_ROUTE_BLOCKER: Int = 0x4000000
    public const val WALL_SOUTH_ROUTE_BLOCKER: Int = 0x8000000
    public const val WALL_SOUTH_WEST_ROUTE_BLOCKER: Int = 0x10000000
    public const val WALL_WEST_ROUTE_BLOCKER: Int = 0x20000000
    public const val OBJECT_ROUTE_BLOCKER: Int = 0x40000000
    /*private public const val NOT_IN_USE_BIT_31: Int = 0x80000000 - block player*/

    /* Mixed masks of the above flags below */
    public const val BLOCK_WEST: Int = WALL_EAST or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_EAST: Int = WALL_WEST or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_SOUTH: Int = WALL_NORTH or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_NORTH: Int = WALL_SOUTH or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_SOUTH_WEST: Int = WALL_NORTH or WALL_NORTH_EAST or
        WALL_EAST or OBJECT or FLOOR_DECORATION or UNKNOWN_BIT_20 or FLOOR or
        WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_SOUTH_EAST: Int = WALL_NORTH_WEST or WALL_NORTH or
        WALL_WEST or OBJECT or FLOOR_DECORATION or UNKNOWN_BIT_20 or FLOOR or
        WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_NORTH_WEST: Int = WALL_EAST or WALL_SOUTH_EAST or
        WALL_SOUTH or OBJECT or FLOOR_DECORATION or UNKNOWN_BIT_20 or FLOOR or
        WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_NORTH_EAST: Int = WALL_SOUTH or WALL_SOUTH_WEST or
        WALL_WEST or OBJECT or FLOOR_DECORATION or UNKNOWN_BIT_20 or FLOOR or
        WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_NORTH_AND_SOUTH_EAST: Int = WALL_NORTH or WALL_NORTH_EAST or
        WALL_EAST or WALL_SOUTH_EAST or WALL_SOUTH or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_NORTH_AND_SOUTH_WEST: Int = WALL_NORTH_WEST or WALL_NORTH or
        WALL_SOUTH or WALL_SOUTH_WEST or WALL_WEST or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_NORTH_EAST_AND_WEST: Int = WALL_NORTH_WEST or WALL_NORTH or
        WALL_NORTH_EAST or WALL_EAST or WALL_WEST or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER

    public const val BLOCK_SOUTH_EAST_AND_WEST: Int = WALL_EAST or WALL_SOUTH_EAST or
        WALL_SOUTH or WALL_SOUTH_WEST or WALL_WEST or OBJECT or FLOOR_DECORATION or
        UNKNOWN_BIT_20 or FLOOR or WALL_NORTH_EAST_ROUTE_BLOCKER
}
