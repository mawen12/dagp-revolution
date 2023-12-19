package com.mawen.search.core.query.builder;

import com.mawen.search.core.query.Criteria;
import com.mawen.search.core.query.CriteriaQuery;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class CriteriaQueryBuilder extends BaseQueryBuilder<CriteriaQuery, CriteriaQueryBuilder> {

	private final Criteria criteria;

	public CriteriaQueryBuilder(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		this.criteria = criteria;
	}

	public Criteria getCriteria() {
		return criteria;
	}

	@Override
	public CriteriaQuery build() {
		return new CriteriaQuery(this);
	}

}
