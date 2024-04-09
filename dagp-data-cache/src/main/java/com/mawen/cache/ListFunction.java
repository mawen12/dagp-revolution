package com.mawen.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/4/4
 */
@FunctionalInterface
public interface ListFunction<T, R> extends SingleFunction<List<T>, List<R>> {

	static <T, R> Function<List<T>, List<R>> of(ListFunction<T, R> getter, Function<List<T>, List<R>> fallbackGetter, Function<R, T> uniqueKeyGetter) {
		if (getter == null) {
			throw new IllegalArgumentException("ListFunction Getter must not be null");
		}
		if (fallbackGetter == null) {
			throw new IllegalArgumentException("Fallback Getter must not be null");
		}
		if (uniqueKeyGetter == null) {
			throw new IllegalArgumentException("UniqueKey Getter must not be null");
		}
		return getter.compose(fallbackGetter, uniqueKeyGetter);
	}

	static <T, R> Function<List<T>, List<R>> ofMap(Function<List<T>, List<R>> fallbackGetter, Function<R, T> uniqueKeyGetter) {
		MapCacheListFunction<T, R> mapCacheListFunction = new MapCacheListFunction<>();
		return of(mapCacheListFunction, fallbackGetter, uniqueKeyGetter);
	}

	default Function<List<T>, List<R>> compose(Function<List<T>, List<R>> fallbackGetter, Function<R, T> uniqueKeyGetter) {
		return keys -> {
			List<R> cacheResult = apply(keys);

			if (cacheResult == null) {
				cacheResult = new ArrayList<>();
			}
			else if (cacheResult.size() == keys.size()) {
				return cacheResult;
			}

			Set<T> cacheKeys = cacheResult.stream().map(uniqueKeyGetter).collect(Collectors.toSet());
			List<T> nonExistsInCacheKeys = keys.stream().filter(key -> !cacheKeys.contains(key)).collect(Collectors.toList());

			List<R> fallbackResult = fallbackGetter.apply(nonExistsInCacheKeys);

			return Stream.of(cacheResult, fallbackResult).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
		};
	}
}
