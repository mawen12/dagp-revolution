package com.mawen.search.core.convert;

import java.time.temporal.TemporalAccessor;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class TemporalPropertyValueConverter extends AbstractPropertyValueConverter {

	private final List<ElasticsearchDateConverter> dateConverters;

	public TemporalPropertyValueConverter(PersistentProperty<?> property,
			List<ElasticsearchDateConverter> dateConverters) {

		super(property);
		this.dateConverters = dateConverters;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object read(Object value) {

		String s = value.toString();
		Class<?> actualType = getProperty().getActualType();

		for (ElasticsearchDateConverter dateConverter : dateConverters) {
			try {
				return dateConverter.parse(s, (Class<? extends TemporalAccessor>) actualType);
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

		if (!TemporalAccessor.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}

		try {
			return dateConverters.get(0).format((TemporalAccessor) value);
		}
		catch (Exception e) {
			throw new MappingException(
					String.format("Unable to convert value '%s' of property '%s'", value, getProperty().getName()), e);
		}
	}

}
