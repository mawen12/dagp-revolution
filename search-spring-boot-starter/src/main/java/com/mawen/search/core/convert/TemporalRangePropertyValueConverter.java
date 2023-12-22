package com.mawen.search.core.convert;

import java.time.temporal.TemporalAccessor;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class TemporalRangePropertyValueConverter extends AbstractRangePropertyValueConverter<TemporalAccessor> {

	private final List<ElasticsearchDateConverter> dateConverters;

	public TemporalRangePropertyValueConverter(PersistentProperty<?> property, List<ElasticsearchDateConverter> dateConverters) {

		super(property);

		Assert.notEmpty(dateConverters, "dateConverters must not be empty.");
		this.dateConverters = dateConverters;
	}

	@Override
	protected String format(TemporalAccessor temporal) {
		return dateConverters.get(0).format(temporal);
	}

	@Override
	protected TemporalAccessor parse(String value) {

		Class<?> type = getGenericType();
		for (ElasticsearchDateConverter converters : dateConverters) {
			try {
				return converters.parse(value, (Class<? extends TemporalAccessor>) type);
			}
			catch (Exception e) {
				if (log.isTraceEnabled()) {
					log.trace(e.getMessage(), e);
				}
			}
		}

		throw new MappingException(String.format("Unable to convert value '%s' to %s for property '%s'", value,
				type.getTypeName(), getProperty().getName()));
	}

}
