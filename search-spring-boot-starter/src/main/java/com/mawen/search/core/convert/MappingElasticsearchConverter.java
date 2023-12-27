package com.mawen.search.core.convert;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.FetchSourceFilter;
import com.mawen.search.core.domain.Field;
import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.mapping.PropertyValueConverter;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.core.query.Query;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.DefaultSpELExpressionEvaluator;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.mapping.model.SpELContext;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;
import org.springframework.data.mapping.model.SpELExpressionParameterValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class MappingElasticsearchConverter implements ElasticsearchConverter, ApplicationContextAware, InitializingBean {
	private static final String INCOMPATIBLE_TYPES = "Cannot convert %1$s of type %2$s into an instance of %3$s! Implement a custom Converter<%2$s, %3$s> and register it with the CustomConversions.";
	private static final String INVALID_TYPE_TO_READ = "Expected to read Document %s into type %s but didn't find a PersistentEntity for the latter!";


	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;
	private final SpELContext spELContext = new SpELContext(new MapAccessor());
	private final EntityInstantiators instantiators = new EntityInstantiators();
	private CustomConversions conversions;


	public MappingElasticsearchConverter(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		this(mappingContext, null);
	}

	public MappingElasticsearchConverter(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext, @Nullable GenericConversionService conversionService) {
		this.conversions = new ElasticsearchCustomConversions(Collections.emptyList());

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.mappingContext = mappingContext;
		this.conversionService = conversionService != null ? conversionService : new DefaultConversionService();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		if (mappingContext instanceof ApplicationContextAware) {
			((ApplicationContextAware) mappingContext).setApplicationContext(applicationContext);
		}
	}

	@Override
	public MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public ConversionService getConversionService() {
		return conversionService;
	}

	public void setConversions(CustomConversions conversions) {
		Assert.notNull(conversions, "CustomConversions must not be null");
		this.conversions = conversions;
	}

	@Override
	public void afterPropertiesSet() {
		DateFormatterRegistrar.addDateConverters(conversionService);
		this.conversions.registerConvertersIn(this.conversionService);
	}

	// region read/write

	@Override
	public <R> R read(Class<R> type, Document source) {

		Reader reader = new Reader(mappingContext, conversionService, conversions, spELContext, instantiators);
		return reader.read(type, source);
	}

	@Override
	public void write(Object source, Document sink) {

		Assert.notNull(source, "source to map must not be null");

		Writer writer = new Writer(mappingContext, conversionService, conversions);
		writer.write(source, sink);
	}

	// endregion

	// region queries
	@Override
	public void updateQuery(Query query, @Nullable Class<?> domainClass) {

		Assert.notNull(query, "query must not be null");

		if (query instanceof BaseQuery) {
			if (((BaseQuery) query).isQueryIsUpdatedByConverter()) {
				return;
			}
		}

		if (domainClass == null) {
			return;
		}

		updatePropertiesInFieldsAndSourceFilter(query, domainClass);

		if (query instanceof CriteriaQuery) {
			updatePropertiesInCriteriaQuery((CriteriaQuery) query, domainClass);
		}

		if (query instanceof BaseQuery) {
			((BaseQuery) query).setQueryIsUpdatedByConverter(true);
		}
	}

	private void updatePropertiesInFieldsAndSourceFilter(Query query, Class<?> domainClass) {

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainClass);

		if (persistentEntity != null) {
			List<String> fields = query.getFields();

			if (!fields.isEmpty()) {
				query.setFields(updateFieldNames(fields, persistentEntity));
			}

			SourceFilter sourceFilter = query.getSourceFilter();

			if (sourceFilter != null) {

				String[] includes = null;
				String[] excludes = null;

				if (sourceFilter.getIncludes() != null) {
					includes = updateFieldNames(Arrays.asList(sourceFilter.getIncludes()), persistentEntity)
							.toArray(new String[]{});
				}

				if (sourceFilter.getExcludes() != null) {
					excludes = updateFieldNames(Arrays.asList(sourceFilter.getExcludes()), persistentEntity)
							.toArray(new String[]{});
				}

				query.addSourceFilter(new FetchSourceFilter(includes, excludes));
			}
		}
	}


	private List<String> updateFieldNames(List<String> fieldNames, ElasticsearchPersistentEntity<?> persistentEntity) {
		return fieldNames.stream().map(fieldName -> updateFieldName(persistentEntity, fieldName))
				.collect(Collectors.toList());
	}

	private String updateFieldName(ElasticsearchPersistentEntity<?> persistentEntity, String fieldName) {
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(fieldName);
		return persistentProperty != null ? persistentProperty.getFieldName() : fieldName;
	}

	@Override
	public String updateFieldNames(String propertyPath, ElasticsearchPersistentEntity<?> persistentEntity) {

		Assert.notNull(propertyPath, "propertyPath must not be null");
		Assert.notNull(persistentEntity, "persistentEntity must not be null");

		String[] properties = propertyPath.split("\\.", 2);

		if (properties.length > 0) {
			String propertyName = properties[0];
			String fieldName = updateFieldName(persistentEntity, propertyName);

			if (properties.length > 1) {
				ElasticsearchPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(propertyName);
				return (persistentProperty != null)
						? fieldName + "." + updateFieldNames(properties[1], mappingContext.getPersistentEntity(persistentProperty))
						: fieldName;
			}
			else {
				return fieldName;
			}
		}
		else {
			return propertyPath;
		}

	}

	private void updatePropertiesInCriteriaQuery(CriteriaQuery criteriaQuery, Class<?> domainClass) {

		Assert.notNull(criteriaQuery, "criteriaQuery must not be null");
		Assert.notNull(domainClass, "domainClass must not be null");

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainClass);

		if (persistentEntity != null) {
			for (Criteria chainedCriteria : criteriaQuery.getCriteria().getCriteriaChain()) {
				updatePropertiesInCriteria(chainedCriteria, persistentEntity);
			}
			for (Criteria subCriteria : criteriaQuery.getCriteria().getSubCriteria()) {
				for (Criteria chainedCriteria : subCriteria.getCriteriaChain()) {
					updatePropertiesInCriteria(chainedCriteria, persistentEntity);
				}
			}
		}
	}

	private void updatePropertiesInCriteria(Criteria criteria, ElasticsearchPersistentEntity<?> persistentEntity) {

		Field field = criteria.getField();

		if (field == null) {
			return;
		}

		String[] fieldNames = field.getName().split("\\.");

		ElasticsearchPersistentEntity<?> currentEntity = persistentEntity;
		ElasticsearchPersistentProperty persistentProperty = null;
		int propertyCount = 0;
		boolean isNested = false;

		for (int i = 0; i < fieldNames.length; i++) {
			persistentProperty = currentEntity.getPersistentProperty(fieldNames[i]);

			if (persistentProperty != null) {
				propertyCount++;
				fieldNames[i] = persistentProperty.getFieldName();

				com.mawen.search.core.annotation.Field fieldAnnotation = persistentProperty
						.findAnnotation(com.mawen.search.core.annotation.Field.class);

				if (fieldAnnotation != null && fieldAnnotation.type() == FieldType.Nested) {
					isNested = true;
				}

				try {
					currentEntity = mappingContext.getPersistentEntity(persistentProperty.getActualType());
				} catch (Exception e) {
					// using system types like UUIDs will lead to java.lang.reflect.InaccessibleObjectException in JDK 16
					// so if we cannot get an entity here, bail out.
					currentEntity = null;
				}
			}

			if (currentEntity == null) {
				break;
			}
		}

		field.setName(String.join(".", fieldNames));

		if (propertyCount > 1 && isNested) {
			List<String> propertyNames = Arrays.asList(fieldNames);
			field.setPath(String.join(".", propertyNames.subList(0, propertyCount - 1)));
		}

		if (persistentProperty != null) {

			if (persistentProperty.hasPropertyValueConverter()) {
				PropertyValueConverter propertyValueConverter = Objects
						.requireNonNull(persistentProperty.getPropertyValueConverter());
				criteria.getQueryCriteriaEntries().forEach(criteriaEntry -> {

					if (criteriaEntry.getKey().hasValue()) {
						Object value = criteriaEntry.getValue();

						if (value.getClass().isArray()) {
							Object[] objects = (Object[]) value;

							for (int i = 0; i < objects.length; i++) {
								objects[i] = propertyValueConverter.write(objects[i]);
							}
						} else {
							criteriaEntry.setValue(propertyValueConverter.write(value));
						}
					}
				});
			}

			com.mawen.search.core.annotation.Field fieldAnnotation = persistentProperty
					.findAnnotation(com.mawen.search.core.annotation.Field.class);

			if (fieldAnnotation != null) {
				field.setFieldType(fieldAnnotation.type());
			}
		}
	}

	// endregion

	static class MapValueAccessor {

		final Map<String, Object> target;

		MapValueAccessor(Map<String, Object> target) {
			this.target = target;
		}

		@Nullable
		public Object get(ElasticsearchPersistentProperty property) {

			String fieldName = property.getFieldName();

			if (target instanceof Document) {
				Document document = (Document) target;

				if (property.isIdProperty() && document.hasId()) {
					Object id = null;

					if (!fieldName.contains(".")) {
						id = target.get(fieldName);
					}
					return id != null ? id : document.getId();
				}

				if (property.isVersionProperty() && document.hasVersion()) {
					return document.getVersion();
				}

			}

			if (property.hasExplicitFieldName() || !fieldName.contains(".")) {
				return target.get(fieldName);
			}

			Iterator<String> parts = Arrays.asList(fieldName.split("\\.")).iterator();
			Map<String, Object> source = target;
			Object result = null;

			while (parts.hasNext()) {

				result = source.get(parts.next());

				if (parts.hasNext()) {
					source = getAsMap(result);
				}
			}

			return result;
		}

		public void set(ElasticsearchPersistentProperty property, @Nullable Object value) {

			if (value != null) {

				if (property.isIdProperty()) {
					((Document) target).setId(value.toString());
				}

				if (property.isVersionProperty()) {
					((Document) target).setVersion((Long) value);
				}
			}

			target.put(property.getFieldName(), value);
		}

		private Map<String, Object> getAsMap(Object result) {

			if (result instanceof Map) {
				// noinspection unchecked
				return (Map<String, Object>) result;
			}

			throw new IllegalArgumentException(String.format("%s is not a Map.", result));
		}
	}


	@Slf4j
	private static class Reader extends Base {

		private final SpELContext spELContext;
		private final EntityInstantiators instantiators;

		public Reader(
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
				GenericConversionService conversionService, CustomConversions conversions,
				SpELContext spELContext, EntityInstantiators instantiators) {

			super(mappingContext, conversionService, conversions);
			this.spELContext = spELContext;
			this.instantiators = instantiators;
		}

		@SuppressWarnings("unchecked")
		private static <T> T getCollectionWithSingleElement(TypeInformation<?> collectionType,
				TypeInformation<?> componentType, Object element) {
			Collection<Object> collection = CollectionFactory.createCollection(collectionType.getType(),
					componentType.getType(), 1);
			collection.add(element);
			return (T) collection;
		}

		@SuppressWarnings("unchecked")
		/**
		 * Reads the given source into the given type.
		 *
		 * @param type they type to convert the given source to.
		 * @param source the source to create an object of the given type from.
		 * @return the object that was read
		 */
		<R> R read(Class<R> type, Document source) {

			TypeInformation<R> typeInformation = ClassTypeInformation.from((Class<R>) ClassUtils.getUserClass(type));
			R r = read(typeInformation, source);

			if (r == null) {
				throw new MappingException("could not convert into object of class " + type);
			}

			return r;
		}

		@Nullable
		@SuppressWarnings("unchecked")
		private <R> R read(TypeInformation<R> typeInformation, Map<String, Object> source) {

			Assert.notNull(source, "Source must not be null!");

			Class<? extends R> rawType = typeInformation.getType();

			if (conversions.hasCustomReadTarget(source.getClass(), rawType)) {
				return conversionService.convert(source, rawType);
			}

			if (Document.class.isAssignableFrom(rawType)) {
				return (R) source;
			}

			if (typeInformation.isMap()) {
				return readMap(typeInformation, source);
			}

			if (typeInformation.equals(ClassTypeInformation.OBJECT)) {
				return (R) source;
			}

			// Retrieve persistent entity info
			ElasticsearchPersistentEntity<?> entity = mappingContext.getPersistentEntity(typeInformation);

			if (entity == null) {
				throw new MappingException(String.format(INVALID_TYPE_TO_READ, source, typeInformation.getType()));
			}

			return readEntity(entity, source);
		}

		@SuppressWarnings("unchecked")
		private <R> R readMap(TypeInformation<?> type, Map<String, Object> source) {

			Assert.notNull(source, "Document must not be null!");

			Class<?> mapType = type.getType();

			TypeInformation<?> keyType = type.getComponentType();
			TypeInformation<?> valueType = type.getMapValueType();

			Class<?> rawKeyType = keyType != null ? keyType.getType() : null;
			Class<?> rawValueType = valueType != null ? valueType.getType() : null;

			Map<Object, Object> map = CollectionFactory.createMap(mapType, rawKeyType, source.keySet().size());

			for (Map.Entry<String, Object> entry : source.entrySet()) {

				Object key = entry.getKey();

				if (rawKeyType != null && !rawKeyType.isAssignableFrom(key.getClass())) {
					key = conversionService.convert(key, rawKeyType);
				}

				Object value = entry.getValue();
				TypeInformation<?> defaultedValueType = valueType != null ? valueType : ClassTypeInformation.OBJECT;

				if (value instanceof Map) {
					map.put(key, read(defaultedValueType, (Map<String, Object>) value));
				}
				else if (value instanceof List) {
					map.put(key,
							readCollectionOrArray(valueType != null ? valueType : ClassTypeInformation.LIST, (List<Object>) value));
				}
				else {
					map.put(key, getPotentiallyConvertedSimpleRead(value, rawValueType));
				}
			}

			return (R) map;
		}

		private <R> R readEntity(ElasticsearchPersistentEntity<?> entity, Map<String, Object> source) {

			ElasticsearchPersistentEntity<?> targetEntity = computeClosestEntity(entity, source);

			SpELExpressionEvaluator evaluator = new DefaultSpELExpressionEvaluator(source, spELContext);
			MapValueAccessor accessor = new MapValueAccessor(source);

			ParameterValueProvider<ElasticsearchPersistentProperty> propertyValueProvider = getParameterProvider(entity, accessor, evaluator);

			EntityInstantiator instantiator = instantiators.getInstantiatorFor(targetEntity);

			@SuppressWarnings({"unchecked"})
			R instance = (R) instantiator.createInstance(targetEntity, propertyValueProvider);

			if (!targetEntity.requiresPropertyPopulation()) {
				return instance;
			}

			ElasticsearchPropertyValueProvider valueProvider = new ElasticsearchPropertyValueProvider(accessor, evaluator);
			R result = readProperties(targetEntity, instance, valueProvider);

			if (source instanceof Document) {
				Document document = (Document) source;
				if (document.hasId()) {
					ElasticsearchPersistentProperty idProperty = targetEntity.getIdProperty();
					PersistentPropertyAccessor<R> propertyAccessor = new ConvertingPropertyAccessor<>(
							targetEntity.getPropertyAccessor(result), conversionService);
					// Only deal with String because ES generated Ids are strings !
					if (idProperty != null && idProperty.isReadable() && idProperty.getType().isAssignableFrom(String.class)) {
						propertyAccessor.setProperty(idProperty, document.getId());
					}
				}

				if (document.hasVersion()) {
					long version = document.getVersion();
					ElasticsearchPersistentProperty versionProperty = targetEntity.getVersionProperty();
					// Only deal with Long because ES versions are longs !
					if (versionProperty != null && versionProperty.getType().isAssignableFrom(Long.class)) {
						// check that a version was actually returned in the response, -1 would indicate that
						// a search didn't request the version ids in the response, which would be an issue
						Assert.isTrue(version != -1, "Version in response is -1");
						targetEntity.getPropertyAccessor(result).setProperty(versionProperty, version);
					}
				}

				if (targetEntity.hasSeqNoPrimaryTermProperty() && document.hasSeqNo() && document.hasPrimaryTerm()) {
					if (isAssignedSeqNo(document.getSeqNo()) && isAssignedPrimaryTerm(document.getPrimaryTerm())) {
						SeqNoPrimaryTerm seqNoPrimaryTerm = new SeqNoPrimaryTerm(document.getSeqNo(), document.getPrimaryTerm());
						ElasticsearchPersistentProperty property = targetEntity.getRequiredSeqNoPrimaryTermProperty();
						targetEntity.getPropertyAccessor(result).setProperty(property, seqNoPrimaryTerm);
					}
				}
			}

			return result;
		}

		private ParameterValueProvider<ElasticsearchPersistentProperty> getParameterProvider(
				ElasticsearchPersistentEntity<?> entity, MapValueAccessor source, SpELExpressionEvaluator evaluator) {

			ElasticsearchPropertyValueProvider provider = new ElasticsearchPropertyValueProvider(source, evaluator);

			PersistentEntityParameterValueProvider<ElasticsearchPersistentProperty> parameterProvider = new PersistentEntityParameterValueProvider<>(
					entity, provider, null);

			return new ConverterAwareSpELExpressionParameterValueProvider(evaluator, conversionService, parameterProvider);
		}

		private boolean isAssignedSeqNo(long seqNo) {
			return seqNo >= 0;
		}

		private boolean isAssignedPrimaryTerm(long primaryTerm) {
			return primaryTerm > 0;
		}

		protected <R> R readProperties(ElasticsearchPersistentEntity<?> entity, R instance,
				ElasticsearchPropertyValueProvider valueProvider) {

			PersistentPropertyAccessor<R> accessor = new ConvertingPropertyAccessor<>(entity.getPropertyAccessor(instance),
					conversionService);

			for (ElasticsearchPersistentProperty property : entity) {

				if (!property.isReadable()) {
					continue;
				}

				Object value = valueProvider.getPropertyValue(property);
				if (value != null) {
					accessor.setProperty(property, value);
				}
			}

			return accessor.getBean();
		}

		@Nullable
		protected <R> R readValue(@Nullable Object value, ElasticsearchPersistentProperty property,
				TypeInformation<?> type) {

			if (value == null) {
				return null;
			}

			Class<?> rawType = type.getType();

			if (property.hasPropertyValueConverter()) {
				// noinspection unchecked
				return (R) propertyConverterRead(property, value);
			}
			else if (TemporalAccessor.class.isAssignableFrom(property.getType())
					&& !conversions.hasCustomReadTarget(value.getClass(), rawType)) {

				// log at most 5 times
				String propertyName = property.getOwner().getType().getSimpleName() + '.' + property.getName();
				String key = propertyName + "-read";
				int count = propertyWarnings.computeIfAbsent(key, k -> 0);
				if (count < 5) {
					log.warn(String.format(
							"Type %s of property %s is a TemporalAccessor class but has neither a @Field annotation defining the date type nor a registered converter for reading!"
									+ " It cannot be mapped from a complex object in Elasticsearch!",
							property.getType().getSimpleName(), propertyName));
					propertyWarnings.put(key, count + 1);
				}
			}

			return readValue(value, type);
		}

		@Nullable
		@SuppressWarnings("unchecked")
		private <T> T readValue(Object value, TypeInformation<?> type) {

			Class<?> rawType = type.getType();

			if (conversions.hasCustomReadTarget(value.getClass(), rawType)) {
				return (T) conversionService.convert(value, rawType);
			}
			else if (value instanceof List) {
				return (T) readCollectionOrArray(type, (List<Object>) value);
			}
			else if (value.getClass().isArray()) {
				return (T) readCollectionOrArray(type, Arrays.asList((Object[]) value));
			}
			else if (value instanceof Map) {

				TypeInformation<?> collectionComponentType = getCollectionComponentType(type);
				if (collectionComponentType != null) {
					Object o = read(collectionComponentType, (Map<String, Object>) value);
					return getCollectionWithSingleElement(type, collectionComponentType, o);
				}
				return (T) read(type, (Map<String, Object>) value);
			}
			else {

				TypeInformation<?> collectionComponentType = getCollectionComponentType(type);
				if (collectionComponentType != null
						&& collectionComponentType.isAssignableFrom(ClassTypeInformation.from(value.getClass()))) {
					Object o = getPotentiallyConvertedSimpleRead(value, collectionComponentType);
					return getCollectionWithSingleElement(type, collectionComponentType, o);
				}

				return (T) getPotentiallyConvertedSimpleRead(value, rawType);
			}
		}

		/**
		 * @param type the type to check
		 * @return true if type is a collectoin, null otherwise,
		 */
		@Nullable
		TypeInformation<?> getCollectionComponentType(TypeInformation<?> type) {
			return type.isCollectionLike() ? type.getComponentType() : null;
		}

		private Object propertyConverterRead(ElasticsearchPersistentProperty property, Object source) {
			PropertyValueConverter propertyValueConverter = Objects.requireNonNull(property.getPropertyValueConverter());

			if (source instanceof String[]) {
				// convert to a List
				source = Arrays.asList((String[]) source);
			}

			if (source instanceof List) {
				source = ((List<?>) source).stream().map(it -> convertOnRead(propertyValueConverter, it))
						.collect(Collectors.toList());
			}
			else if (source instanceof Set) {
				source = ((Set<?>) source).stream().map(it -> convertOnRead(propertyValueConverter, it))
						.collect(Collectors.toSet());
			}
			else {
				source = convertOnRead(propertyValueConverter, source);
			}
			return source;
		}

		private Object convertOnRead(PropertyValueConverter propertyValueConverter, Object source) {
			return propertyValueConverter.read(source);
		}

		/**
		 * Reads the given {@link Collection} into a collection of the given {@link TypeInformation}.
		 *
		 * @param targetType must not be {@literal null}.
		 * @param source     must not be {@literal null}.
		 * @return the converted {@link Collection} or array, will never be {@literal null}.
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		private Object readCollectionOrArray(TypeInformation<?> targetType, Collection<?> source) {

			Assert.notNull(targetType, "Target type must not be null!");

			Class<?> collectionType = targetType.isSubTypeOf(Collection.class) //
					? targetType.getType() //
					: List.class;

			TypeInformation<?> componentType = targetType.getComponentType() != null //
					? targetType.getComponentType() //
					: ClassTypeInformation.OBJECT;
			Class<?> rawComponentType = componentType.getType();

			Collection<Object> items = targetType.getType().isArray() //
					? new ArrayList<>(source.size()) //
					: CollectionFactory.createCollection(collectionType, rawComponentType, source.size());

			if (source.isEmpty()) {
				return getPotentiallyConvertedSimpleRead(items, targetType);
			}

			for (Object element : source) {

				if (element instanceof Map) {
					items.add(read(componentType, (Map<String, Object>) element));
				}
				else {

					if (!Object.class.equals(rawComponentType) && element instanceof Collection) {
						if (!rawComponentType.isArray() && !ClassUtils.isAssignable(Iterable.class, rawComponentType)) {
							throw new MappingException(
									String.format(INCOMPATIBLE_TYPES, element, element.getClass(), rawComponentType));
						}
					}
					if (element instanceof List) {
						items.add(readCollectionOrArray(componentType, (Collection<Object>) element));
					}
					else {
						items.add(getPotentiallyConvertedSimpleRead(element, rawComponentType));
					}
				}
			}

			return getPotentiallyConvertedSimpleRead(items, targetType.getType());
		}

		@Nullable
		private Object getPotentiallyConvertedSimpleRead(@Nullable Object value, TypeInformation<?> targetType) {
			return getPotentiallyConvertedSimpleRead(value, targetType.getType());
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Nullable
		private Object getPotentiallyConvertedSimpleRead(@Nullable Object value, @Nullable Class<?> target) {

			if (target == null || value == null || ClassUtils.isAssignableValue(target, value)) {
				return value;
			}

			if (conversions.hasCustomReadTarget(value.getClass(), target)) {
				return conversionService.convert(value, target);
			}

			if (Enum.class.isAssignableFrom(target)) {
				return Enum.valueOf((Class<Enum>) target, value.toString());
			}

			try {
				return conversionService.convert(value, target);
			}
			catch (ConverterNotFoundException e) {
				return convertFromCollectionToObject(value, target);
			}
		}

		/**
		 * we need the conversion from a collection to the first element for example in the case when reading the
		 * constructor parameter of an entity from a scripted return. Originally this was handle in the conversionService,
		 * but will be removed from spring-data-commons, so we do it here
		 */
		@Nullable
		private Object convertFromCollectionToObject(Object value, @Nullable Class<?> target) {

			if (value.getClass().isArray()) {
				value = Arrays.asList(value);
			}

			if (value instanceof Collection<?> && !((Collection<?>) value).isEmpty()) {
				value = ((Collection<?>) value).iterator().next();
			}

			return conversionService.convert(value, target);
		}

		/**
		 * Compute the type to use by checking the given entity against the store type;
		 */
		private ElasticsearchPersistentEntity<?> computeClosestEntity(ElasticsearchPersistentEntity<?> entity,
				Map<String, Object> source) {
			TypeInformation<?> typeToUse = entity.getTypeInformation();

			if (typeToUse == null) {
				return entity;
			}

			if (!entity.getTypeInformation().getType().isInterface() && !entity.getTypeInformation().isCollectionLike()
					&& !entity.getTypeInformation().isMap()
					&& !ClassUtils.isAssignableValue(entity.getType(), typeToUse.getType())) {
				return entity;
			}

			return mappingContext.getRequiredPersistentEntity(typeToUse);
		}

		enum NoOpParameterValueProvider implements ParameterValueProvider<ElasticsearchPersistentProperty> {

			INSTANCE;

			@Override
			public <T> T getParameterValue(Parameter<T, ElasticsearchPersistentProperty> parameter) {
				return null;
			}
		}

		class ElasticsearchPropertyValueProvider implements PropertyValueProvider<ElasticsearchPersistentProperty> {

			final MapValueAccessor accessor;
			final SpELExpressionEvaluator evaluator;

			ElasticsearchPropertyValueProvider(MapValueAccessor accessor, SpELExpressionEvaluator evaluator) {
				this.accessor = accessor;
				this.evaluator = evaluator;
			}

			@Override
			public <T> T getPropertyValue(ElasticsearchPersistentProperty property) {

				String expression = property.getSpelExpression();
				Object value = expression != null ? evaluator.evaluate(expression) : accessor.get(property);

				if (value == null) {
					return null;
				}

				return readValue(value, property, property.getTypeInformation());
			}
		}

		/**
		 * Extension of {@link SpELExpressionParameterValueProvider} to recursively trigger value conversion on the raw
		 * resolved SpEL value.
		 *
		 * @author Mark Paluch
		 */
		private class ConverterAwareSpELExpressionParameterValueProvider
				extends SpELExpressionParameterValueProvider<ElasticsearchPersistentProperty> {

			/**
			 * Creates a new {@link ConverterAwareSpELExpressionParameterValueProvider}.
			 *
			 * @param evaluator         must not be {@literal null}.
			 * @param conversionService must not be {@literal null}.
			 * @param delegate          must not be {@literal null}.
			 */
			public ConverterAwareSpELExpressionParameterValueProvider(SpELExpressionEvaluator evaluator,
					ConversionService conversionService, ParameterValueProvider<ElasticsearchPersistentProperty> delegate) {

				super(evaluator, conversionService, delegate);
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.model.SpELExpressionParameterValueProvider#potentiallyConvertSpelValue(java.lang.Object, org.springframework.data.mapping.PreferredConstructor.Parameter)
			 */
			@Override
			protected <T> T potentiallyConvertSpelValue(Object object,
					Parameter<T, ElasticsearchPersistentProperty> parameter) {
				return readValue(object, parameter.getType());
			}
		}
	}


	@Slf4j
	private static class Writer extends Base {

		public Writer(
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
				GenericConversionService conversionService, CustomConversions conversions) {
			super(mappingContext, conversionService, conversions);
		}

		private static boolean hasEmptyValue(Object value) {

			if (value instanceof String && ((String) value).isEmpty() || value instanceof Collection<?> && ((Collection<?>) value).isEmpty()
					|| value instanceof Map<?, ?> && ((Map<?, ?>) value).isEmpty()) {
				return true;
			}

			return false;
		}

		private static Collection<?> asCollection(Object source) {

			if (source instanceof Collection) {
				return (Collection<?>) source;
			}

			return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
		}

		void write(Object source, Document sink) {

			if (source instanceof Map) {
				sink.putAll((Map<String, Object>) source);
				return;
			}
			Class<?> entityType = ClassUtils.getUserClass(source.getClass());
			TypeInformation<?> typeInformation = ClassTypeInformation.from(entityType);
			writeInternal(source, sink, typeInformation);
		}

		private void writeInternal(@Nullable Object source, Map<String, Object> sink,
				@Nullable TypeInformation<?> typeInformation) {

			if (null == source) {
				return;
			}

			Class<?> entityType = source.getClass();
			Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(entityType, Map.class);

			if (customTarget.isPresent()) {
				Map<String, Object> result = conversionService.convert(source, Map.class);

				if (result != null) {
					sink.putAll(result);
				}
				return;
			}

			if (Map.class.isAssignableFrom(entityType)) {
				writeMapInternal((Map<Object, Object>) source, sink, ClassTypeInformation.MAP);
				return;
			}

			if (Collection.class.isAssignableFrom(entityType)) {
				writeCollectionInternal((Collection<?>) source, ClassTypeInformation.LIST, (Collection<?>) sink);
				return;
			}

			ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(entityType);
			writeInternal(source, sink, entity);
		}

		private void writeInternal(@Nullable Object source, Map<String, Object> sink,
				@Nullable ElasticsearchPersistentEntity<?> entity) {

			if (source == null) {
				return;
			}

			if (null == entity) {
				throw new MappingException("No mapping metadata found for entity of type " + source.getClass().getName());
			}

			PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);
			writeProperties(entity, accessor, new MapValueAccessor(sink));
		}

		private boolean requiresTypeHint(Class<?> type) {

			return !isSimpleType(type) && !ClassUtils.isAssignable(Collection.class, type)
					&& !conversions.hasCustomWriteTarget(type, Document.class);
		}

		private boolean isSimpleType(Object value) {
			return isSimpleType(value.getClass());
		}

		private boolean isSimpleType(Class<?> type) {
			return !Map.class.isAssignableFrom(type) && conversions.isSimpleType(type);
		}

		/**
		 * Writes the given {@link Map} to the given {@link Document} considering the given {@link TypeInformation}.
		 *
		 * @param source       must not be {@literal null}.
		 * @param sink         must not be {@literal null}.
		 * @param propertyType must not be {@literal null}.
		 */
		private Map<String, Object> writeMapInternal(Map<?, ?> source, Map<String, Object> sink,
				TypeInformation<?> propertyType) {

			for (Map.Entry<?, ?> entry : source.entrySet()) {

				Object key = entry.getKey();
				Object value = entry.getValue();

				if (isSimpleType(key.getClass())) {

					String simpleKey = potentiallyConvertMapKey(key);
					if (value == null || isSimpleType(value)) {
						sink.put(simpleKey, getPotentiallyConvertedSimpleWrite(value, Object.class));
					}
					else if (value instanceof Collection || value.getClass().isArray()) {
						sink.put(simpleKey,
								writeCollectionInternal(asCollection(value), propertyType.getMapValueType(), new ArrayList<>()));
					}
					else {
						Map<String, Object> document = Document.create();
						TypeInformation<?> valueTypeInfo = propertyType.isMap() ? propertyType.getMapValueType()
								: ClassTypeInformation.OBJECT;
						writeInternal(value, document, valueTypeInfo);

						sink.put(simpleKey, document);
					}
				}
				else {
					throw new MappingException("Cannot use a complex object as a key value.");
				}
			}

			return sink;
		}

		/**
		 * Populates the given {@link Collection sink} with converted values from the given {@link Collection source}.
		 *
		 * @param source the collection to create a {@link Collection} for, must not be {@literal null}.
		 * @param type   the {@link TypeInformation} to consider or {@literal null} if unknown.
		 * @param sink   the {@link Collection} to write to.
		 */
		@SuppressWarnings("unchecked")
		private List<Object> writeCollectionInternal(Collection<?> source, @Nullable TypeInformation<?> type,
				Collection<?> sink) {

			TypeInformation<?> componentType = null;

			List<Object> collection = sink instanceof List ? (List<Object>) sink : new ArrayList<>(sink);

			if (type != null) {
				componentType = type.getComponentType();
			}

			for (Object element : source) {

				Class<?> elementType = element == null ? null : element.getClass();

				if (elementType == null || isSimpleType(elementType)) {
					collection.add(getPotentiallyConvertedSimpleWrite(element,
							componentType != null ? componentType.getType() : Object.class));
				}
				else if (element instanceof Collection || elementType.isArray()) {
					collection.add(writeCollectionInternal(asCollection(element), componentType, new ArrayList<>()));
				}
				else {
					Map<String, Object> document = Document.create();
					writeInternal(element, document, componentType);
					collection.add(document);
				}
			}

			return collection;
		}

		private void writeProperties(ElasticsearchPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor,
				MapValueAccessor sink) {

			for (ElasticsearchPersistentProperty property : entity) {

				if (!property.isWritable()) {
					continue;
				}

				Object value = accessor.getProperty(property);

				if (value == null) {

					if (property.storeNullValue()) {
						sink.set(property, null);
					}
					continue;
				}

				if (!property.storeEmptyValue() && hasEmptyValue(value)) {
					continue;
				}

				if (property.hasPropertyValueConverter()) {
					value = propertyConverterWrite(property, value);
					sink.set(property, value);
				}
				else if (TemporalAccessor.class.isAssignableFrom(property.getActualType())
						&& !conversions.hasCustomWriteTarget(value.getClass())) {

					// log at most 5 times
					String propertyName = entity.getType().getSimpleName() + '.' + property.getName();
					String key = propertyName + "-write";
					int count = propertyWarnings.computeIfAbsent(key, k -> 0);
					if (count < 5) {
						log.warn(String.format(
								"Type %s of property %s is a TemporalAccessor class but has neither a @Field annotation defining the date type nor a registered converter for writing!"
										+ " It will be mapped to a complex object in Elasticsearch!",
								property.getType().getSimpleName(), propertyName));
						propertyWarnings.put(key, count + 1);
					}
				}
				else if (!isSimpleType(value)) {
					writeProperty(property, value, sink);
				}
				else {
					Object writeSimpleValue = getPotentiallyConvertedSimpleWrite(value, Object.class);
					if (writeSimpleValue != null) {
						sink.set(property, writeSimpleValue);
					}
				}
			}
		}

		@SuppressWarnings("unchecked")
		protected void writeProperty(ElasticsearchPersistentProperty property, Object value, MapValueAccessor sink) {

			Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(value.getClass());

			if (customWriteTarget.isPresent()) {
				Class<?> writeTarget = customWriteTarget.get();
				sink.set(property, conversionService.convert(value, writeTarget));
				return;
			}

			TypeInformation<?> valueType = ClassTypeInformation.from(value.getClass());
			TypeInformation<?> type = property.getTypeInformation();

			if (valueType.isCollectionLike()) {
				List<Object> collectionInternal = createCollection(asCollection(value), property);
				sink.set(property, collectionInternal);
				return;
			}

			if (valueType.isMap()) {
				Map<String, Object> mapDbObj = createMap((Map<?, ?>) value, property);
				sink.set(property, mapDbObj);
				return;
			}

			// Lookup potential custom target type
			Optional<Class<?>> basicTargetType = conversions.getCustomWriteTarget(value.getClass());

			if (basicTargetType.isPresent()) {

				sink.set(property, conversionService.convert(value, basicTargetType.get()));
				return;
			}

			ElasticsearchPersistentEntity<?> entity = valueType.isSubTypeOf(property.getType())
					? mappingContext.getRequiredPersistentEntity(value.getClass())
					: mappingContext.getRequiredPersistentEntity(type);

			Object existingValue = sink.get(property);
			Map<String, Object> document = existingValue instanceof Map ? (Map<String, Object>) existingValue
					: Document.create();
			writeInternal(value, document, entity);
			sink.set(property, document);
		}

		private String potentiallyConvertMapKey(Object key) {

			if (key instanceof String) {
				return (String) key;
			}

			if (conversions.hasCustomWriteTarget(key.getClass(), String.class)) {
				Object potentiallyConvertedSimpleWrite = getPotentiallyConvertedSimpleWrite(key, Object.class);

				if (potentiallyConvertedSimpleWrite == null) {
					return key.toString();
				}
				return (String) potentiallyConvertedSimpleWrite;
			}
			return key.toString();
		}

		@Nullable
		private Object getPotentiallyConvertedSimpleWrite(@Nullable Object value, @Nullable Class<?> typeHint) {

			if (value == null) {
				return null;
			}

			if (typeHint != null && Object.class != typeHint) {

				if (conversionService.canConvert(value.getClass(), typeHint)) {
					value = conversionService.convert(value, typeHint);

					if (value == null) {
						return null;
					}
				}
			}

			Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(value.getClass());

			if (customTarget.isPresent()) {
				return conversionService.convert(value, customTarget.get());
			}

			if (ObjectUtils.isArray(value)) {

				if (value instanceof byte[]) {
					return value;
				}
				return asCollection(value);
			}

			return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
		}

		private Object propertyConverterWrite(ElasticsearchPersistentProperty property, Object value) {
			PropertyValueConverter propertyValueConverter = Objects.requireNonNull(property.getPropertyValueConverter());

			if (value instanceof List) {
				value = ((List<?>) value).stream().map(propertyValueConverter::write).collect(Collectors.toList());
			}
			else if (value instanceof Set) {
				value = ((Set<?>) value).stream().map(propertyValueConverter::write).collect(Collectors.toSet());
			}
			else {
				value = propertyValueConverter.write(value);
			}
			return value;
		}

		protected List<Object> createCollection(Collection<?> collection, ElasticsearchPersistentProperty property) {
			return writeCollectionInternal(collection, property.getTypeInformation(), new ArrayList<>(collection.size()));
		}

		protected Map<String, Object> createMap(Map<?, ?> map, ElasticsearchPersistentProperty property) {

			Assert.notNull(map, "Given map must not be null!");
			Assert.notNull(property, "PersistentProperty must not be null!");

			return writeMapInternal(map, new LinkedHashMap<>(map.size()), property.getTypeInformation());
		}
	}


	private static class Base {
		protected final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
		protected final GenericConversionService conversionService;
		protected final CustomConversions conversions;
		protected final ConcurrentHashMap<String, Integer> propertyWarnings = new ConcurrentHashMap<>();

		private Base(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext, GenericConversionService conversionService, CustomConversions conversions) {
			this.mappingContext = mappingContext;
			this.conversionService = conversionService;
			this.conversions = conversions;
		}
	}
}
