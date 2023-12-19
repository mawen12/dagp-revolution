package com.mawen.search.core.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mawen.search.core.aggregation.AggregationsContainer;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.document.NestedMetaData;
import com.mawen.search.core.document.SearchDocument;
import com.mawen.search.core.document.SearchDocumentResponse;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.query.TotalHitsRelation;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class SearchHitMapping<T> {
	private final Class<T> type;
	private final ElasticsearchConverter converter;
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	private SearchHitMapping(Class<T> type, ElasticsearchConverter converter) {
		Assert.notNull(type, "type is null");
		Assert.notNull(converter, "converter is null");

		this.type = type;
		this.converter = converter;
		this.mappingContext = converter.getMappingContext();
	}

	public static <T> SearchHitMapping<T> mappingFor(Class<T> entityClass, ElasticsearchConverter converter) {
		return new SearchHitMapping<>(entityClass, converter);
	}

	public SearchHits<T> mapHits(SearchDocumentResponse searchDocumentResponse, List<T> contents) {
		return mapHitsFromResponse(searchDocumentResponse, contents);
	}

	public SearchScrollHits<T> mapScrollHits(SearchDocumentResponse searchDocumentResponse, List<T> contents) {
		return mapHitsFromResponse(searchDocumentResponse, contents);
	}

	private SearchHitsImpl<T> mapHitsFromResponse(SearchDocumentResponse searchDocumentResponse, List<T> contents) {

		Assert.notNull(searchDocumentResponse, "searchDocumentResponse is null");
		Assert.notNull(contents, "contents is null");

		Assert.isTrue(searchDocumentResponse.getSearchDocuments().size() == contents.size(),
				"Count of documents must match the count of entities");

		long totalHits = searchDocumentResponse.getTotalHits();
		float maxScore = searchDocumentResponse.getMaxScore();
		String scrollId = searchDocumentResponse.getScrollId();

		List<SearchHit<T>> searchHits = new ArrayList<>();
		List<SearchDocument> searchDocuments = searchDocumentResponse.getSearchDocuments();
		for (int i = 0; i < searchDocuments.size(); i++) {
			SearchDocument document = searchDocuments.get(i);
			T content = contents.get(i);
			SearchHit<T> hit = mapHit(document, content);
			searchHits.add(hit);
		}
		AggregationsContainer<?> aggregations = searchDocumentResponse.getAggregations();
		TotalHitsRelation totalHitsRelation = TotalHitsRelation.valueOf(searchDocumentResponse.getTotalHitsRelation());

		return new SearchHitsImpl<>(totalHits, totalHitsRelation, maxScore, scrollId, searchHits,
				aggregations);
	}

	public SearchHit<T> mapHit(SearchDocument searchDocument, T content) {

		Assert.notNull(searchDocument, "searchDocument is null");
		Assert.notNull(content, "content is null");

		return new SearchHit<>(searchDocument.getIndex(), //
				searchDocument.hasId() ? searchDocument.getId() : null, //
				searchDocument.getRouting(), //
				searchDocument.getScore(), //
				searchDocument.getSortValues(), //
				getHighlightsAndRemapFieldNames(searchDocument), //
				mapInnerHits(searchDocument), //
				searchDocument.getNestedMetaData(), //
				searchDocument.getExplanation(), //
				searchDocument.getMatchedQueries(), //
				content); //
	}

	@Nullable
	private Map<String, List<String>> getHighlightsAndRemapFieldNames(SearchDocument searchDocument) {
		Map<String, List<String>> highlightFields = searchDocument.getHighlightFields();

		if (highlightFields == null) {
			return null;
		}

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(type);
		if (persistentEntity == null) {
			return highlightFields;
		}

		return highlightFields.entrySet().stream().collect(Collectors.toMap(entry -> {
			ElasticsearchPersistentProperty property = persistentEntity.getPersistentPropertyWithFieldName(entry.getKey());
			return property != null ? property.getName() : entry.getKey();
		}, Map.Entry::getValue));
	}

	private Map<String, SearchHits<?>> mapInnerHits(SearchDocument searchDocument) {

		Map<String, SearchHits<?>> innerHits = new LinkedHashMap<>();
		Map<String, SearchDocumentResponse> documentInnerHits = searchDocument.getInnerHits();

		if (documentInnerHits != null && documentInnerHits.size() > 0) {

			SearchHitMapping<SearchDocument> searchDocumentSearchHitMapping = SearchHitMapping
					.mappingFor(SearchDocument.class, converter);

			for (Map.Entry<String, SearchDocumentResponse> entry : documentInnerHits.entrySet()) {
				SearchDocumentResponse searchDocumentResponse = entry.getValue();

				SearchHits<SearchDocument> searchHits = searchDocumentSearchHitMapping
						.mapHitsFromResponse(searchDocumentResponse, searchDocumentResponse.getSearchDocuments());

				// map Documents to real objects
				SearchHits<?> mappedSearchHits = mapInnerDocuments(searchHits, type);

				innerHits.put(entry.getKey(), mappedSearchHits);
			}

		}
		return innerHits;
	}


	private SearchHits<?> mapInnerDocuments(SearchHits<SearchDocument> searchHits, Class<T> type) {

		if (searchHits.isEmpty()) {
			return searchHits;
		}

		try {
			ElasticsearchPersistentEntity<?> persistentEntityForType = mappingContext.getPersistentEntity(type);
			NestedMetaData nestedMetaData = searchHits.getSearchHit(0).getContent().getNestedMetaData();
			ElasticsearchPersistentEntityWithNestedMetaData persistentEntityWithNestedMetaData = getPersistentEntity(
					persistentEntityForType, nestedMetaData);

			if (persistentEntityWithNestedMetaData.entity != null) {
				List<SearchHit<Object>> convertedSearchHits = new ArrayList<>();
				Class<?> targetType = persistentEntityWithNestedMetaData.entity.getType();

				// convert the list of SearchHit<SearchDocument> to list of SearchHit<Object>
				searchHits.getSearchHits().forEach(searchHit -> {
					SearchDocument searchDocument = searchHit.getContent();

					Object targetObject = converter.read(targetType, searchDocument);
					convertedSearchHits.add(new SearchHit<>(searchDocument.getIndex(), //
							searchDocument.getId(), //
							searchDocument.getRouting(), //
							searchDocument.getScore(), //
							searchDocument.getSortValues(), //
							searchDocument.getHighlightFields(), //
							searchHit.getInnerHits(), //
							getPersistentEntity(persistentEntityForType, //
									searchHit.getContent().getNestedMetaData()).nestedMetaData, //
							searchHit.getExplanation(), //
							searchHit.getMatchedQueries(), //
							targetObject));
				});

				String scrollId = null;
				if (searchHits instanceof SearchHitsImpl<?>) {
					SearchHitsImpl<?> searchHitsImpl = (SearchHitsImpl<?>) searchHits;
					scrollId = searchHitsImpl.getScrollId();
				}

				return new SearchHitsImpl<>(searchHits.getTotalHits(), //
						searchHits.getTotalHitsRelation(), //
						searchHits.getMaxScore(), //
						scrollId, //
						convertedSearchHits, //
						searchHits.getAggregations());
			}
		}
		catch (Exception e) {
			throw new MappingException("Unable to convert inner hits.", e);
		}

		return searchHits;
	}

	/**
	 * find a {@link ElasticsearchPersistentEntity} following the property chain defined by the nested metadata
	 *
	 * @param persistentEntity base entity
	 * @param nestedMetaData   nested metadata
	 * @return A {@link ElasticsearchPersistentEntityWithNestedMetaData} containing the found entity or null together with
	 * the {@link NestedMetaData} that has mapped field names.
	 */
	private ElasticsearchPersistentEntityWithNestedMetaData getPersistentEntity(
			@Nullable ElasticsearchPersistentEntity<?> persistentEntity, @Nullable NestedMetaData nestedMetaData) {

		NestedMetaData currentMetaData = nestedMetaData;
		List<NestedMetaData> mappedNestedMetaDatas = new LinkedList<>();

		while (persistentEntity != null && currentMetaData != null) {
			ElasticsearchPersistentProperty persistentProperty = persistentEntity
					.getPersistentPropertyWithFieldName(currentMetaData.getField());

			if (persistentProperty == null) {
				persistentEntity = null;
			}
			else {
				persistentEntity = mappingContext.getPersistentEntity(persistentProperty.getActualType());
				mappedNestedMetaDatas.add(0,
						NestedMetaData.of(persistentProperty.getName(), currentMetaData.getOffset(), null));
				currentMetaData = currentMetaData.getChild();
			}
		}

		NestedMetaData mappedNestedMetaData = mappedNestedMetaDatas.stream().reduce(null,
				(result, nmd) -> NestedMetaData.of(nmd.getField(), nmd.getOffset(), result));

		return new ElasticsearchPersistentEntityWithNestedMetaData(persistentEntity, mappedNestedMetaData);
	}

	private static class ElasticsearchPersistentEntityWithNestedMetaData {
		@Nullable
		private ElasticsearchPersistentEntity<?> entity;
		private NestedMetaData nestedMetaData;

		public ElasticsearchPersistentEntityWithNestedMetaData(@Nullable ElasticsearchPersistentEntity<?> entity,
				NestedMetaData nestedMetaData) {
			this.entity = entity;
			this.nestedMetaData = nestedMetaData;
		}
	}
}
