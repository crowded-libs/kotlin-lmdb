package lmdb

internal enum class CursorOption(val option: UInt) {
    FIRST(0u),
    FIRST_DUP(1u),
    GET_BOTH(2u),
    GET_BOTH_RANGE(3u),
    GET_CURRENT(4u),
    GET_MULTIPLE(5u),
    LAST(6u),
    LAST_DUP(7u),
    NEXT(8u),
    NEXT_DUP(9u),
    NEXT_MULTIPLE(10u),
    NEXT_NODUP(11u),
    PREV(12u),
    PREV_DUP(13u),
    PREV_NODUP(14u),
    SET(15u),
    SET_KEY(16u),
    SET_RANGE(17u);
}

internal enum class CursorDeleteOption(val option: UInt) {
    NONE(0u),
    NO_DUP_DATA(0x20u)
}

internal enum class CursorPutOption(val option: UInt) {
    NONE(0u),
    NODUPDATA(0x20u),
    CURRENT(0x40u),
    NOOVERWRITE(0x10u),
    RESERVE(0x10000u),
    APPEND(0x20000u),
    APPENDDUP(0x40000u),
    MULTIPLE(0x80000u)
}