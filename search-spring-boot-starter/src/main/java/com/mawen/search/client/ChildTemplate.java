package com.mawen.search.client;

import java.io.IOException;

import co.elastic.clients.ApiClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.Transport;
import com.mawen.search.client.request.RequestConverter;
import com.mawen.search.client.response.ResponseConverter;
import com.mawen.search.core.convert.ElasticsearchConverter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public abstract class ChildTemplate<T extends Transport, CLIENT extends ApiClient<T, CLIENT>> {

	protected final CLIENT client;
	protected final RequestConverter requestConverter;
	protected final ResponseConverter responseConverter;
	protected final ElasticsearchExceptionTranslator exceptionTranslator;

	public ChildTemplate(CLIENT client, ElasticsearchConverter elasticsearchConverter) {

		this.client = client;
		JsonpMapper jsonpMapper = client._transport().jsonpMapper();
		this.requestConverter = new RequestConverter(elasticsearchConverter, jsonpMapper);
		this.responseConverter = new ResponseConverter();
		exceptionTranslator = new ElasticsearchExceptionTranslator();
	}

	@FunctionalInterface
	public interface ClientCallback<CLIENT, RESULT> {
		RESULT doWithClient(CLIENT client) throws IOException;
	}

	public <RESULT> RESULT execute(ClientCallback<CLIENT, RESULT> callback) {

		Assert.notNull(callback, "callback must not be null");

		try {
			return callback.doWithClient(client);
		} catch (IOException | RuntimeException e) {
			throw exceptionTranslator.translateException(e);
		}
	}

}
