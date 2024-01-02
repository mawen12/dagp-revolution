/*
 * Copyright 2014-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mawen.search.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.mawen.search.core.annotation.DateFormat;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
public abstract class LogEntityIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() throws ParseException {

		indexNameProvider.increment();

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		IndexQuery indexQuery1 = new LogEntityBuilder("1").action("update").date(dateFormatter.parse("2013-10-18 18:01"))
				.code(2).ip("10.10.10.1").buildIndex();

		IndexQuery indexQuery2 = new LogEntityBuilder("2").action("update").date(dateFormatter.parse("2013-10-19 18:02"))
				.code(2).ip("10.10.10.2").buildIndex();

		IndexQuery indexQuery3 = new LogEntityBuilder("3").action("update").date(dateFormatter.parse("2013-10-19 18:03"))
				.code(2).ip("10.10.10.3").buildIndex();

		IndexQuery indexQuery4 = new LogEntityBuilder("4").action("update").date(dateFormatter.parse("2013-10-19 18:04"))
				.code(2).ip("10.10.10.4").buildIndex();

		operations.bulkIndex(Arrays.asList(indexQuery1, indexQuery2, indexQuery3, indexQuery4),
				IndexCoordinates.of(indexNameProvider.indexName()));
	}

	/**
	 * Creates a Query that will be executed as term query on the "ip" field
	 *
	 * @param ip the parameter for the query
	 * @return Query instance
	 */
	abstract Query termQueryForIp(String ip);

	/**
	 * Creates a Query that will be executed as range query on the "ip" field
	 *
	 * @param from the parameter for the query
	 * @param to the parameter for the query
	 * @return Query instance
	 */
	abstract Query rangeQueryForIp(String from, String to);

	@Test
	public void shouldIndexGivenLogEntityWithIPFieldType() {

		Query searchQuery = termQueryForIp("10.10.10.1");
		SearchHits<LogEntity> entities = operations.search(searchQuery, LogEntity.class);

		assertThat(entities).isNotNull().hasSize(1);
	}

	// TODO by mawen 自动生成的 mapping 中对应的字段类型不是 ip
//	@Test // DATAES-66
//	public void shouldThrowExceptionWhenInvalidIPGivenForSearchQuery() {
//
//		Query searchQuery = termQueryForIp("10.10.10");
//
//		operations.search(searchQuery, LogEntity.class);
//
//		assertThatThrownBy(() -> {
//			SearchHits<LogEntity> entities = operations.search(searchQuery, LogEntity.class);
//		}).isInstanceOf(DataAccessException.class);
//	}

	@Test
	public void shouldReturnLogsForGivenIPRanges() {

		Query searchQuery = rangeQueryForIp("10.10.10.1", "10.10.10.3");
		SearchHits<LogEntity> entities = operations.search(searchQuery, LogEntity.class);

		assertThat(entities).isNotNull().hasSize(3);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class LogEntity {

		private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		@Nullable
		@Id private String id;
		@Nullable private String action;
		@Nullable private long sequenceCode;
		@Nullable
		@Field(type = FieldType.Ip) private String ip;
		@Nullable
		@Field(type = FieldType.Date, format = DateFormat.date_time) private Date date;

		private LogEntity() {}

		public LogEntity(String id) {
			this.id = id;
		}

		public static SimpleDateFormat getFormat() {
			return format;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public long getSequenceCode() {
			return sequenceCode;
		}

		public void setSequenceCode(long sequenceCode) {
			this.sequenceCode = sequenceCode;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

	static class LogEntityBuilder {

		private final LogEntity result;

		public LogEntityBuilder(String id) {
			result = new LogEntity(id);
		}

		public LogEntityBuilder action(String action) {
			result.setAction(action);
			return this;
		}

		public LogEntityBuilder code(long sequenceCode) {
			result.setSequenceCode(sequenceCode);
			return this;
		}

		public LogEntityBuilder date(Date date) {
			result.setDate(date);
			return this;
		}

		public LogEntityBuilder ip(String ip) {
			result.setIp(ip);
			return this;
		}

		public LogEntity build() {
			return result;
		}

		public IndexQuery buildIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(result.getId());
			indexQuery.setObject(result);
			return indexQuery;
		}
	}
}
