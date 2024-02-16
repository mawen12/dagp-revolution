package com.mawen.search.core.domain;

import com.mawen.search.core.annotation.FieldType;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface Field {

	String getName();

	void setName(String name);

	@Nullable
	FieldType getFieldType();

	void setFieldType(FieldType fieldType);

	@Nullable
	String getPath();

	void setPath(@Nullable String path);
}
