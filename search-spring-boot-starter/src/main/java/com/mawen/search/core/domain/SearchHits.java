package com.mawen.search.core.domain;

import java.util.Iterator;
import java.util.List;

import com.mawen.search.core.aggregation.AggregationsContainer;
import com.mawen.search.core.query.TotalHitsRelation;

import org.springframework.data.util.Streamable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public interface SearchHits<T> extends Streamable<SearchHit<T>> {

	AggregationsContainer<?> getAggregations();

	float getMaxScore();

	SearchHit<T> getSearchHit(int index);

	List<SearchHit<T>> getSearchHits();

	long getTotalHits();

	TotalHitsRelation getTotalHitsRelation();

	default boolean hasAggregations() {
		return getAggregations() != null;
	}

	default boolean hasSearchHits() {
		return !getSearchHits().isEmpty();
	}

	default Iterator<SearchHit<T>> iterator() {
		return getSearchHits().iterator();
	}
}
