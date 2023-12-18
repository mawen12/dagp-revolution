package com.mawen.search.core.query;

import java.util.List;

import com.mawen.search.core.document.Document;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class UpdateQuery {

	private final String id;
	@Nullable private final Document document;
	@Nullable private final Document upsert;
	@Nullable private final String routing;
	@Nullable private final Boolean scriptedUpsert;
	@Nullable private final Boolean docAsUpsert;
	@Nullable private final Boolean fetchSource;
	@Nullable private final List<String> fetchSourceIncludes;
	@Nullable private final List<String> fetchSourceExcludes;
	@Nullable private final Integer ifSeqNo;
	@Nullable private final Integer ifPrimaryTerm;
	@Nullable private final RefreshPolicy refreshPolicy;
	@Nullable private final Integer retryOnConflict;
	@Nullable private final String timeout;
	@Nullable private final String waitForActiveShards;
	@Nullable private final Query query;
	@Nullable private final Boolean abortOnVersionConflict;
	@Nullable private final Integer batchSize;
	@Nullable private final Integer maxDocs;
	@Nullable private final Integer maxRetries;
	@Nullable private final String pipeline;
	@Nullable private final Float requestsPerSecond;
	@Nullable private final Boolean shouldStoreResult;
	@Nullable private final Integer slices;
	@Nullable private final String indexName;
	@Nullable private final ScriptData scriptData;
}
