package com.mawen.query;

import com.mawen.search.core.annotation.QueryField;
import lombok.Data;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Data
public class PersonQuery {

	@QueryField(value = "name")
	private String name;
}
