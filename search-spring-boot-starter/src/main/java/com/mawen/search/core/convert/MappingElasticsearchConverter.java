package com.mawen.search.core.convert;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mawen.search.core.document.Document;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.domain.FetchSourceFilter;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.domain.SourceFilter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class MappingElasticsearchConverter implements ElasticsearchConverter, ApplicationContextAware, InitializingBean {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;

	public MappingElasticsearchConverter(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		this(mappingContext, null);
	}

	public MappingElasticsearchConverter(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
			@Nullable GenericConversionService conversionService) {

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


	@Override
	public void afterPropertiesSet() {
		DateFormatterRegistrar.addDateConverters(conversionService);
	}

	// region read/write

	@Override
	public <R> R read(Class<R> type, Document source) {

//		Reader reader = new Reader(mappingContext, conversionService, spELContext, instantiators);
//		return reader.read(type, source);

		return null;
	}

	@Override
	public void write(Object source, Document sink) {

		Assert.notNull(source, "source to map must not be null");
//
//		Writer writer = new Writer(mappingContext, conversionService);
//		writer.write(source, sink);
	}

	// endregion

	// region queries
	@Override
	public void updateQuery(Query query, @Nullable Class<?> domainClass) {

		Assert.notNull(query, "query must not be null");

		if (query instanceof BaseQuery) {
			BaseQuery baseQuery = (BaseQuery) query;
			if (baseQuery.queryIsUpdatedByConverter()) {
				return;
			}
		}

		if (domainClass == null) {
			return;
		}

		updatePropertiesInFieldsAndSourceFilter(query, domainClass);

		if (query instanceof BaseQuery) {
			BaseQuery baseQuery = (BaseQuery) query;
			baseQuery.setQueryIsUpdatedByConverter(true);
		}
	}

	private void updatePropertiesInFieldsAndSourceFilter(Query query, Class<?> domainClass) {

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainClass);

		if (persistentEntity != null) {
			List<String> fields = query.getFields();

			if (!fields.isEmpty()) {
				query.setFields(updateFieldNames(fields, persistentEntity));
			}

			List<String> storedFields = query.getStoredFields();
			if (!CollectionUtils.isEmpty(storedFields)) {
				query.setStoredFields(updateFieldNames(storedFields, persistentEntity));
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
				// nested objects may have properties like 'id' which are recognized as isIdProperty() but they are not
				// Documents

				if (property.isIdProperty() && document.hasId()) {
					Object id = null;

					// take the id property from the document source if available
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
}
