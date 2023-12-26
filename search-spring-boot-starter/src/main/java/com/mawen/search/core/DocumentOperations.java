package com.mawen.search.core;

import java.util.List;

import com.mawen.search.core.domain.BulkOptions;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.ByQueryResponse;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.UpdateQuery;
import com.mawen.search.core.query.UpdateResponse;
import com.mawen.search.core.support.IndexedObjectInformation;
import com.mawen.search.core.support.MultiGetItem;

import org.springframework.lang.Nullable;

/**
 * The operations for the Elasticsearch Document APIs
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface DocumentOperations {

	<T> T save(T entity);

	<T> T save(T entity, IndexCoordinates index);

	<T> Iterable<T> save(Iterable<T> entities);

	<T> Iterable<T> save(Iterable<T> entities, IndexCoordinates index);

	<T> Iterable<T> save(T... entities);

	String index(IndexQuery query, IndexCoordinates index);

	@Nullable
	<T> T get(String id, Class<T> clazz);

	@Nullable
	<T> T get(String id, Class<T> clazz, IndexCoordinates index);

	<T> List<MultiGetItem<T>> multiGet(Query query, Class<T> clazz);

	<T> List<MultiGetItem<T>> multiGet(Query query, Class<T> clazz, IndexCoordinates index);

	boolean exists(String id, Class<?> clazz);

	boolean exists(String id, IndexCoordinates index);

	List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries);

	default List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, Class<?> clazz) {
		return bulkIndex(queries, BulkOptions.defaultOptions(), clazz);
	}

	default List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, IndexCoordinates index) {
		return bulkIndex(queries, BulkOptions.defaultOptions(), index);
	}

	List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOperation, Class<?> clazz);

	List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOperation, IndexCoordinates index);

	default void bulkUpdate(List<UpdateQuery> queries, IndexCoordinates index) {
		bulkUpdate(queries, BulkOptions.defaultOptions(), index);
	}

	void bulkUpdate(List<UpdateQuery> queries, Class<?> clazz);

	void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index);

	String delete(String id, IndexCoordinates index);

	String delete(String id, Class<?> entityType);

	String delete(Object entity);

	String delete(Object entity, IndexCoordinates index);

	ByQueryResponse delete(Query query, Class<?> clazz);

	ByQueryResponse delete(Query query, Class<?> clazz, IndexCoordinates index);

	<T> UpdateResponse update(T entity);

	<T> UpdateResponse update(T entity, IndexCoordinates index);

	<T> UpdateResponse update(UpdateQuery updateQuery, IndexCoordinates index);

	ByQueryResponse updateByQuery(UpdateQuery updateQuery, IndexCoordinates index);
}
