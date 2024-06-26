/*
 * Copyright 2020-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mawen.search.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.mawen.search.core.aggregation.AggregationsContainer;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHitSupport;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SearchHitsImpl;
import com.mawen.search.core.domain.SearchHitsIterator;
import com.mawen.search.core.domain.SearchPage;
import com.mawen.search.core.query.TotalHitsRelation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.CloseableIterator;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchHitSupportTest {

	@Test
	void unwrapsSearchHitsIteratorToCloseableIteratorOfEntities() {
		TestStringSearchHitsIterator searchHitsIterator = new TestStringSearchHitsIterator();

		@SuppressWarnings("unchecked")
		CloseableIterator<String> unwrappedIterator = (CloseableIterator<String>) SearchHitSupport
				.unwrapSearchHits(searchHitsIterator);

		// noinspection ConstantConditions
		assertThat(unwrappedIterator.next()).isEqualTo("one");
		assertThat(unwrappedIterator.next()).isEqualTo("two");
		assertThat(unwrappedIterator.hasNext()).isFalse();

		unwrappedIterator.close();

		assertThat(searchHitsIterator.closed).isTrue();
	}

	@Test
	@DisplayName("should return the same list instance in SearchHits and getContent")
	void shouldReturnTheSameListInstanceInSearchHitsAndGetContent() {

		List<SearchHit<String>> hits = new ArrayList<>();
		hits.add(new SearchHit<>(null, null, null, 0, null, null, null, null, null,  "one"));
		hits.add(new SearchHit<>(null, null, null, 0, null, null, null, null, null,  "two"));
		hits.add(new SearchHit<>(null, null, null, 0, null, null, null, null, null,  "three"));
		hits.add(new SearchHit<>(null, null, null, 0, null, null, null, null, null,  "four"));
		hits.add(new SearchHit<>(null, null, null, 0, null, null, null, null, null,  "five"));

		SearchHits<String> originalSearchHits = new SearchHitsImpl<>(hits.size(), TotalHitsRelation.EQUAL_TO, 0, "scroll",
				hits, null);

		SearchPage<String> searchPage = SearchHitSupport.searchPageFor(originalSearchHits, PageRequest.of(0, 3));
		SearchHits<String> searchHits = searchPage.getSearchHits();

		assertThat(searchHits).isEqualTo(originalSearchHits);
		assertThat(searchHits.getSearchHits()).isSameAs(searchPage.getContent());
	}

	private static class TestStringSearchHitsIterator implements SearchHitsIterator<String> {
		private final Iterator<String> iterator = Arrays.asList("one", "two").iterator();
		private boolean closed = false;

		@Override
		public AggregationsContainer<?> getAggregations() {
			return mock(AggregationsContainer.class);
		}

		@Override
		public float getMaxScore() {
			return 0;
		}

		@Override
		public long getTotalHits() {
			return 2;
		}

		@Override
		public TotalHitsRelation getTotalHitsRelation() {
			return mock(TotalHitsRelation.class);
		}

		@Override
		public void close() {
			closed = true;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public SearchHit<String> next() {
			String nextString = iterator.next();
			return new SearchHit<>("index", "id", null, 1.0f, new Object[0], emptyMap(), null, null, null, nextString);
		}
	}

}
