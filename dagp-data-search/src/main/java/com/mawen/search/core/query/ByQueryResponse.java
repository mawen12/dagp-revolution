package com.mawen.search.core.query;

import com.mawen.search.ElasticsearchErrorCause;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class ByQueryResponse {

	private final long took;
	private final boolean timedOut;
	private final long total;
	private final long updated;
	private final long deleted;
	private final int batches;
	private final long versionConflicts;
	private final long noops;
	private final long bulkRetries;
	private final long searchRetries;
	@Nullable
	private final String reasonCancelled;
	private final List<Failure> failures;
	private final List<SearchFailure> searchFailures;

	private ByQueryResponse(long took, boolean timedOut, long total, long updated, long deleted, int batches,
			long versionConflicts, long noops, long bulkRetries, long searchRetries, @Nullable String reasonCancelled,
			List<Failure> failures, List<SearchFailure> searchFailures) {
		this.took = took;
		this.timedOut = timedOut;
		this.total = total;
		this.updated = updated;
		this.deleted = deleted;
		this.batches = batches;
		this.versionConflicts = versionConflicts;
		this.noops = noops;
		this.bulkRetries = bulkRetries;
		this.searchRetries = searchRetries;
		this.reasonCancelled = reasonCancelled;
		this.failures = failures;
		this.searchFailures = searchFailures;
	}

	public static ByQueryResponseBuilder builder() {
		return new ByQueryResponseBuilder();
	}

	@Getter
	public static class Failure {

		@Nullable
		private final String index;
		@Nullable
		private final String type;
		@Nullable
		private final String id;
		@Nullable
		private final Exception cause;
		@Nullable
		private final Integer status;
		@Nullable
		private final Long seqNo;
		@Nullable
		private final Long term;
		@Nullable
		private final Boolean aborted;
		@Nullable
		private final ElasticsearchErrorCause elasticsearchErrorCause;

		private Failure(@Nullable String index, @Nullable String type, @Nullable String id, @Nullable Exception cause,
				@Nullable Integer status, @Nullable Long seqNo, @Nullable Long term, @Nullable Boolean aborted,
				@Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
			this.index = index;
			this.type = type;
			this.id = id;
			this.cause = cause;
			this.status = status;
			this.seqNo = seqNo;
			this.term = term;
			this.aborted = aborted;
			this.elasticsearchErrorCause = elasticsearchErrorCause;
		}

		public static FailureBuilder builder() {
			return new FailureBuilder();
		}

		public static final class FailureBuilder {
			@Nullable
			private String index;
			@Nullable
			private String type;
			@Nullable
			private String id;
			@Nullable
			private Exception cause;
			@Nullable
			private Integer status;
			@Nullable
			private Long seqNo;
			@Nullable
			private Long term;
			@Nullable
			private Boolean aborted;
			@Nullable
			private ElasticsearchErrorCause elasticsearchErrorCause;

			private FailureBuilder() {
			}

			public FailureBuilder withIndex(String index) {
				this.index = index;
				return this;
			}

			public FailureBuilder withType(String type) {
				this.type = type;
				return this;
			}

			public FailureBuilder withId(String id) {
				this.id = id;
				return this;
			}

			public FailureBuilder withCause(Exception cause) {
				this.cause = cause;
				return this;
			}

			public FailureBuilder withStatus(Integer status) {
				this.status = status;
				return this;
			}

			public FailureBuilder withSeqNo(Long seqNo) {
				this.seqNo = seqNo;
				return this;
			}

			public FailureBuilder withTerm(Long term) {
				this.term = term;
				return this;
			}

			public FailureBuilder withAborted(Boolean aborted) {
				this.aborted = aborted;
				return this;
			}

			public FailureBuilder withErrorCause(ElasticsearchErrorCause elasticsearchErrorCause) {
				this.elasticsearchErrorCause = elasticsearchErrorCause;
				return this;
			}

			public Failure build() {
				return new Failure(index, type, id, cause, status, seqNo, term, aborted, elasticsearchErrorCause);
			}
		}
	}

	@Getter
	public static class SearchFailure {
		private final Throwable reason;
		@Nullable
		private final Integer status;
		@Nullable
		private final String index;
		@Nullable
		private final Integer shardId;
		@Nullable
		private final String nodeId;

		private SearchFailure(Throwable reason, @Nullable Integer status, @Nullable String index, @Nullable Integer shardId,
				@Nullable String nodeId) {
			this.reason = reason;
			this.status = status;
			this.index = index;
			this.shardId = shardId;
			this.nodeId = nodeId;
		}

		public static SearchFailureBuilder builder() {
			return new SearchFailureBuilder();
		}


		public static final class SearchFailureBuilder {
			private Throwable reason;
			@Nullable
			private Integer status;
			@Nullable
			private String index;
			@Nullable
			private Integer shardId;
			@Nullable
			private String nodeId;

			private SearchFailureBuilder() {
			}

			public SearchFailureBuilder withReason(Throwable reason) {
				this.reason = reason;
				return this;
			}

			public SearchFailureBuilder withStatus(Integer status) {
				this.status = status;
				return this;
			}

			public SearchFailureBuilder withIndex(String index) {
				this.index = index;
				return this;
			}

			public SearchFailureBuilder withShardId(Integer shardId) {
				this.shardId = shardId;
				return this;
			}

			public SearchFailureBuilder withNodeId(String nodeId) {
				this.nodeId = nodeId;
				return this;
			}

			public SearchFailure build() {
				return new SearchFailure(reason, status, index, shardId, nodeId);
			}
		}

	}

	public static final class ByQueryResponseBuilder {
		private long took;
		private boolean timedOut;
		private long total;
		private long updated;
		private long deleted;
		private int batches;
		private long versionConflicts;
		private long noops;
		private long bulkRetries;
		private long searchRetries;
		@Nullable
		private String reasonCancelled;
		private List<Failure> failures = Collections.emptyList();
		private List<SearchFailure> searchFailures = Collections.emptyList();

		private ByQueryResponseBuilder() {
		}

		public ByQueryResponseBuilder withTook(long took) {
			this.took = took;
			return this;
		}

		public ByQueryResponseBuilder withTimedOut(boolean timedOut) {
			this.timedOut = timedOut;
			return this;
		}

		public ByQueryResponseBuilder withTotal(long total) {
			this.total = total;
			return this;
		}

		public ByQueryResponseBuilder withUpdated(long updated) {
			this.updated = updated;
			return this;
		}

		public ByQueryResponseBuilder withDeleted(long deleted) {
			this.deleted = deleted;
			return this;
		}

		public ByQueryResponseBuilder withBatches(int batches) {
			this.batches = batches;
			return this;
		}

		public ByQueryResponseBuilder withVersionConflicts(long versionConflicts) {
			this.versionConflicts = versionConflicts;
			return this;
		}

		public ByQueryResponseBuilder withNoops(long noops) {
			this.noops = noops;
			return this;
		}

		public ByQueryResponseBuilder withBulkRetries(long bulkRetries) {
			this.bulkRetries = bulkRetries;
			return this;
		}

		public ByQueryResponseBuilder withSearchRetries(long searchRetries) {
			this.searchRetries = searchRetries;
			return this;
		}

		public ByQueryResponseBuilder withReasonCancelled(String reasonCancelled) {
			this.reasonCancelled = reasonCancelled;
			return this;
		}

		public ByQueryResponseBuilder withFailures(List<Failure> failures) {
			this.failures = failures;
			return this;
		}

		public ByQueryResponseBuilder withSearchFailure(List<SearchFailure> searchFailures) {
			this.searchFailures = searchFailures;
			return this;
		}

		public ByQueryResponse build() {
			return new ByQueryResponse(took, timedOut, total, updated, deleted, batches, versionConflicts, noops, bulkRetries,
					searchRetries, reasonCancelled, failures, searchFailures);
		}
	}
}
