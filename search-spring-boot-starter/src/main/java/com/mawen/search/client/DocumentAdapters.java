package com.mawen.search.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.get.GetResult;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.NestedIdentity;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import com.mawen.search.client.query.builder.SearchDocumentResponseBuilder;
import com.mawen.search.client.response.ResponseConverter;
import com.mawen.search.client.util.TypeUtils;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.document.NestedMetaData;
import com.mawen.search.core.document.SearchDocument;
import com.mawen.search.core.document.SearchDocumentAdapter;
import com.mawen.search.core.document.SearchDocumentResponse;
import com.mawen.search.core.support.MultiGetItem;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Slf4j
public class DocumentAdapters {


	private DocumentAdapters() {
	}


	public static SearchDocument from(Hit<?> hit, JsonpMapper jsonpMapper) {

		Assert.notNull(hit, "hit must not be null");

		Map<String, List<String>> highlightFields = hit.highlight();

		Map<String, SearchDocumentResponse> innerHits = new LinkedHashMap<>();
		hit.innerHits().forEach((name, innerHitsResult) -> {
			// noinspection ReturnOfNull
			innerHits.put(name, SearchDocumentResponseBuilder.from(innerHitsResult.hits(), null, null, null,
					searchDocument -> null, jsonpMapper));
		});

		NestedMetaData nestedMetaData = from(hit.nested());

		List<String> matchedQueries = hit.matchedQueries();

		Function<Map<String, JsonData>, EntityAsMap> fromFields = fields -> {
			StringBuilder sb = new StringBuilder("{");
			final boolean[] firstField = {true};
			hit.fields().forEach((key, jsonData) -> {
				if (!firstField[0]) {
					sb.append(',');
				}
				sb.append('"').append(key).append("\":") //
						.append(jsonData.toJson(jsonpMapper).toString());
				firstField[0] = false;
			});
			sb.append('}');
			return new EntityAsMap().fromJson(sb.toString());
		};

		EntityAsMap hitFieldsAsMap = fromFields.apply(hit.fields());

		Map<String, List<Object>> documentFields = new LinkedHashMap<>();
		hitFieldsAsMap.forEach((key, value) -> {
			if (value instanceof List) {
				// noinspection unchecked
				documentFields.put(key, (List<Object>) value);
			}
			else {
				documentFields.put(key, Collections.singletonList(value));
			}
		});

		Document document;
		Object source = hit.source();
		if (source == null) {
			document = Document.from(hitFieldsAsMap);
		}
		else {
			if (source instanceof EntityAsMap) {
				EntityAsMap entityAsMap = (EntityAsMap) source;
				document = Document.from(entityAsMap);
			}
			else if (source instanceof JsonData) {
				JsonData jsonData = (JsonData) source;
				document = Document.from(jsonData.to(EntityAsMap.class));
			}
			else {

				if (log.isWarnEnabled()) {
					log.warn(String.format("Cannot map from type " + source.getClass().getName()));
				}
				document = Document.create();
			}
		}
		document.setIndex(hit.index());
		document.setId(hit.id());

		if (hit.version() != null) {
			document.setVersion(hit.version());
		}
		document.setSeqNo(hit.seqNo() != null && hit.seqNo() >= 0 ? hit.seqNo() : -2); // -2 was the default value in the
		// old client
		document.setPrimaryTerm(hit.primaryTerm() != null && hit.primaryTerm() > 0 ? hit.primaryTerm() : 0);

		float score = hit.score() != null ? hit.score().floatValue() : Float.NaN;
		return new SearchDocumentAdapter(document, score, hit.sort().stream().map(TypeUtils::toString).toArray(),
				documentFields, highlightFields, innerHits, nestedMetaData, matchedQueries, hit.routing());
	}

	public static SearchDocument from(CompletionSuggestOption<EntityAsMap> completionSuggestOption) {

		Document document = completionSuggestOption.source() != null ? Document.from(completionSuggestOption.source())
				: Document.create();
		document.setIndex(completionSuggestOption.index());

		if (completionSuggestOption.id() != null) {
			document.setId(completionSuggestOption.id());
		}

		float score = completionSuggestOption.score() != null ? completionSuggestOption.score().floatValue() : Float.NaN;
		return new SearchDocumentAdapter(document, score, new Object[]{}, Collections.emptyMap(), Collections.emptyMap(),
				Collections.emptyMap(), null, null, completionSuggestOption.routing());
	}

	@Nullable
	private static NestedMetaData from(@Nullable NestedIdentity nestedIdentity) {

		if (nestedIdentity == null) {
			return null;
		}

		NestedMetaData child = from(nestedIdentity.nested());
		return NestedMetaData.of(nestedIdentity.field(), nestedIdentity.offset(), child);
	}

	/**
	 * Creates a {@link Document} from a {@link GetResponse} where the found document is contained as {@link EntityAsMap}.
	 *
	 * @param getResponse the response instance
	 * @return the Document
	 */
	@Nullable
	public static Document from(GetResult<EntityAsMap> getResponse) {

		Assert.notNull(getResponse, "getResponse must not be null");

		if (!getResponse.found()) {
			return null;
		}

		Document document = getResponse.source() != null ? Document.from(getResponse.source()) : Document.create();
		document.setIndex(getResponse.index());
		document.setId(getResponse.id());

		if (getResponse.version() != null) {
			document.setVersion(getResponse.version());
		}

		if (getResponse.seqNo() != null) {
			document.setSeqNo(getResponse.seqNo());
		}

		if (getResponse.primaryTerm() != null) {
			document.setPrimaryTerm(getResponse.primaryTerm());
		}

		return document;
	}

	/**
	 * Creates a list of {@link MultiGetItem}s from a {@link MgetResponse} where the data is contained as
	 * {@link EntityAsMap} instances.
	 *
	 * @param mgetResponse the response instance
	 * @return list of multiget items
	 */
	public static List<MultiGetItem<Document>> from(MgetResponse<EntityAsMap> mgetResponse) {

		Assert.notNull(mgetResponse, "mgetResponse must not be null");

		return mgetResponse.docs().stream() //
				.map(itemResponse -> MultiGetItem.of( //
						itemResponse.isFailure() ? null : from(itemResponse.result()), //
						ResponseConverter.getFailure(itemResponse)))
				.collect(Collectors.toList());
	}
}
