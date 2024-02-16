package com.mawen.search.core.query;

import com.mawen.search.core.document.Document;
import com.mawen.search.core.domain.ScriptData;
import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.core.support.ScriptType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UpdateQuery {

	private final String id;
	@Nullable
	private final Document document;
	@Nullable
	private final Document upsert;
	@Nullable
	private final String routing;
	@Nullable
	private final Boolean scriptedUpsert;
	@Nullable
	private final Boolean docAsUpsert;
	@Nullable
	private final Boolean fetchSource;
	@Nullable
	private final List<String> fetchSourceIncludes;
	@Nullable
	private final List<String> fetchSourceExcludes;
	@Nullable
	private final Integer ifSeqNo;
	@Nullable
	private final Integer ifPrimaryTerm;
	@Nullable
	private final RefreshPolicy refreshPolicy;
	@Nullable
	private final Integer retryOnConflict;
	@Nullable
	private final String timeout;
	@Nullable
	private final String waitForActiveShards;
	@Nullable
	private final Query query;
	@Nullable
	private final Boolean abortOnVersionConflict;
	@Nullable
	private final Integer batchSize;
	@Nullable
	private final Integer maxDocs;
	@Nullable
	private final Integer maxRetries;
	@Nullable
	private final String pipeline;
	@Nullable
	private final Float requestsPerSecond;
	@Nullable
	private final Boolean shouldStoreResult;
	@Nullable
	private final Integer slices;
	@Nullable
	private final String indexName;
	@Nullable
	private final ScriptData scriptData;

	private UpdateQuery(String id, @Nullable String script, @Nullable Map<String, Object> params,
			@Nullable Document document, @Nullable Document upsert, @Nullable String lang, @Nullable String routing,
			@Nullable Boolean scriptedUpsert, @Nullable Boolean docAsUpsert, @Nullable Boolean fetchSource,
			@Nullable List<String> fetchSourceIncludes, @Nullable List<String> fetchSourceExcludes, @Nullable Integer ifSeqNo,
			@Nullable Integer ifPrimaryTerm, @Nullable RefreshPolicy refreshPolicy, @Nullable Integer retryOnConflict,
			@Nullable String timeout, @Nullable String waitForActiveShards, @Nullable Query query,
			@Nullable Boolean abortOnVersionConflict, @Nullable Integer batchSize, @Nullable Integer maxDocs,
			@Nullable Integer maxRetries, @Nullable String pipeline, @Nullable Float requestsPerSecond,
			@Nullable Boolean shouldStoreResult, @Nullable Integer slices, @Nullable ScriptType scriptType,
			@Nullable String scriptName, @Nullable String indexName) {

		this.id = id;
		this.document = document;
		this.upsert = upsert;
		this.routing = routing;
		this.scriptedUpsert = scriptedUpsert;
		this.docAsUpsert = docAsUpsert;
		this.fetchSource = fetchSource;
		this.fetchSourceIncludes = fetchSourceIncludes;
		this.fetchSourceExcludes = fetchSourceExcludes;
		this.ifSeqNo = ifSeqNo;
		this.ifPrimaryTerm = ifPrimaryTerm;
		this.refreshPolicy = refreshPolicy;
		this.retryOnConflict = retryOnConflict;
		this.timeout = timeout;
		this.waitForActiveShards = waitForActiveShards;
		this.query = query;
		this.abortOnVersionConflict = abortOnVersionConflict;
		this.batchSize = batchSize;
		this.maxDocs = maxDocs;
		this.maxRetries = maxRetries;
		this.pipeline = pipeline;
		this.requestsPerSecond = requestsPerSecond;
		this.shouldStoreResult = shouldStoreResult;
		this.slices = slices;
		this.indexName = indexName;

		if (scriptType != null || lang != null || script != null || scriptName != null || params != null) {
			this.scriptData = new ScriptData(scriptType, lang, script, scriptName, params);
		}
		else {
			this.scriptData = null;
		}
	}

	public static Builder builder(String id) {
		return new Builder(id);
	}

	public static Builder builder(Query query) {
		return new Builder(query);
	}

	public static final class Builder {

		@Nullable
		String waitForActiveShards;
		private String id;
		@Nullable
		private String script = null;
		@Nullable
		private Map<String, Object> params;
		@Nullable
		private Document document = null;
		@Nullable
		private Document upsert = null;
		@Nullable
		private String lang = null;
		@Nullable
		private String routing = null;
		@Nullable
		private Boolean scriptedUpsert;
		@Nullable
		private Boolean docAsUpsert;
		@Nullable
		private Boolean fetchSource;
		@Nullable
		private Integer ifSeqNo;
		@Nullable
		private Integer ifPrimaryTerm;
		@Nullable
		private RefreshPolicy refreshPolicy;
		@Nullable
		private Integer retryOnConflict;
		@Nullable
		private String timeout;
		@Nullable
		private List<String> fetchSourceIncludes;
		@Nullable
		private List<String> fetchSourceExcludes;
		@Nullable
		private Query query;
		@Nullable
		private Boolean abortOnVersionConflict;
		@Nullable
		private Integer batchSize;
		@Nullable
		private Integer maxDocs;
		@Nullable
		private Integer maxRetries;
		@Nullable
		private String pipeline;
		@Nullable
		private Float requestsPerSecond;
		@Nullable
		private Boolean shouldStoreResult;
		@Nullable
		private Integer slices;
		@Nullable
		private ScriptType scriptType;
		@Nullable
		private String scriptName;
		@Nullable
		private String indexName;

		private Builder(String id) {
			this.id = id;
		}

		private Builder(Query query) {
			this.query = query;
		}

		public Builder withScript(String script) {
			this.script = script;
			return this;
		}

		public Builder withParams(Map<String, Object> params) {
			this.params = params;
			return this;
		}

		public Builder withDocument(Document document) {
			this.document = document;
			return this;
		}

		public Builder withUpsert(Document upsert) {
			this.upsert = upsert;
			return this;
		}

		public Builder withLang(String lang) {
			this.lang = lang;
			return this;
		}

		public Builder withRouting(String routing) {
			this.routing = routing;
			return this;
		}

		public Builder withScriptedUpsert(Boolean scriptedUpsert) {
			this.scriptedUpsert = scriptedUpsert;
			return this;
		}

		public Builder withDocAsUpsert(Boolean docAsUpsert) {
			this.docAsUpsert = docAsUpsert;
			return this;
		}

		public Builder withFetchSource(Boolean fetchSource) {
			this.fetchSource = fetchSource;
			return this;
		}

		public Builder withIfSeqNo(Integer ifSeqNo) {
			this.ifSeqNo = ifSeqNo;
			return this;
		}

		public Builder withIfPrimaryTerm(Integer ifPrimaryTerm) {
			this.ifPrimaryTerm = ifPrimaryTerm;
			return this;
		}

		public Builder withRefreshPolicy(RefreshPolicy refreshPolicy) {
			this.refreshPolicy = refreshPolicy;
			return this;
		}

		public Builder withRetryOnConflict(Integer retryOnConflict) {
			this.retryOnConflict = retryOnConflict;
			return this;
		}

		public Builder withTimeout(String timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder withWaitForActiveShards(String waitForActiveShards) {
			this.waitForActiveShards = waitForActiveShards;
			return this;
		}

		public Builder withFetchSourceIncludes(List<String> fetchSourceIncludes) {
			this.fetchSourceIncludes = fetchSourceIncludes;
			return this;
		}

		public Builder withFetchSourceExcludes(List<String> fetchSourceExcludes) {
			this.fetchSourceExcludes = fetchSourceExcludes;
			return this;
		}

		public Builder withAbortOnVersionConflict(Boolean abortOnVersionConflict) {
			this.abortOnVersionConflict = abortOnVersionConflict;
			return this;
		}

		public Builder withBatchSize(Integer batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public Builder withMaxDocs(Integer maxDocs) {
			this.maxDocs = maxDocs;
			return this;
		}

		public Builder withMaxRetries(Integer maxRetries) {
			this.maxRetries = maxRetries;
			return this;
		}

		public Builder withPipeline(String pipeline) {
			this.pipeline = pipeline;
			return this;
		}

		public Builder withRequestsPerSecond(Float requestsPerSecond) {
			this.requestsPerSecond = requestsPerSecond;
			return this;
		}

		public Builder withShouldStoreResult(Boolean shouldStoreResult) {
			this.shouldStoreResult = shouldStoreResult;
			return this;
		}

		public Builder withSlices(Integer slices) {
			this.slices = slices;
			return this;
		}

		public Builder withScriptType(ScriptType scriptType) {
			this.scriptType = scriptType;
			return this;
		}

		public Builder withScriptName(String scriptName) {
			this.scriptName = scriptName;
			return this;
		}

		public UpdateQuery build() {

			if (script == null && document == null && query == null) {
				throw new IllegalArgumentException("either script, document or query must be set");
			}

			return new UpdateQuery(id, script, params, document, upsert, lang, routing, scriptedUpsert, docAsUpsert,
					fetchSource, fetchSourceIncludes, fetchSourceExcludes, ifSeqNo, ifPrimaryTerm, refreshPolicy, retryOnConflict,
					timeout, waitForActiveShards, query, abortOnVersionConflict, batchSize, maxDocs, maxRetries, pipeline,
					requestsPerSecond, shouldStoreResult, slices, scriptType, scriptName, indexName);
		}

		public Builder withIndex(@Nullable String indexName) {
			this.indexName = indexName;
			return this;
		}

	}
}
