package com.mawen.search.core.document;

import com.mawen.search.core.aggregation.AggregationsContainer;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class SearchDocumentResponse {

	private final long totalHits;
	private final String totalHitsRelation;
	private final float maxScore;
	@Nullable
	private final String scrollId;
	private final List<SearchDocument> searchDocuments;
	@Nullable
	private final AggregationsContainer<?> aggregations;

	@Nullable
	String pointInTimeId;

	public SearchDocumentResponse(long totalHits, String totalHitsRelation, float maxScore, @Nullable String scrollId,
			@Nullable String pointInTimeId, List<SearchDocument> searchDocuments, @Nullable AggregationsContainer<?> aggregationsContainer) {
		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.maxScore = maxScore;
		this.scrollId = scrollId;
		this.pointInTimeId = pointInTimeId;
		this.searchDocuments = searchDocuments;
		this.aggregations = aggregationsContainer;
	}

	@FunctionalInterface
	public interface EntityCreator<T> extends Function<SearchDocument, CompletableFuture<T>> {}
}
