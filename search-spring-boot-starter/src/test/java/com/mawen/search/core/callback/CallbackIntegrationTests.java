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
package com.mawen.search.core.callback;

import java.util.Collections;
import java.util.List;

import com.mawen.search.core.AbstractElasticsearchTemplate;
import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import com.mawen.search.core.event.AfterLoadCallback;
import com.mawen.search.core.event.BeforeConvertCallback;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
@SpringIntegrationTest
abstract class CallbackIntegrationTests {

	@Autowired private ElasticsearchOperations originalOperations;
	// need a spy here on the abstract implementation class
	private AbstractElasticsearchTemplate operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@Nullable private static SeqNoPrimaryTerm seqNoPrimaryTerm = null;

	@Configuration
	static class Config {

		@Component
		static class SampleEntityBeforeConvertCallback implements BeforeConvertCallback<SampleEntity> {
			@Override
			public SampleEntity onBeforeConvert(SampleEntity entity, IndexCoordinates index) {
				entity.setText("converted");
				if (seqNoPrimaryTerm != null) {
					entity.setSeqNoPrimaryTerm(seqNoPrimaryTerm);
				}
				return entity;
			}
		}

		@Component
		static class SampleEntityAfterLoadCallback implements AfterLoadCallback<SampleEntity> {

			@Override
			public com.mawen.search.core.document.Document onAfterLoad(
					com.mawen.search.core.document.Document document, Class<SampleEntity> type,
					IndexCoordinates indexCoordinates) {

				document.put("className", type.getName());
				return document;
			}
		}
	}

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		seqNoPrimaryTerm = null;
		operations = (AbstractElasticsearchTemplate) spy(originalOperations);

		// store one entity to have a seq_no and primary_term
		seqNoPrimaryTerm = operations.save(new SampleEntity("1", "initial")).getSeqNoPrimaryTerm();
	}

	@Test // DATAES-68
	void shouldCallBeforeConvertCallback() {
		SampleEntity entity = new SampleEntity("1", "test");

		SampleEntity saved = operations.save(entity);

		assertThat(saved.getText()).isEqualTo("converted");
	}

	@Test // DATAES-972
	@DisplayName("should apply conversion result to IndexQuery on save")
	void shouldApplyConversionResultToIndexQueryOnSave() {

		SampleEntity entity = new SampleEntity("1", "test");

		operations.save(entity);

		ArgumentCaptor<IndexQuery> indexQueryCaptor = ArgumentCaptor.forClass(IndexQuery.class);
		verify(operations, times(2)).doIndex(indexQueryCaptor.capture(), any());

		final IndexQuery capturedIndexQuery = indexQueryCaptor.getValue();

		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.getPrimaryTerm());
	}

	@Test // DATAES-972
	@DisplayName("should apply conversion result to IndexQuery when not set ")
	void shouldApplyConversionResultToIndexQueryWhenNotSet() {

		SampleEntity entity = new SampleEntity("1", "test");

		operations.save(entity);

		ArgumentCaptor<IndexQuery> indexQueryCaptor = ArgumentCaptor.forClass(IndexQuery.class);
		verify(operations, times(2)).doIndex(indexQueryCaptor.capture(), any());

		final IndexQuery capturedIndexQuery = indexQueryCaptor.getValue();

		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.getPrimaryTerm());
	}

	@Test // DATAES-972
	@DisplayName("should not apply conversion result to IndexQuery when already set ")
	void shouldNotApplyConversionResultToIndexQueryWhenAlreadySet() {

		SeqNoPrimaryTerm seqNoPrimaryTermOriginal = seqNoPrimaryTerm;
		seqNoPrimaryTerm = new SeqNoPrimaryTerm(7, 8);

		SampleEntity entity = new SampleEntity("1", "test");

		final IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(entity.getId());
		indexQuery.setObject(entity);
		indexQuery.setRouting("12");
		indexQuery.setSeqNo(seqNoPrimaryTermOriginal.getSequenceNumber());
		indexQuery.setPrimaryTerm(seqNoPrimaryTermOriginal.getPrimaryTerm());

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		ArgumentCaptor<IndexQuery> indexQueryCaptor = ArgumentCaptor.forClass(IndexQuery.class);
		verify(operations, times(2)).doIndex(indexQueryCaptor.capture(), any());

		final IndexQuery capturedIndexQuery = indexQueryCaptor.getValue();

		assertThat(capturedIndexQuery.getRouting()).isEqualTo("12");
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTermOriginal.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTermOriginal.getPrimaryTerm());
	}

	@Test // DATAES-972
	@DisplayName("should apply conversion result to IndexQuery in bulkIndex")
	void shouldApplyConversionResultToIndexQueryInBulkIndex() {

		SampleEntity entity = new SampleEntity("1", "test");

		final IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(entity.getId());
		indexQuery.setObject(entity);

		operations.bulkIndex(Collections.singletonList(indexQuery), SampleEntity.class);

		ArgumentCaptor<List<IndexQuery>> indexQueryListCaptor = ArgumentCaptor.forClass(List.class);
		verify(operations, times(1)).bulkOperation(indexQueryListCaptor.capture(), any(), any());

		final List<IndexQuery> capturedIndexQueries = indexQueryListCaptor.getValue();
		assertThat(capturedIndexQueries).hasSize(1);
		final IndexQuery capturedIndexQuery = capturedIndexQueries.get(0);

		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.getPrimaryTerm());
	}

	@Test // #2009
	@DisplayName("should invoke after load callback")
	void shouldInvokeAfterLoadCallback() {

		SampleEntity entity = new SampleEntity("1", "test");
		operations.save(entity);

		SampleEntity loaded = operations.get(entity.getId(), SampleEntity.class);

		assertThat(loaded).isNotNull();
		assertThat(loaded.className).isEqualTo(SampleEntity.class.getName());
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable private String text;

//		@ReadOnlyProperty
		@Nullable private String className;

		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;

		public SampleEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}
}
