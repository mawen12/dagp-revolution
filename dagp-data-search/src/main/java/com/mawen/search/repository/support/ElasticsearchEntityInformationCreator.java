package com.mawen.search.repository.support;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface ElasticsearchEntityInformationCreator {

	<T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass);

}
