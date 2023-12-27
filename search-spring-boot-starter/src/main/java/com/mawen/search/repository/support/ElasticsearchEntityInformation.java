package com.mawen.search.repository.support;

import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface ElasticsearchEntityInformation<T, ID> extends EntityInformation<T, ID> {

	boolean isDynamicIndex();

	@Nullable
	String getIndexName(T entity);

	String getIdAttribute();

	IndexCoordinates getIndexCoordinates();

	@Nullable
	Long getVersion(T entity);

}
