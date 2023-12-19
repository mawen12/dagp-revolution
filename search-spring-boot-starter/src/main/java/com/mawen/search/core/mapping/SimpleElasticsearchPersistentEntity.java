package com.mawen.search.core.mapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Routing;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class SimpleElasticsearchPersistentEntity<T>
		extends BasicPersistentEntity<T, ElasticsearchPersistentProperty>
		implements ElasticsearchPersistentEntity<T> {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Nullable
	private final Document document;
	private final Map<String, ElasticsearchPersistentProperty> fieldNamePropertyCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Expression> indexNameExpressions = new ConcurrentHashMap<>();
	@Nullable
	private String indexName;
	private final Lazy<EvaluationContext> indexNameEvaluationContext = Lazy.of(this::getIndexNameEvaluationContext);
	@Nullable
	private ElasticsearchPersistentProperty seqNoPrimaryTermProperty;
	@Nullable
	private ElasticsearchPersistentProperty indexNameProperty;
	@Nullable
	private String routing;

	public SimpleElasticsearchPersistentEntity(TypeInformation<T> typeInformation) {

		super(typeInformation);

		Class<T> clazz = typeInformation.getType();

		document = AnnotatedElementUtils.findMergedAnnotation(clazz, Document.class);
		if (document != null) {
			this.indexName = document.indexName();
		}

		Routing routingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Routing.class);
		if (routingAnnotation != null) {
			Assert.hasText(routingAnnotation.value(), "@Routing annotation must contain a non-empty value");
			this.routing = routingAnnotation.value();
		}
	}

	private String getIndexName() {
		return indexName != null ? indexName : getTypeInformation().getType().getSimpleName();
	}

	@Override
	public IndexCoordinates getIndexCoordinates() {
		return resolve(IndexCoordinates.of(getIndexName()));
	}

	@Override
	public void addPersistentProperty(ElasticsearchPersistentProperty property) {
		super.addPersistentProperty(property);

		if (property.isSeqNoPrimaryTermProperty()) {

			ElasticsearchPersistentProperty seqNoPrimaryTermProperty = this.seqNoPrimaryTermProperty;

			if (seqNoPrimaryTermProperty != null) {
				throw new MappingException(String.format(
						"Attempt to add SeqNoPrimaryTerm property %s but already have property %s registered "
								+ "as SeqNoPrimaryTerm property. Check your entity configuration!",
						property.getField(), seqNoPrimaryTermProperty.getField()));
			}

			this.seqNoPrimaryTermProperty = property;

			if (hasVersionProperty()) {
				warnAboutBothSeqNoPrimaryTermAndVersionProperties();
			}
		}

		if (property.isVersionProperty() && (hasSeqNoPrimaryTermProperty())) {
			warnAboutBothSeqNoPrimaryTermAndVersionProperties();
		}

		if (property.isIndexNameProperty()) {

			if (!property.getActualType().isAssignableFrom(String.class)) {
				throw new MappingException("@IndexName annotation must be put on String property");
			}

			if (indexNameProperty != null) {
				throw new MappingException("@IndexName annotation can only be put on one property in an entity");
			}

			this.indexNameProperty = property;
		}
	}

	private void warnAboutBothSeqNoPrimaryTermAndVersionProperties() {
		log.warn(String.format(
				"Both SeqNoPrimaryTerm and @Version properties are defined on %s. Version will not be sent in index requests when seq_no is sent!",
				getType()));
	}

	@Nullable
	@Override
	public ElasticsearchPersistentProperty getPersistentPropertyWithFieldName(String fieldName) {

		Assert.notNull(fieldName, "fieldName must not be null");

		return fieldNamePropertyCache.computeIfAbsent(fieldName, key -> {
			AtomicReference<ElasticsearchPersistentProperty> propertyRef = new AtomicReference<>();
			doWithProperties((PropertyHandler<ElasticsearchPersistentProperty>) property -> {
				if (key.equals(property.getFieldName())) {
					propertyRef.set(property);
				}
			});

			return propertyRef.get();
		});
	}

	@Override
	public boolean hasSeqNoPrimaryTermProperty() {
		return seqNoPrimaryTermProperty != null;
	}

	@Override
	@Nullable
	public ElasticsearchPersistentProperty getSeqNoPrimaryTermProperty() {
		return seqNoPrimaryTermProperty;
	}

	@Nullable
	@Override
	public ElasticsearchPersistentProperty getIndexNameProperty() {
		return indexNameProperty;
	}

	@Nullable
	@Override
	public String resolveRouting(T bean) {
		if (routing == null) {
			return null;
		}

		ElasticsearchPersistentProperty persistentProperty = getPersistentProperty(routing);

		if (persistentProperty != null) {
			Object propertyValue = getPropertyAccessor(bean).getProperty(persistentProperty);

			return propertyValue != null ? propertyValue.toString() : null;
		}

		throw new MappingException("Could not resolve expression: " + routing + " for object of class " + bean.getClass().getCanonicalName());
	}

	// region SpEL handling
	private IndexCoordinates resolve(IndexCoordinates indexCoordinates) {

		String[] indexNames = indexCoordinates.getIndexNames();
		String[] resolvedNames = new String[indexNames.length];

		for (int i = 0; i < indexNames.length; i++) {
			resolvedNames[i] = resolve(indexNames[i]);
		}

		return IndexCoordinates.of(resolvedNames);
	}

	private String resolve(String name) {

		Assert.notNull(name, "name must not be null");

		Expression expression = getExpressionForIndexName(name);

		String resolvedName = expression != null ? expression.getValue(indexNameEvaluationContext.get(), String.class)
				: null;
		return resolvedName != null ? resolvedName : name;
	}

	@Nullable
	private Expression getExpressionForIndexName(String name) {
		return indexNameExpressions.computeIfAbsent(name, s -> {
			Expression expr = PARSER.parseExpression(s, ParserContext.TEMPLATE_EXPRESSION);
			return expr instanceof LiteralExpression ? null : expr;
		});
	}


	private EvaluationContext getIndexNameEvaluationContext() {

		Expression expression = getExpressionForIndexName(getIndexName());
		ExpressionDependencies expressionDependencies = expression != null ? ExpressionDependencies.discover(expression)
				: ExpressionDependencies.none();

		// noinspection ConstantConditions
		return getEvaluationContext(null, expressionDependencies);
	}

	// endregion

}
