package com.mawen.search.core.query;

import java.util.Collections;
import java.util.List;

import com.mawen.search.core.aggregation.AggregationsContainer;

import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class SearchHitsImpl<T> implements SearchScrollHits<T> {

	private final long totalHits;
	private final TotalHitsRelation totalHitsRelation;
	private final float maxScore;
	@Nullable
	private final String scrollId;
	private final List<? extends SearchHit<T>> searchHits;
	private final Lazy<List<SearchHit<T>>> unmodifiableSearchHits;
	@Nullable
	private final AggregationsContainer<?> aggregations;
	@Nullable
	private String pointInTimeId;

	/**
	 * @param totalHits         the number of total hits for the search
	 * @param totalHitsRelation the relation {@see TotalHitsRelation}, must not be {@literal null}
	 * @param maxScore          the maximum score
	 * @param scrollId          the scroll id if available
	 * @param searchHits        must not be {@literal null}
	 * @param aggregations      the aggregations if available
	 */
	public SearchHitsImpl(long totalHits, TotalHitsRelation totalHitsRelation, float maxScore, @Nullable String scrollId,
			@Nullable String pointInTimeId, List<? extends SearchHit<T>> searchHits,
			@Nullable AggregationsContainer<?> aggregations) {

		Assert.notNull(searchHits, "searchHits must not be null");

		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.pointInTimeId = pointInTimeId;
		this.searchHits = searchHits;
		this.aggregations = aggregations;
		this.unmodifiableSearchHits = Lazy.of(() -> Collections.unmodifiableList(searchHits));
	}

	// region getter
	@Override
	public long getTotalHits() {
		return totalHits;
	}

	@Override
	public TotalHitsRelation getTotalHitsRelation() {
		return totalHitsRelation;
	}

	@Override
	public float getMaxScore() {
		return maxScore;
	}

	@Override
	@Nullable
	public String getScrollId() {
		return scrollId;
	}

	@Override
	public List<SearchHit<T>> getSearchHits() {
		return unmodifiableSearchHits.get();
	}

	@Override
	public SearchHit<T> getSearchHit(int index) {
		return searchHits.get(index);
	}

	@Override
	@Nullable
	public AggregationsContainer<?> getAggregations() {
		return aggregations;
	}

	@Nullable
	@Override
	public String getPointInTimeId() {
		return pointInTimeId;
	}

	@Override
	public String toString() {
		return "SearchHits{" + //
				"totalHits=" + totalHits + //
				", totalHitsRelation=" + totalHitsRelation + //
				", maxScore=" + maxScore + //
				", scrollId='" + scrollId + '\'' + //
				", pointInTimeId='" + pointInTimeId + '\'' + //
				", searchHits={" + searchHits.size() + " elements}" + //
				", aggregations=" + aggregations + //
				'}';
	}

}
