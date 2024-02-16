package com.mawen.search.client;

import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.mawen.search.core.IndexOperations;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class IndicesTemplate extends ChildTemplate<ElasticsearchTransport, ElasticsearchIndicesClient>
		implements IndexOperations {

	protected final ElasticsearchConverter elasticsearchConverter;
	@Nullable
	protected final Class<?> boundClass;
	@Nullable protected final IndexCoordinates boundIndex;

	public IndicesTemplate(ElasticsearchIndicesClient client,
			ElasticsearchConverter elasticsearchConverter, Class<?> boundClass) {
		super(client, elasticsearchConverter);

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null");
		Assert.notNull(boundClass, "boundClass may not be null");

		this.elasticsearchConverter = elasticsearchConverter;
		this.boundClass = boundClass;
		this.boundIndex = null;
	}

	public IndicesTemplate(ElasticsearchIndicesClient client,
			ElasticsearchConverter elasticsearchConverter, IndexCoordinates boundIndex) {
		super(client, elasticsearchConverter);

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null");
		Assert.notNull(boundIndex, "boundIndex must not be null");

		this.elasticsearchConverter = elasticsearchConverter;
		this.boundClass = null;
		this.boundIndex = boundIndex;
	}



	@Override
	public boolean delete() {
		return doDelete(getIndexCoordinates());
	}

	private boolean doDelete(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		if (doExists(indexCoordinates)) {
			DeleteIndexRequest deleteIndexRequest = requestConverter.indicesDeleteRequest(indexCoordinates);
			DeleteIndexResponse deleteIndexResponse = execute(client -> client.delete(deleteIndexRequest));
			return deleteIndexResponse.acknowledged();
		}

		return false;
	}

	@Override
	public boolean exists() {
		return doExists(getIndexCoordinates());
	}

	private boolean doExists(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		ExistsRequest existsRequest = requestConverter.indicesExistsRequest(indexCoordinates);
		BooleanResponse existsResponse = execute(client -> client.exists(existsRequest));
		return existsResponse.value();
	}

	@Override
	public void refresh() {
		refresh(getIndexCoordinates());
	}

	@Override
	public void refresh(IndexCoordinates index) {
		RefreshRequest refreshRequest = requestConverter.indicesRefreshRequest(index);
		execute(client -> client.refresh(refreshRequest));
	}

	@Override
	public IndexCoordinates getIndexCoordinates() {
		return (boundClass != null) ? getIndexCoordinatesFor(boundClass) : Objects.requireNonNull(boundIndex);
	}

	public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return getRequiredPersistentEntity(clazz).getIndexCoordinates();
	}

	ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}
}
