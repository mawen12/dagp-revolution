package com.mawen.entity;

import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.IndexName;
import lombok.Data;

import org.springframework.data.annotation.Id;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Data
@Document(dynamicIndex = true)
public class DynamicPerson {

	@Id
	private Long id;

	@Field("name")
	private String name;

	@IndexName
	private String index;
}
