package com.mawen.search.core.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mawen.search.support.DefaultStringObjectMap;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class MapDocument implements Document {

	static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final DefaultStringObjectMap<?> delegate;

	@Nullable
	private String index;
	@Nullable
	private String id;
	@Nullable
	private Long version;
	@Nullable
	private Long seqNo;
	@Nullable
	private Long primaryTerm;

	MapDocument() {
		this(new LinkedHashMap<>());
	}

	MapDocument(Map<String, ?> delegate) {
		this.delegate = new DefaultStringObjectMap<>(delegate);
	}

	@Override
	public boolean hasId() {
		return this.id != null;
	}

	@Override
	public String getId() {

		Assert.isTrue(hasId(), "No Id associated with this Document");

		return this.id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getIndex() {
		return this.index;
	}

	@Override
	public void setIndex(String index) {
		this.index = index;
	}

	@Override
	public boolean hasVersion() {
		return this.version != null;
	}

	@Override
	public long getVersion() {

		Assert.isTrue(hasVersion(), "No version associated with this Document");

		return this.version;
	}

	@Override
	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public boolean hasSeqNo() {
		return this.seqNo != null;
	}

	@Override
	public long getSeqNo() {

		Assert.isTrue(hasSeqNo(), "No seq_no associated with this Document");

		return this.seqNo;
	}

	@Override
	public void setSeqNo(long seqNo) {
		this.seqNo = seqNo;
	}

	@Override
	public boolean hasPrimaryTerm() {
		return this.primaryTerm != null;
	}

	@Override
	public long getPrimaryTerm() {

		Assert.isTrue(hasPrimaryTerm(), "No primary_term associated with this Document");

		return this.primaryTerm;
	}

	@Override
	public void setPrimaryTerm(long primaryTerm) {
		this.primaryTerm = primaryTerm;
	}

	@Override
	public String toJson() {
		try {
			return OBJECT_MAPPER.writeValueAsString(this);
		}
		catch (JsonProcessingException e) {
			throw new MappingException("Cannot render document to JSON", e);
		}
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

		String id = hasId() ? getId() : "?";
		String version = hasVersion() ? Long.toString(getVersion()) : "?";

		return getClass().getSimpleName() + '@' + id + '#' + version + ' ' + toJson();
	}
}
