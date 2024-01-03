package com.mawen.search.core.document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mawen.search.support.StringObjectMap;

import org.springframework.data.mapping.MappingException;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface Document extends StringObjectMap<Document> {

	static Document create() {
		return new MapDocument();
	}

	static Document from(Map<String, ? extends Object> map) {

		Assert.notNull(map, "Map must not be null");

		if (map instanceof LinkedHashMap) {
			return new MapDocument(map);
		}

		return new MapDocument(new LinkedHashMap<>(map));
	}

	static Document parse(String json) {

		Assert.notNull(json, "JSON must not be null");

		return new MapDocument().fromJson(json);
	}

	default Document fromJson(String json) {

		Assert.notNull(json, "JSON must not be null");

		clear();

		try {
			putAll(MapDocument.OBJECT_MAPPER.readerFor(Map.class).readValue(json));
		}
		catch (JsonProcessingException e) {
			throw new MappingException("Cannot parse JSON", e);
		}
		return this;
	}

	default boolean hasId() {
		return false;
	}

	default String getIndex() {
		return null;
	}

	default void setIndex(String index) {
		throw new UnsupportedOperationException();
	}

	default String getId() {
		throw new UnsupportedOperationException();
	}

	default void setId(String id) {
		throw new UnsupportedOperationException();
	}

	default boolean hasVersion() {
		return false;
	}

	default long getVersion() {
		throw new UnsupportedOperationException();
	}

	default void setVersion(long version) {
		throw new UnsupportedOperationException();
	}

	default boolean hasSeqNo() {
		return false;
	}

	default long getSeqNo() {
		throw new UnsupportedOperationException();
	}

	default void setSeqNo(long seqNo) {
		throw new UnsupportedOperationException();
	}

	default boolean hasPrimaryTerm() {
		return false;
	}

	default long getPrimaryTerm() {
		throw new UnsupportedOperationException();
	}

	default void setPrimaryTerm(long primaryTerm) {
		throw new UnsupportedOperationException();
	}

	default <R> R transform(Function<? super Document, ? extends R> transformer) {

		Assert.notNull(transformer, "transformer must not be null");

		return transformer.apply(this);
	}
}
