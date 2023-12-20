package com.mawen.search.core.query.builder;

import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.query.CriteriaQuery;
import lombok.Getter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class CriteriaQueryBuilder extends BaseQueryBuilder<CriteriaQuery, CriteriaQueryBuilder> {

	private final Criteria criteria;

	public CriteriaQueryBuilder(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		this.criteria = criteria;
	}

	@Override
	public CriteriaQuery build() {
		return new CriteriaQuery(this);
	}

}
