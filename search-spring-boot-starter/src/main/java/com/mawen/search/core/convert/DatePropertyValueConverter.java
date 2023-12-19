package com.mawen.search.core.convert;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class DatePropertyValueConverter extends AbstractPropertyValueConverter {

	private final List<ElasticsearchDateConverter> dateConverters;

	public DatePropertyValueConverter(PersistentProperty<?> property, List<ElasticsearchDateConverter> dateConverters) {

		super(property);
		this.dateConverters = dateConverters;
	}

	@Override
	public Object read(Object value) {

		String s = value.toString();

		for (ElasticsearchDateConverter dateConverter : dateConverters) {
			try {
				return dateConverter.parse(s);
			}
			catch (Exception e) {
				if (log.isTraceEnabled()) {
					log.trace(e.getMessage(), e);
				}
			}
		}

		throw new MappingException(String.format("Unable to convert value '%s' to %s for property '%s'", s,
				getProperty().getActualType().getTypeName(), getProperty().getName()));
	}

	@Override
	public Object write(Object value) {

		if (!Date.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}

		try {
			return dateConverters.get(0).format((Date) value);
		}
		catch (Exception e) {
			throw new MappingException(
					String.format("Unable to convert value '%s' of property '%s'", value, getProperty().getName()), e);
		}
	}
}
