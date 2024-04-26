package com.mawen.search.repository.query.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mawen.search.core.annotation.QueryField;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.repository.query.ParamQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
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

		Criteria current;
		if (annotation.type() == QueryField.Type.NESTED) { // handle nested
			current = new Criteria(annotation.value()[0]);

			if (value != null && value.getClass().isArray()) {
				Object[] values = (Object[]) value;
				List<ParamQuery> paramQueries = Arrays.asList(values).stream().filter(Objects::nonNull).map(ParamQuery::new).collect(Collectors.toList());
				for (ParamQuery query : paramQueries) {
					for (ParamQuery.ParamQueryField field : query) {
						current.nested(from(field.getValue(), field.getQueryField()));
					}
				}
			} else {
				ParamQuery paramQuery = new ParamQuery(value);
				for (ParamQuery.ParamQueryField field : paramQuery) {
					current.nested(from(field.getValue(), field.getQueryField()));
				}
			}
		}
		else { // handle non-nested
			current = new Criteria();
			Criteria.Operator relation = annotation.relation();
			if (relation == Criteria.Operator.OR) {
				Criteria temp = new Criteria();
				for (String fieldName : annotation.value()) {

					temp = temp.or(fieldName);

					annotation.type().from(temp, value);
				}
				current.subCriteria(temp);
			}
			else {
				for (String fieldName : annotation.value()) {

					current = current.and(fieldName);

					annotation.type().from(current, value);
				}
			}
		}

		return current;
	}

}
