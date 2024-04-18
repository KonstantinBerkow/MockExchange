package io.github.konstantinberkow.mockexchange.flow_extensions

class ToPair<T1, T2> : suspend (T1, T2) -> Pair<T1, T2> {

    override suspend fun invoke(first: T1, second: T2): Pair<T1, T2> {
        return first to second
    }
}
