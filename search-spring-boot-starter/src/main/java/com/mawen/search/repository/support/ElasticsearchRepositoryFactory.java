package com.mawen.search.repository.support;

import java.lang.reflect.Method;
import java.util.Optional;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.repository.query.ElasticsearchParamQuery;
import com.mawen.search.repository.query.ElasticsearchPartQuery;
import com.mawen.search.repository.query.ElasticsearchQueryMethod;
import com.mawen.search.repository.query.ElasticsearchStringQuery;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.springframework.data.querydsl.QuerydslUtils.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ElasticsearchRepositoryFactory extends RepositoryFactorySupport {

	private final ElasticsearchOperations elasticsearchOperations;
	private final ElasticsearchEntityInformationCreator entityInformationCreator;

	public ElasticsearchRepositoryFactory(ElasticsearchOperations elasticsearchOperations) {

		Assert.notNull(elasticsearchOperations, "ElasticsearchOperations must not be null!");

		this.elasticsearchOperations = elasticsearchOperations;
		this.entityInformationCreator = new ElasticsearchEntityInformationCreatorImpl(
				elasticsearchOperations.getElasticsearchConverter().getMappingContext());
	}

	private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
		return QUERY_DSL_PRESENT && QuerydslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
	}

	@Override
	public <T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return entityInformationCreator.getEntityInformation(domainClass);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, getEntityInformation(metadata.getDomainType()),
				elasticsearchOperations);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		if (isQueryDslRepository(metadata.getRepositoryInterface())) {
			throw new IllegalArgumentException("QueryDsl Support has not been implemented yet.");
		}

		return SimpleElasticsearchRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(new ElasticsearchQueryLookupStrategy());
	}

	@Override
	protected RepositoryMetadata getRepositoryMetadata(Class<?> repositoryInterface) {
		return new ElasticsearchRepositoryMetadata(repositoryInterface);
	}

	@Override
	protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
		return RepositoryComposition.RepositoryFragments.empty();
	}

	private class ElasticsearchQueryLookupStrategy implements QueryLookupStrategy {

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {

			ElasticsearchQueryMethod queryMethod = new ElasticsearchQueryMethod(method, metadata, factory,
					elasticsearchOperations.getElasticsearchConverter().getMappingContext());
			String namedQueryName = queryMethod.getNamedQueryName();

			if (queryMethod.hasAnnotatedParamQuery()) {
				return new ElasticsearchParamQuery(queryMethod, elasticsearchOperations);
			}
			else if (namedQueries.hasQuery(namedQueryName)) {
				String namedQuery = namedQueries.getQuery(namedQueryName);
				return new ElasticsearchStringQuery(queryMethod, elasticsearchOperations, namedQuery);
			}
			else if (queryMethod.hasAnnotatedQuery()) {
				return new ElasticsearchStringQuery(queryMethod, elasticsearchOperations, queryMethod.getAnnotatedQuery());
			}
			return new ElasticsearchPartQuery(queryMethod, elasticsearchOperations);
		}
	}
}
