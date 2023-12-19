package com.mawen.search.core.convert;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class NumberRangePropertyValueConverter extends AbstractRangePropertyValueConverter<Number> {

	public NumberRangePropertyValueConverter(PersistentProperty<?> property, Class<?> genericType) {
		super(property, genericType);
	}

	@Override
	protected String format(Number value) {
		return String.valueOf(value);
	}

	@Override
	protected Number parse(String value) {

		Class<?> type = getGenericType();
		if (Integer.class.isAssignableFrom(type)) {
			return Integer.valueOf(value);
		}
		else if (Float.class.isAssignableFrom(type)) {
			return Float.valueOf(value);
		}
		else if (Long.class.isAssignableFrom(type)) {
			return Long.valueOf(value);
		}
		else if (Double.class.isAssignableFrom(type)) {
			return Double.valueOf(value);
		}

		throw new MappingException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				type.getTypeName(), getProperty().getName()));
	}
}
