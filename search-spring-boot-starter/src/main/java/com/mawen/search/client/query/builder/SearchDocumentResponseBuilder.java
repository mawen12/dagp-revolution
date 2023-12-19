package com.mawen.search.client.query.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchTemplateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonpMapper;
import com.mawen.search.client.DocumentAdapters;
import com.mawen.search.client.aggregation.ElasticsearchAggregations;
import com.mawen.search.client.EntityAsMap;
import com.mawen.search.core.query.TotalHitsRelation;
import com.mawen.search.core.document.SearchDocument;
import com.mawen.search.core.document.SearchDocumentResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class SearchDocumentResponseBuilder {

	public static <T> SearchDocumentResponse from(ResponseBody<EntityAsMap> responseBody,
			SearchDocumentResponse.EntityCreator<T> entityCreator, JsonpMapper jsonpMapper) {

		Assert.notNull(responseBody, "responseBody must not be null");
		Assert.notNull(entityCreator, "entityCreator must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		HitsMetadata<EntityAsMap> hitsMetadata = responseBody.hits();
		String scrollId = responseBody.scrollId();
		Map<String, Aggregate> aggregations = responseBody.aggregations();
		Map<String, List<Suggestion<EntityAsMap>>> suggest = responseBody.suggest();
		String pointInTimeId = responseBody.pitId();

		return from(hitsMetadata, scrollId, pointInTimeId, aggregations, suggest, entityCreator, jsonpMapper);
	}


	public static <T> SearchDocumentResponse from(SearchTemplateResponse<EntityAsMap> response,
			SearchDocumentResponse.EntityCreator<T> entityCreator, JsonpMapper jsonpMapper) {

		Assert.notNull(response, "response must not be null");
		Assert.notNull(entityCreator, "entityCreator must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		HitsMetadata<EntityAsMap> hitsMetadata = response.hits();
		String scrollId = response.scrollId();
		Map<String, Aggregate> aggregations = response.aggregations();
		Map<String, List<Suggestion<EntityAsMap>>> suggest = response.suggest();
		String pointInTimeId = response.pitId();

		return from(hitsMetadata, scrollId, pointInTimeId, aggregations, suggest, entityCreator, jsonpMapper);
	}


	public static <T> SearchDocumentResponse from(HitsMetadata<?> hitsMetadata, @Nullable String scrollId,
			@Nullable String pointInTimeId, @Nullable Map<String, Aggregate> aggregations,
			Map<String, List<Suggestion<EntityAsMap>>> suggestES, SearchDocumentResponse.EntityCreator<T> entityCreator,
			JsonpMapper jsonpMapper) {

		Assert.notNull(hitsMetadata, "hitsMetadata must not be null");

		long totalHits;
		String totalHitsRelation;

		TotalHits responseTotalHits = hitsMetadata.total();
		if (responseTotalHits != null) {
			totalHits = responseTotalHits.value();
			switch (responseTotalHits.relation().jsonValue()) {
				case "eq":
					totalHitsRelation = TotalHitsRelation.EQUAL_TO.name();
				case "gte":
					totalHitsRelation = TotalHitsRelation.GREATER_THAN_OR_EQUAL_TO.name();
				default:
					totalHitsRelation = TotalHitsRelation.OFF.name();
			}
			;
		}
		else {
			totalHits = hitsMetadata.hits().size();
			totalHitsRelation = "OFF";
		}

		float maxScore = hitsMetadata.maxScore() != null ? hitsMetadata.maxScore().floatValue() : Float.NaN;

		List<SearchDocument> searchDocuments = new ArrayList<>();
		for (Hit<?> hit : hitsMetadata.hits()) {
			searchDocuments.add(DocumentAdapters.from(hit, jsonpMapper));
		}

		ElasticsearchAggregations aggregationsContainer = aggregations != null ? new ElasticsearchAggregations(aggregations)
				: null;


		return new SearchDocumentResponse(totalHits, totalHitsRelation, maxScore, scrollId, pointInTimeId, searchDocuments,
				aggregationsContainer);
	}
}
