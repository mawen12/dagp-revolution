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
import com.mawen.search.CustomElasticsearchTemplateConfiguration;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import com.mawen.search.test.ElasticsearchTemplateConfiguration;
import com.mawen.search.test.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import com.mawen.search.utils.ResourceUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.StreamUtils;
import org.springframework.test.context.ContextConfiguration;

import static com.mawen.search.utils.IdGenerator.*;
import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
@ContextConfiguration(classes = {ElasticsearchRepositorySimpleIntegrationTest.Config.class })
class ElasticsearchRepositorySimpleIntegrationTest {

	@Autowired
	private ElasticsearchClient client;
	@Autowired
	private IndexNameProvider indexNameProvider;
	@Autowired
	private SampleRepository sampleRepository;

	@Configuration
	@Import({CustomElasticsearchTemplateConfiguration.class })
	@EnableElasticsearchRepositories(basePackages = {"com.mawen.search.repository" },
			considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("repository");
		}

	}

	@BeforeEach
	void before() throws IOException {
		indexNameProvider.increment();

		ElasticsearchIndicesClient indices = client.indices();

		ExistsRequest existsRequest = ExistsRequest.of(f -> f.index(Arrays.asList(indexNameProvider.indexName())));
		if (indices.exists(existsRequest).value()) {
			indices.delete(DeleteIndexRequest.of(f -> f.index(indexNameProvider.indexName())));
		}

		String sampleIndex = ResourceUtil.readFileFromClasspath("sample-index.json");
		CreateIndexRequest request = CreateIndexRequest.of(c -> c//
				.withJson(new StringReader(sampleIndex))
				.index(indexNameProvider.indexName()));
		indices.create(request);
	}

	@Test
	void shouldSaveSingleSimpleDocument() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, null, "some message");

		// when
		sampleRepository.save(sampleEntity);

		// then
		assertThat(sampleRepository.count()).isEqualTo(1L);
		Optional<SampleEntity> result = sampleRepository.findById(id);
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
			entities.add(new SampleEntity(id, null, "some message" + i));
		}

		// when
		sampleRepository.saveAll(entities);

		// then
		assertThat(sampleRepository.count()).isEqualTo(100);
		Iterable<SampleEntity> result = sampleRepository.findAll();
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
		SampleEntity sampleEntity = new SampleEntity(id, "type", "some message");
		sampleRepository.save(sampleEntity);

		// when
		Optional<SampleEntity> result = sampleRepository.findById(id);

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(sampleEntity);
	}

	@Test
	void shouldReturnCountOfDocuments() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "some message");
		sampleRepository.save(sampleEntity);

		// when
		long count = sampleRepository.count();

		// then
		assertThat(count).isGreaterThanOrEqualTo(1L);
	}

	@Test
	void shouldDeleteDocument() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "some message");
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.deleteById(id);

		// then
		Optional<SampleEntity> result = sampleRepository.findById(id);
		assertThat(result).isNotPresent();
	}

	@Test
	void shouldFindAllByIdQuery() {

		// given
		List<SampleEntity> entities = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			String id = nextIdAsString();
			entities.add(new SampleEntity(id, null, "some message" + i));
		}
		sampleRepository.saveAll(entities);

		// when
		Iterable<SampleEntity> result = sampleRepository.findAllById(entities.stream().map(SampleEntity::getId).collect(Collectors.toList()));

		// then
		assertThat(result).isNotNull().hasSize(entities.size());
	}

	@Test
	void shouldSaveIterableEntities() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "type", "some message");
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "type1", "some message2");
		List<SampleEntity> sampleEntities = Arrays.asList(sampleEntity1, sampleEntity2);

		// when
		sampleRepository.saveAll(sampleEntities);

		// then
		Iterable<SampleEntity> entities = sampleRepository.findAll();
		assertThat(entities).hasSize(2);
	}

	@Test
	void shouldReturnTrueGivenDocumentWithIdExists() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "hello world");
		sampleRepository.save(sampleEntity);

		// when
		boolean exist = sampleRepository.existsById(id);

		// then
		assertThat(exist).isTrue();
	}

	@Test
	void shouldReturnFalseGivenDocumentWithIdDoestNotExist() {

		// given
		String id = nextIdAsString();

		// when
		boolean exist = sampleRepository.existsById(id);

		// then
		assertThat(exist).isFalse();
	}

	@Test
	void shouldDeleteAll() {

		// given
		SampleEntity sampleEntity = new SampleEntity(nextIdAsString(), "type", "Hello");
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.deleteAll();

		// then
		Iterable<SampleEntity> entities = sampleRepository.findAll();
		assertThat(entities).isEmpty();
	}

	@Test
	void shouldDeleteById() {

		// given
		String id = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity(id, "type", "hello world.");
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.deleteById(id);

		// then
		Optional<SampleEntity> result = sampleRepository.findById(id);
		assertThat(result).isEmpty();
	}

	@Test
	void shouldDeleteAllById() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "type", "hello");
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "type", "some message");
		SampleEntity sampleEntity3 = new SampleEntity(nextIdAsString(), "type", "world");
		sampleRepository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		sampleRepository.deleteAllById(Arrays.asList(sampleEntity1.getId(), sampleEntity3.getId()));

		// then
		Iterable<SampleEntity> all = sampleRepository.findAll();
		assertThat(all).isNotNull();
		assertThat(all).hasSize(1);
		assertThat(all).extracting(SampleEntity::getId).containsExactly(sampleEntity2.getId());
	}

	@Test
	void shouldDeleteByType() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "book", null);
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "article", null);
		SampleEntity sampleEntity3 = new SampleEntity(nextIdAsString(), "image", null);
		sampleRepository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		sampleRepository.deleteByType("article");

		// then
		Iterable<SampleEntity> all = sampleRepository.findAll();
		assertThat(all).hasSize(2);
	}

	@Test
	void shouldDeleteEntity() {

		// given
		SampleEntity sampleEntity = new SampleEntity(nextIdAsString(), "book", "hello world");
		sampleRepository.save(sampleEntity);

		// when
		sampleRepository.delete(sampleEntity);

		// then
		Optional<SampleEntity> result = sampleRepository.findById(sampleEntity.getId());
		assertThat(result).isEmpty();
	}

	@Test
	void shouldReturnIterableEntities() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), null, "hello");
		sampleRepository.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), null, "hello");
		sampleRepository.save(sampleEntity2);

		// when
		List<SampleEntity> entities = sampleRepository.searchById(sampleEntity1.getId());

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
	}

	@Test
	void shouldDeleteIterableEntities() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), "type", "Hello world");
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), "type", "Hello world");
		sampleRepository.save(sampleEntity2);

		// when
		sampleRepository.deleteAll(Arrays.asList(sampleEntity1, sampleEntity2));

		// then
		assertThat(sampleRepository.findById(sampleEntity1.getId())).isNotPresent();
		assertThat(sampleRepository.findById(sampleEntity2.getId())).isNotPresent();
	}

	@Test
	void shouldIndexEntity() {

		// given
		SampleEntity sampleEntity = new SampleEntity(nextIdAsString(), null, "some message");

		// when
		sampleRepository.save(sampleEntity);

		// then
		Iterable<SampleEntity> all = sampleRepository.findAll();
		assertThat(all).hasSize(1);
	}

	@Test
	void shouldSortByGivenField() {

		// given
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), null, "world");
		sampleRepository.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), null, "hello");
		sampleRepository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> all = sampleRepository.findAll(Sort.by(Order.asc("id")));

		// then
		assertThat(all).isNotNull();
	}

	@Test
	void shouldIndexNotEmptyList() {

		// given
		List<SampleEntity> list = new ArrayList<>();
		SampleEntity sampleEntity1 = new SampleEntity(nextIdAsString(), null, "word");
		SampleEntity sampleEntity2 = new SampleEntity(nextIdAsString(), null, "hello");
		list.add(sampleEntity1);
		list.add(sampleEntity2);

		// when
		Iterable<SampleEntity> savedAll = sampleRepository.saveAll(list);

		// then
		assertThat(savedAll).containsExactlyElementsOf(list);
	}

	@Test
	void shouldNotReturnNullValuesInFindAllById() {

		// given
		String id1 = "id-one";
		SampleEntity sampleEntity1 = new SampleEntity(id1, null, null);
		sampleRepository.save(sampleEntity1);
		String id2 = "id-two";
		SampleEntity sampleEntity2 = new SampleEntity(id2, null, null);
		sampleRepository.save(sampleEntity2);
		String id3 = "id-three";
		SampleEntity sampleEntity3 = new SampleEntity(id3, null, null);
		sampleRepository.save(sampleEntity3);

		// when
		Iterable<SampleEntity> allById = sampleRepository.findAllById(Arrays.asList(id1, "does-not-exist", id2, id3));
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
		Iterable<SampleEntity> all = sampleRepository.saveAll(list);

		// then
		assertThat(all).hasSize(0);
	}

	interface SampleRepository extends ElasticsearchRepository<SampleEntity, String> {

		void deleteAllByMessage(String message);

		void deleteByType(String type);

		List<SampleEntity> searchById(String id);

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Id
		private String id;
		@Field(type = FieldType.Text)
		private String type;
		@Field(type = FieldType.Text)
		private String message;
	}

}