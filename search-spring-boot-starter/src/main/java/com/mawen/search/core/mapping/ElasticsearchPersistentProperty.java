package com.mawen.search.core.mapping;

import org.springframework.data.mapping.PersistentProperty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface ElasticsearchPersistentProperty extends PersistentProperty<ElasticsearchPersistentProperty> {

	String getFieldName();

	boolean hasExplicitFieldName();

	boolean isSeqNoPrimaryTermProperty();

	boolean hasPropertyValueConverter();

	PropertyValueConverter getPropertyValueConverter();

	boolean isIndexNameProperty();

	String getIndexName();

	default Class<?> getActualTypeOrNull() {
		try {
			return getActualType();
		}
		catch (Exception e) {
			return null;
		}
	}

}
