package com.mawen.search.core.convert;

import com.mawen.search.core.mapping.PropertyValueConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;

import java.time.temporal.TemporalAccessor;
import java.util.List;

/**
 * {@link TemporalAccessor 时间访问器} 和 Elasticsearch 日期类型互相转换的 {@link PropertyValueConverter}
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Slf4j
public class TemporalPropertyValueConverter extends AbstractPropertyValueConverter {

	private final List<ElasticsearchDateConverter> dateConverters;

	public TemporalPropertyValueConverter(PersistentProperty<?> property,
			List<ElasticsearchDateConverter> dateConverters) {

		super(property);
		this.dateConverters = dateConverters;
	}

	@Override
	public Object read(Object value) {

		String s = value.toString();
		Class<?> actualType = property.getActualType();

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

		throw new MappingException(
				String.format(READ_EXCEPTION_MESSAGE, s, property.getActualType().getTypeName(), property.getName()));
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
			throw new MappingException(String.format(WRITE_EXCEPTION_MESSAGE, value, property.getName()), e);
		}
	}

}
