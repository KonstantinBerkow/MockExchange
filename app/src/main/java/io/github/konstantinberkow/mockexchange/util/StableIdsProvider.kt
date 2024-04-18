package io.github.konstantinberkow.mockexchange.util

class StableIdsProvider<in T : Any> {

    private var serialId: Long = 0

    private val savedIds = mutableMapOf<T, Long>()

    fun getIdFor(item: T): Long {
        return savedIds.getOrPut(item) {
            ++serialId
        }
    }
}