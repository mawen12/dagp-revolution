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
public class DateRangePropertyValueConverter extends AbstractRangePropertyValueConverter<Date> {

	private final List<ElasticsearchDateConverter> dateConverters;

	public DateRangePropertyValueConverter(PersistentProperty<?> property, List<ElasticsearchDateConverter> dateConverters) {

		super(property);

		this.dateConverters = dateConverters;
	}

	@Override
	protected String format(Date value) {
		return dateConverters.get(0).format(value);
	}

	@Override
	protected Date parse(String value) {

		for (ElasticsearchDateConverter converters : dateConverters) {
			try {
				return converters.parse(value);
			}
			catch (Exception e) {
				if (log.isTraceEnabled()) {
					log.trace(e.getMessage(), e);
				}
			}
		}

		throw new MappingException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				getGenericType().getTypeName(), getProperty().getName()));
	}
}
