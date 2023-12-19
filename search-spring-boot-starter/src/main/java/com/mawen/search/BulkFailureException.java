package com.mawen.search;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class BulkFailureException extends SearchException {

	private final Map<String, FailureDetails> failedDocuments;

	public BulkFailureException(String msg, Map<String, FailureDetails> failedDocuments) {
		super(msg);
		this.failedDocuments = failedDocuments;
	}

	public Map<String, FailureDetails> getFailedDocuments() {
		return failedDocuments;
	}

	@Data
	@AllArgsConstructor
	public static class FailureDetails {

		private final Integer status;
		private final String errorMessage;
	}

}
