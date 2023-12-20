package com.mawen.search.client.util;

import java.io.StringReader;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.Slices;
import co.elastic.clients.elasticsearch._types.SortMode;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.search.BoundaryScanner;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.core.search.HighlighterFragmenter;
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder;
import co.elastic.clients.elasticsearch.core.search.HighlighterTagsSchema;
import co.elastic.clients.elasticsearch.core.search.HighlighterType;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.domain.Order;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.UpdateResponse;
import com.mawen.search.core.refresh.RefreshPolicy;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class TypeUtils {

	@Nullable
	public static BoundaryScanner boundaryScanner(@Nullable String value) {

		if (value != null) {
			switch (value.toLowerCase()) {
				case "chars":
					return BoundaryScanner.Chars;
				case "sentence":
					return BoundaryScanner.Sentence;
				case "word":
					return BoundaryScanner.Word;
				default:
					return null;
			}
		}
		return null;
	}

	@Nullable
	public static FieldType fieldType(String type) {

		for (FieldType fieldType : FieldType.values()) {

			if (fieldType.jsonValue().equals(type)) {
				return fieldType;
			}
		}
		return null;
	}

	@Nullable
	public static String toString(@Nullable FieldValue fieldValue) {

		if (fieldValue == null) {
			return null;
		}

		switch (fieldValue._kind()) {
			case Double: {
				return String.valueOf(fieldValue.doubleValue());
			}
			case Long: {
				return String.valueOf(fieldValue.longValue());
			}
			case Boolean: {
				return String.valueOf(fieldValue.booleanValue());
			}
			case String: {
				return fieldValue.stringValue();
			}
			case Null: {
				return null;
			}
			case Any: {
				return fieldValue.anyValue().toString();
			}

			default:
				throw new IllegalStateException("Unexpected value: " + fieldValue._kind());
		}
	}

	@Nullable
	static Object toObject(@Nullable FieldValue fieldValue) {

		if (fieldValue == null) {
			return null;
		}

		switch (fieldValue._kind()) {
			case Double: {
				return Double.valueOf(fieldValue.doubleValue());
			}
			case Long: {
				return Long.valueOf(fieldValue.longValue());
			}
			case Boolean: {
				return Boolean.valueOf(fieldValue.booleanValue());
			}
			case String: {
				return fieldValue.stringValue();
			}
			case Null: {
				return null;
			}
			case Any: {
				return fieldValue.anyValue().toString();
			}

			default:
				throw new IllegalStateException("Unexpected value: " + fieldValue._kind());
		}
	}

	@Nullable
	public static FieldValue toFieldValue(@Nullable Object fieldValue) {

		if (fieldValue == null) {
			return FieldValue.NULL;
		}

		if (fieldValue instanceof Boolean) {
			return (Boolean) fieldValue ? FieldValue.TRUE : FieldValue.FALSE;
		}

		if (fieldValue instanceof String) {
			return FieldValue.of((String) fieldValue);
		}

		if (fieldValue instanceof Long) {
			return FieldValue.of((Long) fieldValue);
		}

		if (fieldValue instanceof Integer) {
			return FieldValue.of((long) (Integer) fieldValue);
		}

		if (fieldValue instanceof Double) {
			return FieldValue.of((Double) fieldValue);
		}

		if (fieldValue instanceof Float) {
			return FieldValue.of((double) (Float) fieldValue);
		}

		return FieldValue.of(JsonData.of(fieldValue));
	}

	@Nullable
	static SortOrder sortOrder(@Nullable Sort.Direction direction) {

		if (direction == null) {
			return null;
		}

		switch (direction) {
			case ASC:
				return SortOrder.Asc;
			case DESC:
				return SortOrder.Desc;
		}

		return null;
	}

	@Nullable
	public static HighlighterFragmenter highlighterFragmenter(@Nullable String value) {

		if (value != null) {
			switch (value.toLowerCase()) {
				case "simple":
					return HighlighterFragmenter.Simple;
				case "span":
					return HighlighterFragmenter.Span;
				default:
					return null;
			}
		}

		return null;
	}

	@Nullable
	public static HighlighterOrder highlighterOrder(@Nullable String value) {

		if (value != null && ("score".equals(value.toLowerCase()))) {
			return HighlighterOrder.Score;
		}

		return null;
	}

	@Nullable
	public static HighlighterType highlighterType(@Nullable String value) {

		if (value != null) {
			switch (value.toLowerCase()) {
				case "unified":
					return HighlighterType.Unified;
				case "plain":
					return HighlighterType.Plain;
				case "fvh":
					return HighlighterType.FastVector;
				default:
					return null;
			}
		}

		return null;
	}

	@Nullable
	public static HighlighterEncoder highlighterEncoder(@Nullable String value) {

		if (value != null) {
			switch (value.toLowerCase()) {
				case "default":
					return HighlighterEncoder.Default;
				case "html":
					return HighlighterEncoder.Html;
				default:
					return null;
			}
		}

		return null;
	}

	@Nullable
	public static HighlighterTagsSchema highlighterTagsSchema(@Nullable String value) {

		if (value != null) {
			if ("styled".equals(value.toLowerCase())) {
				return HighlighterTagsSchema.Styled;
			}
		}

		return null;
	}

	@Nullable
	static OpType opType(@Nullable IndexQuery.OpType opType) {

		if (opType != null) {
			switch (opType) {
				case INDEX:
					return OpType.Index;
				case CREATE:
					return OpType.Create;
			}
			;
		}
		return null;
	}

	public static Refresh refresh(@Nullable RefreshPolicy refreshPolicy) {

		if (refreshPolicy == null) {
			return Refresh.False;
		}

		switch (refreshPolicy) {
			case IMMEDIATE:
				return Refresh.True;
			case WAIT_UNTIL:
				return Refresh.WaitFor;
			case NONE:
				return Refresh.False;
		}

		return Refresh.False;
	}

	@Nullable
	public static UpdateResponse.Result result(@Nullable Result result) {

		if (result == null) {
			return null;
		}

		switch (result) {
			case Created:
				return UpdateResponse.Result.CREATED;
			case Updated:
				return UpdateResponse.Result.UPDATED;
			case Deleted:
				return UpdateResponse.Result.DELETED;
			case NotFound:
				return UpdateResponse.Result.NOT_FOUND;
			case NoOp:
				return UpdateResponse.Result.NOOP;
		}

		return null;
	}

	@Nullable
	public static SearchType searchType(@Nullable Query.SearchType searchType) {

		if (searchType == null) {
			return null;
		}

		switch (searchType) {
			case QUERY_THEN_FETCH:
				return SearchType.QueryThenFetch;
			case DFS_QUERY_THEN_FETCH:
				return SearchType.DfsQueryThenFetch;
		}

		return null;
	}

	@Nullable
	public static Slices slices(@Nullable Long count) {

		if (count == null) {
			return null;
		}

		return Slices.of(s -> s.value(Math.toIntExact(count)));
	}

	@Nullable
	public static SortMode sortMode(Order.Mode mode) {

		switch (mode) {
			case min:
				return SortMode.Min;
			case max:
				return SortMode.Max;
			case median:
				return SortMode.Median;
			case avg:
				return SortMode.Avg;
		}

		return null;
	}

	@Nullable
	public static Time time(@Nullable Duration duration) {

		if (duration == null) {
			return null;
		}

		return Time.of(t -> t.time(duration.toMillis() + "ms"));
	}

	@Nullable
	public static String timeStringMs(@Nullable Duration duration) {

		if (duration == null) {
			return null;
		}

		return duration.toMillis() + "ms";
	}

	public static Integer waitForActiveShardsCount(@Nullable String value) {
		// values taken from the RHLC implementation
		if (value == null) {
			return -2;
		}
		else if ("all".equals(value.toUpperCase())) {
			return -1;
		}
		else {
			try {
				return Integer.parseInt(value);
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegale value for waitForActiveShards" + value);
			}
		}
	}

	@Nullable
	public static Float toFloat(@Nullable Long value) {
		return value != null ? Float.valueOf(value) : null;
	}

	@Nullable
	public static TypeMapping typeMapping(@Nullable Document mapping) {
		if (mapping != null) {
			return TypeMapping.of(b -> b.withJson(new StringReader(mapping.toJson())));
		}
		return null;
	}

	@Nullable
	public static Document typeMapping(@Nullable TypeMapping typeMapping) {
		return (typeMapping != null) ? Document.parse(removePrefixFromJson(typeMapping.toString())) : null;
	}

	public static String removePrefixFromJson(String jsonWithPrefix) {
		return jsonWithPrefix.substring(jsonWithPrefix.indexOf("{"));
	}

	@Nullable
	public static IndexSettings indexSettings(@Nullable Map<String, Object> settings) {
		return settings != null ? IndexSettings.of(b -> b.withJson(new StringReader(Document.from(settings).toJson())))
				: null;
	}

	public static Map<String, JsonData> paramsMap(Map<String, Object> params) {

		Assert.notNull(params, "params must not be null");

		Map<String, JsonData> mappedParams = new LinkedHashMap<>();
		params.forEach((key, value) -> {
			mappedParams.put(key, JsonData.of(value));
		});
		return mappedParams;
	}
}
