package com.mawen.search.repository.query.parser;

import java.lang.reflect.Field;
import java.util.Optional;

import com.mawen.search.core.annotation.QueryField;
import com.mawen.search.core.domain.Criteria;
import org.apache.commons.lang3.ArrayUtils;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/21
 */
public abstract class AbstractParamQueryCreator<T, S> {

	protected final Optional<ParameterAccessor> parameters;
	protected final Object paramQuery;

	protected AbstractParamQueryCreator(Object paramQuery, Optional<ParameterAccessor> parameterAccessor) {

		Assert.notNull(paramQuery, "ParamQuery must not be null");
		Assert.notNull(parameterAccessor, "ParameterAccessor must not be null");

		this.parameters = parameterAccessor;
		this.paramQuery = paramQuery;
	}

	public T createQuery() {
		return complete(createCriteria(), null);
	}

	private S createCriteria() {

		S base = null;

		Field[] fields = paramQuery.getClass().getDeclaredFields();
		for (Field field : fields) {

			if (!field.isAnnotationPresent(QueryField.class)) {
				continue;
			}

			QueryField annotation = field.getAnnotation(QueryField.class);
			if (ArrayUtils.isEmpty(annotation.value())) {
				continue;
			}

			Object value = null;

			try {
				field.setAccessible(true);
				value = field.get(paramQuery);
			}
			catch (IllegalAccessException e) {}

			S criteria = create(value, annotation);

			base = base == null
					? criteria
					: (annotation.relation() == Criteria.Operator.AND
						? and(base, criteria)
						: or(base, criteria));
		}

		return base;
	}

	protected abstract S create(Object value, QueryField annotation);

	protected abstract S and(S base, S criteria);

	protected abstract S or(S base, S criteria);

	protected abstract T complete(@Nullable S criteria, Sort sort);
}
