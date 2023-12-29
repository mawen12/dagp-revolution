package com.mawen.search;

import org.springframework.lang.Nullable;

/**
 * represent {@code response=404}
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ResourceNotFoundException extends SearchException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
