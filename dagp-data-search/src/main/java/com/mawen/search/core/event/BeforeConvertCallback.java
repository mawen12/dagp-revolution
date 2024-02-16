package com.mawen.search.core.event;

import com.mawen.search.core.mapping.IndexCoordinates;
import org.springframework.data.mapping.callback.EntityCallback;

/**
 * Calling being invoked before a domain object is converted to be persisted.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface BeforeConvertCallback<T> extends EntityCallback<T> {

	T onBeforeConvert(T entity, IndexCoordinates index);

}
