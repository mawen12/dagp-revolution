package com.mawen.search.core.query;

import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.query.builder.CriteriaQueryBuilder;
import lombok.Getter;

import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * 标准查询，使用指定字段构建的查询
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class CriteriaQuery extends BaseQuery {

	private final Criteria criteria;

	public CriteriaQuery(CriteriaQueryBuilder builder) {
		super(builder);
		this.criteria = builder.getCriteria();
	}

	public CriteriaQuery(Criteria criteria) {
		this(criteria, Pageable.unpaged());
	}

	public CriteriaQuery(Criteria criteria, Pageable pageable) {

		Assert.notNull(criteria, "Criteria must not be null!");
		Assert.notNull(pageable, "Pageable must not be null!");

		this.criteria = criteria;
		this.pageable = pageable;
		this.addSort(pageable.getSort());
	}

	public static CriteriaQueryBuilder builder(Criteria criteria) {
		return new CriteriaQueryBuilder(criteria);
	}

	public static Query fromQuery(CriteriaQuery source) {
		return fromQuery(source, new CriteriaQuery(source.criteria));
	}

	public static <T extends CriteriaQuery> T fromQuery(CriteriaQuery source, T destination) {

		Assert.notNull(source, "source must not be null");
		Assert.notNull(destination, "destination must not be null");

		destination.addCriteria(source.getCriteria());

		if (source.getSort() != null) {
			destination.addSort(source.getSort());
		}

		return destination;
	}

	public final <T extends CriteriaQuery> T addCriteria(Criteria criteria) {

		Assert.notNull(criteria, "Cannot add null criteria.");

		this.criteria.and(criteria);
		return (T) this;
	}

}
