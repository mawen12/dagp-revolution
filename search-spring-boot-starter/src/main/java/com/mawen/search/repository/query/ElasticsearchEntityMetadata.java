package com.mawen.search.repository.query;

import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;

import org.springframework.data.repository.core.EntityMetadata;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface ElasticsearchEntityMetadata<T> extends EntityMetadata<T> {

	String getIndexName();

	ElasticsearchPersistentEntity<?> getEntity();

}
