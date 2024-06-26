package com.mawen.search.core.domain;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class SearchHitSupport {

	private SearchHitSupport() {
	}

	@Nullable
	public static Object unwrapSearchHits(@Nullable Object result) {

		if (result == null) {
			return result;
		}

		if (result instanceof SearchHit<?>) {
			return ((SearchHit<?>) result).getContent();
		}

		if (result instanceof List<?>) {
			return ((List<?>) result).stream() //
					.map(SearchHitSupport::unwrapSearchHits) //
					.collect(Collectors.toList());
		}

		if (result instanceof Stream<?>) {
			return ((Stream<?>) result).map(SearchHitSupport::unwrapSearchHits);
		}

		if (result instanceof SearchHits<?>) {
			return unwrapSearchHits(((SearchHits<?>) result).getSearchHits());
		}

		if (result instanceof SearchHitsIterator<?>) {
			return unwrapSearchHitsIterator((SearchHitsIterator<?>) result);
		}

		if (result instanceof SearchPage<?>) {
			SearchPage<?> searchPage = (SearchPage<?>) result;
			List<?> content = (List<?>) SearchHitSupport.unwrapSearchHits(searchPage.getSearchHits());
			return new PageImpl<>(content, searchPage.getPageable(), searchPage.getTotalElements());
		}

		return result;
	}

	private static CloseableIterator<?> unwrapSearchHitsIterator(SearchHitsIterator<?> iterator) {

		return new CloseableIterator() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Object next() {
				return unwrapSearchHits(iterator.next());
			}

			@Override
			public void close() {
				iterator.close();
			}
		};
	}

	public static <T> SearchPage<T> searchPageFor(SearchHits<T> searchHits, @Nullable Pageable pageable) {
		return new SearchPageImpl<>(searchHits, (pageable != null) ? pageable : Pageable.unpaged());
	}

	static class SearchPageImpl<T> extends PageImpl<SearchHit<T>> implements SearchPage<T> {

		private final SearchHits<T> searchHits;

		public SearchPageImpl(SearchHits<T> searchHits, Pageable pageable) {
			super(searchHits.getSearchHits(), pageable, searchHits.getTotalHits());
			this.searchHits = searchHits;
		}

		@Override
		public SearchHits<T> getSearchHits() {
			return searchHits;
		}

		/*
		 * return the same instance as in getSearchHits().getSearchHits()
		 */
		@Override
		public List<SearchHit<T>> getContent() {
			return searchHits.getSearchHits();
		}
	}
}
