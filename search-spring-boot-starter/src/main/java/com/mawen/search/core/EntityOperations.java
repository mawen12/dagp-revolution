package com.mawen.search.core;

import java.util.Map;

import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.SeqNoPrimaryTerm;
import com.mawen.search.core.routing.RoutingResolver;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class EntityOperations {

	private static final String ID_FIELD = "id";

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context;

	public EntityOperations(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context) {

		Assert.notNull(context, "context must not be null");

		this.context = context;
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	<T> Entity<T> forEntity(T entity) {

		Assert.notNull(entity, "Bean must not be null!");

		if (entity instanceof Map) {
			return new SimpleMappedEntity((Map<String, Object>) entity);
		}

		return MappedEntity.of(entity, context);
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> AdaptableEntity<T> forEntity(T entity, ConversionService conversionService,
			RoutingResolver routingResolver) {

		Assert.notNull(entity, "Bean must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		if (entity instanceof Map) {
			return new SimpleMappedEntity((Map<String, Object>) entity);
		}

		return AdaptableMappedEntity.of(entity, context, conversionService, routingResolver);
	}


	IndexCoordinates determineIndex(Entity<?> entity, @Nullable String index) {
		return determineIndex(entity.getPersistentEntity(), index);
	}


	IndexCoordinates determineIndex(ElasticsearchPersistentEntity<?> persistentEntity, @Nullable String index) {
		return index != null ? IndexCoordinates.of(index) : persistentEntity.getIndexCoordinates();
	}


	interface Entity<T> {


		@Nullable
		Object getId();

		/**
		 * Returns whether the entity is versioned, i.e. if it contains a version property.
		 *
		 * @return
		 */
		default boolean isVersionedEntity() {
			return false;
		}

		/**
		 * Returns the value of the version if the entity has a version property, {@literal null} otherwise.
		 *
		 * @return
		 */
		@Nullable
		Object getVersion();

		/**
		 * Returns the underlying bean.
		 *
		 * @return
		 */
		T getBean();

		/**
		 * Returns whether the entity is considered to be new.
		 *
		 * @return
		 */
		boolean isNew();

		/**
		 * Returns the {@link ElasticsearchPersistentEntity} associated with this entity.
		 *
		 * @return can be {@literal null} if this entity is not mapped.
		 */
		@Nullable
		ElasticsearchPersistentEntity<?> getPersistentEntity();

		/**
		 * Returns the required {@link ElasticsearchPersistentEntity}.
		 *
		 * @return
		 * @throws IllegalStateException if no {@link ElasticsearchPersistentEntity} is associated with this entity.
		 */
		default ElasticsearchPersistentEntity<?> getRequiredPersistentEntity() {

			ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity();
			if (persistentEntity == null) {
				throw new IllegalStateException("No ElasticsearchPersistentEntity available for this entity!");
			}

			return persistentEntity;
		}
	}

	/**
	 * Information and commands on an entity.
	 *
	 * @author Mark Paluch
	 * @author Christoph Strobl
	 */
	public interface AdaptableEntity<T> extends Entity<T> {

		/**
		 * Populates the identifier of the backing entity if it has an identifier property and there's no identifier
		 * currently present.
		 *
		 * @param id can be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		T populateIdIfNecessary(@Nullable Object id);

		/**
		 * Initializes the version property of the of the current entity if available.
		 *
		 * @return the entity with the version property updated if available.
		 */
		T initializeVersionProperty();

		/**
		 * Increments the value of the version property if available.
		 *
		 * @return the entity with the version property incremented if available.
		 */
		T incrementVersion();

		/**
		 * Returns the current version value if the entity has a version property.
		 *
		 * @return the current version or {@literal null} in case it's uninitialized or the entity doesn't expose a version
		 * property.
		 */
		@Override
		@Nullable
		Number getVersion();

		/**
		 * Returns whether there is a property with type SeqNoPrimaryTerm in this entity.
		 *
		 * @return true if there is SeqNoPrimaryTerm property
		 * @since 4.0
		 */
		boolean hasSeqNoPrimaryTerm();

		/**
		 * Returns SeqNoPropertyTerm for this entity.
		 *
		 * @return SeqNoPrimaryTerm, may be {@literal null}
		 * @since 4.0
		 */
		@Nullable
		SeqNoPrimaryTerm getSeqNoPrimaryTerm();

		/**
		 * returns the routing for the entity if it is available
		 *
		 * @return routing if available
		 * @since 4.1
		 */
		@Nullable
		String getRouting();
	}

	/**
	 * @param <T>
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	private static class MapBackedEntity<T extends Map<String, Object>> implements AdaptableEntity<T> {

		private final T map;

		public MapBackedEntity(T map) {

			Assert.notNull(map, "map must not be null");

			this.map = map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getId()
		 */
		@Override
		public Object getId() {
			return map.get(ID_FIELD);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#populateIdIfNecessary(java.lang.Object)
		 */
		@Nullable
		@Override
		public T populateIdIfNecessary(@Nullable Object id) {

			map.put(ID_FIELD, id);

			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#initializeVersionProperty()
		 */
		@Override
		public T initializeVersionProperty() {
			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#getVersion()
		 */
		@Override
		@Nullable
		public Number getVersion() {
			return null;
		}

		@Override
		public boolean hasSeqNoPrimaryTerm() {
			return false;
		}

		@Override
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#incrementVersion()
		 */
		@Override
		public T incrementVersion() {
			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getBean()
		 */
		@Override
		public T getBean() {
			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#isNew()
		 */
		@Override
		public boolean isNew() {
			return map.get(ID_FIELD) != null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getPersistentEntity()
		 */
		@Override
		public ElasticsearchPersistentEntity<?> getPersistentEntity() {
			return null;
		}

		@Override
		public String getRouting() {
			return null;
		}
	}

	/**
	 * Plain entity without applying further mapping.
	 *
	 * @param <T>
	 * @since 3.2
	 */
	private static class UnmappedEntity<T extends Map<String, Object>> extends MapBackedEntity<T> {

		UnmappedEntity(T map) {
			super(map);
		}
	}

	/**
	 * Simple mapped entity without an associated {@link ElasticsearchPersistentEntity}.
	 *
	 * @param <T>
	 * @since 3.2
	 */
	private static class SimpleMappedEntity<T extends Map<String, Object>> extends MapBackedEntity<T> {

		SimpleMappedEntity(T map) {
			super(map);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.UnmappedEntity#getId()
		 */
		@Override
		public Object getId() {
			return getBean().get(ID_FIELD);
		}
	}

	/**
	 * Mapped entity with an associated {@link ElasticsearchPersistentEntity}.
	 *
	 * @param <T>
	 * @since 3.2
	 */
	private static class MappedEntity<T> implements Entity<T> {

		private final ElasticsearchPersistentEntity<?> entity;
		private final IdentifierAccessor idAccessor;
		private final PersistentPropertyAccessor<T> propertyAccessor;

		private MappedEntity(ElasticsearchPersistentEntity<?> entity, IdentifierAccessor idAccessor,
				PersistentPropertyAccessor<T> propertyAccessor) {

			Assert.notNull(entity, "entity must not ne null");
			Assert.notNull(idAccessor, "idAccessor must not ne null");
			Assert.notNull(propertyAccessor, "propertyAccessor must not ne null");

			this.entity = entity;
			this.idAccessor = idAccessor;
			this.propertyAccessor = propertyAccessor;
		}

		private static <T> MappedEntity<T> of(T bean,
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context) {

			ElasticsearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
			IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
			PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

			return new MappedEntity<>(entity, identifierAccessor, propertyAccessor);
		}

		@Override
		public Object getId() {
			return idAccessor.getIdentifier();
		}

		@Override
		public boolean isVersionedEntity() {
			return entity.hasVersionProperty();
		}

		@Override
		@Nullable
		public Object getVersion() {
			return propertyAccessor.getProperty(entity.getVersionProperty());
		}

		@Override
		public T getBean() {
			return propertyAccessor.getBean();
		}

		@Override
		public boolean isNew() {
			return entity.isNew(propertyAccessor.getBean());
		}

		@Override
		public ElasticsearchPersistentEntity<?> getPersistentEntity() {
			return entity;
		}
	}

	/**
	 * @param <T>
	 * @since 3.2
	 */
	private static class AdaptableMappedEntity<T> extends MappedEntity<T> implements AdaptableEntity<T> {

		private final ElasticsearchPersistentEntity<?> entity;
		private final ConvertingPropertyAccessor<T> propertyAccessor;
		private final IdentifierAccessor identifierAccessor;
		private final ConversionService conversionService;
		private final RoutingResolver routingResolver;

		private AdaptableMappedEntity(T bean, ElasticsearchPersistentEntity<?> entity,
				IdentifierAccessor identifierAccessor, ConvertingPropertyAccessor<T> propertyAccessor,
				ConversionService conversionService, RoutingResolver routingResolver) {

			super(entity, identifierAccessor, propertyAccessor);

			this.entity = entity;
			this.propertyAccessor = propertyAccessor;
			this.identifierAccessor = identifierAccessor;
			this.conversionService = conversionService;
			this.routingResolver = routingResolver;
		}

		static <T> AdaptableEntity<T> of(T bean,
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context,
				ConversionService conversionService, RoutingResolver routingResolver) {

			ElasticsearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
			IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
			PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

			return new AdaptableMappedEntity<>(bean, entity, identifierAccessor,
					new ConvertingPropertyAccessor<>(propertyAccessor, conversionService), conversionService, routingResolver);
		}

		@Override
		public T getBean() {
			return propertyAccessor.getBean();
		}

		@Nullable
		@Override
		public T populateIdIfNecessary(@Nullable Object id) {

			if (id == null) {
				return null;
			}

			T bean = propertyAccessor.getBean();
			ElasticsearchPersistentProperty idProperty = entity.getIdProperty();

			if (idProperty == null) {
				return bean;
			}

			if (identifierAccessor.getIdentifier() != null) {
				return bean;
			}

			propertyAccessor.setProperty(idProperty, id);

			return propertyAccessor.getBean();
		}

		@Override
		@Nullable
		public Number getVersion() {

			ElasticsearchPersistentProperty versionProperty = entity.getVersionProperty();
			return versionProperty != null ? propertyAccessor.getProperty(versionProperty, Number.class) : null;
		}

		@Override
		public boolean hasSeqNoPrimaryTerm() {
			return entity.hasSeqNoPrimaryTermProperty();
		}

		@Override
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {

			ElasticsearchPersistentProperty seqNoPrimaryTermProperty = entity.getRequiredSeqNoPrimaryTermProperty();

			return propertyAccessor.getProperty(seqNoPrimaryTermProperty, SeqNoPrimaryTerm.class);
		}

		@Override
		public T initializeVersionProperty() {

			if (!entity.hasVersionProperty()) {
				return propertyAccessor.getBean();
			}

			ElasticsearchPersistentProperty versionProperty = entity.getRequiredVersionProperty();

			propertyAccessor.setProperty(versionProperty, versionProperty.getType().isPrimitive() ? 1 : 0);

			return propertyAccessor.getBean();
		}

		@Override
		public T incrementVersion() {

			ElasticsearchPersistentProperty versionProperty = entity.getRequiredVersionProperty();
			Number version = getVersion();
			Number nextVersion = version == null ? 0 : version.longValue() + 1;

			propertyAccessor.setProperty(versionProperty, nextVersion);

			return propertyAccessor.getBean();
		}

		@Override
		public String getRouting() {

			String routing = routingResolver.getRouting(propertyAccessor.getBean());

			if (routing != null) {
				return routing;
			}

			return null;
		}

	}
}
