package com.mawen.search.core.query;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mawen.search.client.util.ScrollState;
import com.mawen.search.core.aggregation.AggregationsContainer;
import com.mawen.search.core.domain.PitSearchAfterHits;
import com.mawen.search.core.domain.PointInTime;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHitsIterator;
import com.mawen.search.core.domain.SearchScrollHits;
import lombok.Getter;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Deprecated
public abstract class StreamQueries {

	// utility constructor
	private StreamQueries() {
	}

	public static <T> SearchHitsIterator<T> streamResults(int maxCount, SearchScrollHits<T> searchHits,
			Function<String, SearchScrollHits<T>> continueScrollFunction, Consumer<List<String>> clearScrollConsumer) {

		Assert.notNull(searchHits, "searchHits must not be null.");
		Assert.notNull(searchHits.getScrollId(), "scrollId of searchHits must not be null.");
		Assert.notNull(continueScrollFunction, "continueScrollFunction must not be null.");
		Assert.notNull(clearScrollConsumer, "clearScrollConsumer must not be null.");

		AggregationsContainer<?> aggregations = searchHits.getAggregations();
		float maxScore = searchHits.getMaxScore();
		long totalHits = searchHits.getTotalHits();
		TotalHitsRelation totalHitsRelation = searchHits.getTotalHitsRelation();

		return new SearchHitsIterator() {

			private volatile AtomicInteger currentCount = new AtomicInteger();
			private volatile Iterator<SearchHit<T>> currentScrollHits = searchHits.iterator();
			private volatile boolean continueScroll = currentScrollHits.hasNext();
			private volatile ScrollState scrollState = new ScrollState(searchHits.getScrollId());
			private volatile boolean isClosed = false;

			@Override
			public void close() {
				if (!isClosed) {
					clearScrollConsumer.accept(scrollState.getScrollIds());
					isClosed = true;
				}
			}

			@Override
			@Nullable
			public AggregationsContainer<?> getAggregations() {
				return aggregations;
			}

			@Override
			public float getMaxScore() {
				return maxScore;
			}

			@Override
			public long getTotalHits() {
				return totalHits;
			}

			@Override
			public TotalHitsRelation getTotalHitsRelation() {
				return totalHitsRelation;
			}

			@Override
			public boolean hasNext() {

				boolean hasNext = false;

				if (!isClosed && continueScroll && (maxCount <= 0 || currentCount.get() < maxCount)) {

					if (!currentScrollHits.hasNext()) {
						SearchScrollHits<T> nextPage = continueScrollFunction.apply(scrollState.getScrollId());
						currentScrollHits = nextPage.iterator();
						scrollState.updateScrollId(nextPage.getScrollId());
						continueScroll = currentScrollHits.hasNext();
					}
					hasNext = currentScrollHits.hasNext();
				}

				if (!hasNext) {
					close();
				}

				return hasNext;
			}

			@Override
			public SearchHit<T> next() {
				if (hasNext()) {
					currentCount.incrementAndGet();
					return currentScrollHits.next();
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static <T> SearchHitsIterator<T> streamResults(int maxCount, PitSearchAfterHits<T> searchHits,
	                                                      Function<BaseQuery, PitSearchAfterHits<T>> continueSearchFunction, Consumer<String> clearPitConsumer) {

		Assert.notNull(searchHits,"searchHits must not be null");
		Assert.notNull(searchHits.getPit(),"pit of searchHits must not be null");
		Assert.notNull(continueSearchFunction,"continueSearchFunction must not be null");
		Assert.notNull(clearPitConsumer,"clearPitConsumer must not be null");

		AggregationsContainer<?> aggregations = searchHits.getAggregations();
		float maxScore = searchHits.getMaxScore();
		long totalHits = searchHits.getTotalHits();
		TotalHitsRelation totalHitsRelation = searchHits.getTotalHitsRelation();

		return new SearchHitsIterator<T>() {

			private volatile AtomicInteger currentCount = new AtomicInteger();
			private volatile Iterator<SearchHit<T>> currentSearchAfterHits = searchHits.iterator();
			private volatile boolean continueSearch = currentSearchAfterHits.hasNext();
			private volatile PitSearchAfter pitSearchAfter = new PitSearchAfter(searchHits.getQuery(), searchHits.getPit());
			private volatile boolean isClosed = false;

			@Override
			public AggregationsContainer<?> getAggregations() {
				return aggregations;
			}

			@Override
			public float getMaxScore() {
				return maxScore;
			}

			@Override
			public long getTotalHits() {
				return totalHits;
			}

			@Override
			public TotalHitsRelation getTotalHitsRelation() {
				return totalHitsRelation;
			}

			@Override
			public void close() {
				if (!isClosed) {
					clearPitConsumer.accept(pitSearchAfter.getPit());
					isClosed = true;
				}
			}

			@Override
			public boolean hasNext() {

				boolean hasNext = false;

				if (!isClosed && continueSearch && (maxCount <= 0 || currentCount.get() < maxCount)) {

					if (!currentSearchAfterHits.hasNext()) {
						PitSearchAfterHits<T> nextPage = continueSearchFunction.apply(pitSearchAfter.getBaseQuery());
						List<SearchHit<T>> hits = nextPage.getSearchHits();
						if (!CollectionUtils.isEmpty(hits)) {
							List<Object> sortOptions = hits.get(hits.size() - 1).getSortValues();
							pitSearchAfter.getBaseQuery().setSearchAfter(sortOptions);
							pitSearchAfter.getBaseQuery().setPointInTime(new PointInTime(pitSearchAfter.getPit(), Duration.ofMinutes(5)));
						}
						currentSearchAfterHits = nextPage.iterator();
						continueSearch = currentSearchAfterHits.hasNext();
					}
					hasNext = currentSearchAfterHits.hasNext();
				}

				if (!hasNext) {
					close();
				}

				return hasNext;
			}

			@Override
			public SearchHit<T> next() {
				if (hasNext()) {
					currentCount.incrementAndGet();
					return currentSearchAfterHits.next();
				}
				throw new NoSuchElementException();
			}
		};
	}

	@Getter
	static private class PitSearchAfter {
		private final BaseQuery baseQuery;
		// 原始的 query 中的排序信息，用于在完成查询后，回写信息
		@Nullable private final Sort sort;
		private final String pit;

		PitSearchAfter(BaseQuery baseQuery, String pit) {
			this.baseQuery = baseQuery;
			this.sort = baseQuery.getSort();
			this.pit = pit;
		}
	}

}
