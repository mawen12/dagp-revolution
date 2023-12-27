package com.mawen.search.repository.query.parser;

import com.mawen.search.core.annotation.QueryField;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.repository.query.ParamQuery;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 *
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public abstract class AbstractParamQueryCreator<T, S> {

	protected final ParamQuery paramQuery;

	protected AbstractParamQueryCreator(ParamQuery paramQuery) {

		Assert.notNull(paramQuery, "ParamQuery must not be null");

		this.paramQuery = paramQuery;
	}

	public T createQuery() {
		return complete(createCriteria(paramQuery), null);
	}

	private S createCriteria(ParamQuery paramQuery) {

		S base = null;

		for (ParamQuery.ParamQueryField field : paramQuery) {

			S criteria = create(field.getValue(), field.getQueryField());

			base = base == null
					? criteria
					: (field.getQueryField().relation() == Criteria.Operator.AND
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
