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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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

		BETWEEN{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				Range<Object> range = asRange(value);
				criteria.between(range.getLowerBound(), range.getUpperBound());
			}
		},
		LESS_THAN{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.lessThan(value);
			}
		},
		LESS_THAN_EQUAL{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.lessThanEqual(value);
			}
		},
		GREATER_THAN{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.greaterThan(value);
			}
		},
		GREATER_THAN_EQUAL{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.greaterThanEqual(value);
			}
		},
		BEFORE{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.lessThanEqual(value);
			}
		},
		AFTER{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.greaterThanEqual(value);
			}
		},
		LIKE{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.contains(value.toString());
			}
		},
		STARTING_WITH{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.startsWith(value.toString());
			}
		},
		ENDING_WITH{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.endsWith(value.toString());
			}
		},
		EMPTY{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				if (value instanceof Boolean) {
					boolean val = (boolean) value;
					if (val) {
						criteria.empty();
					} else {
						criteria.notEmpty();
					}
				}
			}
		},
		CONTAINING{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.contains(value.toString());
			}
		},
		NOT_IN{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.notIn(asArray(value));
			}
		},
		IN{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.in(asArray(value));
			}
		},
		REGEX{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.expression(value.toString());
			}
		},
		EXISTS{
			@Override
			public void doFrom(Criteria criteria, @Nullable Object value) {
				if (value instanceof Boolean) {
					boolean val = (boolean) value;
					if (val) {
						criteria.exists();
					} else {
						criteria.exists().not();
					}
				}
			}
		},
		BOOLEAN{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				if (value instanceof Boolean) {
					boolean val = (boolean) value;
					criteria.is(val);
				}
			}
		},
		NEGATING_SIMPLE_PROPERTY{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.is(value).not();
			}
		},
		SIMPLE_PROPERTY{
			@Override
			public void doFrom(Criteria criteria, Object value) {
				criteria.is(value.toString());
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
