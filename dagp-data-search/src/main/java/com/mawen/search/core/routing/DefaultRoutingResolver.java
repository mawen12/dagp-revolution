package com.mawen.search.core.routing;

import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class DefaultRoutingResolver implements RoutingResolver {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ? extends ElasticsearchPersistentProperty> mappingContext;

	public DefaultRoutingResolver(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ? extends ElasticsearchPersistentProperty> mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public String getRouting() {
		return null;
	}

	@Override
	@Nullable
	public <T> String getRouting(T bean) {

		ElasticsearchPersistentEntity<T> persistentEntity = (ElasticsearchPersistentEntity<T>) mappingContext
				.getPersistentEntity(bean.getClass());

		if (persistentEntity != null) {
			return persistentEntity.resolveRouting(bean);
		}

		return null;
	}
}
