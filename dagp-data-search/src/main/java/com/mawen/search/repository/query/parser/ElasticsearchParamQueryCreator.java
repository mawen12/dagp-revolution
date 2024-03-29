package com.mawen.search.repository.query.parser;

import com.mawen.search.core.annotation.QueryField;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.repository.query.ParamQuery;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * 特定于 Elasticsearch 的查询构造器，支持从 {@link }
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ElasticsearchParamQueryCreator extends AbstractParamQueryCreator<CriteriaQuery, CriteriaQuery> {

	public ElasticsearchParamQueryCreator(ParamQuery paramQuery) {
		super(paramQuery);
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

		Criteria current = new Criteria();
		Criteria.Operator relation = annotation.relation();
		if (relation == Criteria.Operator.OR) {
			Criteria temp = new Criteria();
			for (String field : annotation.value()) {

				temp = temp.or(field);

				annotation.type().from(current, value);
			}
			current.subCriteria(temp);
		}
		else {
			for (String field : annotation.value()) {

				current = current.and(field);

				annotation.type().from(current, value);
			}
		}

		return current;
	}
}
