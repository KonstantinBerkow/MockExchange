package io.github.konstantinberkow.mockexchange.entity

const val CentsInUnit: UInt = 100u

fun UInt.toUnit(): UInt {
    return this / CentsInUnit
}

fun UInt.toCents(): UInt {
    return this * CentsInUnit
}
