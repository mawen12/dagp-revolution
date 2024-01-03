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
import java.util.Objects;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.domain.BulkOptions;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.event.AfterConvertCallback;
import com.mawen.search.core.event.AfterSaveCallback;
import com.mawen.search.core.event.BeforeConvertCallback;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.MoreLikeThisQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

abstract class ElasticsearchTemplateCallbackTests {

	protected AbstractElasticsearchTemplate template;

	@Mock protected SearchResponse searchResponse;

	private final IndexCoordinates index = IndexCoordinates.of("index");

	@Spy private ValueCapturingAfterSaveCallback afterSaveCallback = new ValueCapturingAfterSaveCallback();
	@Spy private ValueCapturingAfterConvertCallback afterConvertCallback = new ValueCapturingAfterConvertCallback();
	@Spy private ValueCapturingBeforeConvertCallback beforeConvertCallback = new ValueCapturingBeforeConvertCallback();

	protected final void initTemplate(AbstractElasticsearchTemplate template) {
		this.template = template;
	}

	@Test
	void saveOneShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity);

		verify(afterSaveCallback).onAfterSave(eq(entity), any());
		assertThat(saved.firstname).isEqualTo("after-save");
	}

	@Test
	void saveWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity, index);

		verify(afterSaveCallback).onAfterSave(eq(entity), eq(index));
		assertThat(saved.firstname).isEqualTo("after-save");
	}

	@Test
	void saveArrayShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(entity1, entity2);

		verify(afterSaveCallback, times(2)).onAfterSave(any(), any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().firstname).isEqualTo("after-save");
		assertThat(savedIterator.next().firstname).isEqualTo("after-save");
	}

	@Test
	void saveIterableShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(Arrays.asList(entity1, entity2));

		verify(afterSaveCallback, times(2)).onAfterSave(any(), any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().firstname).isEqualTo("after-save");
		assertThat(savedIterator.next().firstname).isEqualTo("after-save");
	}

	@Test
	void saveIterableWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(Arrays.asList(entity1, entity2), index);

		verify(afterSaveCallback, times(2)).onAfterSave(any(), eq(index));
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().firstname).isEqualTo("after-save");
		assertThat(savedIterator.next().firstname).isEqualTo("after-save");
	}

	@Test
	void indexShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity = new Person("init", "luke");

		IndexQuery indexQuery = indexQueryForEntity(entity);
		template.index(indexQuery, index);

		verify(afterSaveCallback).onAfterSave(eq(entity), eq(index));
		Person savedPerson = (Person) indexQuery.getObject();
		assertThat(savedPerson.firstname).isEqualTo("after-save");
	}

	private IndexQuery indexQueryForEntity(Person entity) {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setObject(entity);
		return indexQuery;
	}

	@Test
	void bulkIndexShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		IndexQuery query1 = indexQueryForEntity(entity1);
		IndexQuery query2 = indexQueryForEntity(entity2);
		template.bulkIndex(Arrays.asList(query1, query2), index);

		verify(afterSaveCallback, times(2)).onAfterSave(any(), eq(index));
		Person savedPerson1 = (Person) query1.getObject();
		Person savedPerson2 = (Person) query2.getObject();
		assertThat(savedPerson1.firstname).isEqualTo("after-save");
		assertThat(savedPerson2.firstname).isEqualTo("after-save");
	}

	@Test
	void bulkIndexWithOptionsShouldInvokeAfterSaveCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		IndexQuery query1 = indexQueryForEntity(entity1);
		IndexQuery query2 = indexQueryForEntity(entity2);
		template.bulkIndex(Arrays.asList(query1, query2), BulkOptions.defaultOptions(), index);

		verify(afterSaveCallback, times(2)).onAfterSave(any(), eq(index));
		Person savedPerson1 = (Person) query1.getObject();
		Person savedPerson2 = (Person) query2.getObject();
		assertThat(savedPerson1.firstname).isEqualTo("after-save");
		assertThat(savedPerson2.firstname).isEqualTo("after-save");
	}

	@Test
	void getShouldInvokeAfterConvertCallback() {

		template.setEntityCallbacks(EntityCallbacks.create(afterConvertCallback));

		Person result = template.get("init", Person.class);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		assertThat(result.firstname).isEqualTo("after-convert");
	}

	private Document lukeDocument() {
		return Document.create().append("id", "init").append("firstname", "luke");
	}

	@Test // DATAES-772
	void getWithCoordinatesShouldInvokeAfterConvertCallback() {

		template.setEntityCallbacks(EntityCallbacks.create(afterConvertCallback));

		Person result = template.get("init", Person.class, index);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), eq(index));
		assertThat(result.firstname).isEqualTo("after-convert");
	}


	@Test
	void moreLikeThisShouldInvokeAfterConvertCallback() {

		template.setEntityCallbacks(EntityCallbacks.create(afterConvertCallback));

		SearchHits<Person> results = template.search(moreLikeThisQuery(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.getSearchHit(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(results.getSearchHit(1).getContent().firstname).isEqualTo("after-convert");
	}

	private MoreLikeThisQuery moreLikeThisQuery() {
		MoreLikeThisQuery query = new MoreLikeThisQuery();
		query.setId("init");
		query.addFields("id");
		return query;
	}

	@Test // DATAES-772
	void searchViaMoreLikeThisShouldInvokeAfterConvertCallback() {

		template.setEntityCallbacks(EntityCallbacks.create(afterConvertCallback));

		SearchHits<Person> results = template.search(moreLikeThisQuery(), Person.class);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		List<SearchHit<Person>> hits = results.getSearchHits();
		assertThat(hits.get(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(hits.get(1).getContent().firstname).isEqualTo("after-convert");
	}

	@Test
	void searchViaMoreLikeThisWithIndexCoordinatesShouldInvokeAfterConvertCallback() {

		template.setEntityCallbacks(EntityCallbacks.create(afterConvertCallback));

		SearchHits<Person> results = template.search(moreLikeThisQuery(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<SearchHit<Person>> hits = results.getSearchHits();
		assertThat(hits.get(0).getContent().firstname).isEqualTo("after-convert");
		assertThat(hits.get(1).getContent().firstname).isEqualTo("after-convert");
	}

	@Test // DATAES-785
	void saveOneShouldInvokeBeforeConvertCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(beforeConvertCallback));

		Person entity = new Person("init1", "luke1");

		Person saved = template.save(entity, index);

		verify(beforeConvertCallback).onBeforeConvert(any(), eq(index));
		assertThat(saved.firstname).isEqualTo("before-convert");
	}

	@Test
	void saveAllShouldInvokeBeforeConvertCallbacks() {

		template.setEntityCallbacks(EntityCallbacks.create(beforeConvertCallback));

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(Arrays.asList(entity1, entity2), index);

		verify(beforeConvertCallback, times(2)).onBeforeConvert(any(), eq(index));
		Iterator<Person> iterator = saved.iterator();
		assertThat(iterator.next().firstname).isEqualTo("before-convert");
		assertThat(iterator.next().firstname).isEqualTo("before-convert");
	}

	static class Person {
		@Nullable
		@Id String id;
		@Nullable String firstname;

		public Person(@Nullable String id, @Nullable String firstname) {
			this.id = id;
			this.firstname = firstname;
		}

		public Person() {}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(@Nullable String firstname) {
			this.firstname = firstname;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Person person = (Person) o;

			if (!Objects.equals(id, person.id))
				return false;
			return Objects.equals(firstname, person.firstname);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (firstname != null ? firstname.hashCode() : 0);
			return result;
		}
	}

	static class ValueCapturingEntityCallback<T> {

		private final List<T> values = new ArrayList<>(1);

		protected void capture(T value) {
			values.add(value);
		}

		public List<T> getValues() {
			return values;
		}

		@Nullable
		public T getValue() {
			return CollectionUtils.lastElement(values);
		}

	}

	static class ValueCapturingAfterSaveCallback extends ValueCapturingEntityCallback<Person>
			implements AfterSaveCallback<Person> {

		@Override
		public Person onAfterSave(Person entity, IndexCoordinates index) {

			capture(entity);
			return new Person() {
				{
					id = entity.id;
					firstname = "after-save";
				}
			};
		}
	}

	static class ValueCapturingAfterConvertCallback extends ValueCapturingEntityCallback<Person>
			implements AfterConvertCallback<Person> {

		@Override
		public Person onAfterConvert(Person entity, Document document, IndexCoordinates indexCoordinates) {

			capture(entity);
			return new Person() {
				{
					id = entity.id;
					firstname = "after-convert";
				}
			};
		}
	}

	static class ValueCapturingBeforeConvertCallback extends ValueCapturingEntityCallback<Person>
			implements BeforeConvertCallback<Person> {

		@Override
		public Person onBeforeConvert(Person entity, IndexCoordinates indexCoordinates) {

			capture(entity);
			return new Person() {
				{
					id = entity.id;
					firstname = "before-convert";
				}
			};
		}
	}
}
