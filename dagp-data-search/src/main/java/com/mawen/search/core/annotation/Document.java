package com.mawen.search.core.annotation;

import java.lang.annotation.*;


/**
 * Identifies a domain object to be persisted to Elasticsearch.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Document {

	/**
	 * Name of the Elasticsearch index.
	 * <ul>
	 *     <li>Lowercase only</li>
	 *     <li>Cannot include \, /, *, ?, ", &gt;, &lt;, |, ` ` (space character), ,, #</li>
	 *     <li>Cannot start with -, _, +</li>
	 *     <li>Cannot be . or ..</li>
	 *     <li>Cannot be longer than 255 bytes ()</li>
	 * </ul>
	 */
	String indexName() default "";

	/**
	 * determine index is dynamic or not.
	 * if the index is dynamic, it will read index from {@link IndexName} in Domain Object field or method.
	 * otherwise will use {@link #indexName()} directly.
	 */
	boolean dynamicIndex() default false;

	VersionType versionType() default VersionType.EXTERNAL;

	enum VersionType {
		INTERNAL("internal"), //
		EXTERNAL("external"), //
		EXTERNAL_GTE("external_gte"), //
		FORCE("force");

		private final String esName;

		VersionType(String esName) {
			this.esName = esName;
		}

		public String getEsName() {
			return esName;
		}
	}
}
