package com.mawen.search.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.mawen.search.InvalidApiUsageException;
import com.mawen.search.ResourceNotFoundException;
import com.mawen.search.core.annotation.Document.VersionType;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.annotation.WriteOnlyProperty;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SearchHitsIterator;
import com.mawen.search.core.domain.SearchScrollHits;
import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.core.query.HighlightQuery;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.MoreLikeThisQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.StringQuery;
import com.mawen.search.core.query.TotalHitsRelation;
import com.mawen.search.core.query.UpdateQuery;
import com.mawen.search.core.query.builder.BaseQueryBuilder;
import com.mawen.search.core.query.builder.FetchSourceFilterBuilder;
import com.mawen.search.core.query.builder.IndexQueryBuilder;
import com.mawen.search.core.query.highlight.Highlight;
import com.mawen.search.core.query.highlight.HighlightField;
import com.mawen.search.core.support.MultiGetItem;
import com.mawen.search.core.support.ScriptType;
import com.mawen.search.test.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import org.assertj.core.api.SoftAssertions;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;

import static com.mawen.search.core.annotation.FieldType.*;
import static com.mawen.search.utils.IdGenerator.*;
import static com.mawen.search.utils.IndexBuilder.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;


@SpringIntegrationTest
public abstract class ElasticsearchIntegrationTests {

	static final Integer INDEX_MAX_RESULT_WINDOW = 10_000;

	private static final String MULTI_INDEX_PREFIX = "test-index";
	private static final String MULTI_INDEX_ALL = MULTI_INDEX_PREFIX + "*";
	private static final String MULTI_INDEX_1_NAME = MULTI_INDEX_PREFIX + "-1";
	private static final String MULTI_INDEX_2_NAME = MULTI_INDEX_PREFIX + "-2";
	private static final String MULTI_INDEX_3_NAME = MULTI_INDEX_PREFIX + "-3";

	@Autowired protected ElasticsearchOperations operations;
	@Autowired protected IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {

		indexNameProvider.increment();
	}

	protected abstract Query queryWithIds(String... ids);

	// protected abstract BaseQueryBuilder<?, ?> getMatchAllQueryBuilder();

	private Query queryWithIds(Collection<String> ids) {
		return queryWithIds(ids.toArray(new String[ids.size()]));
	}

	protected abstract BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery();

	protected abstract BaseQueryBuilder<?, ?> getBuilderWithMatchQuery(String field, String value);

	protected abstract BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value);

	protected abstract BaseQueryBuilder<?, ?> getBuilderWithWildcardQuery(String field, String value);

	protected abstract Query getQueryWithCollapse(String collapseField, @Nullable String innerHits,
			@Nullable Integer size);

	protected abstract Query getMatchAllQueryWithFilterForId(String id);

	protected abstract Query getQueryForParentId(String type, String id, @Nullable String route);


	@Test // #2304
	public void shouldThrowDataAccessExceptionIfDocumentDoesNotExistWhileDoingPartialUpdateByEntity() {

		// given
		String documentId = nextIdAsString();
		String messageBeforeUpdate = "some test message";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		assertThatThrownBy(() -> operations.update(sampleEntity)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test // #2405
	public void shouldNotIgnoreIdFromIndexQuery() {
		String indexName = indexNameProvider.indexName();
		IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);

		SampleEntity object1 = SampleEntity.builder().id("objectId1").message("objectMessage1").build();
		SampleEntity object2 = SampleEntity.builder().id("objectId2").message("objectMessage2").build();
		List<IndexQuery> indexQueries = Arrays.asList(
				new IndexQueryBuilder().withIndex(indexName).withId("idFromQuery1").withObject(object1)
						.withOpType(IndexQuery.OpType.INDEX).build(),
				new IndexQueryBuilder().withIndex(indexName).withId("idFromQuery2").withObject(object2)
						.withOpType(IndexQuery.OpType.CREATE).build());
		operations.bulkIndex(indexQueries, indexCoordinates);

		boolean foundObject1 = operations.exists("idFromQuery1", indexCoordinates);
		assertThat(foundObject1).isTrue();
		boolean foundObject2 = operations.exists("idFromQuery2", indexCoordinates);
		assertThat(foundObject2).isTrue();
	}

	@Test
	public void shouldThrowDataAccessExceptionIfDocumentDoesNotExistWhileDoingPartialUpdate() {

		Document document = Document
				.create();
		UpdateQuery updateQuery = UpdateQuery.builder(nextIdAsString()).withDocument(document).build();
		assertThatThrownBy(() -> operations.update(updateQuery, IndexCoordinates.of(indexNameProvider.indexName())))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	public void shouldReturnCountForGivenCriteriaQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when

		long count = operations.count(criteriaQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void shouldReturnCountForGivenSearchQuery() {

		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();

		long count = operations.count(searchQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void shouldReturnObjectForGivenId() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();
		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		SampleEntity sampleEntity1 = operations.get(documentId, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(sampleEntity1).isEqualTo(sampleEntity);
	}

	@Test
	public void shouldReturnObjectsForGivenIdsUsingMultiGet() {

		// given
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some message")
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		Query query = queryWithIds(documentId, documentId2);
		List<MultiGetItem<SampleEntity>> sampleEntities = operations.multiGet(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(sampleEntities).hasSize(2);
		assertThat(sampleEntities.get(0).getItem()).isEqualTo(sampleEntity1);
		assertThat(sampleEntities.get(1).getItem()).isEqualTo(sampleEntity2);
	}

	@Test
	public void shouldReturnNullObjectForNotExistingIdUsingMultiGet() {

		// given
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some message")
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		List<String> idsToSearch = Arrays.asList(documentId, nextIdAsString(), documentId2);
		assertThat(idsToSearch).hasSize(3);

		Query query = queryWithIds(idsToSearch);
		List<MultiGetItem<SampleEntity>> sampleEntities = operations.multiGet(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(sampleEntities).hasSize(3);
		assertThat(sampleEntities.get(0).getItem()).isEqualTo(sampleEntity1);
		assertThat(sampleEntities.get(1).getItem()).isNull();
		assertThat(sampleEntities.get(2).getItem()).isEqualTo(sampleEntity2);
	}

	@Test // #1678
	@DisplayName("should return failure in multiget result")
	void shouldReturnFailureInMultigetResult() {

		Query query = queryWithIds("42");
		List<MultiGetItem<SampleEntity>> sampleEntities = operations.multiGet(query, SampleEntity.class,
				IndexCoordinates.of("not-existing-index"));

		// then
		assertThat(sampleEntities).hasSize(1);
		assertThat(sampleEntities.get(0).isFailed()).isTrue();
		assertThat(sampleEntities.get(0).getFailure()).isNotNull();
	}

	@Test
	public void shouldReturnSearchHitsForGivenSearchQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();

		// when
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getTotalHitsRelation()).isEqualByComparingTo(TotalHitsRelation.EQUAL_TO);
	}

	@Test
	public void shouldDoBulkIndex() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some message")
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();

		// when
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(2);
	}

	@Test // #2362
	@DisplayName("should do bulk index into different indices")
	void shouldDoBulkIndexIntoDifferentIndices() {

		String indexName = indexNameProvider.indexName();
		String documentId1 = "1";
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message").build();
		IndexQuery indexQuery1 = new IndexQueryBuilder() //
				.withId(documentId1) //
				.withObject(sampleEntity1) //
				.withIndex(indexName + "-" + documentId1) //
				.build();
		String documentId2 = "2";
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some message").build();
		IndexQuery indexQuery2 = new IndexQueryBuilder() //
				.withId(documentId2) //
				.withObject(sampleEntity2) //
				.withIndex(indexName + "-" + documentId2) //
				.build();

		List<IndexQuery> indexQueries = Arrays.asList(indexQuery1, indexQuery2);

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexName));

		SearchHits<SampleEntity> searchHits = operations.search(operations.matchAllQuery(), SampleEntity.class,
				IndexCoordinates.of(indexName + "*"));

		assertThat(searchHits.getTotalHits()).isEqualTo(2);
		searchHits.forEach(searchHit -> {
			assertThat(searchHit.getIndex()).isEqualTo(indexName + "-" + searchHit.getId());
		});
	}

	@Test
	public void shouldDoBulkUpdate() {

		// given
		String documentId = nextIdAsString();
		String messageBeforeUpdate = "some test message";
		String messageAfterUpdate = "test message";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Document document = Document.create();
		document.put("message", messageAfterUpdate);
		UpdateQuery updateQuery = UpdateQuery.builder(documentId) //
				.withDocument(document) //
				.build();

		List<UpdateQuery> queries = new ArrayList<>();
		queries.add(updateQuery);

		// when
		operations.bulkUpdate(queries, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		SampleEntity indexedEntity = operations.get(documentId, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(indexedEntity.getMessage()).isEqualTo(messageAfterUpdate);
	}

	@Test
	public void shouldDeleteDocumentForGivenId() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		operations.delete(documentId, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = getTermQuery("id", documentId);
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getTotalHits()).isEqualTo(0);
	}

	@Test
	public void shouldDeleteEntityForGivenId() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		operations.delete(documentId, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = getTermQuery("id", documentId);
		SearchHits<SampleEntity> sampleEntities = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(sampleEntities.getTotalHits()).isEqualTo(0);
	}

	@Test
	public void shouldDeleteDocumentForGivenQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		Query query = getTermQuery("id", documentId);
		operations.delete(query, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = getTermQuery("id", documentId);
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(searchHits.getTotalHits()).isEqualTo(0);
	}

	protected abstract Query getTermQuery(String field, String value);

	@Test
	public void shouldDeleteAcrossIndexWhenNoMatchingDataPresent() {

		// given
		SampleEntity sampleEntity = SampleEntity.builder() //
				.message("positive") //
				.version(System.currentTimeMillis()) //
				.build();

		IndexQuery idxQuery1 = new IndexQueryBuilder().withId(nextIdAsString()).withObject(sampleEntity).build();

		operations.index(idxQuery1, IndexCoordinates.of(MULTI_INDEX_1_NAME));

		IndexQuery idxQuery2 = new IndexQueryBuilder().withId(nextIdAsString()).withObject(sampleEntity).build();

		operations.index(idxQuery2, IndexCoordinates.of(MULTI_INDEX_2_NAME));

		// when
		Query query = getTermQuery("message", "negative");

		operations.delete(query, SampleEntity.class, IndexCoordinates.of("test-index-*"));

		// then
		Query searchQuery = getTermQuery("message", "positive");

		assertThat(operations.count(searchQuery, IndexCoordinates.of("test-index-*"))).isEqualTo(2);
	}

	@Test
	public void shouldFilterSearchResultsForGivenFilter() {

		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = getMatchAllQueryWithFilterForId(documentId);

		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	@Test
	public void shouldSortResultsGivenSortCriteria() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyz").rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = operations.matchAllQuery();
		query.addSort(Sort.by(Sort.Direction.ASC, "rate"));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(3);
		assertThat(searchHits.getSearchHit(0).getContent().getRate()).isEqualTo(sampleEntity2.getRate());
	}

	@Test
	public void shouldSortResultsGivenMultipleSortCriteria() {

		SampleEntity sampleEntity1 = SampleEntity.builder().id(nextIdAsString()).message("abc").rate(10)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(nextIdAsString()).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(nextIdAsString()).message("xyz").rate(15)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));
		Query query = operations.matchAllQuery();
		query.addSort(Sort.by(Sort.Direction.ASC, "rate"));
		query.addSort(Sort.by(Sort.Direction.ASC, "message.keyword"));

		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getTotalHits()).isEqualTo(3);
		assertThat(searchHits.getSearchHit(0).getContent().getRate()).isEqualTo(sampleEntity2.getRate());
		assertThat(searchHits.getSearchHit(1).getContent().getMessage()).isEqualTo(sampleEntity1.getMessage());
	}

	@Test
	public void shouldSortResultsGivenNullFirstSortCriteria() {

		// given
		List<IndexQuery> indexQueries;

		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(15)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).rate(10).version(System.currentTimeMillis())
				.build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();
		searchQuery.setPageable(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("message.keyword").nullsFirst())));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(3);
		assertThat(searchHits.getSearchHit(0).getContent().getRate()).isEqualTo(sampleEntity3.getRate());
		assertThat(searchHits.getSearchHit(1).getContent().getMessage()).isEqualTo(sampleEntity1.getMessage());
	}

	@Test
	public void shouldSortResultsGivenNullLastSortCriteria() {

		// given
		List<IndexQuery> indexQueries;

		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(15)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).rate(10).version(System.currentTimeMillis())
				.build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();
		searchQuery.setPageable(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("message.keyword").nullsLast())));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(3);
		assertThat(searchHits.getSearchHit(0).getContent().getRate()).isEqualTo(sampleEntity1.getRate());
		assertThat(searchHits.getSearchHit(1).getContent().getMessage()).isEqualTo(sampleEntity2.getMessage());
	}

	@Test
	public void shouldSortResultsByScore() {

		// given
		List<SampleEntity> entities = Arrays.asList( //
				SampleEntity.builder().id("1").message("green").build(), //
				SampleEntity.builder().id("2").message("yellow green").build(), //
				SampleEntity.builder().id("3").message("blue").build());

		operations.bulkIndex(getIndexQueries(entities), IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = CriteriaQuery.builder(new Criteria("message").is("green"))
				.withPageable(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("_score")))) //
				.build();

		// when
		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(2);
		assertThat(searchHits.getSearchHit(0).getContent().getId()).isEqualTo("2");
		assertThat(searchHits.getSearchHit(1).getContent().getId()).isEqualTo("1");
	}

	@Test
	public void shouldExecuteStringQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		StringQuery stringQuery = new StringQuery(StringQuery.MATCH_ALL);

		// when
		SearchHits<SampleEntity> searchHits = operations.search(stringQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	@Test
	public void shouldReturnPageableResultsGivenStringQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		StringQuery stringQuery = new StringQuery(StringQuery.MATCH_ALL, PageRequest.of(0, 10));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(stringQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldReturnSortedResultsGivenStringQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		StringQuery stringQuery = new StringQuery(StringQuery.MATCH_ALL, PageRequest.of(0, 10),
				Sort.by(Sort.Order.asc("message.keyword")));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(stringQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldReturnObjectMatchingGivenStringQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		StringQuery stringQuery = new StringQuery(" { \"term\":  { \"id\":  " + documentId + "}}");

		// when
		SearchHit<SampleEntity> sampleEntity1 = operations.searchOne(stringQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(sampleEntity1).isNotNull();
		assertThat(sampleEntity1.getContent().getId()).isEqualTo(documentId);
	}

	@Test
	public void shouldExecuteGivenCriteriaQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

		// when
		SearchHit<SampleEntity> sampleEntity1 = operations.searchOne(criteriaQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(sampleEntity1).isNotNull();
	}

	@Test
	public void shouldDeleteGivenCriteriaQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

		// when
		operations.delete(criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		StringQuery stringQuery = new StringQuery(StringQuery.MATCH_ALL);
		SearchHits<SampleEntity> sampleEntities = operations.search(stringQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(sampleEntities).isEmpty();
	}

	@Test
	public void shouldReturnFieldsBasedOnSourceFilter() {

		// given
		String documentId = nextIdAsString();
		String message = "some test message";
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(message)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		FetchSourceFilterBuilder sourceFilter = new FetchSourceFilterBuilder();
		sourceFilter.withIncludes("message");

		Query searchQuery = getBuilderWithMatchAllQuery().withSourceFilter(sourceFilter.build()).build();

		// when
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getMessage()).isEqualTo(message);
	}

	@Test
	public void shouldReturnSimilarResultsGivenMoreLikeThisQuery() {

		// given
		String sampleMessage = "So we build a web site or an application and want to add search to it, "
				+ "and then it hits us: getting search working is hard. We want our search solution to be fast,"
				+ " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, "
				+ "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, "
				+ "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";

		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId1).message(sampleMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		String documentId2 = nextIdAsString();

		operations.index(
				getIndexQuery(
						SampleEntity.builder().id(documentId2).message(sampleMessage).version(System.currentTimeMillis()).build()),
				IndexCoordinates.of(indexNameProvider.indexName()));

		MoreLikeThisQuery moreLikeThisQuery = new MoreLikeThisQuery();
		moreLikeThisQuery.setId(documentId2);
		moreLikeThisQuery.addFields("message");
		moreLikeThisQuery.setMinDocFreq(1);

		// when
		SearchHits<SampleEntity> searchHits = operations.search(moreLikeThisQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		List<SampleEntity> content = searchHits.getSearchHits().stream().map(SearchHit::getContent)
				.collect(Collectors.toList());
		assertThat(content).contains(sampleEntity);
	}

	@Test // #1787
	@DisplayName("should use Pageable on MoreLikeThis queries")
	void shouldUsePageableOnMoreLikeThisQueries() {

		String sampleMessage = "So we build a web site or an application and want to add search to it, "
				+ "and then it hits us: getting search working is hard. We want our search solution to be fast,"
				+ " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, "
				+ "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, "
				+ "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";
		String referenceId = nextIdAsString();
		Collection<String> ids = IntStream.rangeClosed(1, 10).mapToObj(i -> nextIdAsString()).collect(Collectors.toList());
		ids.add(referenceId);
		ids.stream()
				.map(id -> getIndexQuery(
						SampleEntity.builder().id(id).message(sampleMessage).version(System.currentTimeMillis()).build()))
				.forEach(indexQuery -> operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName())));

		MoreLikeThisQuery moreLikeThisQuery = new MoreLikeThisQuery();
		moreLikeThisQuery.setId(referenceId);
		moreLikeThisQuery.addFields("message");
		moreLikeThisQuery.setMinDocFreq(1);
		moreLikeThisQuery.setPageable(PageRequest.of(0, 5));

		SearchHits<SampleEntity> searchHits = operations.search(moreLikeThisQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getTotalHits()).isEqualTo(10);
		assertThat(searchHits.getSearchHits()).hasSize(5);

		Collection<String> returnedIds = searchHits.getSearchHits().stream().map(SearchHit::getId)
				.collect(Collectors.toList());

		moreLikeThisQuery.setPageable(PageRequest.of(1, 5));

		searchHits = operations.search(moreLikeThisQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getTotalHits()).isEqualTo(10);
		assertThat(searchHits.getSearchHits()).hasSize(5);

		searchHits.getSearchHits().stream().map(SearchHit::getId).forEach(returnedIds::add);

		assertThat(returnedIds).hasSize(10);
		assertThat(ids).containsAll(returnedIds);
	}

	@Test
	public void shouldReturnResultsWithScanAndScrollForGivenCriteriaQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scroll.getScrollId(), 1000,
					SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scroll.getScrollId());
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsWithScanAndScrollForGivenSearchQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then

		Query searchQuery = operations.matchAllQuery();
		searchQuery.setPageable(PageRequest.of(0, 10));

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				searchQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scroll.getScrollId(), 1000,
					SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scroll.getScrollId());
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsWithScanAndScrollForSpecifiedFieldsForCriteriaQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addFields("message");
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		String scrollId = scroll.getScrollId();
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scrollId = scroll.getScrollId();
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scrollId, 1000, SampleEntity.class,
					IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsWithScanAndScrollForSpecifiedFieldsForSearchCriteria() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = getBuilderWithMatchAllQuery().withFields("message").withPageable(PageRequest.of(0, 10)).build();

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				searchQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		String scrollId = scroll.getScrollId();
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scrollId = scroll.getScrollId();
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scrollId, 1000, SampleEntity.class,
					IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsForScanAndScrollWithCustomResultMapperForGivenCriteriaQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		String scrollId = scroll.getScrollId();
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scrollId = scroll.getScrollId();
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scrollId, 1000, SampleEntity.class,
					IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsForScanAndScrollWithCustomResultMapperForGivenSearchQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = getBuilderWithMatchAllQuery().withPageable(PageRequest.of(0, 10)).build();

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				searchQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		String scrollId = scroll.getScrollId();
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scrollId = scroll.getScrollId();
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scrollId, 1000, SampleEntity.class,
					IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsWithScanAndScrollForGivenCriteriaQueryAndClass() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		String scrollId = scroll.getScrollId();
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scrollId = scroll.getScrollId();
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scrollId, 1000, SampleEntity.class,
					IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsWithScanAndScrollForGivenSearchQueryAndClass() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = getBuilderWithMatchAllQuery().withPageable(PageRequest.of(0, 10)).build();

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				searchQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		String scrollId = scroll.getScrollId();
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scrollId = scroll.getScrollId();
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scrollId, 1000, SampleEntity.class,
					IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnAllResultsWithStreamForGivenCriteriaQuery() {

		operations.bulkIndex(createSampleEntitiesWithMessage("Test message", 30),
				IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		long count = StreamUtils.createStreamFromIterator(operations.searchForStream(criteriaQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()))).count();

		assertThat(count).isEqualTo(30);
	}

	@Test // DATAES-831
	void shouldLimitStreamResultToRequestedSize() {

		operations.bulkIndex(createSampleEntitiesWithMessage("Test message", 30),
				IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.setMaxResults(10);

		long count = StreamUtils.createStreamFromIterator(operations.searchForStream(criteriaQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()))).count();

		assertThat(count).isEqualTo(10);
	}

	private static List<IndexQuery> createSampleEntitiesWithMessage(String message, int numberOfEntities) {
		List<IndexQuery> indexQueries = new ArrayList<>();
		for (int i = 0; i < numberOfEntities; i++) {
			String documentId = UUID.randomUUID().toString();
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage(message);
			sampleEntity.setRate(2);
			sampleEntity.setVersion(System.currentTimeMillis());
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(documentId);
			indexQuery.setObject(sampleEntity);
			indexQueries.add(indexQuery);
		}
		return indexQueries;
	}

	@Test
	public void shouldReturnListForGivenCriteria() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("test test").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("some message").rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery singleCriteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));
		CriteriaQuery multipleCriteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("some").and("message").contains("message"));
		SearchHits<SampleEntity> sampleEntitiesForSingleCriteria = operations.search(singleCriteriaQuery,
				SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		SearchHits<SampleEntity> sampleEntitiesForAndCriteria = operations.search(multipleCriteriaQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		// then
		assertThat(sampleEntitiesForSingleCriteria).hasSize(2);
		assertThat(sampleEntitiesForAndCriteria).hasSize(1);
	}

	@Test
	public void shouldReturnListForGivenStringQuery() {

		// given
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("test test").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("some message").rate(15)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		StringQuery stringQuery = new StringQuery(StringQuery.MATCH_ALL);
		SearchHits<SampleEntity> sampleEntities = operations.search(stringQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(sampleEntities).hasSize(3);
	}

	@Test // #2304
	public void shouldDoPartialUpdateBySuppliedEntityForExistingDocument() {

		// given
		String documentId = nextIdAsString();
		String messageBeforeUpdate = "some test message";
		String messageAfterUpdate = "test message";
		String originalTypeInfo = "some type";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(messageBeforeUpdate)
				.type(originalTypeInfo).version(System.currentTimeMillis()).build();
		operations.save(sampleEntity);

		// modify the entity
		sampleEntity.setMessage(messageAfterUpdate);
		sampleEntity.setType(null);

		// when
		operations.update(sampleEntity);

		// then
		SampleEntity indexedEntity = operations.get(documentId, SampleEntity.class);
		assertThat(indexedEntity.getType()).isEqualTo(originalTypeInfo);
		assertThat(indexedEntity.getMessage()).isEqualTo(messageAfterUpdate);
	}

	@Test
	public void shouldDoPartialUpdateForExistingDocument() {

		// given
		String documentId = nextIdAsString();
		String messageBeforeUpdate = "some test message";
		String messageAfterUpdate = "test message";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Document document = Document.create();
		document.put("message", messageAfterUpdate);
		UpdateQuery updateQuery = UpdateQuery.builder(documentId)//
				.withDocument(document) //
				.build();

		// when
		operations.update(updateQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		SampleEntity indexedEntity = operations.get(documentId, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(indexedEntity.getMessage()).isEqualTo(messageAfterUpdate);
	}

	@Test
	void shouldDoUpdateByQueryForExistingDocument() {

		final String documentId = nextIdAsString();
		final String messageBeforeUpdate = "some test message";
		final String messageAfterUpdate = "test message";

		final SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		final IndexQuery indexQuery = getIndexQuery(sampleEntity);

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		final Query query = operations.matchAllQuery();

		final UpdateQuery updateQuery = UpdateQuery.builder(query)
				.withScriptType(ScriptType.INLINE)
				.withScript("ctx._source['message'] = params['newMessage']").withLang("painless")
				.withParams(Collections.singletonMap("newMessage", messageAfterUpdate)).withAbortOnVersionConflict(true)
				.build();

		operations.updateByQuery(updateQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		SampleEntity indexedEntity = operations.get(documentId, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(indexedEntity.getMessage()).isEqualTo(messageAfterUpdate);
	}

	@Test
	public void shouldDoUpsertIfDocumentDoesNotExist() {

		// given
		String documentId = nextIdAsString();
		Document document = Document.create();
		document.put("message", "test message");
		UpdateQuery updateQuery = UpdateQuery.builder(documentId) //
				.withDocument(document) //
				.withDocAsUpsert(true) //
				.build();

		// when
		operations.update(updateQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		SampleEntity indexedEntity = operations.get(documentId, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(indexedEntity.getMessage()).isEqualTo("test message");
	}

	@Test
	public void shouldReturnSameEntityForMultiSearch() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac").build()));
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));
		List<Query> queries = new ArrayList<>();
		queries.add(getTermQuery("message", "ab"));
		queries.add(getTermQuery("message", "bc"));
		queries.add(getTermQuery("message", "ac"));

		List<SearchHits<SampleEntity>> searchHits = operations.multiSearch(queries, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		for (SearchHits<SampleEntity> sampleEntity : searchHits) {
			assertThat(sampleEntity.getTotalHits()).isEqualTo(1);
		}
	}

	@Test
	public void shouldIndexDocumentForSpecifiedSource() {

		// given
		String documentSource = "{\"id\":\"2333343434\",\"type\":null,\"message\":\"some message\",\"rate\":0,\"available\":false,\"highlightedMessage\":null,\"version\":1385208779482}";
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId("2333343434");
		indexQuery.setSource(documentSource);

		// when
		IndexCoordinates index = IndexCoordinates.of(indexNameProvider.indexName());
		operations.index(indexQuery, index);

		Query searchQuery = getTermQuery("id", indexQuery.getId());

		// then
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class, index);
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getId()).isEqualTo(indexQuery.getId());
	}

	@Test
	public void shouldThrowElasticsearchExceptionWhenNoDocumentSpecified() {

		// given
		final IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId("2333343434");

		// when
		assertThatThrownBy(() -> operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName())))
				.isInstanceOf(InvalidApiUsageException.class);
	}

	@Test // DATAES-848
	public void shouldReturnIndexName() {

		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 3);
		String indexName = indexNameProvider.indexName();
		operations.bulkIndex(entities, IndexCoordinates.of(indexName));
		Query query = getBuilderWithTermQuery("message", "message").withPageable(PageRequest.of(0, 100)).build();

		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class);

		searchHits.forEach(searchHit -> assertThat(searchHit.getIndex()).isEqualTo(indexName));
	}

	@Test
	public void shouldReturnDocumentAboveMinimalScoreGivenQuery() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac").build()));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		Query searchQuery = getBoolQueryWithWildcardsFirstMustSecondShouldAndMinScore("message", "*a*", "message", "*b*",
				2.0F);

		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getMessage()).isEqualTo("ab");
	}

	protected abstract Query getBoolQueryWithWildcardsFirstMustSecondShouldAndMinScore(String firstField,
			String firstValue, String secondField, String secondValue, float minScore);

	@Test
	public void shouldReturnScores() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab xz").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac xz hi").build()));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = getBuilderWithTermQuery("message", "xz").withTrackScores(true).build();

		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getMaxScore()).isGreaterThan(0f);
		assertThat(searchHits.getSearchHit(0).getScore()).isGreaterThan(0f);
	}

	@Test
	public void shouldDoIndexWithoutId() {

		// given
		// document
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setObject(sampleEntity);

		// when
		String documentId = operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(sampleEntity.getId()).isEqualTo(documentId);

		SampleEntity result = operations.get(documentId, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(result.getId()).isEqualTo(documentId);
	}

	@Test
	public void shouldDoBulkIndexWithoutId() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		// when
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = operations.matchAllQuery();
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getTotalHits()).isEqualTo(2);

		assertThat(searchHits.getSearchHit(0).getContent().getId()).isNotNull();
		assertThat(searchHits.getSearchHit(1).getContent().getId()).isNotNull();
	}

	@Test
	public void shouldIndexMapWithIndexNameAndTypeAtRuntime() {

		// given
		Map<String, Object> person1 = new HashMap<>();
		person1.put("userId", "1");
		person1.put("email", "smhdiu@gmail.com");
		person1.put("title", "Mr");
		person1.put("firstName", "Mohsin");
		person1.put("lastName", "Husen");

		Map<String, Object> person2 = new HashMap<>();
		person2.put("userId", "2");
		person2.put("email", "akonczak@gmail.com");
		person2.put("title", "Mr");
		person2.put("firstName", "Artur");
		person2.put("lastName", "Konczak");

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId("1");
		indexQuery1.setObject(person1);

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId("2");
		indexQuery2.setObject(person2);

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);

		// when
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		Query searchQuery = operations.matchAllQuery();
		// noinspection rawtypes
		SearchHits<Map> searchHits = operations.search(searchQuery, Map.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getTotalHits()).isEqualTo(2);
		assertThat(searchHits.getSearchHit(0).getContent().get("userId")).isEqualTo(person1.get("userId"));
		assertThat(searchHits.getSearchHit(1).getContent().get("userId")).isEqualTo(person2.get("userId"));
	}

	@Test // DATAES-523
	public void shouldIndexGteEntityWithVersionType() {

		// given
		String documentId = nextIdAsString();

		GTEVersionEntity entity = new GTEVersionEntity();
		entity.setId(documentId);
		entity.setName("FooBar");
		entity.setVersion(System.currentTimeMillis());

		IndexQueryBuilder indexQueryBuilder = new IndexQueryBuilder().withId(documentId).withVersion(entity.getVersion())
				.withObject(entity);

		IndexCoordinates index = IndexCoordinates.of(indexNameProvider.indexName());
		operations.index(indexQueryBuilder.build(), index);

		Query searchQuery = operations.matchAllQuery();
		// when
		SearchHits<GTEVersionEntity> entities = operations.search(searchQuery, GTEVersionEntity.class, index);
		// then
		assertThat(entities).isNotNull();
		assertThat(entities.getTotalHits()).isGreaterThanOrEqualTo(1);

		// reindex with same version
		operations.index(indexQueryBuilder.build(), index);

		// reindex with version one below
		assertThatThrownBy(() -> operations.index(indexQueryBuilder.withVersion(entity.getVersion() - 1).build(), index))
				.hasMessageContaining("version").hasMessageContaining("conflict");
	}

	@Test
	public void shouldIndexSampleEntityWithIndexAtRuntime() {

		String indexName = indexNameProvider.indexName() + "-custom";

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder().withId(documentId).withObject(sampleEntity).build();

		operations.index(indexQuery, IndexCoordinates.of(indexName));

		Query searchQuery = operations.matchAllQuery();

		// when
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexName));

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexUsingCriteriaQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when
		long count = operations.count(criteriaQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexUsingSearchQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();

		// when
		long count = operations.count(searchQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexAndTypeUsingCriteriaQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when
		long count = operations.count(criteriaQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexAndTypeUsingSearchQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();

		// when
		long count = operations.count(searchQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenMultiIndices() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withObject(sampleEntity1).build();

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withObject(sampleEntity2).build();

		String multiIndex1 = MULTI_INDEX_1_NAME + "-" + indexNameProvider.indexName();
		String multiIndex2 = MULTI_INDEX_2_NAME + "-" + indexNameProvider.indexName();
		operations.index(indexQuery1, IndexCoordinates.of(multiIndex1));
		operations.index(indexQuery2, IndexCoordinates.of(multiIndex2));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when
		long count = operations.count(criteriaQuery, IndexCoordinates.of(multiIndex1, multiIndex2));

		// then
		assertThat(count).isEqualTo(2);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenMultiIndices() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withObject(sampleEntity1).build();

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withObject(sampleEntity2).build();

		String multiIndex1 = MULTI_INDEX_1_NAME + "-" + indexNameProvider.indexName();
		String multiIndex2 = MULTI_INDEX_2_NAME + "-" + indexNameProvider.indexName();

		operations.index(indexQuery1, IndexCoordinates.of(multiIndex1));
		operations.index(indexQuery2, IndexCoordinates.of(multiIndex2));

		Query searchQuery = operations.matchAllQuery();

		// when
		long count = operations.count(searchQuery, IndexCoordinates.of(multiIndex1, multiIndex2));

		// then
		assertThat(count).isEqualTo(2);
	}


	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexNameForSpecificIndex() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withObject(sampleEntity1).build();

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withObject(sampleEntity2).build();

		String multiIndex1 = MULTI_INDEX_1_NAME + "-" + indexNameProvider.indexName();
		String multiIndex2 = MULTI_INDEX_2_NAME + "-" + indexNameProvider.indexName();

		operations.index(indexQuery1, IndexCoordinates.of(multiIndex1));
		operations.index(indexQuery2, IndexCoordinates.of(multiIndex2));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when
		long count = operations.count(criteriaQuery, IndexCoordinates.of(multiIndex1));

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexNameForSpecificIndex() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withObject(sampleEntity1).build();

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withObject(sampleEntity2).build();

		String multiIndex1 = MULTI_INDEX_1_NAME + "-" + indexNameProvider.indexName();
		String multiIndex2 = MULTI_INDEX_2_NAME + "-" + indexNameProvider.indexName();

		operations.index(indexQuery1, IndexCoordinates.of(multiIndex1));
		operations.index(indexQuery2, IndexCoordinates.of(multiIndex2));

		Query searchQuery = operations.matchAllQuery();

		// when
		long count = operations.count(searchQuery, IndexCoordinates.of(multiIndex1));

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void shouldThrowAnExceptionForGivenCriteriaQueryWhenNoIndexSpecifiedForCountQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when
		assertThatThrownBy(() -> operations.count(criteriaQuery, (IndexCoordinates) null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldThrowAnExceptionForGivenSearchQueryWhenNoIndexSpecifiedForCountQuery() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		Query searchQuery = operations.matchAllQuery();

		// when
		assertThatThrownBy(() -> operations.count(searchQuery, (IndexCoordinates) null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldTestResultsAcrossMultipleIndices() {

		String multiIndex1 = MULTI_INDEX_1_NAME + "-" + indexNameProvider.indexName();
		String multiIndex2 = MULTI_INDEX_2_NAME + "-" + indexNameProvider.indexName();
		IndexCoordinates index1 = IndexCoordinates.of(multiIndex1);
		IndexCoordinates index2 = IndexCoordinates.of(multiIndex2);

		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withObject(sampleEntity1).build();

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withObject(sampleEntity2).build();

		operations.index(indexQuery1, index1);
		operations.index(indexQuery2, index2);

		Query searchQuery = operations.matchAllQuery();

		// when
		SearchHits<SampleEntity> sampleEntities = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(multiIndex1, multiIndex2));

		// then
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	public void shouldComposeObjectsReturnedFromHeterogeneousIndexes() {

		String multiIndex1 = MULTI_INDEX_1_NAME + "-" + indexNameProvider.indexName();
		String multiIndex2 = MULTI_INDEX_2_NAME + "-" + indexNameProvider.indexName();
		IndexCoordinates index1 = IndexCoordinates.of(multiIndex1);
		IndexCoordinates index2 = IndexCoordinates.of(multiIndex2);

		HetroEntity1 entity1 = new HetroEntity1(nextIdAsString(), "aFirstName");
		HetroEntity2 entity2 = new HetroEntity2(nextIdAsString(), "aLastName");

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(entity1.getId()).withObject(entity1).build();
		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(entity2.getId()).withObject(entity2).build();

		operations.index(indexQuery1, index1);
		operations.index(indexQuery2, index2);

		// when
		Query searchQuery = operations.matchAllQuery();
		SearchHits<ResultAggregator> page = operations.search(searchQuery, ResultAggregator.class,
				IndexCoordinates.of(multiIndex1, multiIndex2));

		assertThat(page.getTotalHits()).isEqualTo(2);
	}

	@Test // DATAES-525
	public void shouldDeleteOnlyDocumentsMatchedByDeleteQuery() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// document to be deleted
		String documentIdToDelete = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(documentIdToDelete).message("some message")
				.version(System.currentTimeMillis()).build()));

		// remaining document
		String remainingDocumentId = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(remainingDocumentId).message("some other message")
				.version(System.currentTimeMillis()).build()));
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		Query query = operations.idsQuery(Arrays.asList(documentIdToDelete));
		operations.delete(query, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		// document with id "remainingDocumentId" should still be indexed
		Query searchQuery = operations.matchAllQuery();
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getId()).isEqualTo(remainingDocumentId);
	}

	@Test
	public void shouldDeleteOnlyDocumentsMatchedByCriteriaQuery() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		// given
		// document to be deleted
		String documentIdToDelete = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(documentIdToDelete).message("some message")
				.version(System.currentTimeMillis()).build()));

		// remaining document
		String remainingDocumentId = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(remainingDocumentId).message("some other message")
				.version(System.currentTimeMillis()).build()));
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));


		// when
		SearchHits<SampleEntity> searchHits2 = operations.search(operations.matchAllQuery(), SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(searchHits2.getTotalHits()).isEqualTo(2);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(documentIdToDelete));

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getId()).isEqualTo(documentIdToDelete);

		operations.delete(criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		// document with id "remainingDocumentId" should still be indexed
		Query searchQuery = operations.matchAllQuery();
		SearchHits<SampleEntity> searchHits1 = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(searchHits1.getTotalHits()).isEqualTo(1);
		assertThat(searchHits1.getSearchHit(0).getContent().getId()).isEqualTo(remainingDocumentId);
	}

	@Test // DATAES-525
	public void shouldDeleteDocumentForGivenIdOnly() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// document to be deleted
		String documentIdToDelete = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(documentIdToDelete).message("some message")
				.version(System.currentTimeMillis()).build()));

		// remaining document
		String remainingDocumentId = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(remainingDocumentId).message("some other message")
				.version(System.currentTimeMillis()).build()));
		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		operations.delete(documentIdToDelete, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		// document with id "remainingDocumentId" should still be indexed
		Query searchQuery = operations.matchAllQuery();
		SearchHits<SampleEntity> searchHits = operations.search(searchQuery, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));
		assertThat(searchHits.getTotalHits()).isEqualTo(1L);
		assertThat(searchHits.getSearchHit(0).getContent().getId()).isEqualTo(remainingDocumentId);
	}

	@Test
	public void shouldApplyCriteriaQueryToScanAndScrollForGivenCriteriaQuery() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some message that should be found by the scroll query").version(System.currentTimeMillis()).build()));
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some other message that should be found by the scroll query").version(System.currentTimeMillis())
				.build()));
		String notFindableMessage = "this entity must not be found by the scroll query";
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString()).message(notFindableMessage)
				.version(System.currentTimeMillis()).build()));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("message"));
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				criteriaQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scroll.getScrollId(), 1000,
					SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scroll.getScrollId());

		// then
		assertThat(sampleEntities).hasSize(2);
		assertThat(
				sampleEntities.stream().map(SearchHit::getContent).map(SampleEntity::getMessage).collect(Collectors.toList()))
						.doesNotContain(notFindableMessage);
	}

	@Test // DATAES-525
	public void shouldApplySearchQueryToScanAndScrollForGivenSearchQuery() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some message that should be found by the scroll query").version(System.currentTimeMillis()).build()));
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some other message that should be found by the scroll query").version(System.currentTimeMillis())
				.build()));
		String notFindableMessage = "this entity must not be found by the scroll query";
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString()).message(notFindableMessage)
				.version(System.currentTimeMillis()).build()));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		// when
		Query query = getBuilderWithMatchQuery("message", "message").withPageable(PageRequest.of(0, 10)).build();

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000, query,
				SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scroll.getScrollId(), 1000,
					SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scroll.getScrollId());

		// then
		assertThat(sampleEntities).hasSize(2);
		assertThat(
				sampleEntities.stream().map(SearchHit::getContent).map(SampleEntity::getMessage).collect(Collectors.toList()))
						.doesNotContain(notFindableMessage);
	}

	@Test
	public void shouldRespectSourceFilterWithScanAndScrollForGivenSearchQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 3);

		// when
		operations.bulkIndex(entities, IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		SourceFilter sourceFilter = new FetchSourceFilterBuilder().withIncludes("id").build();

		Query searchQuery = getBuilderWithMatchAllQuery() //
				.withPageable(PageRequest.of(0, 10)) //
				.withSourceFilter(sourceFilter) //
				.build(); //

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000,
				searchQuery, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scroll.getScrollId(), 1000,
					SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		}
		((AbstractElasticsearchTemplate) operations).searchScrollClear(scroll.getScrollId());
		assertThat(sampleEntities).hasSize(3);
		assertThat(sampleEntities.stream().map(SearchHit::getContent).map(SampleEntity::getId).collect(Collectors.toList()))
				.doesNotContain((String) null);
		assertThat(
				sampleEntities.stream().map(SearchHit::getContent).map(SampleEntity::getMessage).collect(Collectors.toList()))
						.containsOnly((String) null);
	}

	@Test // DATAES-457
	public void shouldSortResultsGivenSortCriteriaWithScanAndScroll() {

		// given
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyz").rate(10)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = getBuilderWithMatchAllQuery() //
				.withSort(Sort.by(Sort.Order.asc("rate"))) //
				.withSort(Sort.by(Sort.Order.desc("message.keyword"))) //
				.withPageable(PageRequest.of(0, 10)).build();

		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000, query,
				SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scroll.getScrollId(), 1000,
					SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		}

		assertThat(sampleEntities).hasSize(3);
		assertThat(sampleEntities.get(0).getContent().getRate()).isEqualTo(sampleEntity2.getRate());
		assertThat(sampleEntities.get(1).getContent().getRate()).isEqualTo(sampleEntity3.getRate());
		assertThat(sampleEntities.get(1).getContent().getMessage()).isEqualTo(sampleEntity3.getMessage());
		assertThat(sampleEntities.get(2).getContent().getRate()).isEqualTo(sampleEntity1.getRate());
		assertThat(sampleEntities.get(2).getContent().getMessage()).isEqualTo(sampleEntity1.getMessage());
	}

	@Test
	public void shouldSortResultsGivenSortCriteriaFromPageableWithScanAndScroll() {

		// given
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(1)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(3)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyz").rate(2)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = getBuilderWithMatchAllQuery()
				.withPageable(
						PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "rate")))
				.build();

		// when
		SearchScrollHits<SampleEntity> scroll = ((AbstractElasticsearchTemplate) operations).searchScrollStart(1000, query,
				SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		List<SearchHit<SampleEntity>> sampleEntities = new ArrayList<>();
		while (scroll.hasSearchHits()) {
			sampleEntities.addAll(scroll.getSearchHits());
			scroll = ((AbstractElasticsearchTemplate) operations).searchScrollContinue(scroll.getScrollId(), 1000,
					SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		}

		// then
		assertThat(sampleEntities).hasSize(3);
		assertThat(sampleEntities.get(0).getContent().getRate()).isEqualTo(sampleEntity1.getRate());
		assertThat(sampleEntities.get(1).getContent().getRate()).isEqualTo(sampleEntity3.getRate());
		assertThat(sampleEntities.get(1).getContent().getMessage()).isEqualTo(sampleEntity3.getMessage());
		assertThat(sampleEntities.get(2).getContent().getRate()).isEqualTo(sampleEntity2.getRate());
		assertThat(sampleEntities.get(2).getContent().getMessage()).isEqualTo(sampleEntity2.getMessage());
	}

	@Test // DATAES-593
	public void shouldReturnDocumentWithCollapsedField() {

		// given
		SampleEntity sampleEntity = SampleEntity.builder().id(nextIdAsString()).message("message 1").rate(1)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(nextIdAsString()).message("message 2").rate(2)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(nextIdAsString()).message("message 1").rate(1)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity, sampleEntity2, sampleEntity3));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = getQueryWithCollapse("rate", null, null);

		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isEqualTo(3);
		assertThat(searchHits.getSearchHits()).hasSize(2);
		assertThat(searchHits.getSearchHit(0).getContent().getMessage()).isEqualTo("message 1");
		assertThat(searchHits.getSearchHit(1).getContent().getMessage()).isEqualTo("message 2");
	}

	@Test // #1493
	@DisplayName("should return document with collapse field and inner hits")
	public void shouldReturnDocumentWithCollapsedFieldAndInnerHits() {

		// given
		SampleEntity sampleEntity = SampleEntity.builder().id(nextIdAsString()).message("message 1").rate(1)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(nextIdAsString()).message("message 2").rate(2)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntity3 = SampleEntity.builder().id(nextIdAsString()).message("message 1").rate(1)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity, sampleEntity2, sampleEntity3));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = getQueryWithCollapse("rate", "innerHits", null);

		// when
		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isEqualTo(3);
		assertThat(searchHits.getSearchHits()).hasSize(2);
		assertThat(searchHits.getSearchHit(0).getContent().getMessage()).isEqualTo("message 1");
		assertThat(searchHits.getSearchHit(0).getInnerHits("innerHits").getTotalHits()).isEqualTo(2);
		assertThat(searchHits.getSearchHit(1).getContent().getMessage()).isEqualTo("message 2");
		assertThat(searchHits.getSearchHit(1).getInnerHits("innerHits").getTotalHits()).isEqualTo(1);
	}

	@Test // #1997
	@DisplayName("should return document with inner hits size zero")
	void shouldReturnDocumentWithInnerHitsSizeZero() {

		// given
		SampleEntity sampleEntity = SampleEntity.builder().id(nextIdAsString()).message("message 1").rate(1)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		Query query = getQueryWithCollapse("rate", "innerHits", 0);

		// when
		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		// then
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getSearchHit(0).getContent().getMessage()).isEqualTo("message 1");
	}

	private IndexQuery getIndexQuery(SampleEntity sampleEntity) {
		return new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity)
				.withVersion(sampleEntity.getVersion()).build();
	}

	private List<IndexQuery> getIndexQueries(List<SampleEntity> sampleEntities) {
		List<IndexQuery> indexQueries = new ArrayList<>();
		for (SampleEntity sampleEntity : sampleEntities) {
			indexQueries.add(getIndexQuery(sampleEntity));
		}
		return indexQueries;
	}

	@com.mawen.search.core.annotation.Document(indexName = MULTI_INDEX_2_NAME)
	class ResultAggregator {

		private String id;
		private String firstName;
		private String lastName;

		ResultAggregator(String id, String firstName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}




	@Test
	void shouldReturnSortFieldsInSearchHits() {
		IndexCoordinates index = IndexCoordinates.of(indexNameProvider.indexName());

		SearchHitsEntity entity = new SearchHitsEntity();
		entity.setId("1");
		entity.setNumber(1000L);
		entity.setKeyword("thousands");
		IndexQuery indexQuery = new IndexQueryBuilder().withId(entity.getId()).withObject(entity).build();
		operations.index(indexQuery, index);

		Query query = getBuilderWithMatchAllQuery() //
				.withSort(Sort.by(Sort.Direction.DESC, "number")) //
				.build();

		SearchHits<SearchHitsEntity> searchHits = operations.search(query, SearchHitsEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getSearchHits()).hasSize(1);

		SearchHit<SearchHitsEntity> searchHit = searchHits.getSearchHit(0);
		List<Object> sortValues = searchHit.getSortValues();
		assertThat(sortValues).hasSize(1);
		// transport client returns Long, RestHighlevelClient Integer, new ElasticsearchClient String
		java.lang.Object o = sortValues.get(0);
		if (o instanceof Integer) {
			assertThat(o).isInstanceOf(Integer.class).isEqualTo(1000);
		} else if (o instanceof Long) {
			assertThat(o).isInstanceOf(Long.class).isEqualTo(1000L);
		} else if (o instanceof String) {
			assertThat(o).isInstanceOf(String.class).isEqualTo("1000");
		} else {
			fail("unexpected object type " + o);
		}
	}

	@Test // DATAES-715
	void shouldReturnHighlightFieldsInSearchHit() {
		IndexCoordinates index = IndexCoordinates.of("test-index-highlight-entity-template");
		HighlightEntity entity = new HighlightEntity("1",
				"This message is a long text which contains the word to search for "
						+ "in two places, the first being near the beginning and the second near the end of the message");
		IndexQuery indexQuery = new IndexQueryBuilder().withId(entity.getId()).withObject(entity).build();
		operations.index(indexQuery, index);

		Query query = getBuilderWithTermQuery("message", "message") //
				.withHighlightQuery(
						new HighlightQuery(new Highlight(singletonList(new HighlightField("message"))), HighlightEntity.class))
				.build();
		SearchHits<HighlightEntity> searchHits = operations.search(query, HighlightEntity.class, index);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getSearchHits()).hasSize(1);

		SearchHit<HighlightEntity> searchHit = searchHits.getSearchHit(0);
		List<String> highlightField = searchHit.getHighlightField("message");
		assertThat(highlightField).hasSize(2);
		assertThat(highlightField.get(0)).contains("<em>message</em>");
		assertThat(highlightField.get(1)).contains("<em>message</em>");
	}

	@Test
	// DATAES-738
	void shouldSaveEntityWithIndexCoordinates() {
		String id = "42";
		SampleEntity entity = new SampleEntity();
		entity.setId(id);
		entity.setVersion(42L);
		entity.setMessage("message");

		operations.save(entity, IndexCoordinates.of(indexNameProvider.indexName()));

		SampleEntity result = operations.get(id, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(result).isEqualTo(entity);
	}

	@Test
	void shouldSaveEntityWithOutIndexCoordinates() {
		String id = "42";
		SampleEntity entity = new SampleEntity();
		entity.setId(id);
		entity.setVersion(42L);
		entity.setMessage("message");

		operations.save(entity);

		SampleEntity result = operations.get(id, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(result).isEqualTo(entity);
	}

	@Test // DATAES-738
	void shouldSaveEntityIterableWithIndexCoordinates() {
		String id1 = "42";
		SampleEntity entity1 = new SampleEntity();
		entity1.setId(id1);
		entity1.setVersion(42L);
		entity1.setMessage("message");
		String id2 = "43";
		SampleEntity entity2 = new SampleEntity();
		entity2.setId(id2);
		entity2.setVersion(43L);
		entity2.setMessage("message");

		operations.save(Arrays.asList(entity1, entity2), IndexCoordinates.of(indexNameProvider.indexName()));

		SampleEntity result1 = operations.get(id1, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		SampleEntity result2 = operations.get(id2, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(result1).isEqualTo(entity1);
		assertThat(result2).isEqualTo(entity2);
	}

	@Test
	void shouldSaveEntityIterableWithoutIndexCoordinates() {
		String id1 = "42";
		SampleEntity entity1 = new SampleEntity();
		entity1.setId(id1);
		entity1.setVersion(42L);
		entity1.setMessage("message");
		String id2 = "43";
		SampleEntity entity2 = new SampleEntity();
		entity2.setId(id2);
		entity2.setVersion(43L);
		entity2.setMessage("message");

		operations.save(Arrays.asList(entity1, entity2));

		SampleEntity result1 = operations.get(id1, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));
		SampleEntity result2 = operations.get(id2, SampleEntity.class, IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(result1).isEqualTo(entity1);
		assertThat(result2).isEqualTo(entity2);
	}

	@Test // DATAES-745
	void shouldDoExistsWithEntity() {
		String id = "42";
		SampleEntity entity = new SampleEntity();
		entity.setId(id);
		entity.setVersion(42L);
		entity.setMessage("message");

		operations.save(entity);

		assertThat(operations.exists("42", SampleEntity.class)).isTrue();
	}

	@Test
	void shouldDoExistsWithIndexCoordinates() {
		String id = "42";
		SampleEntity entity = new SampleEntity();
		entity.setId(id);
		entity.setVersion(42L);
		entity.setMessage("message");

		operations.save(entity);

		assertThat(operations.exists("42", IndexCoordinates.of(indexNameProvider.indexName()))).isTrue();
	}

	@Test // DATAES-876
	void shouldReturnSeqNoPrimaryTermOnSave() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = operations.save(original);

		assertThatSeqNoPrimaryTermIsFilled(saved);
	}

	@Test
	void shouldReturnSeqNoPrimaryTermOnBulkSave() {
		OptimisticEntity original1 = new OptimisticEntity();
		original1.setMessage("It's fine 1");
		OptimisticEntity original2 = new OptimisticEntity();
		original2.setMessage("It's fine 2");

		Iterable<OptimisticEntity> saved = operations.save(Arrays.asList(original1, original2));

		saved.forEach(this::assertThatSeqNoPrimaryTermIsFilled);
	}

	@Test // DATAES-799
	void getShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = operations.save(original);

		OptimisticEntity retrieved = operations.get(saved.getId(), OptimisticEntity.class);

		assertThatSeqNoPrimaryTermIsFilled(retrieved);
	}

	private void assertThatSeqNoPrimaryTermIsFilled(OptimisticEntity retrieved) {
		assertThat(retrieved.getSeqNoPrimaryTerm()).isNotNull();
		assertThat(retrieved.seqNoPrimaryTerm.getSequenceNumber()).isNotNull();
		assertThat(retrieved.seqNoPrimaryTerm.getSequenceNumber()).isNotNegative();
		assertThat(retrieved.seqNoPrimaryTerm.getPrimaryTerm()).isNotNull();
		assertThat(retrieved.seqNoPrimaryTerm.getPrimaryTerm()).isPositive();
	}

	@Test
	void multigetShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = operations.save(original);

		List<MultiGetItem<OptimisticEntity>> retrievedList = operations.multiGet(
				queryWithIds(Objects.requireNonNull(saved.getId())), OptimisticEntity.class,
				operations.getIndexCoordinatesFor(OptimisticEntity.class));

		assertThat(retrievedList).hasSize(1);
		OptimisticEntity retrieved = retrievedList.get(0).getItem();

		assertThatSeqNoPrimaryTermIsFilled(retrieved);
	}

	@Test // DATAES-799
	void searchShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = operations.save(original);

		SearchHits<OptimisticEntity> retrievedHits = operations.search(queryWithIds(saved.getId()), OptimisticEntity.class);
		OptimisticEntity retrieved = retrievedHits.getSearchHit(0).getContent();

		assertThatSeqNoPrimaryTermIsFilled(retrieved);
	}

	@Test // DATAES-799
	void multiSearchShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = operations.save(original);

		List<Query> queries = singletonList(queryWithIds(saved.getId()));
		List<SearchHits<OptimisticEntity>> retrievedHits = operations.multiSearch(queries, OptimisticEntity.class,
				operations.getIndexCoordinatesFor(OptimisticEntity.class));
		OptimisticEntity retrieved = retrievedHits.get(0).getSearchHit(0).getContent();

		assertThatSeqNoPrimaryTermIsFilled(retrieved);
	}

	@Test // DATAES-799
	void searchForStreamShouldReturnSeqNoPrimaryTerm() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = operations.save(original);

		SearchHitsIterator<OptimisticEntity> retrievedHits = operations.searchForStream(queryWithIds(saved.getId()),
				OptimisticEntity.class);
		OptimisticEntity retrieved = retrievedHits.next().getContent();

		assertThatSeqNoPrimaryTermIsFilled(retrieved);
	}

	@Test // DATAES-799
	void shouldThrowOptimisticLockingFailureExceptionWhenConcurrentUpdateOccursOnEntityWithSeqNoPrimaryTermProperty() {
		OptimisticEntity original = new OptimisticEntity();
		original.setMessage("It's fine");
		OptimisticEntity saved = operations.save(original);

		OptimisticEntity forEdit1 = operations.get(saved.getId(), OptimisticEntity.class);
		OptimisticEntity forEdit2 = operations.get(saved.getId(), OptimisticEntity.class);

		forEdit1.setMessage("It'll be ok");
		operations.save(forEdit1);

		forEdit2.setMessage("It'll be great");
		assertThatThrownBy(() -> operations.save(forEdit2)).isInstanceOf(OptimisticLockingFailureException.class);
	}

	@Test // DATAES-799
	void shouldThrowOptimisticLockingFailureExceptionWhenConcurrentUpdateOccursOnVersionedEntityWithSeqNoPrimaryTermProperty() {
		OptimisticAndVersionedEntity original = new OptimisticAndVersionedEntity();
		original.setMessage("It's fine");
		OptimisticAndVersionedEntity saved = operations.save(original);

		OptimisticAndVersionedEntity forEdit1 = operations.get(saved.getId(), OptimisticAndVersionedEntity.class);
		OptimisticAndVersionedEntity forEdit2 = operations.get(saved.getId(), OptimisticAndVersionedEntity.class);

		forEdit1.setMessage("It'll be ok");
		operations.save(forEdit1);

		forEdit2.setMessage("It'll be great");
		assertThatThrownBy(() -> operations.save(forEdit2)).isInstanceOf(OptimisticLockingFailureException.class);
	}

	@Test // DATAES-799
	void shouldAllowFullReplaceOfEntityWithBothSeqNoPrimaryTermAndVersion() {
		OptimisticAndVersionedEntity original = new OptimisticAndVersionedEntity();
		original.setMessage("It's fine");
		OptimisticAndVersionedEntity saved = operations.save(original);

		OptimisticAndVersionedEntity forEdit = operations.get(saved.getId(), OptimisticAndVersionedEntity.class);

		forEdit.setMessage("It'll be ok");
		operations.save(forEdit);
	}

	@Test // DATAES-908
	void shouldFillVersionOnSaveOne() {
		VersionedEntity saved = operations.save(new VersionedEntity());

		assertThat(saved.getVersion()).isNotNull();
	}

	@Test // DATAES-908
	void shouldFillVersionOnSaveIterable() {
		List<VersionedEntity> iterable = Arrays.asList(new VersionedEntity(), new VersionedEntity());
		Iterator<VersionedEntity> results = operations.save(iterable).iterator();
		VersionedEntity saved1 = results.next();
		VersionedEntity saved2 = results.next();

		assertThat(saved1.getVersion()).isNotNull();
		assertThat(saved2.getVersion()).isNotNull();
	}

	@Test // DATAES-908
	void shouldFillVersionOnSaveArray() {
		VersionedEntity[] array = { new VersionedEntity(), new VersionedEntity() };
		Iterator<VersionedEntity> results = operations.save(array).iterator();
		VersionedEntity saved1 = results.next();
		VersionedEntity saved2 = results.next();

		assertThat(saved1.getVersion()).isNotNull();
		assertThat(saved2.getVersion()).isNotNull();
	}

	@Test // DATAES-908
	void shouldFillVersionOnIndexOne() {
		VersionedEntity entity = new VersionedEntity();
		IndexQuery query = new IndexQueryBuilder().withObject(entity).build();
		operations.index(query, operations.getIndexCoordinatesFor(VersionedEntity.class));

		assertThat(entity.getVersion()).isNotNull();
	}

	@Test // DATAES-908
	void shouldFillVersionOnBulkIndex() {
		VersionedEntity entity1 = new VersionedEntity();
		VersionedEntity entity2 = new VersionedEntity();
		IndexQuery query1 = new IndexQueryBuilder().withObject(entity1).build();
		IndexQuery query2 = new IndexQueryBuilder().withObject(entity2).build();
		operations.bulkIndex(Arrays.asList(query1, query2), VersionedEntity.class);

		assertThat(entity1.getVersion()).isNotNull();
		assertThat(entity2.getVersion()).isNotNull();
	}

	@Test // DATAES-908
	void shouldFillSeqNoPrimaryKeyOnBulkIndex() {
		OptimisticEntity entity1 = new OptimisticEntity();
		OptimisticEntity entity2 = new OptimisticEntity();
		IndexQuery query1 = new IndexQueryBuilder().withObject(entity1).build();
		IndexQuery query2 = new IndexQueryBuilder().withObject(entity2).build();
		operations.bulkIndex(Arrays.asList(query1, query2), OptimisticEntity.class);

		assertThatSeqNoPrimaryTermIsFilled(entity1);
		assertThatSeqNoPrimaryTermIsFilled(entity2);
	}

	@Test // DATAES-907
	@DisplayName("should track_total_hits with default value")
	void shouldTrackTotalHitsWithDefaultValue() {

		Query queryAll = operations.matchAllQuery().setPageable(Pageable.unpaged());

		List<SampleEntity> entities = IntStream.rangeClosed(1, 15_000)
				.mapToObj(i -> SampleEntity.builder().id("" + i).build()).collect(Collectors.toList());

		operations.save(entities);

		queryAll.setTrackTotalHits(null);
		SearchHits<SampleEntity> searchHits = operations.search(queryAll, SampleEntity.class);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(searchHits.getTotalHits()).isEqualTo((long) INDEX_MAX_RESULT_WINDOW);
		softly.assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.GREATER_THAN_OR_EQUAL_TO);
		softly.assertAll();
	}

	@Test // DATAES-907
	@DisplayName("should track total hits")
	void shouldTrackTotalHits() {

		Query queryAll = operations.matchAllQuery().setPageable(Pageable.unpaged());

		List<SampleEntity> entities = IntStream.rangeClosed(1, 15_000)
				.mapToObj(i -> SampleEntity.builder().id("" + i).build()).collect(Collectors.toList());

		operations.save(entities);

		queryAll.setTrackTotalHits(true);
		queryAll.setTrackTotalHitsUpTo(12_345);
		SearchHits<SampleEntity> searchHits = operations.search(queryAll, SampleEntity.class);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(searchHits.getTotalHits()).isEqualTo(15_000L);
		softly.assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.EQUAL_TO);
		softly.assertAll();
	}

	@Test // DATAES-907
	@DisplayName("should track total hits to specific value")
	void shouldTrackTotalHitsToSpecificValue() {

		Query queryAll = operations.matchAllQuery().setPageable(Pageable.unpaged());

		List<SampleEntity> entities = IntStream.rangeClosed(1, 15_000)
				.mapToObj(i -> SampleEntity.builder().id("" + i).build()).collect(Collectors.toList());

		operations.save(entities);

		queryAll.setTrackTotalHits(null);
		queryAll.setTrackTotalHitsUpTo(12_345);
		SearchHits<SampleEntity> searchHits = operations.search(queryAll, SampleEntity.class);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(searchHits.getTotalHits()).isEqualTo(12_345L);
		softly.assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.GREATER_THAN_OR_EQUAL_TO);
		softly.assertAll();
	}

	@Test // DATAES-907
	@DisplayName("should track total hits is off")
	void shouldTrackTotalHitsIsOff() {

		Query queryAll = operations.matchAllQuery().setPageable(Pageable.unpaged());

		List<SampleEntity> entities = IntStream.rangeClosed(1, 15_000)
				.mapToObj(i -> SampleEntity.builder().id("" + i).build()).collect(Collectors.toList());

		operations.save(entities);

		queryAll.setTrackTotalHits(false);
		queryAll.setTrackTotalHitsUpTo(12_345);
		SearchHits<SampleEntity> searchHits = operations.search(queryAll, SampleEntity.class);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(searchHits.getTotalHits()).isEqualTo(10_000L);
		softly.assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
		softly.assertAll();
	}

	@Test // #1800
	@DisplayName("should work with immutable classes")
	void shouldWorkWithImmutableClasses() {

		ImmutableEntity entity = new ImmutableEntity(null, "some text", null);

		ImmutableEntity saved = operations.save(entity);

		assertThat(saved).isNotNull();
		assertThat(saved.getId()).isNotEmpty();
		SeqNoPrimaryTerm seqNoPrimaryTerm = saved.getSeqNoPrimaryTerm();
		assertThat(seqNoPrimaryTerm).isNotNull();

		ImmutableEntity retrieved = operations.get(saved.getId(), ImmutableEntity.class);

		assertThat(retrieved).isEqualTo(saved);
	}

	@Test // #1893
	@DisplayName("should index document from source with version")
	void shouldIndexDocumentFromSourceWithVersion() {

		String source = "{\n" +
						"  \"answer\": 42\n" +
						"}";
		IndexQuery query = new IndexQueryBuilder() //
				.withId("42") //
				.withSource(source) //
				.withVersion(42L) //
				.build();

		operations.index(query, IndexCoordinates.of(indexNameProvider.indexName()));
	}

	@Test // #1945
	@DisplayName("should error on sort with unmapped field and default settings")
	void shouldErrorOnSortWithUnmappedFieldAndDefaultSettings() {

		Sort.Order order = new Sort.Order(Sort.Direction.ASC, "unmappedField");
		Query query = operations.matchAllQuery().addSort(Sort.by(order));

		assertThatThrownBy(() -> operations.search(query, SampleEntity.class));
	}

	// region entities
	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text) private String type;
		@Nullable
		@Field(type = Text) private String message;
		@Nullable private int rate;
		@Nullable private boolean available;
		@Nullable
		@Version private Long version;

		static Builder builder() {
			return new Builder();
		}

		static class Builder {

			@Nullable private String id;
			@Nullable private String type;
			@Nullable private String message;
			@Nullable private Long version;
			@Nullable private int rate;

			public Builder id(String id) {
				this.id = id;
				return this;
			}

			public Builder type(String type) {
				this.type = type;
				return this;
			}

			public Builder message(String message) {
				this.message = message;
				return this;
			}

			public Builder version(Long version) {
				this.version = version;
				return this;
			}

			public Builder rate(int rate) {
				this.rate = rate;
				return this;
			}

			public SampleEntity build() {
				SampleEntity sampleEntity = new SampleEntity();
				sampleEntity.setId(id);
				sampleEntity.setType(type);
				sampleEntity.setMessage(message);
				sampleEntity.setRate(rate);
				sampleEntity.setVersion(version);
				return sampleEntity;
			}
		}

		public SampleEntity() {}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		public int getRate() {
			return rate;
		}

		public void setRate(int rate) {
			this.rate = rate;
		}


		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}

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

	@Test // #2230
	@DisplayName("should work with readonly id")
	void shouldWorkWithReadonlyId() {

		ReadonlyIdEntity entity = new ReadonlyIdEntity();
		entity.setPart1("foo");
		entity.setPart2("bar");
		operations.save(entity);

		ReadonlyIdEntity readEntity = operations.get(entity.getId(), ReadonlyIdEntity.class);

		assertThat(readEntity.getPart1()).isEqualTo(entity.getPart1());
		assertThat(readEntity.getPart2()).isEqualTo(entity.getPart2());
	}

	@Test // #1489
	@DisplayName("should handle non-field-backed properties")
	void shouldHandleNonFieldBackedProperties() {

		NonFieldBackedPropertyClass entity = new NonFieldBackedPropertyClass();
		entity.setId("007");
		entity.setFirstName("James");
		entity.setLastName("Bond");

		operations.save(entity);

		SearchHits<NonFieldBackedPropertyClass> searchHits = operations.search(new CriteriaQuery(Criteria.where("fullName").is("jamesbond")),
				NonFieldBackedPropertyClass.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent()).isEqualTo(entity);
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class SampleEntityUUIDKeyed {
		@Nullable
		@Id private UUID id;
		@Nullable private String type;
		@Nullable
		@Field(type = FieldType.Text) private String message;
		@Nullable private int rate;
		@Nullable private boolean available;
		@Nullable
		@Version private Long version;

		@Nullable
		public UUID getId() {
			return id;
		}

		public void setId(@Nullable UUID id) {
			this.id = id;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		public int getRate() {
			return rate;
		}

		public void setRate(int rate) {
			this.rate = rate;
		}

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "i-need-my-own-index")
	static class Book {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Object) private Author author;
		@Nullable
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();

		public Book(@Nullable String id, @Nullable String name, @Nullable Author author,
				@Nullable Map<java.lang.Integer, Collection<String>> buckets) {
			this.id = id;
			this.name = name;
			this.author = author;
			this.buckets = buckets;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(@Nullable Author author) {
			this.author = author;
		}

		@Nullable
		public Map<java.lang.Integer, Collection<String>> getBuckets() {
			return buckets;
		}

		public void setBuckets(@Nullable Map<java.lang.Integer, Collection<String>> buckets) {
			this.buckets = buckets;
		}

		static Builder builder() {
			return new Builder();
		}

		static class Builder {
			@Nullable private String id;
			@Nullable private String name;
			@Nullable private Author author;
			@Nullable private Map<Integer, Collection<String>> buckets = new HashMap<>();
			@Nullable private String description;

			public Builder id(@Nullable String id) {
				this.id = id;
				return this;
			}

			public Builder name(@Nullable String name) {
				this.name = name;
				return this;
			}

			public Builder author(@Nullable Author author) {
				this.author = author;
				return this;
			}

			public Builder buckets(@Nullable Map<java.lang.Integer, Collection<String>> buckets) {
				this.buckets = buckets;
				return this;
			}

			public Builder description(@Nullable String description) {
				this.description = description;
				return this;
			}

			Book build() {
				return new Book(id, name, author, buckets);
			}
		}
	}

	static class Author {
		@Nullable private String id;
		@Nullable private String name;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}", versionType = VersionType.EXTERNAL_GTE)
	private static class GTEVersionEntity {
		@Nullable
		@Version private Long version;
		@Nullable
		@Id private String id;
		@Nullable private String name;

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "test-index-hetro1-core-template")
	static class HetroEntity1 {
		@Nullable
		@Id private String id;
		@Nullable private String firstName;
		@Nullable
		@Version private Long version;

		HetroEntity1(String id, String firstName) {
			this.id = id;
			this.firstName = firstName;
			this.version = System.currentTimeMillis();
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "test-index-hetro2-core-template")
	static class HetroEntity2 {

		@Nullable
		@Id private String id;
		@Nullable private String lastName;
		@Nullable
		@Version private Long version;

		HetroEntity2(String id, String lastName) {
			this.id = id;
			this.lastName = lastName;
			this.version = System.currentTimeMillis();
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class UseServerConfigurationEntity {

		@Nullable
		@Id private String id;
		@Nullable private String val;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getVal() {
			return val;
		}

		public void setVal(@Nullable String val) {
			this.val = val;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SearchHitsEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Long) Long number;
		@Nullable
		@Field(type = FieldType.Keyword) String keyword;

		public SearchHitsEntity() {}

		public SearchHitsEntity(@Nullable String id, @Nullable java.lang.Long number, @Nullable String keyword) {
			this.id = id;
			this.number = number;
			this.keyword = keyword;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public java.lang.Long getNumber() {
			return number;
		}

		public void setNumber(@Nullable java.lang.Long number) {
			this.number = number;
		}

		@Nullable
		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(@Nullable String keyword) {
			this.keyword = keyword;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "test-index-highlight-entity-template")
	static class HighlightEntity {
		@Nullable
		@Id private String id;
		@Nullable private String message;

		public HighlightEntity(@Nullable String id, @Nullable String message) {
			this.id = id;
			this.message = message;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}")
	static class OptimisticEntity {
		@Nullable
		@Id private String id;
		@Nullable private String message;
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "test-index-optimistic-and-versioned-entity-template")
	static class OptimisticAndVersionedEntity {
		@Nullable
		@Id private String id;
		@Nullable private String message;
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;
		@Nullable
		@Version private Long version;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "test-index-versioned-entity-template")
	static class VersionedEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Version private Long version;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleJoinEntity {
		@Nullable
		@Id
		@Field(type = Keyword) private String uuid;

		@Nullable
		public String getUuid() {
			return uuid;
		}

		public void setUuid(@Nullable String uuid) {
			this.uuid = uuid;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}-immutable")
	private static final class ImmutableEntity {
		@Id
		@Nullable private final String id;
		@Field(type = FieldType.Text) private final String text;
		@Nullable private final SeqNoPrimaryTerm seqNoPrimaryTerm;

		public ImmutableEntity(@Nullable String id, String text, @Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.id = id;
			this.text = text;
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			ImmutableEntity that = (ImmutableEntity) o;

			if (!id.equals(that.id))
				return false;
			if (!text.equals(that.text))
				return false;
			return Objects.equals(seqNoPrimaryTerm, that.seqNoPrimaryTerm);
		}

		@Override
		public int hashCode() {
			int result = id.hashCode();
			result = 31 * result + text.hashCode();
			result = 31 * result + (seqNoPrimaryTerm != null ? seqNoPrimaryTerm.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "ImmutableEntity{" + "id='" + id + '\'' + ", text='" + text + '\'' + ", seqNoPrimaryTerm="
					+ seqNoPrimaryTerm + '}';
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}-immutable-scripted")
	public static final class ImmutableWithScriptedEntity {
		@Id private final String id;
		@Field(type = Integer)
		@Nullable private final int rate;

		public ImmutableWithScriptedEntity(String id, int rate) {
			this.id = id;
			this.rate = rate;
		}

		public String getId() {
			return id;
		}

		public int getRate() {
			return rate;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			ImmutableWithScriptedEntity that = (ImmutableWithScriptedEntity) o;

			if (rate != that.rate)
				return false;
			if (!id.equals(that.id))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int result = id.hashCode();
			result = 31 * result + rate;
			return result;
		}

		@Override
		public String toString() {
			return "ImmutableWithScriptedEntity{" + "id='" + id + '\'' + ", rate=" + rate
					+ '}';
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}-readonly-id")
	static class ReadonlyIdEntity {
		@Field(type = FieldType.Keyword) private String part1;

		@Field(type = FieldType.Keyword) private String part2;

		@Id
		@WriteOnlyProperty
		@AccessType(AccessType.Type.PROPERTY)
		public String getId() {
			return part1 + '-' + part2;
		}

		public String getPart1() {
			return part1;
		}

		public void setPart1(String part1) {
			this.part1 = part1;
		}

		public String getPart2() {
			return part2;
		}

		public void setPart2(String part2) {
			this.part2 = part2;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}-readonly-id")
	static class NonFieldBackedPropertyClass {
		@Id
		@Nullable private String id;

		@Nullable
		@Field(type = Text) private String firstName;

		@Nullable
		@Field(type = Text) private String lastName;

		@Field(type = Keyword)
		@WriteOnlyProperty
		@AccessType(AccessType.Type.PROPERTY)
		public String getFullName() {
			return sanitize(firstName) + sanitize(lastName);
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		private String sanitize(@Nullable String s) {
			return s == null ? "" : s.replaceAll("\\s", "").toLowerCase();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			NonFieldBackedPropertyClass that = (NonFieldBackedPropertyClass) o;

			if (!Objects.equals(id, that.id))
				return false;
			if (!Objects.equals(firstName, that.firstName))
				return false;
			return Objects.equals(lastName, that.lastName);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
			result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
			return result;
		}
	}
	// endregion
}
