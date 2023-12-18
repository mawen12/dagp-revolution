package com.mawen.search.core;

import java.time.Duration;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.BaseQueryBuilder;
import com.mawen.search.core.query.Query;

import org.springframework.lang.Nullable;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public interface SearchOperations {

	default long count(Query query, IndexCoordinates index) {
		return count(query, null, index);
	}

	long count(Query query, Class<?> clazz);

	long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index);

	default <T> SearchHit<T> searchOne(Query query, Class<T> clazz) {
		List<SearchHit<T>> content = search(query, clazz).getSearchHits();
		return content.isEmpty() ? null : content.get(0);
	}

	default <T> SearchHit<T> searchOne(Query query, Class<T> clazz, IndexCoordinates index) {
		List<SearchHit<T>> content = search(query, clazz, index).getSearchHits();
		return content.isEmpty() ? null : content.get(0);
	}

	<T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz);

	<T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index);

	List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes);

	List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes, IndexCoordinates index);

	List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes, List<IndexCoordinates> indexes);

	<T> SearchHits<T> search(Query query, Class<T> clazz);

	<T> SearchHits<T> search(Query query, Class<T> clazz, IndexCoordinates index);

	<T> SearchHits<T> search(MoreLikeThisQuery query, Class<T> clazz);

	<T> SearchHits<T> search(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index);

	<T> SearchHitsIterator<T> searchForStream(Query query, Class<T> clazz);

	<T> SearchHitsIterator<T> searchForStream(Query query, Class<T> clazz, IndexCoordinates index);

	Query matchAllQuery();

	Query idsQuery(List<String> ids);

	BaseQueryBuilder queryBuilderWithIds(List<String> ids);

	default String openPointInTime(IndexCoordinates index, Duration keepAlive) {
		return openPointInTime(index,keepAlive,false);
	}

	String openPointInTime(IndexCoordinates index, Duration keepAlive, Boolean ignoreUnavailable);

	Boolean closePointInTime(String pit);

}
