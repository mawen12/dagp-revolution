package com.mawen.search.core.domain;

import com.mawen.search.core.document.NestedMetaData;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class SearchHit<T> {

	@Nullable
	private final String index;
	@Nullable
	private final String id;
	private final float score;
	private final List<Object> sortValues;
	private final T content;
	private final Map<String, List<String>> highlightFields = new LinkedHashMap<>();
	private final Map<String, SearchHits<?>> innerHits = new LinkedHashMap<>();
	@Nullable
	private final NestedMetaData nestedMetaData;
	@Nullable
	private final String routing;
	private final List<String> matchedQueries = new ArrayList<>();

	public SearchHit(@Nullable String index, @Nullable String id, @Nullable String routing, float score,
			@Nullable Object[] sortValues, @Nullable Map<String, List<String>> highlightFields,
			@Nullable Map<String, SearchHits<?>> innerHits, @Nullable NestedMetaData nestedMetaData,
			@Nullable List<String> matchedQueries, T content) {
		this.index = index;
		this.id = id;
		this.routing = routing;
		this.score = score;
		this.sortValues = (sortValues != null) ? Arrays.asList(sortValues) : new ArrayList<>();

		if (highlightFields != null) {
			this.highlightFields.putAll(highlightFields);
		}

		if (innerHits != null) {
			this.innerHits.putAll(innerHits);
		}

		this.nestedMetaData = nestedMetaData;
		this.content = content;

		if (matchedQueries != null) {
			this.matchedQueries.addAll(matchedQueries);
		}
	}

	public List<Object> getSortValues() {
		return Collections.unmodifiableList(sortValues);
	}

	public Map<String, List<String>> getHighlightFields() {
		return Collections.unmodifiableMap(highlightFields.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.unmodifiableList(entry.getValue()))));
	}

	public List<String> getHighlightField(String field) {

		Assert.notNull(field, "field must not be null");

		return Collections.unmodifiableList(highlightFields.getOrDefault(field, Collections.emptyList()));
	}

	public SearchHits<?> getInnerHits(String name) {
		return innerHits.get(name);
	}

	@Override
	public String toString() {
		return "SearchHit{" + "id='" + id + '\'' + ", score=" + score + ", sortValues=" + sortValues + ", content="
				+ content + ", highlightFields=" + highlightFields + '}';
	}
}
