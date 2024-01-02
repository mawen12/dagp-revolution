package com.mawen.search.repository.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.mawen.search.core.annotation.QueryField;
import lombok.Getter;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class ParamQuery implements Streamable<ParamQuery.ParamQueryField> {

	@Nullable
	private final Field[] fields;
	@Nullable
	private final List<ParamQueryField> paramQueryFields;
	@Nullable
	private final List<ParamQueryField> effectiveFields;
	@Nullable
	private ParamQueryField sortField;

	public ParamQuery(Object parameter) {

		Assert.notNull(parameter, "ParamQuery parameter must not be null");

		this.fields = parameter.getClass().getDeclaredFields();
		this.paramQueryFields = new ArrayList<>(fields.length);
		this.effectiveFields = new ArrayList<>(fields.length);

		for (Field field : fields) {

			ParamQueryField paramQueryField = new ParamQueryField(field, parameter);
			this.paramQueryFields.add(paramQueryField);

			if (paramQueryField.hasQueryField() && paramQueryField.hasValue()) {
				this.effectiveFields.add(paramQueryField);
			}

			if (paramQueryField.isSort()) {
				this.sortField = paramQueryField;
			}
		}
	}

	@Override
	public Iterator<ParamQueryField> iterator() {
		return Optional.ofNullable(effectiveFields).map(List::iterator).orElseGet(Collections::emptyIterator);
	}

	public Sort getSort() {
		return this.sortField != null ? (Sort) sortField.getValue() : null;
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
			return getValue() != null;
		}

		public Object getValue() {
			return value.orElse(null);
		}

		public Class<?> getType() {
			return field.getType();
		}

		public boolean isSort() {
			return Sort.class.equals(field.getType());
		}
	}

}
