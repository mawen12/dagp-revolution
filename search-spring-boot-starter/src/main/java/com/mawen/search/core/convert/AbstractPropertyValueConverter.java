package com.mawen.search.core.convert;

import com.mawen.search.core.mapping.PropertyValueConverter;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public abstract class AbstractPropertyValueConverter implements PropertyValueConverter {

	private final PersistentProperty<?> property;

	protected AbstractPropertyValueConverter(PersistentProperty<?> property) {

		Assert.notNull(property, "property must not be null");
		this.property = property;
	}

	protected PersistentProperty<?> getProperty() {
		return property;
	}
}
