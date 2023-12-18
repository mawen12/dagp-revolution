package com.mawen.search.core.mapping;

/**
 * Interface defining methods to convert the value of an entity-property to a value in Elasticsearch and back.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface PropertyValueConverter {

	/**
	 * Converts a property value to an elasticsearch value.
	 * If the converter cannot convert the value, it must be return a string representation.
	 *
	 * @param value the property value to convert, must not be {@literal null}
	 * @return The elasticsearch property value, must not be {@literal null}
	 */
	Object write(Object value);

	/**
	 * Converts an elasticsearch value to a property value.
	 *
	 * @param value the elasticsearch value to convert, must not be {@literal null}
	 * @return The converted value, must not be {@literal null}
	 */
	Object read(Object value);

}
