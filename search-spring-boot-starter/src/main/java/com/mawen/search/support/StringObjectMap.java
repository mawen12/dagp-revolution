package com.mawen.search.support;

import java.util.Map;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public interface StringObjectMap<M extends StringObjectMap<M>> extends Map<String, Object> {

	default M append(String key, Object value) {

		Assert.notNull(key, "Key must not be null");

		put(key, value);
		return (M)this;
	}

	default <T> T get(Object key, Class<T> type) {

		Assert.notNull(key, "Key must not be null");
		Assert.notNull(type, "Type must not be null");

		return type.cast(get(key));
	}

	default Boolean getBoolean(String key) {
		return get(key, Boolean.class);
	}

	default Integer getInt(String key) {
		return get(key, Integer.class);
	}

	default Long getLong(String key) {
		return get(key, Long.class);
	}

	default String getString(String key) {
		return get(key, String.class);
	}

	String toJson();

	M fromJson(String json);
}
