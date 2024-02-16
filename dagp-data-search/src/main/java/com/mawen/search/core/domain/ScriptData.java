package com.mawen.search.core.domain;

import com.mawen.search.core.support.ScriptType;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class ScriptData {

	@Nullable
	private final ScriptType type;
	@Nullable
	private final String language;
	@Nullable
	private final String script;
	@Nullable
	private final String scriptName;
	@Nullable
	private final Map<String, Object> params;

	public ScriptData(@Nullable ScriptType type, @Nullable String language, @Nullable String script, @Nullable String scriptName, @Nullable Map<String, Object> params) {

		Assert.notNull(type, "type must not be null");

		this.type = type;
		this.language = language;
		this.script = script;
		this.scriptName = scriptName;
		this.params = params;
	}
}
