package com.mawen.search;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Object describing an Elasticsearch error
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public class ElasticsearchErrorCause {

	private final String type;
	private final String reason;
	private final String stackTrace;
	private final ElasticsearchErrorCause causedBy;
	private final List<ElasticsearchErrorCause> rootCause;
	private final List<ElasticsearchErrorCause> suppressed;
}
