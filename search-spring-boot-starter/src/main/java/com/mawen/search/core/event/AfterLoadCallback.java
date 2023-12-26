package com.mawen.search.core.event;

import com.mawen.search.core.document.Document;
import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.mapping.callback.EntityCallback;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface AfterLoadCallback<T> extends EntityCallback<Document> {

	Document onAfterLoad(Document document, Class<T> type, IndexCoordinates index);
}
