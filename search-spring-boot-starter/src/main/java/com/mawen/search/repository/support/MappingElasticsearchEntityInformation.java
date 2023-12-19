package com.mawen.search.repository.support;

import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.repository.core.support.PersistentEntityInformation;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class MappingElasticsearchEntityInformation<T, ID> extends PersistentEntityInformation<T, ID>
		implements ElasticsearchEntityInformation<T, ID> {

	private final ElasticsearchPersistentEntity<T> persistentEntity;

	public MappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> persistentEntity) {
		super(persistentEntity);
		this.persistentEntity = persistentEntity;
	}

	@Override
	public String getIdAttribute() {
		return persistentEntity.getRequiredIdProperty().getFieldName();
	}

	@Override
	public IndexCoordinates getIndexCoordinates() {
		return persistentEntity.getIndexCoordinates();
	}

	@Override
	public Long getVersion(T entity) {

		ElasticsearchPersistentProperty versionProperty = persistentEntity.getVersionProperty();
		try {
			return versionProperty != null ? (Long) persistentEntity.getPropertyAccessor(entity).getProperty(versionProperty)
					: null;
		} catch (Exception e) {
			throw new IllegalStateException("failed to load version field", e);
		}
	}

}
