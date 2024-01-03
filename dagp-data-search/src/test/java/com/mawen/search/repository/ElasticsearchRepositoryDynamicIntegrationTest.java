package com.mawen.search.repository;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.annotation.IndexName;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import com.mawen.search.utils.ResourceUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.StreamUtils;
import org.springframework.test.context.ContextConfiguration;

import static com.mawen.search.utils.IdGenerator.*;
import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
@ContextConfiguration(classes = {ElasticsearchRepositorySimpleIntegrationTest.Config.class })
class ElasticsearchRepositoryDynamicIntegrationTest {

	@Autowired
	private ElasticsearchClient client;
	@Autowired
	private IndexNameProvider indexNameProvider;
	@Autowired
	private SampleRepository sampleRepository;

	@BeforeEach
	void before() throws IOException {
		indexNameProvider.increment();

		ElasticsearchIndicesClient indices = client.indices();
		ExistsRequest existsRequest = ExistsRequest.of(f -> f.index(Arrays.asList(indexNameProvider.indexName())));
		if (indices.exists(existsRequest).value()) {
			indices.delete(DeleteIndexRequest.of(f -> f.index(indexNameProvider.indexName())));
		}

		String sampleIndex = ResourceUtil.readFileFromClasspath("sample-dynamic-index.json");
		CreateIndexRequest request = CreateIndexRequest.of(c -> c//
				.withJson(new StringReader(sampleIndex))
				.index(indexNameProvider.indexName()));
		indices.create(request);
	}

	@Test
	void shouldSaveSingleSimpleDocument() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, null, "some message", indexNameProvider.indexName());

		// when
		sampleRepository.save(sampleEntity);

		// then
		assertThat(sampleRepository.count(indexNameProvider.index())).isEqualTo(1L);
		Optional<SampleEntity> result = sampleRepository.findById(id, indexNameProvider.index());
		assertThat(result).isPresent();
		assertThat(result.get()).satisfies(it -> {
			assertThat(it.getId()).isEqualTo(id);
			assertThat(it.getType()).isNull();
			assertThat(it.getMessage()).isEqualTo("some message");
		});
	}

	@Test
	void shouldSaveMultiSimpleDocument() {

		// given
		List<SampleEntity> entities = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			String id = nextIdAsString();
			entities.add(new SampleEntity(id, null, "some message" + i, indexNameProvider.indexName()));
		}

		// when
		sampleRepository.saveAll(entities, indexNameProvider.index());

		// then
		assertThat(sampleRepository.count(indexNameProvider.index())).isEqualTo(100);
		Iterable<SampleEntity> result = sampleRepository.findAll(Sort.unsorted(), indexNameProvider.index());
		assertThat(result).satisfies(it -> {
			List<? extends SampleEntity> collect = StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());
			IntStream.range(0, collect.size()).forEach(i -> {
				assertThat(collect.get(i)).satisfies(c -> {
					SampleEntity entity = entities.get(i);
					assertThat(c.getId()).isEqualTo(entity.getId());
					assertThat(c.getType()).isEqualTo(entity.getType());
					assertThat(c.getMessage()).isEqualTo(entity.getMessage());
				});
			});
		});
	}

	@Test
	void shouldFindDocumentById() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "some message", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity);

		// when
		Optional<SampleEntity> result = sampleRepository.findById(id, indexNameProvider.index());

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(sampleEntity);
	}

	@Test
	void shouldReturnCountOfDocuments() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "some message", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity);

		// when
		long count = sampleRepository.count(indexNameProvider.index());

		// then
		assertThat(count).isGreaterThanOrEqualTo(1L);
	}

	@Test
	void shouldDeleteDocument() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "some message", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.deleteById(id, indexNameProvider.index());

		// then
		Optional<SampleEntity> result = sampleRepository.findById(id, indexNameProvider.index());
		assertThat(result).isNotPresent();
	}

	@Test
	void shouldFindAllByIdQuery() {

		// given
		List<SampleEntity> entities = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			String id = nextIdAsString();
			entities.add(new SampleEntity(id, null, "some message" + i, indexNameProvider.indexName()));
		}
		sampleRepository.saveAll(entities, indexNameProvider.index());

		// when
		Iterable<SampleEntity> result = sampleRepository.findAllById(entities.stream().map(SampleEntity::getId).collect(Collectors.toList()), indexNameProvider.index());

		// then
		assertThat(result).isNotNull().hasSize(entities.size());
	}

	@Test
	void shouldSaveIterableEntities() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "type", "some message", indexNameProvider.indexName());
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "type1", "some message2", indexNameProvider.indexName());
		List<SampleEntity> sampleEntities = Arrays.asList(sampleEntity1, sampleEntity2);

		// when
		sampleRepository.saveAll(sampleEntities, indexNameProvider.index());

		// then
		Iterable<SampleEntity> entities = sampleRepository.findAll(Sort.unsorted(), indexNameProvider.index());
		assertThat(entities).hasSize(2);
	}

	@Test
	void shouldReturnTrueGivenDocumentWithIdExists() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "hello world", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity);

		// when
		boolean exist = sampleRepository.existsById(id, indexNameProvider.index());

		// then
		assertThat(exist).isTrue();
	}

	@Test
	void shouldReturnFalseGivenDocumentWithIdDoestNotExist() {

		// given
		String id = nextIdAsString();

		// when
		boolean exist = sampleRepository.existsById(id, indexNameProvider.index());

		// then
		assertThat(exist).isFalse();
	}

	@Test
	void shouldDeleteAll() {

		// given
		SampleEntity sampleEntity = new SampleEntity(nextIdAsString(), "type", "Hello", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.deleteAll(IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Iterable<SampleEntity> entities = sampleRepository.findAll(Sort.unsorted(), indexNameProvider.index());
		assertThat(entities).isEmpty();
	}

	@Test
	void shouldDeleteById() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "hello world.", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.deleteById(id, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Optional<SampleEntity> result = sampleRepository.findById(id, indexNameProvider.index());
		assertThat(result).isEmpty();
	}

	@Test
	void shouldDeleteAllById() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "type", "hello", indexNameProvider.indexName());
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "type", "some message", indexNameProvider.indexName());
		SampleEntity sampleEntity3 = new SampleEntity(nextIdAsString(), "type", "world", indexNameProvider.indexName());
		sampleRepository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3), indexNameProvider.index());

		// when
		sampleRepository.deleteAllById(Arrays.asList(sampleEntity1.getId(), sampleEntity3.getId()), indexNameProvider.index());

		// then
		Iterable<SampleEntity> all = sampleRepository.findAll(Sort.unsorted(), indexNameProvider.index());
		assertThat(all).isNotNull();
		assertThat(all).hasSize(1);
		assertThat(all).extracting(SampleEntity::getId).containsExactly(sampleEntity2.getId());
	}

	@Test
	void shouldDeleteByType() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "book", null, indexNameProvider.indexName());
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "article", null, indexNameProvider.indexName());
		SampleEntity sampleEntity3 = new SampleEntity(nextIdAsString(), "image", null, indexNameProvider.indexName());
		sampleRepository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3), indexNameProvider.index());

		// when
		sampleRepository.deleteByType("article", indexNameProvider.index());

		// then
		Iterable<SampleEntity> all = sampleRepository.findAll(Sort.unsorted(), indexNameProvider.index());
		assertThat(all).hasSize(2);
	}

	@Test
	void shouldDeleteEntity() {

		// given
		SampleEntity sampleEntity = new SampleEntity(nextIdAsString(), "book", "hello world", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.delete(sampleEntity);

		// then
		Optional<SampleEntity> result = sampleRepository.findById(sampleEntity.getId(), indexNameProvider.index());
		assertThat(result).isEmpty();
	}

	@Test
	void shouldReturnIterableEntities() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), null, "hello", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), null, "hello", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity2);

		// when
		List<SampleEntity> entities = sampleRepository.searchById(sampleEntity1.getId(), indexNameProvider.index());

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
	}

	@Test
	void shouldDeleteIterableEntities() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "type", "Hello world", indexNameProvider.indexName());
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "type", "Hello world", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity2);

		// when
		sampleRepository.deleteAll(Arrays.asList(sampleEntity1, sampleEntity2), indexNameProvider.index());

		// then
		assertThat(sampleRepository.findById(sampleEntity1.getId(), indexNameProvider.index())).isNotPresent();
		assertThat(sampleRepository.findById(sampleEntity2.getId(), indexNameProvider.index())).isNotPresent();
	}

	@Test
	void shouldIndexEntity() {

		// given
		SampleEntity sampleEntity = new SampleEntity(nextIdAsString(), null, "some message", indexNameProvider.indexName());

		// when
		sampleRepository.save(sampleEntity);

		// then
		Iterable<SampleEntity> all = sampleRepository.findAll(Sort.unsorted(), indexNameProvider.index());
		assertThat(all).hasSize(1);
	}

	@Test
	void shouldSortByGivenField() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), null, "world", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), null, "hello", indexNameProvider.indexName());
		sampleRepository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> all = sampleRepository.findAll(Sort.by(Order.asc("id")), indexNameProvider.index());

		// then
		assertThat(all).isNotNull();
	}

	@Test
	void shouldIndexNotEmptyList() {

		// given
		List<SampleEntity> list = new ArrayList<>();
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), null, "word", indexNameProvider.indexName());
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), null, "hello", indexNameProvider.indexName());
		list.add(sampleEntity1);
		list.add(sampleEntity2);

		// when
		Iterable<SampleEntity> savedAll = sampleRepository.saveAll(list, indexNameProvider.index());

		// then
		assertThat(savedAll).containsExactlyElementsOf(list);
	}

	@Test
	void shouldNotReturnNullValuesInFindAllById() {

		// given
		String id1 = "id-one";
		SampleEntity sampleEntity1 = new SampleEntity(id1, null, null, indexNameProvider.indexName());
		sampleRepository.save(sampleEntity1);
		String id2 = "id-two";
		SampleEntity sampleEntity2 = new SampleEntity(id2, null, null, indexNameProvider.indexName());
		sampleRepository.save(sampleEntity2);
		String id3 = "id-three";
		SampleEntity sampleEntity3 = new SampleEntity(id3, null, null, indexNameProvider.indexName());
		sampleRepository.save(sampleEntity3);

		// when
		Iterable<SampleEntity> allById = sampleRepository.findAllById(Arrays.asList(id1, "does-not-exist", id2, id3), IndexCoordinates.of(indexNameProvider.indexName()));
		List<SampleEntity> results = StreamUtils.createStreamFromIterator(allById.iterator()).collect(Collectors.toList());

		// then
		assertThat(results).hasSize(3);
		assertThat(results.stream().map(SampleEntity::getId).collect(Collectors.toList()))
				.containsExactlyInAnyOrder(id1, id2, id3);
	}

	@Test
	void shouldNotFailOnIndexingEmptyList() {

		// given
		Iterable<SampleEntity> list = Collections.emptyList();

		// when
		Iterable<SampleEntity> all = sampleRepository.saveAll(list, indexNameProvider.index());

		// then
		assertThat(all).hasSize(0);
	}

	interface SampleRepository extends ElasticsearchRepository<SampleEntity, String> {

		void deleteAllByMessage(String message);

		void deleteByType(String type, IndexCoordinates index);

		List<SampleEntity> searchById(String id, IndexCoordinates index);

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	@Document(dynamicIndex = true)
	static class SampleEntity {
		@Id
		private String id;
		@Field(type = FieldType.Text)
		private String type;
		@Field(type = FieldType.Text)
		private String message;
		@IndexName
		private String index;
	}

}