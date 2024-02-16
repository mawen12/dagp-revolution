package com.mawen.search.core.query;

import com.mawen.search.core.domain.IdWithRouting;
import com.mawen.search.core.domain.SourceFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface Query {

	int DEFAULT_PAGE_SIZE = 10;
	Pageable DEFAULT_PAGE = PageRequest.of(0, DEFAULT_PAGE_SIZE);

	static Query findAll() {
		return new StringQuery("{\"match_all\":{}}");
	}

	static Query multiGetQueryWithRouting(List<IdWithRouting> idsWithRouting) {

		Assert.notNull(idsWithRouting, "idsWithRouting must not be null");

		BaseQuery query = new BaseQuery();
		query.setIdsWithRouting(idsWithRouting);
		return query;
	}

	static Query multiGetQuery(Collection<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		BaseQuery query = new BaseQuery();
		query.setIds(ids);
		return query;
	}

	Pageable getPageable();

	<T extends Query> T setPageable(Pageable pageable);

	<T extends Query> T addSort(@Nullable Sort sort);

	@Nullable
	Sort getSort();

	void addFields(String... fields);

	List<String> getFields();

	void setFields(List<String> fields);

	void addSourceFilter(SourceFilter sourceFilter);

	@Nullable
	SourceFilter getSourceFilter();

	float getMinScore();

	boolean getTrackScores();

	@Nullable
	Collection<String> getIds();

	List<IdWithRouting> getIdsWithRouting();

	@Nullable
	String getRoute();

	@Nullable
	SearchType getSearchType();

	default boolean isLimiting() {
		return false;
	}

	@Nullable
	default Integer getMaxResults() {
		return null;
	}

	default Optional<HighlightQuery> getHighlightQuery() {
		return Optional.empty();
	}

	void setHighlightQuery(HighlightQuery highlightQuery);

	@Nullable
	Boolean getTrackTotalHits();

	void setTrackTotalHits(@Nullable Boolean trackTotalHits);

	@Nullable
	Integer getTrackTotalHitsUpTo();

	void setTrackTotalHitsUpTo(@Nullable Integer trackTotalHitsUpTo);

	@Nullable
	Duration getScrollTime();

	void setScrollTime(@Nullable Duration scrollTime);

	default boolean hasScrollTime() {
		return getScrollTime() != null;
	}

	@Nullable
	Duration getTimeout();

	@Nullable
	List<Object> getSearchAfter();

	void setSearchAfter(@Nullable List<Object> searchAfter);

	default Integer getReactiveBatchSize() {
		return 500;
	}

	@Nullable
	Boolean getAllowNoIndices();

	enum SearchType {
		QUERY_THEN_FETCH, DFS_QUERY_THEN_FETCH
	}

}
