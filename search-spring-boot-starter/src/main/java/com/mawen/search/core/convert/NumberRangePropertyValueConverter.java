package com.mawen.search.core.convert;

import com.mawen.search.core.domain.Range;
import com.mawen.search.core.mapping.PropertyValueConverter;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;

/**
 * {@link Range<Number> 数值范围} 与 Elasticsearch 互相转换的 {@link PropertyValueConverter}
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class NumberRangePropertyValueConverter extends AbstractRangePropertyValueConverter<Number> {

	public NumberRangePropertyValueConverter(PersistentProperty<?> property) {
		super(property);
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

		throw new MappingException(String.format(PARSE_EXCEPTION_MESSAGE, value, type.getTypeName(), property.getName()));
	}
}
