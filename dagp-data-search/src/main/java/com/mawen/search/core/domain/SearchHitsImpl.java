package com.mawen.search.core.domain;

import com.mawen.search.core.aggregation.AggregationsContainer;
import com.mawen.search.core.query.TotalHitsRelation;
import lombok.Getter;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
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

	public SearchHitsImpl(long totalHits, TotalHitsRelation totalHitsRelation, float maxScore, @Nullable String scrollId,
			List<? extends SearchHit<T>> searchHits,
			@Nullable AggregationsContainer<?> aggregations) {

		Assert.notNull(searchHits, "searchHits must not be null");

		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.searchHits = searchHits;
		this.aggregations = aggregations;
		this.unmodifiableSearchHits = Lazy.of(() -> Collections.unmodifiableList(searchHits));
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
	public String toString() {
		return "SearchHits{" + //
				"totalHits=" + totalHits + //
				", totalHitsRelation=" + totalHitsRelation + //
				", maxScore=" + maxScore + //
				", scrollId='" + scrollId + '\'' + //
				", searchHits={" + searchHits.size() + " elements}" + //
				", aggregations=" + aggregations + //
				'}';
	}

}
