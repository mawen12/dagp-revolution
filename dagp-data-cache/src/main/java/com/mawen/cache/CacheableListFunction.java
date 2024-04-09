package com.mawen.cache;

import java.util.List;
import java.util.function.Function;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/4/4
 */
public abstract class CacheableListFunction<T, R> implements ListFunction<T, R> {

	abstract List<R> cache(List<R> fallbackResult, Function<R, T> uniqueKeyGetter);

	@Override
	public Function<List<T>, List<R>> compose(Function<List<T>, List<R>> fallbackGetter, Function<R, T> uniqueKeyGetter) {
		return ListFunction.super.compose(fallbackGetter.andThen(ret -> cache(ret, uniqueKeyGetter)), uniqueKeyGetter);
	}
}
