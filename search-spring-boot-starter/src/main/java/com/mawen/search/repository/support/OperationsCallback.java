package com.mawen.search.repository.support;

import com.mawen.search.core.ElasticsearchOperations;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@FunctionalInterface
public interface OperationsCallback<R> {

	@Nullable
	R doWithOperations(ElasticsearchOperations operations);
}
