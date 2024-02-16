package com.mawen.search.core.support;

import com.mawen.search.ElasticsearchErrorCause;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

/**
 * Response object for items returned from multiget requests,
 * encapsulating the returned data and potential error information.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MultiGetItem<T> {

	@Nullable
	private final T item;
	@Nullable
	private final Failure failure;

	public static <T> MultiGetItem<T> of(@Nullable T item, @Nullable Failure failure) {
		return new MultiGetItem<>(item, failure);
	}

	public boolean isFailed() {
		return failure != null;
	}

	public boolean hasItem() {
		return item != null;
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Failure {

		@Nullable
		private final String index;
		@Nullable
		private final String type;
		@Nullable
		private final String id;
		@Nullable
		private final Exception exception;
		@Nullable
		private final ElasticsearchErrorCause elasticsearchErrorCause;

		public static Failure of(String index, @Nullable String type, @Nullable String id, @Nullable Exception exception, @Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
			return new Failure(index, type, id, exception, elasticsearchErrorCause);
		}
	}

}
