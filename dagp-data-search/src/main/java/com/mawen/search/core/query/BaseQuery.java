package com.mawen.search.core.query;

import com.mawen.search.core.domain.IdWithRouting;
import com.mawen.search.core.domain.PointInTime;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.query.builder.BaseQueryBuilder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.addAll;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class BaseQuery implements Query {

	private static final int DEFAULT_REACTIVE_BATCH_SIZE = 500;

	@Nullable
	protected Sort sort;
	protected Pageable pageable = DEFAULT_PAGE;
	protected List<String> fields = new ArrayList<>();
	@Nullable
	protected SourceFilter sourceFilter;
	protected float minScore;
	@Nullable
	protected Collection<String> ids;
	@Nullable
	protected String route;
	@Nullable
	protected SearchType searchType = SearchType.QUERY_THEN_FETCH;
	protected boolean trackScores;
	@Nullable
	protected Integer maxResults;
	@Nullable
	protected HighlightQuery highlightQuery;
	@Nullable
	protected Integer trackTotalHitsUpTo;
	@Nullable
	protected Duration scrollTime;
	@Nullable
	protected Duration timeout;
	@Nullable
	protected List<Object> searchAfter;
	protected List<IdWithRouting> idsWithRouting = Collections.emptyList();
	@Nullable
	private Boolean trackTotalHits;
	@Nullable protected PointInTime pointInTime;

	@Nullable
	private Integer reactiveBatchSize = null;
	@Nullable
	private Boolean allowNoIndices = null;
	/**
	 * @since 0.0.2-SNAPSHOT
	 */
	@Nullable
	private Boolean ignoreUnavailable = null;

	private boolean queryIsUpdatedByConverter = false;

	public BaseQuery() {
	}

	public <Q extends BaseQuery, B extends BaseQueryBuilder<Q, B>> BaseQuery(BaseQueryBuilder<Q, B> builder) {
		this.sort = builder.getSort();
		// do a setPageable after setting the sort, because the pageable may contain an additional sort
		this.setPageable(builder.getPageable() != null ? builder.getPageable() : DEFAULT_PAGE);
		this.fields = builder.getFields();
		this.sourceFilter = builder.getSourceFilter();
		this.minScore = builder.getMinScore();
		this.ids = builder.getIds() == null ? null : builder.getIds();
		this.route = builder.getRoute();
		this.searchType = builder.getSearchType();
		this.trackScores = builder.getTrackScores();
		this.maxResults = builder.getMaxResults();
		this.highlightQuery = builder.getHighlightQuery();
		this.pointInTime = builder.getPointInTime();
		this.trackTotalHits = builder.getTrackTotalHits();
		this.trackTotalHitsUpTo = builder.getTrackTotalHitsUpTo();
		this.scrollTime = builder.getScrollTime();
		this.timeout = builder.getTimeout();
		this.searchAfter = builder.getSearchAfter();
		this.idsWithRouting = builder.getIdsWithRouting();
		this.reactiveBatchSize = builder.getReactiveBatchSize();
		this.allowNoIndices = builder.getAllowNoIndices();
		this.ignoreUnavailable = builder.getIgnoreUnavailable();
	}

	public void setSort(@Nullable Sort sort) {
		this.sort = sort;
	}


	@Override
	public final <T extends Query> T setPageable(Pageable pageable) {

		Assert.notNull(pageable, "Pageable must not be null!");

		this.pageable = pageable;
		return this.addSort(pageable.getSort());
	}

	@Override
	public void addFields(String... fields) {
		addAll(this.fields, fields);
	}

	@Override
	public void setFields(List<String> fields) {

		Assert.notNull(fields, "fields must not be null");

		this.fields.clear();
		this.fields.addAll(fields);
	}

	@Override
	public void addSourceFilter(SourceFilter sourceFilter) {
		this.sourceFilter = sourceFilter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <T extends Query> T addSort(@Nullable Sort sort) {
		if (sort == null) {
			return (T) this;
		}

		if (this.sort == null) {
			this.sort = sort;
		}
		else {
			this.sort = this.sort.and(sort);
		}

		return (T) this;
	}

	@Override
	public float getMinScore() {
		return minScore;
	}

	public void setMinScore(float minScore) {
		this.minScore = minScore;
	}

	@Override
	public boolean getTrackScores() {
		return this.trackScores;
	}

	public void setTrackScores(boolean trackScores) {
		this.trackScores = trackScores;
	}

	@Override
	@Nullable
	public Collection<String> getIds() {
		return ids;
	}

	public void setIds(@Nullable Collection<String> ids) {
		this.ids = ids;
	}

	@Override
	public List<IdWithRouting> getIdsWithRouting() {

		if (!isEmpty(idsWithRouting)) {
			return Collections.unmodifiableList(idsWithRouting);
		}

		if (!isEmpty(ids)) {
			return ids.stream().map(id -> new IdWithRouting(id, route)).collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	public void setIdsWithRouting(List<IdWithRouting> idsWithRouting) {

		Assert.notNull(idsWithRouting, "idsWithRouting must not be null");

		this.idsWithRouting = idsWithRouting;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public void setSearchType(@Nullable SearchType searchType) {
		this.searchType = searchType;
	}

	@Override
	public boolean isLimiting() {
		return maxResults != null;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	@Override
	public Optional<HighlightQuery> getHighlightQuery() {
		return Optional.ofNullable(highlightQuery);
	}

	@Override
	public void setHighlightQuery(HighlightQuery highlightQuery) {
		this.highlightQuery = highlightQuery;
	}

	@Override
	public void setTrackTotalHits(@Nullable Boolean trackTotalHits) {
		this.trackTotalHits = trackTotalHits;
	}

	@Override
	public void setTrackTotalHitsUpTo(@Nullable Integer trackTotalHitsUpTo) {
		this.trackTotalHitsUpTo = trackTotalHitsUpTo;
	}

	@Override
	public void setScrollTime(@Nullable Duration scrollTime) {
		this.scrollTime = scrollTime;
	}

	public void setTimeout(@Nullable Duration timeout) {
		this.timeout = timeout;
	}

	public void setPointInTime(@Nullable PointInTime pointInTime) {
		this.pointInTime = pointInTime;
	}

	@Override
	public void setSearchAfter(@Nullable List<Object> searchAfter) {
		this.searchAfter = searchAfter;
	}

	@Override
	public Integer getReactiveBatchSize() {
		return reactiveBatchSize != null ? reactiveBatchSize : DEFAULT_REACTIVE_BATCH_SIZE;
	}

	public void setReactiveBatchSize(Integer reactiveBatchSize) {
		this.reactiveBatchSize = reactiveBatchSize;
	}

	public boolean isQueryIsUpdatedByConverter() {
		return queryIsUpdatedByConverter;
	}

	public void setQueryIsUpdatedByConverter(boolean queryIsUpdatedByConverter) {
		this.queryIsUpdatedByConverter = queryIsUpdatedByConverter;
	}

	/**
	 * @since 0.0.2-SNAPSHOT
	 */
	@Override
	public void setIgnoreUnavailable(boolean ignoreUnavailable) {
		this.ignoreUnavailable = ignoreUnavailable;
	}
}
