package com.mawen.search.core.mapping;

import com.mawen.search.core.annotation.Document;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface ElasticsearchPersistentEntity<T> extends PersistentEntity<T, ElasticsearchPersistentProperty> {

	IndexCoordinates getIndexCoordinates();

	boolean isDynamicIndex();

	ElasticsearchPersistentProperty getPersistentPropertyWithFieldName(String fieldName);

	boolean hasSeqNoPrimaryTermProperty();

	ElasticsearchPersistentProperty getSeqNoPrimaryTermProperty();

	default ElasticsearchPersistentProperty getRequiredSeqNoPrimaryTermProperty() {

		ElasticsearchPersistentProperty seqNoPrimaryTermProperty = this.getSeqNoPrimaryTermProperty();

		Assert.isTrue(seqNoPrimaryTermProperty != null, String.format("Required SeqNoPrimaryTerm property not found for %s", this.getType()));

		return seqNoPrimaryTermProperty;
	}

	ElasticsearchPersistentProperty getIndexNameProperty();

	default ElasticsearchPersistentProperty getRequiredIndexNameProperty() {

		ElasticsearchPersistentProperty indexNameProperty = this.getIndexNameProperty();

		Assert.isTrue(indexNameProperty != null, String.format("Required IndexName property not found for %s", this.getType()));

		return indexNameProperty;
	}

	Document.VersionType getVersionType();

	@Nullable
	String resolveRouting(T bean);
}
