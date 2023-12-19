package com.mawen.search.core.query;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mawen.search.client.util.ScrollState;
import com.mawen.search.core.aggregation.AggregationsContainer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
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

}
