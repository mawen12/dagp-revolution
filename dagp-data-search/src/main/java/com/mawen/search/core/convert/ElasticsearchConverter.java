package com.mawen.search.core.convert;

import com.mawen.search.core.document.Document;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.query.Query;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 *
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface ElasticsearchConverter extends EntityConverter<ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty, Object, Document> {


	default ProjectionFactory getProjectionFactory() {
		return new SpelAwareProxyProjectionFactory();
	}

	// region write

	default String convertId(Object idValue) {

		Assert.notNull(idValue, "idValue must not be null!");

		if (!getConversionService().canConvert(idValue.getClass(), String.class)) {
			return idValue.toString();
		}

		String converted = getConversionService().convert(idValue, String.class);

		if (converted == null) {
			return idValue.toString();
		}

		return converted;
	}


	default Document mapObject(@Nullable Object source) {

		Document target = Document.create();

		if (source != null) {
			write(source, target);
		}
		return target;
	}
	// endregion

	// region query

	void updateQuery(Query query, @Nullable Class<?> domainClass);


	public String updateFieldNames(String propertyPath, ElasticsearchPersistentEntity<?> persistentEntity);
	// endregion
}
