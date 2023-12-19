package com.mawen.search.repository.query.parser;

import java.util.Collection;
import java.util.Iterator;

import com.mawen.search.InvalidApiUsageException;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.query.CriteriaQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchQueryCreator extends AbstractQueryCreator<CriteriaQuery, CriteriaQuery> {

	private final MappingContext<?, ElasticsearchPersistentProperty> context;

	public ElasticsearchQueryCreator(PartTree tree, ParameterAccessor parameters,
	                                 MappingContext<?, ElasticsearchPersistentProperty> context) {
		super(tree, parameters);
		this.context = context;
	}

	public ElasticsearchQueryCreator(PartTree tree, MappingContext<?, ElasticsearchPersistentProperty> context) {
		super(tree);
		this.context = context;
	}

	@Override
	protected CriteriaQuery create(Part part, Iterator<Object> iterator) {
		PersistentPropertyPath<ElasticsearchPersistentProperty> path = context
				.getPersistentPropertyPath(part.getProperty());
		return new CriteriaQuery(from(part,
				new Criteria(path.toDotPath(ElasticsearchPersistentProperty.QueryPropertyToFieldNameConverter.INSTANCE)),
				iterator));
	}

	@Override
	protected CriteriaQuery and(Part part, CriteriaQuery base, Iterator<Object> iterator) {
		if (base == null) {
			return create(part, iterator);
		}
		PersistentPropertyPath<ElasticsearchPersistentProperty> path = context
				.getPersistentPropertyPath(part.getProperty());
		return base.addCriteria(from(part,
				new Criteria(path.toDotPath(ElasticsearchPersistentProperty.QueryPropertyToFieldNameConverter.INSTANCE)),
				iterator));
	}

	@Override
	protected CriteriaQuery or(CriteriaQuery base, CriteriaQuery query) {
		return new CriteriaQuery(base.getCriteria().or(query.getCriteria()));
	}

	@Override
	protected CriteriaQuery complete(@Nullable CriteriaQuery query, Sort sort) {

		if (query == null) {
			// this is the case in a findAllByOrderByField method, add empty criteria
			query = new CriteriaQuery(new Criteria());
		}
		return query.addSort(sort);
	}

	private Criteria from(Part part, Criteria criteria, Iterator<?> parameters) {

		Part.Type type = part.getType();

		switch (type) {
			case TRUE:
				return criteria.is(true);
			case FALSE:
				return criteria.is(false);
			case NEGATING_SIMPLE_PROPERTY:
				return criteria.is(parameters.next()).not();
			case REGEX:
				return criteria.expression(parameters.next().toString());
			case LIKE:
			case STARTING_WITH:
				return criteria.startsWith(parameters.next().toString());
			case ENDING_WITH:
				return criteria.endsWith(parameters.next().toString());
			case CONTAINING:
				return criteria.contains(parameters.next().toString());
			case GREATER_THAN:
				return criteria.greaterThan(parameters.next());
			case AFTER:
			case GREATER_THAN_EQUAL:
				return criteria.greaterThanEqual(parameters.next());
			case LESS_THAN:
				return criteria.lessThan(parameters.next());
			case BEFORE:
			case LESS_THAN_EQUAL:
				return criteria.lessThanEqual(parameters.next());
			case BETWEEN:
				return criteria.between(parameters.next(), parameters.next());
			case IN:
				return criteria.in(asArray(parameters.next()));
			case NOT_IN:
				return criteria.notIn(asArray(parameters.next()));
			case SIMPLE_PROPERTY:
				Object firstParameter = parameters.next();
				if (firstParameter != null) {
					return criteria.is(firstParameter);
				}
				else {
					// searching for null is a must_not (exists)
					return criteria.exists().not();
				}

			case EXISTS:
			case IS_NOT_NULL:
				return criteria.exists();
			case IS_NULL:
				return criteria.not().exists();
			case IS_EMPTY:
				return criteria.empty();
			case IS_NOT_EMPTY:
				return criteria.notEmpty();
			default:
				throw new InvalidApiUsageException("Illegal criteria found '" + type + "'.");
		}
	}

	private Object[] asArray(Object o) {
		if (o instanceof Collection) {
			return ((Collection<?>) o).toArray();
		}
		else if (o.getClass().isArray()) {
			return (Object[]) o;
		}
		return new Object[] {o};
	}
}