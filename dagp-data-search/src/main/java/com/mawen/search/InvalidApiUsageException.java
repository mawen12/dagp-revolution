package com.mawen.search;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class InvalidApiUsageException extends SearchException {

	public InvalidApiUsageException(String message) {
		super(message);
	}

	public InvalidApiUsageException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
