package com.mawen.search.core.query.builder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.mawen.search.core.domain.IdWithRouting;
import com.mawen.search.core.domain.IndexBoost;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.HighlightQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.RescorerQuery;
import lombok.Getter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public abstract class BaseQueryBuilder<Q extends BaseQuery, SELF extends BaseQueryBuilder<Q, SELF>> {

	protected final List<RescorerQuery> rescorerQueries = new ArrayList<>();
	private final List<String> fields = new ArrayList<>();
	private final Collection<String> ids = new ArrayList<>();
	private final List<IdWithRouting> idsWithRouting = new ArrayList<>();
	@Nullable
	Integer reactiveBatchSize;
	@Nullable
	private Sort sort;
	@Nullable
	private Pageable pageable;
	@Nullable
	private SourceFilter sourceFilter;
	private float minScore;
	@Nullable
	private String route;
	@Nullable
	private Query.SearchType searchType = Query.SearchType.QUERY_THEN_FETCH;
	private boolean trackScores;
	@Nullable
	private String preference;
	@Nullable
	private Integer maxResults;
	@Nullable
	private HighlightQuery highlightQuery;
	@Nullable
	private Boolean trackTotalHits;
	@Nullable
	private Integer trackTotalHitsUpTo;
	@Nullable
	private Duration scrollTime;
	@Nullable
	private Duration timeout;
	@Nullable
	private List<Object> searchAfter;
	@Nullable
	private List<IndexBoost> indicesBoost;
	@Nullable
	private Boolean requestCache;
	@Nullable
	private Boolean allowNoIndices;

	public boolean getTrackScores() {
		return trackScores;
	}

	public SELF withPageable(Pageable pageable) {
		this.pageable = pageable;
		return self();
	}

	public SELF withSort(Sort sort) {
		if (this.sort == null) {
			this.sort = sort;
		}
		else {
			this.sort = this.sort.and(sort);
		}
		return self();
	}

	public SELF withMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
		return self();
	}

	/**
	 * Set Ids for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param ids list of id values
	 */
	public SELF withIds(String... ids) {

		this.ids.clear();
		this.ids.addAll(Arrays.asList(ids));
		return self();
	}

	/**
	 * Set Ids for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param ids list of id values
	 */
	public SELF withIds(Collection<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		this.ids.clear();
		this.ids.addAll(ids);
		return self();
	}

	public SELF withTrackScores(boolean trackScores) {
		this.trackScores = trackScores;
		return self();
	}

	public SELF withMinScore(float minScore) {
		this.minScore = minScore;
		return self();
	}

	public SELF withPreference(String preference) {
		this.preference = preference;
		return self();
	}

	public SELF withSourceFilter(SourceFilter sourceFilter) {
		this.sourceFilter = sourceFilter;
		return self();
	}

	public SELF withFields(String... fields) {

		this.fields.clear();
		Collections.addAll(this.fields, fields);
		return self();
	}

	public SELF withFields(Collection<String> fields) {

		Assert.notNull(fields, "fields must not be null");

		this.fields.clear();
		this.fields.addAll(fields);
		return self();
	}

	public SELF withHighlightQuery(HighlightQuery highlightQuery) {
		this.highlightQuery = highlightQuery;
		return self();
	}

	public SELF withRoute(String route) {
		this.route = route;
		return self();
	}

	public SELF withIndicesBoost(@Nullable List<IndexBoost> indicesBoost) {
		this.indicesBoost = indicesBoost;
		return self();
	}

	public SELF withIndicesBoost(IndexBoost... indicesBoost) {
		this.indicesBoost = Arrays.asList(indicesBoost);
		return self();
	}

	public SELF withSearchType(@Nullable Query.SearchType searchType) {
		this.searchType = searchType;
		return self();
	}

	public SELF withTrackTotalHits(@Nullable Boolean trackTotalHits) {
		this.trackTotalHits = trackTotalHits;
		return self();
	}

	public SELF withTrackTotalHitsUpTo(@Nullable Integer trackTotalHitsUpTo) {
		this.trackTotalHitsUpTo = trackTotalHitsUpTo;
		return self();
	}

	public SELF withTimeout(@Nullable Duration timeout) {
		this.timeout = timeout;
		return self();
	}

	public SELF withScrollTime(@Nullable Duration scrollTime) {
		this.scrollTime = scrollTime;
		return self();
	}

	public SELF withSearchAfter(@Nullable List<Object> searchAfter) {
		this.searchAfter = searchAfter;
		return self();
	}

	public SELF withRequestCache(@Nullable Boolean requestCache) {
		this.requestCache = requestCache;
		return self();
	}

	/**
	 * Set Ids with routing values for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param idsWithRouting list of id values, must not be {@literal null}
	 * @since 4.3
	 */
	public SELF withIdsWithRouting(List<IdWithRouting> idsWithRouting) {

		Assert.notNull(idsWithRouting, "idsWithRouting must not be null");

		this.idsWithRouting.clear();
		this.idsWithRouting.addAll(idsWithRouting);
		return self();
	}

	public SELF withRescorerQueries(List<RescorerQuery> rescorerQueries) {

		Assert.notNull(rescorerQueries, "rescorerQueries must not be null");

		this.rescorerQueries.clear();
		this.rescorerQueries.addAll(rescorerQueries);
		return self();
	}

	public SELF withRescorerQuery(RescorerQuery rescorerQuery) {

		Assert.notNull(rescorerQuery, "rescorerQuery must not be null");

		this.rescorerQueries.add(rescorerQuery);
		return self();
	}

	/**
	 * @since 5.1
	 */
	public SELF withReactiveBatchSize(@Nullable Integer reactiveBatchSize) {
		this.reactiveBatchSize = reactiveBatchSize;
		return self();
	}

	public SELF withAllowNoIndices(@Nullable Boolean allowNoIndices) {
		this.allowNoIndices = allowNoIndices;
		return self();
	}

	public abstract Q build();

	private SELF self() {
		// noinspection unchecked
		return (SELF) this;
	}
}
