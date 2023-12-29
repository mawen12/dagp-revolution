package com.mawen.search.repository;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mawen.search.core.annotation.DateFormat;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.annotation.ParamQuery;
import com.mawen.search.core.annotation.QueryField;
import com.mawen.search.core.annotation.QueryField.Type;
import com.mawen.search.core.annotation.ValueConverter;
import com.mawen.search.core.domain.Range;
import com.mawen.search.core.mapping.PropertyValueConverter;
import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import com.mawen.search.utils.IndexNameProvider;
import com.mawen.search.utils.ResourceUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
@ContextConfiguration(classes = {ElasticsearchRepositoryParamQueryIntegrationTest.Config.class})
class ElasticsearchRepositoryParamQueryIntegrationTest {

	@Autowired
	private ElasticsearchClient client;
	@Autowired
	private IndexNameProvider indexNameProvider;
	@Autowired
	private ParamQueryRepository paramQueryRepository;

	@Configuration
	@Import({ElasticsearchTemplateConfiguration.class})
	@EnableElasticsearchRepositories(basePackages = {"com.mawen.search.repository"},
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
		String sampleIndex = ResourceUtil.readFileFromClasspath("param-query-index.json");
		CreateIndexRequest request = CreateIndexRequest.of(c -> c//
				.withJson(new StringReader(sampleIndex))
				.index(indexNameProvider.indexName()));
		indices.create(request);


	}

	@Test
	void shouldQueryIdCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		paramQueryRepository.save(entity1);

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setId("1");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities.get(0)).isNotNull();
		assertThat(entities.get(0)).isEqualTo(entity1);
	}

	@Test
	void shouldQueryIdsCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setIds(Arrays.asList("1", "2"));
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities.get(0)).isNotNull();
		assertThat(entities.get(0)).isEqualTo(entity1);
		assertThat(entities.get(1)).isNotNull();
		assertThat(entities.get(1)).isEqualTo(entity2);
	}

	@Test
	void shouldQueryKeyWordCorrectly() {


	}


	interface ParamQueryRepository extends ElasticsearchRepository<ParamQueryEntity, String> {

		List<ParamQueryEntity> listByQuery(@ParamQuery EntityQuery query);

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class ParamQueryEntity {
		@Id
		private String id;

		@Field(value = "chineseName", type = FieldType.Keyword)
		private String chineseName;

		@Field(value = "englishName", type = FieldType.Keyword)
		private String englishName;

		@Field(value = "assetCode", type = FieldType.Keyword)
		private String assetCode;

		@Field(value = "assetMean", type = FieldType.Text)
		private String assetMean;

		@Field(value = "assetType", type = FieldType.Object)
		private AssetType assetType;

		@Field(value = "extendedAttrs", type = FieldType.Nested)
		private List<ExtendedAttr> extendedAttrs;

		@Field(value = "isDeleted", type = FieldType.Boolean)
		private Boolean isDeleted;

		@Field(value = "publishBatchList", type = FieldType.Nested)
		private List<PublishBatch> publishBatchList;

		@Field(value = "publishState", type = FieldType.Object)
		@ValueConverter(PublishState.class)
		private PublishState publishState;

		@Field(value = "timestamp", type = FieldType.Date, format = DateFormat.epoch_millis)
		private Date timestamp;

		@Field(value = "viewCount", type = FieldType.Integer)
		private Integer viewCount;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		static class AssetType {

			@Id
			private Long id;

			@Field(value = "name", type = FieldType.Text)
			private String name;

		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		static class ExtendedAttr {
			@Field(value = "keyCode", type = FieldType.Keyword)
			private String keyCode;
			@Field(value = "keyLabel", type = FieldType.Keyword)
			private String keyLabel;
			@Field(value = "valueId", type = FieldType.Keyword)
			private String valueId;
			@Field(value = "valueLabel", type = FieldType.Text)
			private String valueLabel;
			@Field(value = "pathIds", type = FieldType.Keyword)
			private String pathIds;
			@Field(value = "pathLabels", type = FieldType.Text)
			private String pathLabels;
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

		@Getter
		@JsonFormat(shape = JsonFormat.Shape.OBJECT)
		enum PublishState implements PropertyValueConverter {

			UNPUBLISHED("未发布"),
			PROCESSING("流转中"),
			PUBLISHED("已发布");

			private final String label;

			PublishState(String label) {
				this.label = label;
			}

			public String getValue() {
				return this.name();
			}

			@JsonCreator
			public static PublishState value(@JsonProperty("value") String value) {
				return StringUtils.hasLength(value) ? Enum.valueOf(PublishState.class, value) : null;
			}

			@Override
			public Object write(Object value) {
				return value;
			}

			@Override
			public Object read(Object value) {
				if (value instanceof Map) {
					return value(((Map<String, String>) value).get("value"));
				}
				return value;
			}
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class EntityQuery {

		@QueryField(value = "id")
		private String id;

		@QueryField(value = "ids", type = Type.IN)
		private List<String> ids;

		@QueryField(value = {"chineseName", "englishName"})
		private String keyword;

		@QueryField(value = "chineseName")
		private String chineseName;

		@QueryField(value = "chineseName", type = Type.STARTING_WITH)
		private String chineseNamePrefix;

		@QueryField(value = "chineseName", type = Type.ENDING_WITH)
		private String chineseNameSuffix;

		@QueryField(value = "chineseName", type = Type.LIKE)
		private String chineseNameLike;

		@QueryField(value = "englishName")
		private String englishName;

		@QueryField(value = "englishName", type = Type.STARTING_WITH)
		private String englishNamePrefix;

		@QueryField(value = "englishName", type = Type.ENDING_WITH)
		private String englishNameSuffix;

		@QueryField(value = "englishName", type = Type.LIKE)
		private String englishNameLike;

		@QueryField(value = "assetCode")
		private String assetCode;

		@QueryField(value = "assetCode", type = Type.IN)
		private List<String> assetCodes;

		@QueryField(value = "isDeleted")
		private Boolean isDeleted;

		@QueryField(value = "assetType.id")
		private Long assetTypeId;

		@QueryField(value = "assetType.id", type = Type.IN)
		private List<Long> assetTypeIds;

		@QueryField(value = "assetType.id", type = Type.NEGATING_SIMPLE_PROPERTY)
		private Long assetTypeIdExclude;

		@QueryField(value = "assetType", type = Type.EXISTS)
		private Boolean assetTypeExists;

		@QueryField(value = "publishBatchList.id")
		private Long publishBatchId;

		@QueryField(value = "publishBatchList.id", type = Type.IN)
		private Long publishBatchIds;

		@QueryField(value = "publishState")
		private ParamQueryEntity.PublishState publishState;

		@QueryField(value = "timestamp", type = Type.BETWEEN)
		private Range<Date> timestampRange;

		@QueryField(value = "viewCount", type = Type.LESS_THAN)
		private Integer viewCountLessThan;

		@QueryField(value = "viewCount", type = Type.LESS_THAN_EQUAL)
		private Integer viewCountLessThanEqual;

		@QueryField(value = "viewCount", type = Type.GREATER_THAN)
		private Integer viewCountGreaterThan;

		@QueryField(value = "viewCount", type = Type.GREATER_THAN_EQUAL)
		private Integer viewCountGreaterThanEqual;
	}
}