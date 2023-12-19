package com.mawen.search.client.response;

import java.util.List;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.BulkIndexByScrollFailure;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetError;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.json.JsonpMapper;
import com.mawen.search.ElasticsearchErrorCause;
import com.mawen.search.client.EntityAsMap;
import com.mawen.search.core.query.ByQueryResponse;
import com.mawen.search.core.support.MultiGetItem;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class ResponseConverter {

	private final JsonpMapper jsonpMapper;

	public ResponseConverter(JsonpMapper jsonpMapper) {
		this.jsonpMapper = jsonpMapper;
	}

	// region document operations

	@Nullable
	public static MultiGetItem.Failure getFailure(MultiGetResponseItem<EntityAsMap> itemResponse) {

		MultiGetError responseFailure = itemResponse.isFailure() ? itemResponse.failure() : null;

		return responseFailure != null
				? MultiGetItem.Failure.of(responseFailure.index(), null, responseFailure.id(), null,
				toErrorCause(responseFailure.error()))
				: null;
	}

	@Nullable
	private static ElasticsearchErrorCause toErrorCause(@Nullable ErrorCause errorCause) {

		if (errorCause != null) {
			return new ElasticsearchErrorCause( //
					errorCause.type(), //
					errorCause.reason(), //
					errorCause.stackTrace(), //
					toErrorCause(errorCause.causedBy()), //
					errorCause.rootCause().stream().map(ResponseConverter::toErrorCause).collect(Collectors.toList()), //
					errorCause.suppressed().stream().map(ResponseConverter::toErrorCause).collect(Collectors.toList()));
		}
		else {
			return null;
		}
	}

	private ByQueryResponse.Failure byQueryResponseFailureOf(BulkIndexByScrollFailure failure) {
		return ByQueryResponse.Failure.builder() //
				.withIndex(failure.index()) //
				.withType(failure.type()) //
				.withId(failure.id()) //
				.withStatus(failure.status())//
				.withErrorCause(toErrorCause(failure.cause())).build();
	}

	public ByQueryResponse byQueryResponse(DeleteByQueryResponse response) {
		// the code for the methods taking a DeleteByQueryResponse or a UpdateByQueryResponse is duplicated because the
		// Elasticsearch responses do not share a common class
		// noinspection DuplicatedCode
		List<ByQueryResponse.Failure> failures = response.failures().stream().map(this::byQueryResponseFailureOf)
				.collect(Collectors.toList());

		ByQueryResponse.ByQueryResponseBuilder builder = ByQueryResponse.builder();

		if (response.took() != null) {
			builder.withTook(response.took());
		}

		if (response.timedOut() != null) {
			builder.withTimedOut(response.timedOut());
		}

		if (response.total() != null) {
			builder.withTotal(response.total());
		}

		if (response.deleted() != null) {
			builder.withDeleted(response.deleted());
		}

		if (response.batches() != null) {
			builder.withBatches(Math.toIntExact(response.batches()));
		}

		if (response.versionConflicts() != null) {
			builder.withVersionConflicts(response.versionConflicts());
		}

		if (response.noops() != null) {
			builder.withNoops(response.noops());
		}

		if (response.retries() != null) {
			builder.withBulkRetries(response.retries().bulk());
			builder.withSearchRetries(response.retries().search());
		}

		builder.withFailures(failures);

		return builder.build();
	}

	// endregion

	// region helper functions

	public ByQueryResponse byQueryResponse(UpdateByQueryResponse response) {
		// the code for the methods taking a DeleteByQueryResponse or a UpdateByQueryResponse is duplicated because the
		// Elasticsearch responses do not share a common class
		// noinspection DuplicatedCode
		List<ByQueryResponse.Failure> failures = response.failures().stream().map(this::byQueryResponseFailureOf)
				.collect(Collectors.toList());

		ByQueryResponse.ByQueryResponseBuilder builder = ByQueryResponse.builder();

		if (response.took() != null) {
			builder.withTook(response.took());
		}

		if (response.timedOut() != null) {
			builder.withTimedOut(response.timedOut());
		}

		if (response.total() != null) {
			builder.withTotal(response.total());
		}

		if (response.deleted() != null) {
			builder.withDeleted(response.deleted());
		}

		if (response.batches() != null) {
			builder.withBatches(Math.toIntExact(response.batches()));
		}

		if (response.versionConflicts() != null) {
			builder.withVersionConflicts(response.versionConflicts());
		}

		if (response.noops() != null) {
			builder.withNoops(response.noops());
		}

		if (response.retries() != null) {
			builder.withBulkRetries(response.retries().bulk());
			builder.withSearchRetries(response.retries().search());
		}

		builder.withFailures(failures);

		return builder.build();
	}

	private long timeToLong(Time time) {

		if (time.isTime()) {
			return Long.parseLong(time.time());
		}
		else {
			return time.offset();
		}
	}
	// endregion
}
