package com.mawen.search.repository.support;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface ElasticsearchEntityInformationCreator {

	<T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass);

}
