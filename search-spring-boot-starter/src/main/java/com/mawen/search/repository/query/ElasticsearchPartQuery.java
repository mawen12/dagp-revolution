package com.mawen.search.repository.query;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.repository.query.parser.ElasticsearchQueryCreator;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchPartQuery extends AbstractElasticsearchRepositoryQuery{

	private final PartTree tree;
	private final MappingContext<?, ElasticsearchPersistentProperty> mappingContext;

	public ElasticsearchPartQuery(ElasticsearchQueryMethod method, ElasticsearchOperations elasticsearchOperations) {
		super(method, elasticsearchOperations);
		this.tree = new PartTree(queryMethod.getName(), queryMethod.getResultProcessor().getReturnedType().getDomainType());
		this.mappingContext = elasticsearchConverter.getMappingContext();
	}

	@Override
	public boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isDeleteQuery() {
		return tree.isDelete();
	}

	@Override
	protected boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor accessor) {

		BaseQuery query = new ElasticsearchQueryCreator(tree, accessor, mappingContext).createQuery();

		if (tree.getMaxResults() != null) {
			query.setMaxResults(tree.getMaxResults());
		}

		return query;
	}
}
