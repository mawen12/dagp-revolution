package com.mawen.search.repository.query.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mawen.search.core.annotation.QueryField;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.query.CriteriaQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/21
 */
public class ElasticsearchParamQueryCreator extends AbstractParamQueryCreator<CriteriaQuery, CriteriaQuery> {

	public ElasticsearchParamQueryCreator(Object paramQuery, ParameterAccessor parameterAccessor) {
		super(paramQuery, Optional.of(parameterAccessor));
	}

	@Override
	protected CriteriaQuery create(Object value, QueryField annotation) {
		return new CriteriaQuery(from(value, annotation));
	}

	@Override
	protected CriteriaQuery and(CriteriaQuery base, CriteriaQuery criteria) {
		return new CriteriaQuery(base.getCriteria().and(criteria.getCriteria()));
	}

	@Override
	protected CriteriaQuery or(CriteriaQuery base, CriteriaQuery criteria) {
		return new CriteriaQuery(base.getCriteria().or(criteria.getCriteria()));
	}

	@Override
	protected CriteriaQuery complete(@Nullable CriteriaQuery criteria, Sort sort) {

		if (criteria == null) {
			criteria = new CriteriaQuery(new Criteria());
		}
		return criteria.addSort(sort);
	}

	private Criteria from(Object value, QueryField annotation) {

		Criteria current = null;
		for (String field : annotation.value()) {
			if (current == null) {
				current = new Criteria(field);
				annotation.type().from(current, value);
			}
			else {
				Criteria newCriteria = new Criteria(field);
				annotation.type().from(newCriteria, value);
				current.and(newCriteria);

				current = newCriteria;
			}
		}

		return current;
	}
}
