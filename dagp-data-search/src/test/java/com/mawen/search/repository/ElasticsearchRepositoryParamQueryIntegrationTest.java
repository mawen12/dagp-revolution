package com.mawen.search.repository;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
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
import com.mawen.search.core.annotation.SourceFilters;
import com.mawen.search.core.annotation.ValueConverter;
import com.mawen.search.core.domain.Criteria.Operator;
import com.mawen.search.core.domain.Range;
import com.mawen.search.core.domain.Range.Bound;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

		ExistsRequest existsRequest = ExistsRequest.of(f -> f.index(Arrays.asList(indexNameProvider.indexName())));
		if (indices.exists(existsRequest).value()) {
			indices.delete(DeleteIndexRequest.of(f -> f.index(indexNameProvider.indexName())));
		}

		String sampleIndex = ResourceUtil.readFileFromClasspath("param-query-index.json");
		CreateIndexRequest request = CreateIndexRequest.of(c -> c//
				.withJson(new StringReader(sampleIndex))
				.index(indexNameProvider.indexName()));
		indices.create(request);
	}

	@Test
	void shouldQueryByIdCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
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
	void shouldQueryByIdsCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
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
	void shouldQueryByKeyWordCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "发证机关国家或地区", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setKeyword("地区");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryByChineseNameCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setChineseName("发证机关国家或地区");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1));
	}

	@Test
	void shouldQueryByChineseNamePrefixCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setChineseNamePrefix("发证");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity1));
	}

	@Test
	void shouldQueryByChineseNameSuffixCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setChineseNameSuffix("代码");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity2));
	}

	@Test
	void shouldQueryByChineseNameLikeCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setChineseNameLike("国家");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity1));
	}

	@Test
	void shouldQueryByEnglishNameCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setEnglishName("Job Sequence Code");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity2));
	}

	@Test
	void shouldQueryByEnglishNamePrefixCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setEnglishNamePrefix("Job");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity2));
	}

	@Test
	void shouldQueryByEnglishNameSuffixCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setEnglishNameSuffix("Code");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity2));
	}

	@Test
	void shouldQueryByEnglishNameLike() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setEnglishNameLike("Sequence");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity2));
	}

	@Test
	void shouldQueryByAssetCodeCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setAssetCode("TEC_00000000000448");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity1));
	}

	@Test
	void shouldQueryByAssetCodesCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setAssetCodes(Arrays.asList("TEC_00000000000448", "TEC_00000000000522"));
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryByIsDeletedCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setIsDeleted(true);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);
		entityQuery.setIsDeleted(false);
		List<ParamQueryEntity> entities1 = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity1));

		assertThat(entities1).isNotNull();
		assertThat(entities1).hasSize(1);
		assertThat(entities1).containsExactlyInAnyOrderElementsOf(Collections.singletonList(entity2));
	}

	@Test
	void shouldQueryByExtendAttrCorrectly() {

		// given
		List<ParamQueryEntity.ExtendedAttr> extendedAttrs = new ArrayList<>();
		extendedAttrs.add(new ParamQueryEntity.ExtendedAttr("keyCode", "keyLabel", "valueId", "valueLabel", null, null));

		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), extendedAttrs, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setExtendedAttrsKeyLabel("keyLabel");
		entityQuery.setExtendedAttrsValueId("valueId");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactly(entity1);
	}


	@Test
	void shouldQueryByRecommendExtendAttrCorrectly() {

		// given
		List<ParamQueryEntity.ExtendedAttr> extendedAttrs = new ArrayList<>();
		extendedAttrs.add(new ParamQueryEntity.ExtendedAttr("keyCode", "keyLabel", "valueId", "valueLabel", null, null));

		List<ParamQueryEntity.RecommendExtendedAttr> recommendExtendedAttrs = new ArrayList<>();
		recommendExtendedAttrs.add(new ParamQueryEntity.RecommendExtendedAttr("1", extendedAttrs));

		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, recommendExtendedAttrs, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setRecommendExtendedAttrsKeyLabel("keyLabel");
		entityQuery.setRecommendExtendedAttrsValueId("valueId");
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactly(entity1);
	}



	@Test
	void shouldQueryByAssetTypeIdCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setAssetTypeId(1L);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryByAssetTypeIdsCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(2L, "技术标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setAssetTypeIds(Arrays.asList(1L, 2L));
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryByAssetTypeIdExcludeCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(2L, "技术标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setAssetTypeIdExclude(1L);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity2));
	}

	@Test
	void shouldQueryByAssetTypeExistsCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, null, null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setAssetTypeExists(true);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);
		entityQuery.setAssetTypeExists(false);
		List<ParamQueryEntity> entities1 = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1));
		assertThat(entities1).hasSize(1);
		assertThat(entities1).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity2));
	}

	@Test
	void shouldQueryByPublishBatchIdCorrectly() {

		// given
		List<ParamQueryEntity.PublishBatch> publishBatches = new ArrayList<>();
		publishBatches.add(new ParamQueryEntity.PublishBatch("1", "T00001", new Date()));

		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, publishBatches,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, publishBatches,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setPublishBatchId(1L);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryByPublishBatchIdsCorrectly() {

		// given
		List<ParamQueryEntity.PublishBatch> publishBatches = new ArrayList<>();
		publishBatches.add(new ParamQueryEntity.PublishBatch("1", "T00001", new Date()));

		List<ParamQueryEntity.PublishBatch> publishBatches1 = new ArrayList<>();
		publishBatches1.add(new ParamQueryEntity.PublishBatch("2", "T00002", new Date()));


		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, publishBatches,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, publishBatches1,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setPublishBatchIds(Arrays.asList(1L, 2L));
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryByPublishStateCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setPublishState(ParamQueryEntity.PublishState.UNPUBLISHED);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1));
	}

	@Test
	void shouldQueryByTimestampRangeCorrectly() {

		// given
		LocalDateTime start = LocalDateTime.of(2023, 1, 1, 01, 01);
		Date startDate = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
		LocalDateTime end = LocalDateTime.of(2023, 2, 1, 01, 01);
		Date endDate = Date.from(end.atZone(ZoneId.systemDefault()).toInstant());

		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, startDate, 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, endDate, 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setTimestampRange(Range.of(Bound.inclusive(startDate), Bound.inclusive(startDate)));
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1));
	}

	@Test
	void shouldQueryByViewCountLessThanCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setViewCountLessThan(5);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity2));
	}

	@Test
	void shouldQueryByViewCountLessThanEqualCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setViewCountLessThanEqual(10);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryByViewCountGreaterThanCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setViewCountGreaterThan(9);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1));
	}

	@Test
	void shouldQueryByViewCountGreaterThanEqualCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setViewCountGreaterThanEqual(10);
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
		assertThat(entities).containsExactlyInAnyOrderElementsOf(Arrays.asList(entity1));
	}

	@Test
	void shouldQueryByUnsortedCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setSort(Sort.unsorted());
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyInAnyOrder(entity2, entity1);
	}

	@Test
	void shouldQuerySortDescCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setSort(Sort.by("viewCount").descending());
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyElementsOf(Arrays.asList(entity1, entity2));
	}

	@Test
	void shouldQueryBySortAscCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setSort(Sort.by("viewCount").ascending());
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(2);
		assertThat(entities).containsExactlyElementsOf(Arrays.asList(entity2, entity1));
	}

	@Test
	void shouldQueryBySortNullFirstCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		ParamQueryEntity entity3 = new ParamQueryEntity("3", "对公客户证件类型", "ID Document Type of Corporate Customer", "TEC_00000000000561",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), null);

		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2, entity3));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setSort(Sort.by(Sort.Order.asc("viewCount").with(Sort.NullHandling.NULLS_FIRST)));
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(3);
		assertThat(entities).containsExactlyElementsOf(Arrays.asList(entity3, entity2, entity1));
	}

	@Test
	void shouldByQuerySortNullLastCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		ParamQueryEntity entity3 = new ParamQueryEntity("3", "对公客户证件类型", "ID Document Type of Corporate Customer", "TEC_00000000000561",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), null);

		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2, entity3));

		// when
		EntityQuery entityQuery = new EntityQuery();
		entityQuery.setSort(Sort.by(Sort.Order.asc("viewCount").with(Sort.NullHandling.NULLS_LAST)));
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(3);
		assertThat(entities).containsExactlyElementsOf(Arrays.asList(entity2, entity1, entity3));
	}

	@Test
	void shouldQueryByUnPageableCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		ParamQueryEntity entity3 = new ParamQueryEntity("3", "对公客户证件类型", "ID Document Type of Corporate Customer", "TEC_00000000000561",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), null);

		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2, entity3));

		// when
		EntityQuery entityQuery = new EntityQuery();
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(Pageable.unpaged(), entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(3);
		assertThat(entities).containsExactlyInAnyOrder(entity2, entity1, entity3);
	}

	@Test
	void shouldQueryByPagedCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		ParamQueryEntity entity3 = new ParamQueryEntity("3", "对公客户证件类型", "ID Document Type of Corporate Customer", "TEC_00000000000561",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), null);

		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2, entity3));

		// when
		EntityQuery entityQuery = new EntityQuery();
		List<ParamQueryEntity> entities = paramQueryRepository.listByQuery(PageRequest.of(0, 1), entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(1);
	}

	@Test
	void shouldQueryIncludeCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		ParamQueryEntity entity3 = new ParamQueryEntity("3", "对公客户证件类型", "ID Document Type of Corporate Customer", "TEC_00000000000561",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), null);

		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2, entity3));

		// when
		EntityQuery entityQuery = new EntityQuery();
		List<ParamQueryEntity> entities = paramQueryRepository.listByQueryThenInclude(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(3);
		assertThat(entities).satisfies(it -> {
			it.forEach(e -> {
				assertThat(e.getId()).isNotNull();
				assertThat(e.getChineseName()).isNotNull();
				assertThat(e.getEnglishName()).isNull();
				assertThat(e.getAssetMean()).isNull();
				assertThat(e.getAssetType()).isNull();
				assertThat(e.getExtendedAttrs()).isNull();
				assertThat(e.getIsDeleted()).isNull();
				assertThat(e.getPublishBatchList()).isNull();
				assertThat(e.getPublishState()).isNull();
				assertThat(e.getTimestamp()).isNull();
				assertThat(e.getViewCount()).isNull();
			});
		});
	}

	@Test
	void shouldQueryExcludeCorrectly() {

		// given
		ParamQueryEntity entity1 = new ParamQueryEntity("1", "发证机关国家或地区", "Licence-issuing Countries or Area", "TEC_00000000000448",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, true, null,
				ParamQueryEntity.PublishState.UNPUBLISHED, new Date(), 10);
		ParamQueryEntity entity2 = new ParamQueryEntity("2", "岗位序列代码", "Job Sequence Code", "TEC_00000000000522",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), 0);
		ParamQueryEntity entity3 = new ParamQueryEntity("3", "对公客户证件类型", "ID Document Type of Corporate Customer", "TEC_00000000000561",
				null, new ParamQueryEntity.AssetType(1L, "数据标准"), null, null, false, null,
				ParamQueryEntity.PublishState.PUBLISHED, new Date(), null);

		paramQueryRepository.saveAll(Arrays.asList(entity1, entity2, entity3));

		// when
		EntityQuery entityQuery = new EntityQuery();
		List<ParamQueryEntity> entities = paramQueryRepository.listByQueryThenExclude(entityQuery);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities).hasSize(3);
		assertThat(entities).satisfies(it -> {
			it.forEach(e -> {
				assertThat(e.getId()).isNotNull();
				assertThat(e.getChineseName()).isNotNull();
				assertThat(e.getEnglishName()).isNull();
			});
		});
	}

	interface ParamQueryRepository extends ElasticsearchRepository<ParamQueryEntity, String> {

		List<ParamQueryEntity> listByQuery(@ParamQuery EntityQuery query);

		List<ParamQueryEntity> listByQuery(Pageable pageable, @ParamQuery EntityQuery query);

		@SourceFilters(includes = {"id", "chineseName"})
		List<ParamQueryEntity> listByQueryThenInclude(@ParamQuery EntityQuery query);

		@SourceFilters(excludes = "englishName")
		List<ParamQueryEntity> listByQueryThenExclude(@ParamQuery EntityQuery query);
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

		@Field(value = "recommendExtendedAttrs", type = FieldType.Nested)
		private List<RecommendExtendedAttr> recommendExtendedAttrs;

		@Field(value = "isDeleted", type = FieldType.Boolean)
		private Boolean isDeleted;

		@Field(value = "publishBatchList", type = FieldType.Nested)
		private List<PublishBatch> publishBatchList;

		@Field(value = "publishState", type = FieldType.Object)
		@ValueConverter(PublishStateConverter.class)
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
		static class RecommendExtendedAttr {
			@Id
			private String id;
			@Field(value = "extendedAttrs", type = FieldType.Nested)
			private List<ExtendedAttr> extendedAttrs;
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
		enum PublishState {

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
		}

		static class PublishStateConverter implements PropertyValueConverter {

			@Override
			public Object write(Object value) {
				return value;
			}

			@Override
			public Object read(Object value) {
				if (value instanceof Map) {
					return PublishState.value(((Map<String, String>) value).get("value"));
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

		@QueryField(value = "id", type = Type.IN)
		private List<String> ids;

		@QueryField(value = {"chineseName", "englishName"}, relation = Operator.OR)
		private String keyword;

		@QueryField(value = "chineseName.keyword")
		private String chineseName;

		@QueryField(value = "chineseName.keyword", type = Type.STARTING_WITH)
		private String chineseNamePrefix;

		@QueryField(value = "chineseName.keyword", type = Type.ENDING_WITH)
		private String chineseNameSuffix;

		@QueryField(value = "chineseName.keyword", type = Type.LIKE)
		private String chineseNameLike;

		@QueryField(value = "englishName.keyword")
		private String englishName;

		@QueryField(value = "englishName.keyword", type = Type.STARTING_WITH)
		private String englishNamePrefix;

		@QueryField(value = "englishName.keyword", type = Type.ENDING_WITH)
		private String englishNameSuffix;

		@QueryField(value = "englishName.keyword", type = Type.LIKE)
		private String englishNameLike;

		@QueryField(value = "assetCode")
		private String assetCode;

		@QueryField(value = "assetCode", type = Type.IN)
		private List<String> assetCodes;

		@QueryField(value = "assetType.id")
		private Long assetTypeId;

		@QueryField(value = "assetType.id", type = Type.IN)
		private List<Long> assetTypeIds;

		@QueryField(value = "assetType.id", type = Type.NEGATING_SIMPLE_PROPERTY)
		private Long assetTypeIdExclude;

		@QueryField(value = "assetType", type = Type.EXISTS)
		private Boolean assetTypeExists;

		@QueryField(value = "extendedAttrs.keyLabel")
		private String extendedAttrsKeyLabel;

		@QueryField(value = "extendedAttrs.valueId")
		private String extendedAttrsValueId;

		@QueryField(value = "recommendExtendedAttrs.extendedAttrs.keyLabel")
		private String recommendExtendedAttrsKeyLabel;

		@QueryField(value = "recommendExtendedAttrs.extendedAttrs.valueId")
		private String recommendExtendedAttrsValueId;

		@QueryField(value = "isDeleted")
		private Boolean isDeleted;

		@QueryField(value = "publishBatchList.id")
		private Long publishBatchId;

		@QueryField(value = "publishBatchList.id", type = Type.IN)
		private List<Long> publishBatchIds;

		@QueryField(value = "publishState.value")
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

		private Sort sort;
	}
}