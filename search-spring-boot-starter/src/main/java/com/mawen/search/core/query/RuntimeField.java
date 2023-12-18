package com.mawen.search.core.query;

import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class RuntimeField {
	private final String name;
	/**
	 * the type of the runtime field (long, keyword, etc.)
	 */
	private final String type;
	@Nullable
	private final String script;

	/**
	 * @since 5.2
	 */
	@Nullable
	Map<String, Object> params;

	public RuntimeField(String name, String type) {
		this(name, type, null, null);
	}

	public RuntimeField(String name, String type, String script) {
		this(name, type, script, null);
	}

	public RuntimeField(String name, String type, @Nullable String script, @Nullable Map<String, Object> params) {

		Assert.notNull(name, "name must not be null");
		Assert.notNull(type, "type must not be null");

		this.name = name;
		this.type = type;
		this.script = script;
		this.params = params;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the mapping as a Map like it is needed for the Elasticsearch client
	 */
	public Map<String, Object> getMapping() {
		Map<String, Object> map = new HashMap<>();
		map.put("type", type);

		if (script != null) {
			map.put("script", script);
		}
		return map;
	}

	/**
	 * @since 4.4
	 */
	public String getType() {
		return type;
	}

	/**
	 * @since 4.4
	 */
	public @Nullable String getScript() {
		return script;
	}

	/**
	 * @since 5.2
	 */
	@Nullable
	public Map<String, Object> getParams() {
		return params;
	}
}
