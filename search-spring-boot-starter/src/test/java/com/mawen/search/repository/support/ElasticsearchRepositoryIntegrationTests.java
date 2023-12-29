package com.mawen.search.repository.support;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mawen.search.UncategorizedElasticsearchException;
import com.mawen.search.core.annotation.DateFormat;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.annotation.ValueConverter;
import com.mawen.search.core.mapping.PropertyValueConverter;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.repository.ElasticsearchRepository;
import com.mawen.search.repository.support.ElasticsearchRepositoryIntegrationTests.ComplexEntity.ModifyType;
import com.mawen.search.utils.IndexNameProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.assertj.core.api.Assertions;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import static com.mawen.search.utils.IdGenerator.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Michael Wirth
 * @author Peter-Josef Meisch
 * @author Murali Chevuri
 */
@SpringIntegrationTest
abstract class ElasticsearchRepositoryIntegrationTests {

	@Autowired
	private SampleElasticsearchRepository repository;
	@Autowired
	private ComplexElasticsearchRepository complexRepository;
	@Autowired
	private ElasticsearchClient client;
	@Autowired
	private IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
	}

	@Test
	void shouldDoBulkIndexDocument() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		// when
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2));

		// then
		Optional<SampleEntity> entity1FromElasticSearch = repository.findById(documentId1);
		assertThat(entity1FromElasticSearch.isPresent()).isTrue();

		Optional<SampleEntity> entity2FromElasticSearch = repository.findById(documentId2);
		assertThat(entity2FromElasticSearch.isPresent()).isTrue();
	}

	@Test
	void shouldSaveDocument() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		// when
		repository.save(sampleEntity);

		// then
		Optional<SampleEntity> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch).isPresent();
	}

	@Test
	void throwExceptionWhenTryingToInsertWithVersionButWithoutId() {

		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		// when
		assertThatThrownBy(() -> repository.save(sampleEntity)).isInstanceOf(UncategorizedElasticsearchException.class);
	}

	@Test
	void shouldFindDocumentById() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		Optional<SampleEntity> entityFromElasticSearch = repository.findById(documentId);

		// then
		assertThat(entityFromElasticSearch).isPresent();
		assertThat(entityFromElasticSearch.get()).isEqualTo(sampleEntity);
	}

	@Test
	void shouldReturnCountOfDocuments() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		Long count = repository.count();

		// then
		assertThat(count).isGreaterThanOrEqualTo(1L);
	}

	@Test
	void shouldDeleteDocument() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		repository.deleteById(documentId);

		// then
		Optional<SampleEntity> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch).isNotPresent();
	}

	@Test
	void shouldFindAllByIdQuery() {

		// create more than 10 documents to see that the number of input ids is set as requested size
		int numEntities = 20;
		List<String> ids = new ArrayList<>(numEntities);
		List<SampleEntity> entities = new ArrayList<>(numEntities);
		for (int i = 0; i < numEntities; i++) {
			String documentId = nextIdAsString();
			ids.add(documentId);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage("hello world.");
			sampleEntity.setVersion(System.currentTimeMillis());
			entities.add(sampleEntity);
		}
		repository.saveAll(entities);

		Iterable<SampleEntity> sampleEntities = repository.findAllById(ids);

		assertThat(sampleEntities).isNotNull().hasSize(numEntities);
	}

	@Test
	void shouldSaveIterableEntities() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());

		Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity1, sampleEntity2);

		// when
		repository.saveAll(sampleEntities);

		// then
		Iterable<SampleEntity> entities = repository.findAll();
		assertThat(entities).hasSize(2);
	}

	@Test
	void shouldReturnTrueGivenDocumentWithIdExists() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertThat(exist).isTrue();
	}

	@Test
	void shouldReturnFalseGivenDocumentWithIdDoesNotExist() {

		// given
		String documentId = nextIdAsString();

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertThat(exist).isFalse();
	}

	@Test
	void shouldDeleteAll() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		repository.deleteAll();

		// then
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).isEmpty();
	}

	@Test
	void shouldDeleteById() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		long result = repository.deleteSampleEntityById(documentId);

		// then
		Iterable<SampleEntity> sampleEntities = repository.searchById(documentId);
		assertThat(sampleEntities).isEmpty();
		assertThat(result).isEqualTo(1L);
	}

	@Test
	void shouldDeleteAllById() {

		// given
		String id1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(id1);
		sampleEntity1.setMessage("hello world 1");
		sampleEntity1.setAvailable(true);
		sampleEntity1.setVersion(System.currentTimeMillis());

		String id2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(id2);
		sampleEntity2.setMessage("hello world 2");
		sampleEntity2.setAvailable(true);
		sampleEntity2.setVersion(System.currentTimeMillis());

		String id3 = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(id3);
		sampleEntity3.setMessage("hello world 3");
		sampleEntity3.setAvailable(false);
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		repository.deleteAllById(Arrays.asList(id1, id3));

		// then
		Iterable<SampleEntity> all = repository.findAll();
		assertThat(all).isNotNull();
		assertThat(all).hasSize(1);
		assertThat(all).extracting(SampleEntity::getId).containsExactly(id2);
	}

	@Test
	void shouldDeleteByMessageAndReturnList() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world 1");
		sampleEntity1.setAvailable(true);
		sampleEntity1.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setMessage("hello world 2");
		sampleEntity2.setAvailable(true);
		sampleEntity2.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId);
		sampleEntity3.setMessage("hello world 3");
		sampleEntity3.setAvailable(false);
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		repository.deleteByAvailable(true);

		// then
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(1);
	}

	@Test
	void shouldDeleteByListForMessage() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world 1");
		sampleEntity1.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setMessage("hello world 2");
		sampleEntity2.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId);
		sampleEntity3.setMessage("hello world 3");
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		repository.deleteByMessage("hello world 3");

		// then
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	void shouldDeleteByType() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setType("book");
		sampleEntity1.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("article");
		sampleEntity2.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId);
		sampleEntity3.setType("image");
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		repository.deleteByType("article");

		// then
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	void shouldDeleteEntity() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		repository.delete(sampleEntity);

		// then
		Iterable<SampleEntity> sampleEntities = repository.searchById(documentId);
		assertThat(sampleEntities).isEmpty();
	}

	@Test
	void shouldReturnIterableEntities() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity1);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> sampleEntities = repository.searchById(documentId1);

		// then
		assertThat(sampleEntities).isNotNull();
	}

	@Test
	void shouldDeleteIterableEntities() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity2, sampleEntity2);

		// when
		repository.deleteAll(sampleEntities);

		// then
		Assertions.assertThat(repository.findById(documentId1)).isNotPresent();
		Assertions.assertThat(repository.findById(documentId2)).isNotPresent();
	}

	@Test
	void shouldIndexEntity() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setVersion(System.currentTimeMillis());
		sampleEntity.setMessage("some message");

		// when
		repository.save(sampleEntity);

		// then
		Iterable<SampleEntity> entities = repository.findAll();
		assertThat(entities).hasSize(1);
	}

	@Test
	void shouldSortByGivenField() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("world");
		repository.save(sampleEntity);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello");
		repository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> sampleEntities = repository.findAll(Sort.by(Order.asc("message.keyword")));

		// then
		assertThat(sampleEntities).isNotNull();
	}

	@Test
	void shouldIndexNotEmptyList() {
		// given
		List<SampleEntity> list = new ArrayList<>();
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("world");
		list.add(sampleEntity1);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello");
		list.add(sampleEntity2);

		Iterable<SampleEntity> savedEntities = repository.saveAll(list);

		assertThat(savedEntities).containsExactlyElementsOf(list);
	}

	@Test
	void shouldNotFailOnIndexingEmptyList() {
		Iterable<SampleEntity> savedEntities = repository.saveAll(Collections.emptyList());

		assertThat(savedEntities).hasSize(0);
	}

	@Test
	void shouldNotReturnNullValuesInFindAllById() throws IOException {

		// given
		String documentId1 = "id-one";
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		repository.save(sampleEntity1);
		String documentId2 = "id-two";
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		repository.save(sampleEntity2);
		String documentId3 = "id-three";
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		repository.save(sampleEntity3);

		Iterable<SampleEntity> allById = repository
				.findAllById(Arrays.asList("id-one", "does-not-exist", "id-two", "where-am-i", "id-three"));
		List<SampleEntity> results = StreamUtils.createStreamFromIterator(allById.iterator()).collect(Collectors.toList());

		assertThat(results).hasSize(3);
		assertThat(results.stream().map(SampleEntity::getId).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("id-one", "id-two", "id-three");
	}

	@Test
	void shouldSaveComplexDocument() throws IOException {

		ElasticsearchIndicesClient indices = client.indices();

		@Language("JSON")
		String propertyJson = "{\n" +
				"  \"properties\": {\n" +
				"    \"id\": {\n" +
				"      \"type\": \"keyword\"\n" +
				"    },\n" +
				"    \"isDeleted\": {\n" +
				"      \"type\": \"boolean\"\n" +
				"    },\n" +
				"    \"modifyType\": {\n" +
				"      \"properties\": {\n" +
				"        \"label\": {\n" +
				"          \"type\": \"keyword\"\n" +
				"        },\n" +
				"        \"value\": {\n" +
				"          \"type\": \"keyword\"\n" +
				"        }\n" +
				"      }\n" +
				"    },\n" +
				"    \"publishBatchList\": {\n" +
				"      \"type\": \"nested\",\n" +
				"      \"properties\": {\n" +
				"        \"id\": {\n" +
				"          \"type\": \"keyword\"\n" +
				"        },\n" +
				"        \"batchName\": {\n" +
				"          \"type\": \"keyword\"\n" +
				"        },\n" +
				"        \"createTime\": {\n" +
				"          \"type\": \"date\",\n" +
				"          \"format\": \"epoch_millis||yyyy-MM-dd HH:mm:ss.SSS\"\n" +
				"        }\n" +
				"      }\n" +
				"    },\n" +
				"    \"hot\": {\n" +
				"      \"type\": \"integer\"\n" +
				"    }\n" +
				"  }\n" +
				"}";
		TypeMapping mapping = TypeMapping.of(b -> b.withJson(new StringReader(propertyJson)));

		CreateIndexRequest request = new CreateIndexRequest.Builder()
				.index("test-complex-1")
				.mappings(mapping)
				.build();
		indices.create(request);

		assertThat(complexRepository.count()).isEqualTo(0L);

		List<ComplexEntity.PublishBatch> publishBatchList = new ArrayList<>();
		publishBatchList.add(new ComplexEntity.PublishBatch("1", "mawen_batch", new Date()));
		publishBatchList.add(new ComplexEntity.PublishBatch("2", "jack_batch", new Date()));
		ComplexEntity complexEntity = new ComplexEntity("1", true, ModifyType.DELETE, publishBatchList, 1);

		complexRepository.save(complexEntity);
		assertThat(complexRepository.count()).isEqualTo(1L);

		Optional<ComplexEntity> result = complexRepository.findById(complexEntity.getId());
		assertThat(result).isPresent();
		assertThat(result.get()).satisfies(it -> {
			assertThat(it.getId()).isEqualTo("1");
			assertThat(it.getHot()).isEqualTo(1);
			assertThat(it.getModifyType()).isEqualTo(ModifyType.DELETE);
			assertThat(it.getDeleted()).isTrue();
			assertThat(it.getPublishBatchList()).hasSize(2);
			IntStream.range(0, 2).forEach(i -> {
				assertThat(it.getPublishBatchList().get(i).getId()).isEqualTo(complexEntity.getPublishBatchList().get(i).getId());
				assertThat(it.getPublishBatchList().get(i).getBatchName()).isEqualTo(complexEntity.getPublishBatchList().get(i).getBatchName());
				assertThat(it.getPublishBatchList().get(i).getCreateTime()).isEqualTo(complexEntity.getPublishBatchList().get(i).getCreateTime());
			});
		});
	}

	private static List<SampleEntity> createSampleEntitiesWithMessage(String message, int numberOfEntities) {

		List<SampleEntity> sampleEntities = new ArrayList<>();
		long idBase = (long) (Math.random() * 100);
		long versionBase = System.currentTimeMillis();

		for (int i = 0; i < numberOfEntities; i++) {
			String documentId = String.valueOf(idBase + i);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage(message);
			sampleEntity.setRate(2);
			sampleEntity.setVersion(versionBase + i);
			sampleEntities.add(sampleEntity);
		}
		return sampleEntities;
	}

	@Data
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id
		private String id;
		@Nullable
		@Field(type = FieldType.Text)
		private String type;
		@Nullable
		@Field(type = FieldType.Text)
		private String message;
		@Nullable
		private int rate;
		@Nullable
		private boolean available;
		@Nullable
		@Version
		private Long version;

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			SampleEntity that = (SampleEntity) o;

			if (rate != that.rate)
				return false;
			if (available != that.available)
				return false;
			if (!Objects.equals(id, that.id))
				return false;
			if (!Objects.equals(type, that.type))
				return false;
			if (!Objects.equals(message, that.message))
				return false;
			return Objects.equals(version, that.version);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (message != null ? message.hashCode() : 0);
			result = 31 * result + rate;
			result = 31 * result + (available ? 1 : 0);
			result = 31 * result + (version != null ? version.hashCode() : 0);
			return result;
		}
	}

	interface SampleElasticsearchRepository extends ElasticsearchRepository<SampleEntity, String> {

		long deleteSampleEntityById(String id);

		long deleteByAvailable(boolean available);

		/**
		 * 删除方法仅支持返回数量
		 */
		long deleteByMessage(String message);

		void deleteByType(String type);

		Iterable<SampleEntity> searchById(String id);
	}

	@Document(indexName = "test-complex-1")
	static class ComplexEntity {

		@Id
		private String id;

		@Field(value = "isDeleted", type = FieldType.Boolean)
		private Boolean isDeleted;

		@Field(value = "modifyType", type = FieldType.Object)
		@ValueConverter(ModifyTypeConverter.class)
		private ModifyType modifyType;

		@Field(value = "publishBatchList", type = FieldType.Nested)
		private List<PublishBatch> publishBatchList;

		@Field(value = "hot", type = FieldType.Integer)
		private Integer hot;

		public ComplexEntity() {
		}

		public ComplexEntity(String id, Boolean isDeleted, ModifyType modifyType, List<PublishBatch> publishBatchList, Integer hot) {
			this.id = id;
			this.isDeleted = isDeleted;
			this.modifyType = modifyType;
			this.publishBatchList = publishBatchList;
			this.hot = hot;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Boolean getDeleted() {
			return isDeleted;
		}

		public void setDeleted(Boolean deleted) {
			isDeleted = deleted;
		}

		public ModifyType getModifyType() {
			return modifyType;
		}

		public void setModifyType(ModifyType modifyType) {
			this.modifyType = modifyType;
		}

		public List<PublishBatch> getPublishBatchList() {
			return publishBatchList;
		}

		public void setPublishBatchList(List<PublishBatch> publishBatchList) {
			this.publishBatchList = publishBatchList;
		}

		public Integer getHot() {
			return hot;
		}

		public void setHot(Integer hot) {
			this.hot = hot;
		}

		@Getter
		@JsonFormat(shape = Shape.OBJECT)
		enum ModifyType {

			INSERT("新增"),
			UPDATE("修改"),
			DELETE("删除");

			private String label;

			ModifyType(String label) {
				this.label = label;
			}

			public String getValue() {
				return this.name();
			}

			@JsonCreator
			public static ModifyType value(@JsonProperty("value") String value) {
				return StringUtils.hasLength(value) ? Enum.valueOf(ModifyType.class, value) : null;
			}
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		static class PublishBatch {

			@Id
			private String id;

			@Field(value = "batchName", type = FieldType.Keyword)
			private String batchName;

			@Field(value = "createTime", type = FieldType.Date, format = DateFormat.epoch_millis)
			private Date createTime;
		}
	}


	enum ModifyTypeConverter implements PropertyValueConverter {

		INSTANCE;

		@Override
		public Object write(Object value) {
			return value;
		}

		@Override
		public Object read(Object value) {
			if (value instanceof Map) {
				return ModifyType.value(((Map<String, String>) value).get("value"));
			}
			return value;
		}
	}

	interface ComplexElasticsearchRepository extends ElasticsearchRepository<ComplexEntity, String> {

	}
}
