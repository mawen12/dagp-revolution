package com.mawen.search;

/**
 * represent {@code version_conflict_engine_exception}
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class VersionConflictException extends SearchException {

	public VersionConflictException(String message) {
		super(message);
	}

	public VersionConflictException(String message, Throwable cause) {
		super(message, cause);
	}
}
