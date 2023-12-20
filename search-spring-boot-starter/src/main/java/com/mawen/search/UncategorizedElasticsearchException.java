package com.mawen.search;

import lombok.Getter;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class UncategorizedElasticsearchException extends SearchException {

	@Nullable
	final String responseBody;
	@Nullable
	private final Integer statusCode;

	public UncategorizedElasticsearchException(String msg) {
		this(msg, null);
	}

	public UncategorizedElasticsearchException(String msg, @Nullable Throwable cause) {
		this(msg, null, null, cause);
	}

	public UncategorizedElasticsearchException(String msg, @Nullable Integer statusCode, @Nullable String responseBody,
			@Nullable Throwable cause) {
		super(msg, cause);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}
}
