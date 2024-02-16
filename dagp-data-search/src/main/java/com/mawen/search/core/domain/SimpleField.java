package com.mawen.search.core.domain;

import com.mawen.search.core.annotation.FieldType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class SimpleField implements Field {
	private String name;
	@Nullable
	private FieldType fieldType;
	@Nullable
	private String path;

	public SimpleField(String name) {

		Assert.hasText(name, "name must not be null");

		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {

		Assert.hasText(name, "name must not be null");

		this.name = name;
	}

	@Nullable
	@Override
	public FieldType getFieldType() {
		return fieldType;
	}

	@Override
	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	@Override
	@Nullable
	public String getPath() {
		return path;
	}

	@Override
	public void setPath(@Nullable String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return getName();
	}
}
