package com.mawen.search.repository.query;

import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import lombok.Getter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class SimpleElasticsearchEntityMetadata<T> implements ElasticsearchEntityMetadata<T> {

	private final Class<T> type;
	private final ElasticsearchPersistentEntity<?> entity;

	public SimpleElasticsearchEntityMetadata(Class<T> type, ElasticsearchPersistentEntity<?> entity) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(entity, "Entity must not be null!");

		this.type = type;
		this.entity = entity;
	}

	@Override
	public String getIndexName() {
		return entity.getIndexCoordinates().getIndexName();
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}
}
