package com.mawen.search.core.convert;

import com.mawen.search.core.mapping.PropertyValueConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;

import java.util.Date;
import java.util.List;

/**
 * {@link Date 日期} 和 Elasticsearch 日期类型互相转换的 {@link PropertyValueConverter}
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
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

		throw new MappingException(String.format(READ_EXCEPTION_MESSAGE, s, property.getActualType().getTypeName(), property.getName()));
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
			throw new MappingException(String.format(WRITE_EXCEPTION_MESSAGE, value, property.getName()), e);
		}
	}
}
