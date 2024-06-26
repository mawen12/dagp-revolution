package com.mawen.search.core.query.builder;

import com.mawen.search.core.domain.IdWithRouting;
import com.mawen.search.core.domain.PointInTime;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.HighlightQuery;
import com.mawen.search.core.query.Query;
import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public abstract class BaseQueryBuilder<Q extends BaseQuery, SELF extends BaseQueryBuilder<Q, SELF>> {

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
	private Integer maxResults;
	@Nullable
	private HighlightQuery highlightQuery;
	@Nullable
	private Boolean trackTotalHits;
	@Nullable
	private Integer trackTotalHitsUpTo;
	@Nullable private PointInTime pointInTime;
	@Nullable
	private Duration scrollTime;
	@Nullable
	private Duration timeout;
	@Nullable
	private List<Object> searchAfter;
	@Nullable
	private Boolean allowNoIndices;
	/**
	 * @since 0.0.2-SNAPSHOT
	 */
	@Nullable
	private Boolean ignoreUnavailable;

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

	public SELF withIds(String... ids) {

		this.ids.clear();
		this.ids.addAll(Arrays.asList(ids));
		return self();
	}

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

	public SELF withPointInTime(@Nullable PointInTime pointInTime) {
		this.pointInTime = pointInTime;
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

	public SELF withIdsWithRouting(List<IdWithRouting> idsWithRouting) {

		Assert.notNull(idsWithRouting, "idsWithRouting must not be null");

		this.idsWithRouting.clear();
		this.idsWithRouting.addAll(idsWithRouting);
		return self();
	}

	public SELF withReactiveBatchSize(@Nullable Integer reactiveBatchSize) {
		this.reactiveBatchSize = reactiveBatchSize;
		return self();
	}

	public SELF withAllowNoIndices(@Nullable Boolean allowNoIndices) {
		this.allowNoIndices = allowNoIndices;
		return self();
	}

	public SELF withIgnoreUnavailable(Boolean ignoreAvailable) {
		this.ignoreUnavailable = ignoreAvailable;
		return self();
	}

	public abstract Q build();

	private SELF self() {
		// noinspection unchecked
		return (SELF) this;
	}
}
