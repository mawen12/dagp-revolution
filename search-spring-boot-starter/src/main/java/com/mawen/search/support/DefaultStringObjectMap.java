package com.mawen.search.support;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class DefaultStringObjectMap<T extends StringObjectMap<T>> implements StringObjectMap<T> {

	static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private final LinkedHashMap<String, Object> delegate;

	public DefaultStringObjectMap() {
		this(new LinkedHashMap<>());
	}

	public DefaultStringObjectMap(Map<String, ? extends Object> map) {
		this.delegate = new LinkedHashMap<>(map);
	}

	@Override
	public String toJson() {
		try {
			return OBJECT_MAPPER.writeValueAsString(this);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cannot render document to JSON", e);
		}
	}

	@Override
	public T fromJson(String json) {

		Assert.notNull(json, "JSON must not be null");

		delegate.clear();
		try {
			delegate.putAll(OBJECT_MAPPER.readerFor(Map.class).readValue(json));
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cannot parse JSON", e);
		}
		return (T)this;
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return delegate.get(key);
	}

	@Override
	public Object getOrDefault(Object key, Object defaultValue) {
		return delegate.getOrDefault(key, defaultValue);
	}

	@Override
	public Object put(String key, Object value) {
		return delegate.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<String> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<Object> values() {
		return delegate.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public String toString() {
		return "DefaultStringObjectMap: " + toJson();
	}
}
