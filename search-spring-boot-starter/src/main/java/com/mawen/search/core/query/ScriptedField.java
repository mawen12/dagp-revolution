package com.mawen.search.core.query;

import org.springframework.util.Assert;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class ScriptedField {
	private final String fieldName;
	private final ScriptData scriptData;

	/**
	 * @since 5.2
	 */
	public static ScriptedField of(String fieldName, ScriptData scriptData) {
		return new ScriptedField(fieldName, scriptData);
	}

	public ScriptedField(String fieldName, ScriptData scriptData) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(scriptData, "scriptData must not be null");

		this.fieldName = fieldName;
		this.scriptData = scriptData;
	}

	public String getFieldName() {
		return fieldName;
	}

	public ScriptData getScriptData() {
		return scriptData;
	}
}
