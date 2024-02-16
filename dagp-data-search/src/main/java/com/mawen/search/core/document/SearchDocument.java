package com.mawen.search.core.document;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface SearchDocument extends Document {

	float getScore();

	Map<String, List<Object>> getFields();

	@Nullable
	default <V> V getFieldValue(final String name) {
		List<Object> values = getFields().get(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return (V) values.get(0);
	}

	@Nullable
	default Object[] getSortValues() {
		return null;
	}

	@Nullable
	default Map<String, List<String>> getHighlightFields() {
		return null;
	}

	@Nullable
	default Map<String, SearchDocumentResponse> getInnerHits() {
		return null;
	}

	@Nullable
	default NestedMetaData getNestedMetaData() {
		return null;
	}

	@Nullable
	default String getRouting() {
		return getFieldValue("_routing");
	}

	@Nullable
	List<String> getMatchedQueries();
}
