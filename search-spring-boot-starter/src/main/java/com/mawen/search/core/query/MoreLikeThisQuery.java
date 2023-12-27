package com.mawen.search.core.query;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static com.mawen.search.core.query.Query.*;
import static java.util.Collections.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
@Setter
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

	public void addSearchIndices(String... searchIndices) {
		addAll(this.searchIndices, searchIndices);
	}

	public void addSearchTypes(String... searchTypes) {
		addAll(this.searchTypes, searchTypes);
	}

	public void addFields(String... fields) {
		addAll(this.fields, fields);
	}

	public void addStopWords(String... stopWords) {
		addAll(this.stopWords, stopWords);
	}

	public void setPageable(Pageable pageable) {

		Assert.notNull(pageable, "pageable must not be null");

		this.pageable = pageable;
	}
}
