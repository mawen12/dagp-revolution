package com.mawen.search.core.convert;

import java.util.Date;
import java.util.List;

import com.mawen.search.core.domain.Range;
import com.mawen.search.core.mapping.PropertyValueConverter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * {@link Range<Date> 日期范围} 与 Elasticsearch 互相转换的 {@link PropertyValueConverter}
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Slf4j
public class DateRangePropertyValueConverter extends AbstractRangePropertyValueConverter<Date> {

	private final List<ElasticsearchDateConverter> dateConverters;

	public DateRangePropertyValueConverter(PersistentProperty<?> property, List<ElasticsearchDateConverter> dateConverters) {

		super(property);

		Assert.notEmpty(dateConverters, "dateConverters must not be empty.");

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

		throw new MappingException(String.format(PARSE_EXCEPTION_MESSAGE, value, getGenericType().getTypeName(), property.getName()));
	}
}
