package com.mawen.search.repository.query;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.mawen.search.core.annotation.QueryField;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ParamQuery implements Streamable<ParamQuery.ParamQueryField> {

	private final com.mawen.search.core.annotation.ParamQuery paramQuery;
	@Nullable
	private final Field[] fields;
	private final Lazy<List<ParamQueryField>> effectiveFields;

	public ParamQuery(Object parameter) {

		Assert.notNull(parameter, "ParamQuery parameter must not be null");

		this.paramQuery = AnnotatedElementUtils.findMergedAnnotation(parameter.getClass(), com.mawen.search.core.annotation.ParamQuery.class);
		Assert.notNull(paramQuery, String.format("Parameter %s must annotated with @ParamQuery", parameter.getClass().getName()));

		this.fields = parameter.getClass().getDeclaredFields();
		this.effectiveFields = Lazy.of(() ->
				Arrays.stream(fields)
						.map(field -> new ParamQueryField(field, parameter))
						.filter(ParamQueryField::hasQueryField)
						.filter(ParamQueryField::hasValue)
						.collect(Collectors.toList()));
	}

	@Nullable
	public List<ParamQueryField> getEffectiveFields() {
		return effectiveFields.get();
	}

	@Override
	public Iterator<ParamQueryField> iterator() {
		return effectiveFields.get().iterator();
	}


	public static class ParamQueryField {

		@Nullable
		private final QueryField queryField;
		private final Field field;
		private final Object owner;
		private final Lazy<Object> value;

		public ParamQueryField(Field field, Object owner) {

			Assert.notNull(field, "field must not be null");
			Assert.notNull(owner, "owner must not be null");

			this.queryField = AnnotatedElementUtils.findMergedAnnotation(field, QueryField.class);
			this.field = field;
			this.owner = owner;
			this.value = Lazy.of(() -> {

				Object value = null;
				try {
					field.setAccessible(true);
					value = field.get(owner);
				}
				catch (IllegalAccessException e) {}
				return value;
			});
		}

		public boolean hasQueryField() {
			return queryField != null;
		}

		public QueryField getQueryField() {
			return queryField;
		}

		public boolean hasValue() {
			return value.get() != null;
		}

		public Object getValue() {
			return value.get();
		}
	}

}
