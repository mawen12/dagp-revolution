package com.mawen.search.repository.query;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.repository.query.parser.ElasticsearchParamQueryCreator;
import com.mawen.search.repository.query.parser.ParamSubject;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * 通过解析带有 {@link com.mawen.search.core.annotation.ParamQuery} 注解的参数值和方法名，构造 {@link CriteriaQuery} 来进行查询的 {@link RepositoryQuery} 实现
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ElasticsearchParamQuery extends AbstractElasticsearchRepositoryQuery {

	private final ParamSubject paramSubject;

	/**
	 * 使用给定的 {@link ElasticsearchQueryMethod} 和 {@link ElasticsearchOperations} 创建一个 {@link ElasticsearchParamQuery}
	 *
	 * @param method 不能为空
	 * @param elasticsearchOperations 不能为空
	 */
	public ElasticsearchParamQuery(ElasticsearchQueryMethod method, ElasticsearchOperations elasticsearchOperations) {

		super(method, elasticsearchOperations);

		String methodName = method.getName();
		this.paramSubject = new ParamSubject(methodName);
	}

	@Override
	public boolean isCountQuery() {
		return paramSubject.isCount();
	}

	@Override
	protected boolean isDeleteQuery() {
		return paramSubject.isDelete();
	}

	@Override
	protected boolean isExistsQuery() {
		return paramSubject.isExists();
	}

	@Override
	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor accessor) {

		ParamQuery paramQuery = new ParamQuery(accessor.getParamQuery());
		CriteriaQuery query = new ElasticsearchParamQueryCreator(paramQuery).createQuery();

		Sort sort = paramQuery.getSort();
		if (sort != null) {
			query.setSort(sort);
		}

		SourceFilter sourceFilter = this.queryMethod.getSourceFilter(accessor, this.elasticsearchConverter);
		if (sourceFilter != null) {
			query.addSourceFilter(sourceFilter);
		}

		return query;
	}
}
