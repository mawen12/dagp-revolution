package com.mawen.search.core.annotation;

import lombok.Getter;

/**
 * document field type mapped elasticsearch.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public enum FieldType {

	Auto("auto"), //
	Object("object"),
	Text("text"), //
	Keyword("keyword"), //
	Long("long"), //
	Integer("integer"), //
	Short("short"), //
	Byte("byte"), //
	Double("double"), //
	Float("float"), //
	Half_Float("half_float"), //
	Scaled_Float("scaled_float"), //
	Date("date"), //
	Date_Nanos("date_nanos"), //
	Boolean("boolean"), //
	Binary("binary"), //
	Integer_Range("integer_range"), //
	Float_Range("float_range"), //
	Long_Range("long_range"), //
	Double_Range("double_range"), //
	Date_Range("date_range"), //
	Nested("nested"), //
	Ip("ip")
	;

	private final String mappedName;

	FieldType(String mappedName) {
		this.mappedName = mappedName;
	}
}