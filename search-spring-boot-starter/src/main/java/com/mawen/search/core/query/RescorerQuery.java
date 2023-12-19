package com.mawen.search.core.query;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class RescorerQuery {

	private final Query query;
	private ScoreMode scoreMode = ScoreMode.Default;
	@Nullable
	private Integer windowSize;
	@Nullable
	private Float queryWeight;
	@Nullable
	private Float rescoreQueryWeight;

	public RescorerQuery(Query query) {

		Assert.notNull(query, "query must not be null");

		this.query = query;
	}

	public Query getQuery() {
		return query;
	}

	public ScoreMode getScoreMode() {
		return scoreMode;
	}

	@Nullable
	public Integer getWindowSize() {
		return windowSize;
	}

	@Nullable
	public Float getQueryWeight() {
		return queryWeight;
	}

	@Nullable
	public Float getRescoreQueryWeight() {
		return rescoreQueryWeight;
	}

	public RescorerQuery withScoreMode(ScoreMode scoreMode) {

		Assert.notNull(scoreMode, "scoreMode must not be null");

		this.scoreMode = scoreMode;
		return this;
	}

	public RescorerQuery withWindowSize(int windowSize) {
		this.windowSize = windowSize;
		return this;
	}

	public RescorerQuery withQueryWeight(float queryWeight) {
		this.queryWeight = queryWeight;
		return this;
	}

	public RescorerQuery withRescoreQueryWeight(float rescoreQueryWeight) {
		this.rescoreQueryWeight = rescoreQueryWeight;
		return this;
	}

	public enum ScoreMode {
		Default, Avg, Max, Min, Total, Multiply
	}
}
