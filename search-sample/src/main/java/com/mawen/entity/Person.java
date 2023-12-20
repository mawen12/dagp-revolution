package com.mawen.entity;

import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import lombok.Data;

import org.springframework.data.annotation.Id;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
@Data
@Document(indexName = "person")
public class Person {

	@Id
	@Field(value = "id")
	private String id;

	@Field(type = FieldType.Text, value = "name")
	private String name;
}
