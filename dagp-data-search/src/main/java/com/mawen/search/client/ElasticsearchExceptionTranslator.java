package com.mawen.search.client;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import com.mawen.search.*;
import org.elasticsearch.client.ResponseException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ElasticsearchExceptionTranslator {

	public ElasticsearchExceptionTranslator() {
	}

	/**
	 * translates an Exception if possible. Exceptions that are no {@link RuntimeException}s are wrapped in a
	 * RuntimeException
	 *
	 * @param throwable the Exception to map
	 * @return the potentially translated RuntimeException.
	 */
	public RuntimeException translateException(Throwable throwable) {

		RuntimeException runtimeException = throwable instanceof RuntimeException ? (RuntimeException) throwable
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = translateExceptionIfPossible(runtimeException);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
	}

	public SearchException translateExceptionIfPossible(RuntimeException ex) {

		checkForConflictException(ex);

		if (ex instanceof ElasticsearchException) {
			ElasticsearchException elasticsearchException = (ElasticsearchException) ex;
			ErrorResponse response = elasticsearchException.response();
			String errorType = response.error().type();
			String errorReason = response.error().reason() != null ? response.error().reason() : "undefined reason";
			String causedByReason = response.error().causedBy() != null ? response.error().causedBy().reason() : errorReason;

			if (response.status() == 404) {

				if ("index_not_found_exception".equals(errorType)) {
					// noinspection RegExpRedundantEscape
					Pattern pattern = Pattern.compile(".*no such index \\[(.*)\\]");
					String index = "";
					Matcher matcher = pattern.matcher(errorReason);
					if (matcher.matches()) {
						index = matcher.group(1);
					}
					return new NoSuchIndexException(index);
				}

				return new ResourceNotFoundException(errorReason);
			}

			return new UncategorizedElasticsearchException(causedByReason, response.status(), null, ex);
		}

		Throwable cause = ex.getCause();
		if (cause instanceof IOException) {
			return new InvalidApiUsageException(ex.getMessage(), ex);
		}

		return null;
	}

	private void checkForConflictException(Throwable exception) {
		Integer status = null;
		String message = null;

		if (exception instanceof ResponseException) {
			ResponseException responseException = (ResponseException) exception;
			status = responseException.getResponse().getStatusLine().getStatusCode();
			message = responseException.getMessage();
		}
		else if (exception.getCause() != null) {
			checkForConflictException(exception.getCause());
		}

		if (status != null && message != null) {
			if (status == 409 && message.contains("type\":\"version_conflict_engine_exception"))
				if (message.contains("version conflict, required seqNo")) {
					throw new OptimisticLockingFailureException("Cannot index a document due to seq_no+primary_term conflict",
							exception);
				}
				else if (message.contains("version conflict, current version [")) {
					throw new VersionConflictException("version conflict", exception);
				}
		}
	}
}
