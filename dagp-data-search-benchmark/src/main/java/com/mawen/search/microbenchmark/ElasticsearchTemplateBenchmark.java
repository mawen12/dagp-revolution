package com.mawen.search.microbenchmark;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.mawen.search.client.ElasticsearchTemplate;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.builder.IndexQueryBuilder;
import com.mawen.search.microbenchmark.support.AbstractMicrobenchmark;
import com.mawen.search.microbenchmark.support.RestClientHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.core.Set;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import org.springframework.data.annotation.Id;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/2/18
 */
public class ElasticsearchTemplateBenchmark extends AbstractMicrobenchmark {
	private static final String INDEX_PREFIX = "benchmark";

	private ElasticsearchClient elasticsearchClient;
	private ElasticsearchTemplate elasticsearchTemplate;

	@Setup
	public void setUp() {
		RestClient restClient = RestClientHelper.localDefault();
		JsonpMapper jsonpMapper = new JacksonJsonpMapper();
		ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper, null);
		this.elasticsearchClient = new ElasticsearchClient(transport);

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(Set.of(Person.class));
		ElasticsearchConverter elasticsearchConverter = new MappingElasticsearchConverter(mappingContext);
		this.elasticsearchTemplate = new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
	}

	@TearDown
	public void tearDown() throws IOException {
		elasticsearchClient.indices().delete(DeleteIndexRequest.of(b -> b.index(INDEX_PREFIX)));
		elasticsearchClient.shutdown();
	}

	@Benchmark
	public void clientSingle() throws IOException {
		elasticsearchClient.index(indexRequest(getPerson(0)));
	}

	@Benchmark
	public void clientBatchSeq() throws IOException {
		List<BulkOperation> ops = IntStream.of(0, 1000).mapToObj(i -> bulkOptions(getPerson(i))).collect(Collectors.toList());
		elasticsearchClient.bulk(BulkRequest.of(b -> b.operations(ops)));
	}

	@Benchmark
	public void clientBatchSeqWithoutId() throws IOException {
		List<BulkOperation> ops = IntStream.of(0, 1000).mapToObj(i -> {
			Person person = getPerson(i);
			person.setId(null);
			return bulkOptions(person);
		}).collect(Collectors.toList());
		elasticsearchClient.bulk(BulkRequest.of(b -> b.operations(ops)));
	}

	@Benchmark
	public void clientBatchPara() throws IOException {
		List<BulkOperation> ops = IntStream.of(0, 1000).parallel().mapToObj(i -> bulkOptions(getPerson(i))).collect(Collectors.toList());
		elasticsearchClient.bulk(BulkRequest.of(b -> b.operations(ops)));
	}

	@Benchmark
	public void templateSingle() {
		elasticsearchTemplate.index(indexQuery(getPerson(0)), IndexCoordinates.of(INDEX_PREFIX));
	}

	@Benchmark
	public void templateBatchSeq() {
		List<IndexQuery> queries = IntStream.of(0, 1000).mapToObj(i -> indexQuery(getPerson(i))).collect(Collectors.toList());
		elasticsearchTemplate.bulkIndex(queries, Person.class);
	}

	@Benchmark
	public void templateBatchPara() {
		List<IndexQuery> queries = IntStream.of(0, 1000).parallel().mapToObj(i -> indexQuery(getPerson(i))).collect(Collectors.toList());
		elasticsearchTemplate.bulkIndex(queries, Person.class);
	}

	public Person getPerson(int i) {
		return new Person(UUID.randomUUID().toString(), "jack" + i, i, true);
	}

	public IndexRequest<Person> indexRequest(Person person) {
		return IndexRequest.of(b -> b
				.index(INDEX_PREFIX)
				.id(person.getId())
				.document(person)
		);
	}

	public BulkOperation bulkOptions(Person person) {
		return BulkOperation.of(b -> b
				.index(indexOperation(person))
		);
	}

	public IndexOperation<Person> indexOperation(Person person) {
		return IndexOperation.of(b -> b
				.index(INDEX_PREFIX)
				.id(person.getId())
				.document(person)
		);
	}

	public IndexQuery indexQuery(Person person) {
		return new IndexQueryBuilder()
				.withId(person.getId())
				.withIndex(INDEX_PREFIX)
				.withObject(person)
				.build();
	}

	@Data
	@Accessors(chain = true)
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Person {

		@Id
		private String id;

		@Field(type = FieldType.Keyword)
		private String name;

		@Field(type = FieldType.Integer)
		private Integer age;

		@Field(type = FieldType.Boolean)
		private Boolean gender;

	}
}
