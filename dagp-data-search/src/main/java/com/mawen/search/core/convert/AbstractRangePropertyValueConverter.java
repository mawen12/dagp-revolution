package com.mawen.search.core.convert;

import com.mawen.search.core.domain.Range;
import com.mawen.search.core.mapping.PropertyValueConverter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link PropertyValueConverter} 的抽象实现，支持 {@link Range 范围} 类型的转换
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public abstract class AbstractRangePropertyValueConverter<T> extends AbstractPropertyValueConverter {

	protected static final String PARSE_EXCEPTION_MESSAGE = READ_EXCEPTION_MESSAGE;

	protected static final String LT_FIELD = "lt";
	protected static final String LTE_FIELD = "lte";
	protected static final String GT_FIELD = "gt";
	protected static final String GTE_FIELD = "gte";

	protected AbstractRangePropertyValueConverter(PersistentProperty<?> property) {
		super(property);
	}

	@Override
	public Object write(Object value) {
		Assert.notNull(value, "value must not be null.");

		if (!Range.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}

		try {
			// noinspection unchecked
			Range<T> range = (Range<T>) value;
			Range.Bound<T> lowerBound = range.getLowerBound();
			Range.Bound<T> upperBound = range.getUpperBound();
			Map<String, Object> target = new LinkedHashMap<>();

			if (lowerBound.getValue().isPresent()) {
				String lowerBoundValue = format(lowerBound.getValue().get());
				if (lowerBound.isInclusive()) {
					target.put(GTE_FIELD, lowerBoundValue);
				}
				else {
					target.put(GT_FIELD, lowerBoundValue);
				}
			}

			if (upperBound.getValue().isPresent()) {
				String upperBoundValue = format(upperBound.getValue().get());
				if (upperBound.isInclusive()) {
					target.put(LTE_FIELD, upperBoundValue);
				}
				else {
					target.put(LT_FIELD, upperBoundValue);
				}
			}

			return target;

		}
		catch (Exception e) {
			throw new MappingException(String.format(WRITE_EXCEPTION_MESSAGE, value, property.getName()), e);
		}
	}

	@Override
	public Object read(Object value) {

		Assert.notNull(value, "value must not be null.");
		Assert.isInstanceOf(Map.class, value, "value must be instance of Map.");

		try {
			// noinspection unchecked
			Map<String, Object> source = (Map<String, Object>) value;
			Range.Bound<T> lowerBound;
			Range.Bound<T> upperBound;

			if (source.containsKey(GTE_FIELD)) {
				lowerBound = Range.Bound.inclusive(parse((String) source.get(GTE_FIELD)));
			}
			else if (source.containsKey(GT_FIELD)) {
				lowerBound = Range.Bound.exclusive(parse((String) source.get(GT_FIELD)));
			}
			else {
				lowerBound = Range.Bound.unbounded();
			}

			if (source.containsKey(LTE_FIELD)) {
				upperBound = Range.Bound.inclusive(parse((String) source.get(LTE_FIELD)));
			}
			else if (source.containsKey(LT_FIELD)) {
				upperBound = Range.Bound.exclusive(parse((String) source.get(LT_FIELD)));
			}
			else {
				upperBound = Range.Bound.unbounded();
			}

			return Range.of(lowerBound, upperBound);

		}
		catch (Exception e) {
			throw new MappingException(String.format(READ_EXCEPTION_MESSAGE, value, property.getActualType().getName(), property.getName()), e);
		}
	}

	protected Class<?> getGenericType() {
		return property.getTypeInformation().getTypeArguments().get(0).getType();
	}

	protected abstract String format(T value);

	protected abstract T parse(String value);
}
