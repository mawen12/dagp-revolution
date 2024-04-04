package com.mawen.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/4/4
 */
public class MapCacheListFunction<T, R> extends CacheableListFunction<T, R> {

	private final Map<T, R> cache;


	MapCacheListFunction() {
		this(new HashMap<>());
	}

	MapCacheListFunction(Map<T, R> map) {
		if (map == null) {
			throw new IllegalArgumentException("map must not be null");
		}
		this.cache = map;
	}

	MapCacheListFunction(Supplier<Map<T, R>> mapSupplier) {
		if (mapSupplier == null) {
			throw new IllegalArgumentException("Map Supplier must not be null");
		}
		Map<T, R> map = mapSupplier.get();
		if (map == null) {
			throw new IllegalArgumentException("map must not be null");
		}
		this.cache = map;
	}


	@Override
	public List<R> apply(List<T> ts) {
		if (!cache.isEmpty()) {
			return ts.stream().map(cache::get).filter(Objects::nonNull).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	@Override
	List<R> cache(List<R> fallbackResult, Function<R, T> uniqueKeyGetter) {
		fallbackResult.forEach(it -> cache.put(uniqueKeyGetter.apply(it), it));
		return fallbackResult;
	}
}
