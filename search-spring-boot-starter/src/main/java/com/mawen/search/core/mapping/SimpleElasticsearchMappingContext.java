package com.mawen.search.core.mapping;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class SimpleElasticsearchMappingContext extends AbstractMappingContext<SimpleElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> {

	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		return !ElasticsearchSimpleTypes.HOLDER.isSimpleType(type.getType());
	}

	@Override
	protected <T> SimpleElasticsearchPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new SimpleElasticsearchPersistentEntity<>(typeInformation);
	}

	@Override
	protected ElasticsearchPersistentProperty createPersistentProperty(Property property,
			SimpleElasticsearchPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new SimpleElasticsearchPersistentProperty(property, owner, simpleTypeHolder);
	}
}


