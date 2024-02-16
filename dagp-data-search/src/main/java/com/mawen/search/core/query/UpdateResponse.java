package com.mawen.search.core.query;

import lombok.Getter;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class UpdateResponse {

	private Result result;

	public UpdateResponse(Result result) {

		Assert.notNull(result, "result must not be null");

		this.result = result;
	}

	public static UpdateResponse of(Result result) {
		return new UpdateResponse(result);
	}

	public enum Result {
		CREATED, UPDATED, DELETED, NOT_FOUND, NOOP;
	}
}
