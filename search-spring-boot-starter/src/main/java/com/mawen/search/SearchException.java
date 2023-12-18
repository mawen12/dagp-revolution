package com.mawen.search;

import org.springframework.lang.Nullable;

/**
 * Root of the hierarchy of Elasticsearch exceptions.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public abstract class SearchException extends RuntimeException {

	protected SearchException(String message) {
		super(message);
	}

	protected SearchException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
