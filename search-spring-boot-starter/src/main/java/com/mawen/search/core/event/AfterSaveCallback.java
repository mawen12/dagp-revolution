package com.mawen.search.core.event;

import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.mapping.callback.EntityCallback;

/**
 * Entity callback triggered after save of an entity
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@FunctionalInterface
public interface AfterSaveCallback<T> extends EntityCallback<T> {

	T onAfterSave(T entity, IndexCoordinates index);
}
