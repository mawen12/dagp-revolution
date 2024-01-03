package com.mawen.search.core.convert;

import com.mawen.search.core.mapping.PropertyValueConverter;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * {@link PropertyValueConverter} 的抽象实现，用于设置和获取 {@link PersistentProperty}
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public abstract class AbstractPropertyValueConverter implements PropertyValueConverter {

	protected static final String WRITE_EXCEPTION_MESSAGE = "Unable to convert value '%s' of property '%s'";
	protected static final String READ_EXCEPTION_MESSAGE = "Unable to convert value '%s' to %s for property '%s'";

	protected final PersistentProperty<?> property;

	protected AbstractPropertyValueConverter(PersistentProperty<?> property) {

		Assert.notNull(property, "property must not be null");
		this.property = property;
	}
}
