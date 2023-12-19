package com.mawen.search.core.query;

import java.time.Duration;

import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.core.support.ActiveShardCount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BulkOptions {

	private static final BulkOptions defaultOptions = builder().build();

	@Nullable
	private final Duration timeout;
	@Nullable
	private final RefreshPolicy refreshPolicy;
	@Nullable
	private final ActiveShardCount waitForActiveShards;
	@Nullable
	private final String pipeline;
	@Nullable
	private final String routingId;

	public static BulkOptionsBuilder builder() {
		return new BulkOptionsBuilder();
	}

	public static BulkOptions defaultOptions() {
		return defaultOptions;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class BulkOptionsBuilder {

		@Nullable
		private Duration timeout;
		@Nullable
		private RefreshPolicy refreshPolicy;
		@Nullable
		private ActiveShardCount waitForActiveShards;
		@Nullable
		private String pipeline;
		@Nullable
		private String routingId;

		public BulkOptionsBuilder withTimeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public BulkOptionsBuilder withRefreshPolicy(RefreshPolicy refreshPolicy) {
			this.refreshPolicy = refreshPolicy;
			return this;
		}

		public BulkOptionsBuilder withWaitForActiveShards(ActiveShardCount waitForActiveShards) {
			this.waitForActiveShards = waitForActiveShards;
			return this;
		}

		public BulkOptionsBuilder withPipeline(String pipeline) {
			this.pipeline = pipeline;
			return this;
		}

		public BulkOptionsBuilder withRoutingId(String routingId) {
			this.routingId = routingId;
			return this;
		}

		public BulkOptions build() {
			return new BulkOptions(timeout, refreshPolicy, waitForActiveShards, pipeline, routingId);
		}
	}
}
