package com.mawen.search.core.mapping;

/**
 * Interface defining methods to convert the value of an entity-property to a value in Elasticsearch and back.
 * 定义了将实体属性值转换为 Elasticsearch 中的值，或反向转换方法的接口
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface PropertyValueConverter {

	/**
	 * 将实体属性值转换为 Elasticsearch 中的值。如果转换器无法转换值，则返回值的字符串表示。
	 *
	 * @param value 待转换的值，不能为空
	 * @return Elasticsearch 的值，不能为空
	 */
	Object write(Object value);

	/**
	 * 将 Elasticsearch 中的值转换为实体属性值。
	 *
	 * @param value 待转换的 Elasticsearch 中的值，不能为空
	 * @return 转换后的值，不能为空
	 */
	Object read(Object value);

}
