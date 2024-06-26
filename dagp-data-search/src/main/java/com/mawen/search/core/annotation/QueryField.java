package com.mawen.search.core.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.Criteria.Operator;
import com.mawen.search.core.domain.Range;
import org.apache.commons.lang3.ArrayUtils;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryField {

	/**
	 * 字段名称，支持映射到多个字段上
	 */
	String[] value();

	/**
	 * 查询方式
	 */
	Type type() default Type.SIMPLE_PROPERTY;

	/**
	 * 参数关系
	 */
	Operator relation() default Operator.AND;

	enum Type {

		BETWEEN {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				Range<Object> range = asRange(value);
				boolean lowerBoundInclusive = range.getLowerBound().isInclusive();
				Object lowerBoundValue = range.getLowerBound().getValue().orElse(null);
				boolean upperBoundInclusive = range.getUpperBound().isInclusive();
				Object upperBoundValue = range.getUpperBound().getValue().orElse(null);

				if (lowerBoundInclusive && upperBoundInclusive) {
					criteria.between(lowerBoundValue, upperBoundValue);
				}
				else {
					if (!lowerBoundInclusive) {
						if (lowerBoundValue != null) {
							criteria.greaterThan(lowerBoundValue);
						}
					}
					else {
						if (lowerBoundValue != null) {
							criteria.greaterThanEqual(lowerBoundValue);
						}
					}

					if (!upperBoundInclusive) {
						if (upperBoundValue != null) {
							criteria.lessThan(upperBoundValue);
						}
					}
					else {
						if (upperBoundValue != null) {
							criteria.lessThanEqual(upperBoundValue);
						}
					}
				}
			}
		},
		LESS_THAN {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.lessThan(value);
			}
		},
		LESS_THAN_EQUAL {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.lessThanEqual(value);
			}
		},
		GREATER_THAN {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.greaterThan(value);
			}
		},
		GREATER_THAN_EQUAL {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.greaterThanEqual(value);
			}
		},
		BEFORE {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.lessThanEqual(value);
			}
		},
		AFTER {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.greaterThanEqual(value);
			}
		},
		LIKE {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				// when value contains whitespace, need to use expression
				String param = value.toString();
				if (StringUtils.containsWhitespace(param)) {
					criteria.expression(param);
				}
				else {
					criteria.contains(param);
				}
			}
		},
		STARTING_WITH {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.startsWith(value.toString());
			}
		},
		ENDING_WITH {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.endsWith(value.toString());
			}
		},
		EMPTY {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				if (value instanceof Boolean) {
					boolean val = (boolean) value;
					if (val) {
						criteria.empty();
					}
					else {
						criteria.notEmpty();
					}
				}
			}
		},
		CONTAINING {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				// when value contains whitespace, need to use expression
				String param = value.toString();
				if (StringUtils.containsWhitespace(param)) {
					criteria.expression(param);
				}
				else {
					criteria.contains(param);
				}
			}
		},
		NOT_IN {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				Object[] array = asArray(value);
				if (ArrayUtils.isNotEmpty(array)) {
					criteria.notIn(array);
				}
			}
		},
		IN {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				Object[] array = asArray(value);
				if (ArrayUtils.isNotEmpty(array)) {
					criteria.in(array);
				}
			}
		},
		REGEX {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.expression(value.toString());
			}
		},
		EXISTS {
			@Override
			public void doFrom(Criteria criteria, @Nullable Object value) {
				if (value instanceof Boolean) {
					boolean val = (boolean) value;
					if (val) {
						criteria.exists();
					}
					else {
						criteria.exists().not();
					}
				}
			}
		},
		NEGATING_SIMPLE_PROPERTY {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.is(value).not();
			}
		},
		SIMPLE_PROPERTY {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.is(value.toString());
			}
		},
		/**
		 * @since 0.0.2
		 */
		NESTED {
			@Override
			public void doFrom(Criteria criteria, Object value) {
				// nothing
			}
		};

		public static Type from(PropertyDescriptor property) {

			Assert.notNull(property, "Property must not be null");

			if (property.getReadMethod().isAnnotationPresent(QueryField.class)) {
				return from(property.getReadMethod().getAnnotation(QueryField.class));
			}

			return null;
		}

		public static Type from(java.lang.reflect.Field field) {

			Assert.notNull(field, "Field must not be null");

			if (field.isAnnotationPresent(QueryField.class)) {
				return from(field.getAnnotation(QueryField.class));
			}

			return null;
		}

		public static Type from(QueryField annotation) {

			Assert.notNull(annotation, "Annotation must not be null");
			return annotation.type();
		}

		public void from(Criteria criteria, @Nullable Object value) {

			Assert.notNull(criteria, "Criteria must not be null");

			if (value != null) {
				doFrom(criteria, value);
			}
		}

		public abstract void doFrom(Criteria criteria, Object value);

		private static Range<Object> asRange(Object value) {
			if (value instanceof Range) {
				return (Range<Object>) value;
			}

			Object[] array = asArray(value);
			if (array.length == 2) {
				return Range.closed(array[0], array[1]);
			}
			else if (array.length == 1) {
				return Range.closed(array[0], array[0]);
			}

			return Range.closed(value, value);
		}

		private static Object[] asArray(Object o) {
			if (o instanceof Collection) {
				return ((Collection<?>) o).toArray();
			}
			else if (o.getClass().isArray()) {
				return (Object[]) o;
			}
			else {
				return new Object[]{o};
			}
		}
	}

}
