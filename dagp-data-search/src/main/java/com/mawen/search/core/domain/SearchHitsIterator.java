package com.mawen.search.core.domain;

import com.mawen.search.core.aggregation.AggregationsContainer;
import com.mawen.search.core.query.TotalHitsRelation;

import org.springframework.data.util.CloseableIterator;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface SearchHitsIterator<T> extends CloseableIterator<SearchHit<T>> {

	AggregationsContainer<?> getAggregations();

	float getMaxScore();

	long getTotalHits();

	TotalHitsRelation getTotalHitsRelation();

	default boolean hasAggregations() {
		return getAggregations() != null;
	}

}
