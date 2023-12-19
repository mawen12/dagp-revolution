package com.mawen.search.repository.query;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchParameter extends Parameter {

	ElasticsearchParameter(MethodParameter parameter) {
		super(parameter);
	}

	@Override
	public boolean isSpecialParameter() {
		return super.isSpecialParameter();
	}


}
