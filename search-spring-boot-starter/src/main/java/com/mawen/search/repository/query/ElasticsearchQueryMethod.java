package com.mawen.search.repository.query;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.mawen.search.InvalidApiUsageException;
import com.mawen.search.core.annotation.Highlight;
import com.mawen.search.core.annotation.ParamQuery;
import com.mawen.search.core.annotation.Query;
import com.mawen.search.core.annotation.SourceFilters;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SearchPage;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.HighlightQuery;
import com.mawen.search.core.query.builder.FetchSourceFilterBuilder;
import com.mawen.search.repository.support.StringQueryUtil;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ElasticsearchQueryMethod extends QueryMethod {

	protected final Method method;
	protected final Class<?> unwrappedReturnType;
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	@Nullable
	private final Query queryAnnotation;
	@Nullable
	private final ParamQuery paramQueryAnnotation;
	@Nullable
	private final Highlight highlightAnnotation;
	private final Lazy<HighlightQuery> highlightQueryLazy = Lazy.of(this::createAnnotatedHighlightQuery);
	@Nullable
	private final SourceFilters sourceFilters;
	@Nullable
	private Boolean unwrappedReturnTypeFromSearchHit;
	@Nullable
	private ElasticsearchEntityMetadata<?> metadata;

	public ElasticsearchQueryMethod(Method method, RepositoryMetadata repositoryMetadata, ProjectionFactory factory,
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {

		super(method, repositoryMetadata, factory);

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.method = method;
		this.mappingContext = mappingContext;
		this.queryAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
		this.paramQueryAnnotation = findParamQueryAnnotation(method);
		this.highlightAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Highlight.class);
		this.sourceFilters = AnnotatedElementUtils.findMergedAnnotation(method, SourceFilters.class);
		this.unwrappedReturnType = potentiallyUnwrapReturnTypeFor(repositoryMetadata, method);

		verifyCountQueryTypes();
	}

	@Override
	protected Parameters<?, ?> createParameters(Method method) {
		return new ElasticsearchParameters(method);
	}

	protected void verifyCountQueryTypes() {

		if (hasCountQueryAnnotation()) {
			TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);

			if (returnType.getType() != long.class && !Long.class.isAssignableFrom(returnType.getType())) {
				throw new InvalidApiUsageException("count query methods must return a Long");
			}
		}
	}

	public boolean hasAnnotatedQuery() {
		return this.queryAnnotation != null;
	}

	@Nullable
	public String getAnnotatedQuery() {
		return queryAnnotation != null ? queryAnnotation.value() : null;
	}

	public boolean hasAnnotatedParamQuery() {
		return this.paramQueryAnnotation != null;
	}

	public boolean hasAnnotatedHighlight() {
		return highlightAnnotation != null;
	}

	public HighlightQuery getAnnotatedHighlightQuery() {

		Assert.isTrue(hasAnnotatedHighlight(), "no Highlight annotation present on " + getName());

		return highlightQueryLazy.get();
	}

	private HighlightQuery createAnnotatedHighlightQuery() {

		Assert.notNull(highlightAnnotation, "highlightAnnotation must not be null");

		return new HighlightQuery(
				com.mawen.search.core.query.highlight.Highlight.of(highlightAnnotation),
				getDomainClass());
	}

	private ParamQuery findParamQueryAnnotation(Method method) {

		for (Parameter parameter : method.getParameters()) {
			ParamQuery annotation = AnnotatedElementUtils.findMergedAnnotation(parameter, ParamQuery.class);
			if (annotation != null) {
				return annotation;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ElasticsearchEntityMetadata<?> getEntityInformation() {

		if (metadata == null) {

			Class<?> returnedObjectType = getReturnedObjectType();
			Class<?> domainClass = getDomainClass();

			if (ClassUtils.isPrimitiveOrWrapper(returnedObjectType)) {

				this.metadata = new SimpleElasticsearchEntityMetadata<>((Class<Object>) domainClass,
						mappingContext.getRequiredPersistentEntity(domainClass));

			}
			else {

				ElasticsearchPersistentEntity<?> returnedEntity = mappingContext.getPersistentEntity(returnedObjectType);
				ElasticsearchPersistentEntity<?> managedEntity = mappingContext.getRequiredPersistentEntity(domainClass);
				returnedEntity = returnedEntity == null || returnedEntity.getType().isInterface() ? managedEntity
						: returnedEntity;
				ElasticsearchPersistentEntity<?> collectionEntity = domainClass.isAssignableFrom(returnedObjectType)
						? returnedEntity
						: managedEntity;

				this.metadata = new SimpleElasticsearchEntityMetadata<>((Class<Object>) returnedEntity.getType(),
						collectionEntity);
			}
		}

		return this.metadata;
	}

	protected MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	public boolean isSearchHitMethod() {

		if (unwrappedReturnTypeFromSearchHit != null && unwrappedReturnTypeFromSearchHit) {
			return true;
		}

		Class<?> methodReturnType = method.getReturnType();

		if (SearchHits.class.isAssignableFrom(methodReturnType)) {
			return true;
		}

		try {
			// dealing with Collection<SearchHit<T>>, getting to T
			ParameterizedType methodGenericReturnType = ((ParameterizedType) method.getGenericReturnType());
			if (isAllowedGenericType(methodGenericReturnType)) {
				ParameterizedType collectionTypeArgument = (ParameterizedType) methodGenericReturnType
						.getActualTypeArguments()[0];
				if (SearchHit.class.isAssignableFrom((Class<?>) collectionTypeArgument.getRawType())) {
					return true;
				}
			}
		}
		catch (Exception ignored) {}

		return false;
	}

	public boolean isSearchPageMethod() {
		return SearchPage.class.isAssignableFrom(methodReturnType());
	}

	public Class<?> methodReturnType() {
		return method.getReturnType();
	}

	protected boolean isAllowedGenericType(ParameterizedType methodGenericReturnType) {
		return Collection.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType())
				|| Stream.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType());
	}

	public boolean isNotSearchHitMethod() {
		return !isSearchHitMethod();
	}

	public boolean isNotSearchPageMethod() {
		return !isSearchPageMethod();
	}

	public boolean hasCountQueryAnnotation() {
		return queryAnnotation != null && queryAnnotation.count();
	}

	@Nullable
	SourceFilter getSourceFilter(ParameterAccessor parameterAccessor, ElasticsearchConverter converter) {

		if (sourceFilters == null || (sourceFilters.includes().length == 0 && sourceFilters.excludes().length == 0)) {
			return null;
		}

		StringQueryUtil stringQueryUtil = new StringQueryUtil(converter.getConversionService());
		FetchSourceFilterBuilder fetchSourceFilterBuilder = new FetchSourceFilterBuilder();

		if (sourceFilters.includes().length > 0) {
			fetchSourceFilterBuilder
					.withIncludes(mapParameters(sourceFilters.includes(), parameterAccessor, stringQueryUtil));
		}

		if (sourceFilters.excludes().length > 0) {
			fetchSourceFilterBuilder
					.withExcludes(mapParameters(sourceFilters.excludes(), parameterAccessor, stringQueryUtil));
		}

		return fetchSourceFilterBuilder.build();
	}

	private String[] mapParameters(String[] source, ParameterAccessor parameterAccessor,
			StringQueryUtil stringQueryUtil) {

		List<String> fieldNames = new ArrayList<>();

		for (String s : source) {

			if (StringUtils.hasText(s)) {
				String fieldName = s;
				if (hasAnnotatedQuery()) {
					 fieldName = stringQueryUtil.replacePlaceholders(s, parameterAccessor);
				}
				// this could be "[\"foo\",\"bar\"]", must be split
				if (fieldName.startsWith("[") && fieldName.endsWith("]")) {
					// noinspection RegExpRedundantEscape
					fieldNames.addAll( //
							Arrays.asList(fieldName.substring(1, fieldName.length() - 2) //
									.replaceAll("\\\"", "") //
									.split(","))); //
				}
				else {
					fieldNames.add(fieldName);
				}
			}
		}

		return fieldNames.toArray(new String[0]);
	}

	// region Copied from QueryMethod base class
	/*
	 * Copied from the QueryMethod class adding support for collections of SearchHit instances. No static method here.
	 */
	private Class<? extends Object> potentiallyUnwrapReturnTypeFor(RepositoryMetadata metadata, Method method) {
		TypeInformation<?> returnType = metadata.getReturnType(method);
		if (!QueryExecutionConverters.supports(returnType.getType())
				&& !ReactiveWrapperConverters.supports(returnType.getType())) {
			return returnType.getType();
		}
		else {
			TypeInformation<?> componentType = returnType.getComponentType();
			if (componentType == null) {
				throw new IllegalStateException(
						String.format("Couldn't find component type for return value of method %s", method));
			}
			else {

				if (SearchHit.class.isAssignableFrom(componentType.getType())) {
					unwrappedReturnTypeFromSearchHit = true;
					return componentType.getComponentType().getType();
				}
				else {
					return componentType.getType();
				}
			}
		}
	}

	void addMethodParameter(BaseQuery query, ElasticsearchParametersParameterAccessor parameterAccessor,
			ElasticsearchConverter elasticsearchConverter) {

		if (hasAnnotatedHighlight()) {
			query.setHighlightQuery(getAnnotatedHighlightQuery());
		}

		SourceFilter sourceFilter = getSourceFilter(parameterAccessor, elasticsearchConverter);
		if (sourceFilter != null) {
			query.addSourceFilter(sourceFilter);
		}
	}
	// endregion
}
