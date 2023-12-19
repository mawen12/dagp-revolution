package com.mawen.search.core.query;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class UpdateResponse {


	private Result result;

	public UpdateResponse(Result result) {

		Assert.notNull(result, "result must not be null");

		this.result = result;
	}

	/**
	 * @since 4.4
	 */
	public static UpdateResponse of(Result result) {
		return new UpdateResponse(result);
	}

	public Result getResult() {
		return result;
	}

	public enum Result {
		CREATED, UPDATED, DELETED, NOT_FOUND, NOOP;
	}
}
