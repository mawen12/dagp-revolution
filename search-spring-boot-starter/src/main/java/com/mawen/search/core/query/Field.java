package com.mawen.search.core.query;

import com.mawen.search.core.annotation.FieldType;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface Field {
	String getName();

	void setName(String name);

	/**
	 * @return The annotated FieldType of the field
	 */
	@Nullable
	FieldType getFieldType();

	/**
	 * @param fieldType sets the field's type
	 */
	void setFieldType(FieldType fieldType);

	/**
	 * @return the path if this is a field for a nested query
	 * @since 4.2
	 */
	@Nullable
	String getPath();

	/**
	 * Sets the path if this field has a multi-part name that should be used in a nested query.
	 *
	 * @param path the value to set
	 * @since 4.2
	 */
	void setPath(@Nullable String path);
}
