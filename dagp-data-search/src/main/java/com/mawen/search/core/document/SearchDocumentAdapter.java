package com.mawen.search.core.document;

import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class SearchDocumentAdapter implements SearchDocument {
	private final float score;
	private final Object[] sortValues;
	private final Map<String, List<Object>> fields = new HashMap<>();
	private final Document delegate;
	private final Map<String, List<String>> highlightFields = new HashMap<>();
	private final Map<String, SearchDocumentResponse> innerHits = new HashMap<>();
	@Nullable
	private final NestedMetaData nestedMetaData;
	@Nullable
	private final List<String> matchedQueries;
	@Nullable
	private final String routing;

	public SearchDocumentAdapter(Document delegate, float score, Object[] sortValues, Map<String, List<Object>> fields,
			Map<String, List<String>> highlightFields, Map<String, SearchDocumentResponse> innerHits,
			@Nullable NestedMetaData nestedMetaData, @Nullable List<String> matchedQueries, @Nullable String routing) {

		this.delegate = delegate;
		this.score = score;
		this.sortValues = sortValues;
		this.fields.putAll(fields);
		this.highlightFields.putAll(highlightFields);
		this.innerHits.putAll(innerHits);
		this.nestedMetaData = nestedMetaData;
		this.matchedQueries = matchedQueries;
		this.routing = routing;
	}

	@Override
	public SearchDocument append(String key, Object value) {
		delegate.append(key, value);

		return this;
	}

	@Override
	public Map<String, List<Object>> getFields() {
		return fields;
	}

	@Override
	public String getIndex() {
		return delegate.getIndex();
	}

	@Override
	public boolean hasId() {
		return delegate.hasId();
	}

	@Override
	public void setId(String id) {
		delegate.setId(id);
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public boolean hasVersion() {
		return delegate.hasVersion();
	}

	@Override
	public long getVersion() {
		return delegate.getVersion();
	}

	@Override
	public void setVersion(long version) {
		delegate.setVersion(version);
	}

	@Override
	public boolean hasSeqNo() {
		return delegate.hasSeqNo();
	}

	@Override
	public long getSeqNo() {
		return delegate.getSeqNo();
	}

	@Override
	public void setSeqNo(long seqNo) {
		delegate.setSeqNo(seqNo);
	}

	@Override
	public boolean hasPrimaryTerm() {
		return delegate.hasPrimaryTerm();
	}

	@Override
	public long getPrimaryTerm() {
		return delegate.getPrimaryTerm();
	}

	@Override
	public void setPrimaryTerm(long primaryTerm) {
		delegate.setPrimaryTerm(primaryTerm);
	}

	@Override
	@Nullable
	public <T> T get(Object key, Class<T> type) {
		return delegate.get(key, type);
	}

	@Override
	public String toJson() {
		return delegate.toJson();
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

		if (delegate.containsKey(key)) {
			return delegate.get(key);
		}

		// fallback to fields
		return fields.get(key);
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SearchDocumentAdapter)) {
			return false;
		}

		SearchDocumentAdapter that = (SearchDocumentAdapter) o;
		return Float.compare(that.score, score) == 0 && delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super Object> action) {
		delegate.forEach(action);
	}

	@Override
	public boolean remove(Object key, Object value) {
		return delegate.remove(key, value);
	}

	@Override
	public String toString() {

		String id = hasId() ? getId() : "?";
		String version = hasVersion() ? Long.toString(getVersion()) : "?";

		return getClass().getSimpleName() + '@' + id + '#' + version + ' ' + toJson();
	}


}
