package com.mawen.search.core.query;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import static com.mawen.search.core.query.Query.*;
import static java.util.Collections.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class MoreLikeThisQuery {

	private final List<String> searchIndices = new ArrayList<>();
	private final List<String> searchTypes = new ArrayList<>();
	private final List<String> fields = new ArrayList<>();
	private final List<String> stopWords = new ArrayList<>();
	@Nullable
	private String id;
	@Nullable
	private String routing;
	@Nullable
	private Float percentTermsToMatch;
	@Nullable
	private Integer minTermFreq;
	@Nullable
	private Integer maxQueryTerms;
	@Nullable
	private Integer minDocFreq;
	@Nullable
	private Integer maxDocFreq;
	@Nullable
	private Integer minWordLen;
	@Nullable
	private Integer maxWordLen;
	@Nullable
	private Float boostTerms;
	private Pageable pageable = DEFAULT_PAGE;


	public void setId(String id) {
		this.id = id;
	}

	public void addSearchIndices(String... searchIndices) {
		addAll(this.searchIndices, searchIndices);
	}

	public void addSearchTypes(String... searchTypes) {
		addAll(this.searchTypes, searchTypes);
	}

	public void addFields(String... fields) {
		addAll(this.fields, fields);
	}

	public void setRouting(String routing) {
		this.routing = routing;
	}

	public void setPercentTermsToMatch(Float percentTermsToMatch) {
		this.percentTermsToMatch = percentTermsToMatch;
	}

	public void setMinTermFreq(Integer minTermFreq) {
		this.minTermFreq = minTermFreq;
	}

	public void setMaxQueryTerms(Integer maxQueryTerms) {
		this.maxQueryTerms = maxQueryTerms;
	}

	public void addStopWords(String... stopWords) {
		addAll(this.stopWords, stopWords);
	}

	public void setMinDocFreq(Integer minDocFreq) {
		this.minDocFreq = minDocFreq;
	}

	public void setMaxDocFreq(Integer maxDocFreq) {
		this.maxDocFreq = maxDocFreq;
	}

	public void setMinWordLen(Integer minWordLen) {
		this.minWordLen = minWordLen;
	}

	public void setMaxWordLen(Integer maxWordLen) {
		this.maxWordLen = maxWordLen;
	}

	public void setBoostTerms(Float boostTerms) {
		this.boostTerms = boostTerms;
	}

	public void setPageable(Pageable pageable) {

		Assert.notNull(pageable, "pageable must not be null");

		this.pageable = pageable;
	}
}
