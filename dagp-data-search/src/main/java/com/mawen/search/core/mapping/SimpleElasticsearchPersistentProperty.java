package com.mawen.search.core.mapping;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.mawen.search.core.annotation.DateFormat;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.annotation.IndexName;
import com.mawen.search.core.annotation.ValueConverter;
import com.mawen.search.core.annotation.WriteOnlyProperty;
import com.mawen.search.core.convert.AbstractPropertyValueConverter;
import com.mawen.search.core.convert.DatePropertyValueConverter;
import com.mawen.search.core.convert.DateRangePropertyValueConverter;
import com.mawen.search.core.convert.ElasticsearchDateConverter;
import com.mawen.search.core.convert.NumberRangePropertyValueConverter;
import com.mawen.search.core.convert.TemporalPropertyValueConverter;
import com.mawen.search.core.convert.TemporalRangePropertyValueConverter;
import com.mawen.search.core.domain.Range;
import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Slf4j
@Getter
public class SimpleElasticsearchPersistentProperty
		extends AnnotationBasedPersistentProperty<ElasticsearchPersistentProperty>
		implements ElasticsearchPersistentProperty {

	private static final List<String> SUPPORTED_ID_PROPERTY_TYPES = Arrays.asList("id", "document");
	private static final PropertyNameFieldNamingStrategy DEFAULT_FIELD_NAMING_STRATEGY = PropertyNameFieldNamingStrategy.INSTANCE;


	private final boolean isId;
	private final boolean isSeqNoPrimaryTerm;
	private final String indexName;
	@Nullable
	private final String annotatedFieldName;
	@Nullable
	private PropertyValueConverter propertyValueConverter;
	private final boolean storeNullValue;
	private final boolean storeEmptyValue;

	public SimpleElasticsearchPersistentProperty(Property property,
			PersistentEntity<?, ElasticsearchPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.annotatedFieldName = getAnnotatedFieldName();
		this.isId = super.isIdProperty() || (SUPPORTED_ID_PROPERTY_TYPES.contains(getFieldName()) && !hasExplicitFieldName());
		this.isSeqNoPrimaryTerm = SeqNoPrimaryTerm.class.isAssignableFrom(getRawType());
		this.indexName = getAnnotatedIndexNameValue();

		initPropertyValueConverter();

		boolean isField = isAnnotationPresent(Field.class);

		if (isVersionProperty() && !getType().equals(Long.class)) {
			throw new MappingException(String.format("Version property %s must be of type Long!", property.getName()));
		}

		this.storeNullValue = isField && getRequiredAnnotation(Field.class).storeNullValue();
		this.storeEmptyValue = isField ? getRequiredAnnotation(Field.class).storeEmptyValue() : true;
	}

	@Override
	protected Association<ElasticsearchPersistentProperty> createAssociation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isIdProperty() {
		return isId;
	}

	@Override
	public String getFieldName() {

		if (annotatedFieldName == null) {
			FieldNamingStrategy fieldNamingStrategy = getFieldNamingStrategy();
			String fieldName = fieldNamingStrategy.getFieldName(this);

			if (!StringUtils.hasText(fieldName)) {
				throw new MappingException(String.format("Invalid (null or empty) field name returned for property %s by %s!", this, getOwner().getType()));
			}

			return fieldName;
		}

		return annotatedFieldName;
	}

	@Override
	public boolean hasExplicitFieldName() {
		return StringUtils.hasText(getAnnotatedFieldName());
	}

	@Override
	public boolean isSeqNoPrimaryTermProperty() {
		return isSeqNoPrimaryTerm;
	}

	@Override
	public boolean hasPropertyValueConverter() {
		return propertyValueConverter != null;
	}

	@Override
	public boolean isWritable() {
		return !isSeqNoPrimaryTermProperty() && !isAnnotationPresent(ReadOnlyProperty.class);
	}

	@Override
	public boolean isReadable() {
		return !isSeqNoPrimaryTermProperty() && !isAnnotationPresent(WriteOnlyProperty.class);
	}

	@Override
	public boolean storeNullValue() {
		return storeNullValue;
	}

	@Override
	public boolean storeEmptyValue() {
		return storeEmptyValue;
	}

	@Override
	public boolean isIndexNameProperty() {
		return indexName != null;
	}

	@Nullable
	private String getAnnotatedFieldName() {

		String name = null;

		if (isAnnotationPresent(Field.class)) {
			name = findAnnotation(Field.class).value();
		}

		return StringUtils.hasText(name) ? name : null;
	}

	@Nullable
	public String getAnnotatedIndexNameValue() {

		if (isAnnotationPresent(IndexName.class)) {
			return findAnnotation(IndexName.class).value();
		}
		return null;
	}

	private void initPropertyValueConverter() {

		initPropertyValueConverterFromAnnotation();

		if (hasPropertyValueConverter()) {
			return;
		}

		Class<?> actualType = getActualTypeOrNull();
		if (actualType == null) {
			return;
		}

		Field field = findAnnotation(Field.class);
		if (field == null) {
			return;
		}

		switch (field.type()) {
			case Date:
			case Date_Nanos: {
				List<ElasticsearchDateConverter> dateConverters = getDateConverters(field, actualType);
				if (dateConverters.isEmpty()) {
					log.warn(String.format("No date formatters configured for property '%s'.", getName()));
					return;
				}

				if (TemporalAccessor.class.isAssignableFrom(actualType)) {
					propertyValueConverter = new TemporalPropertyValueConverter(this, dateConverters);
				}
				else if (Date.class.isAssignableFrom(actualType)) {
					propertyValueConverter = new DatePropertyValueConverter(this, dateConverters);
				}
				else {
					log.warn(String.format("Unsupported type '%s' for date property '%s'.", actualType, getName()));
				}
				break;
			}
			case Date_Range: {
				if (!Range.class.isAssignableFrom(actualType)) {
					return;
				}

				List<ElasticsearchDateConverter> dateConverters = getDateConverters(field, actualType);
				if (dateConverters.isEmpty()) {
					log.warn(String.format("No date formatters configured for property '%s'.", getName()));
					return;
				}

				Class<?> genericType = getGenericType();
				if (TemporalAccessor.class.isAssignableFrom(genericType)) {
					propertyValueConverter = new TemporalRangePropertyValueConverter(this, dateConverters);
				}
				else if (Date.class.isAssignableFrom(genericType)) {
					propertyValueConverter = new DateRangePropertyValueConverter(this, dateConverters);
				}
				else {
					log.warn(
							String.format("Unsupported generic type '{%s' for date range property '%s'.", genericType, getName()));
				}
				break;

			}
			case Integer_Range:
			case Float_Range:
			case Long_Range:
			case Double_Range: {

				if (!Range.class.isAssignableFrom(actualType)) {
					return;
				}

				Class<?> genericType = getGenericType();
				if ((field.type() == FieldType.Integer_Range && !Integer.class.isAssignableFrom(genericType))
						|| (field.type() == FieldType.Float_Range && !Float.class.isAssignableFrom(genericType))
						|| (field.type() == FieldType.Long_Range && !Long.class.isAssignableFrom(genericType))
						|| (field.type() == FieldType.Double_Range && !Double.class.isAssignableFrom(genericType))) {
					log.warn(String.format("Unsupported generic type '%s' for range field type '%s' of property '%s'.",
							genericType, field.type(), getName()));
					return;
				}

				propertyValueConverter = new NumberRangePropertyValueConverter(this);
				break;
			}
			default:
				break;
		}
	}

	private void initPropertyValueConverterFromAnnotation() {

		ValueConverter annotation = findAnnotation(ValueConverter.class);

		if (annotation != null) {
			Class<? extends PropertyValueConverter> clazz = annotation.value();

			if (Enum.class.isAssignableFrom(clazz)) {
				PropertyValueConverter[] enumConstants = clazz.getEnumConstants();

				if (enumConstants == null || enumConstants.length != 1) {
					throw new IllegalArgumentException(clazz + " is an enum with more than 1 constant and cannot be used here");
				}
				propertyValueConverter = enumConstants[0];
			}
			else {
				if (AbstractPropertyValueConverter.class.isAssignableFrom(clazz)) {
					propertyValueConverter = BeanUtils.instantiateClass(BeanUtils.getResolvableConstructor(clazz), this);
				}
				else {
					propertyValueConverter = BeanUtils.instantiateClass(clazz);
				}
			}
		}

	}

	private FieldNamingStrategy getFieldNamingStrategy() {
		return DEFAULT_FIELD_NAMING_STRATEGY;
	}

	private List<ElasticsearchDateConverter> getDateConverters(Field field, Class<?> actualType) {

		DateFormat[] dateFormats = field.format();
		String[] dateFormatPatterns = field.pattern();
		List<ElasticsearchDateConverter> converters = new ArrayList<>();

		if (dateFormats.length == 0 && dateFormatPatterns.length == 0) {
			log.warn(String.format(
					"Property '%s' has @Field type '%s' but has no built-in format or custom date pattern defined. Make sure you have a converter registered for type %s.",
					getName(), field.type().name(), actualType.getSimpleName()));
			return converters;
		}

		// register converters for built-in formats
		for (DateFormat dateFormat : dateFormats) {
			switch (dateFormat) {
				case weekyear:
				case weekyear_week:
				case weekyear_week_day:
					log.warn(String.format(
							"No default converter available for '%s' and date format '%s'. Use a custom converter instead.",
							actualType.getName(), dateFormat.name()));
				default:
					converters.add(ElasticsearchDateConverter.of(dateFormat));
			}
		}

		for (String dateFormatPattern : dateFormatPatterns) {
			if (!StringUtils.hasText(dateFormatPattern)) {
				throw new MappingException(String.format("Date pattern of property '%s' must not be empty", getName()));
			}
			converters.add(ElasticsearchDateConverter.of(dateFormatPattern));
		}

		return converters;
	}

	private Class<?> getGenericType() {

		TypeInformation<?> typeInformation = getTypeInformation();

		if (typeInformation.isCollectionLike()) {
			typeInformation = typeInformation.getComponentType();
		}

		return typeInformation.getTypeArguments().get(0).getType();
	}
}
