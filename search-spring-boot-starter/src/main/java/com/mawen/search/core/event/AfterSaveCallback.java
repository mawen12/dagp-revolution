package com.mawen.search.core.event;

import com.mawen.search.core.mapping.IndexCoordinates;

/**
 * Entity callback triggered after save of an entity
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@FunctionalInterface
public interface AfterSaveCallback<T> extends EntityCallback<T> {

	/**
	 * Entity callback method invoked after a domain object is saved.
	 * Can return either the same or a modified instance of the domain object.
	 *
	 * @param entity the domain object that was saved
	 * @param index must not be {@literal null}
	 * @return the domain object that was persisted
	 */
	T onAfterSave(T entity, IndexCoordinates index);
}
