package com.mawen.search.repository.support;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@FunctionalInterface
public interface OperationsCallback<R> {

	R doWithOperations(ElasticsearchOperations operations);
}
