package com.mawen.search.core.routing;

import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Routing;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.*;

@SpringJUnitConfig({ DefaultRoutingResolverUnitTests.Config.class })
class DefaultRoutingResolverUnitTests {

	@Autowired
	private ApplicationContext applicationContext;
	private SimpleElasticsearchMappingContext mappingContext;

	@Nullable
	private RoutingResolver routingResolver;

	@Configuration
	static class Config {
		@Bean
		SpelRouting spelRouting() {
			return new SpelRouting();
		}
	}

	@BeforeEach
	void setUp() {
		mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setApplicationContext(applicationContext);

		routingResolver = new DefaultRoutingResolver(mappingContext);
	}

	@Test // #1218
	@DisplayName("should throw an exception on unknown property")
	void shouldThrowAnExceptionOnUnknownProperty() {

		InvalidRoutingEntity entity = new InvalidRoutingEntity("42", "route 66");

		assertThatThrownBy(() -> routingResolver.getRouting(entity)).isInstanceOf(MappingException.class);
	}

	@Test // #1218
	@DisplayName("should return the routing from the entity")
	void shouldReturnTheRoutingFromTheEntity() {

		ValidRoutingEntity entity = new ValidRoutingEntity("42", "route 66");

		String routing = routingResolver.getRouting(entity);

		assertThat(routing).isEqualTo("route 66");
	}

	@Document(indexName = "routing-resolver-test")
	@Routing("theRouting")
	static class ValidRoutingEntity {
		@Nullable @Id
		private String id;
		@Nullable private String theRouting;

		public ValidRoutingEntity(@Nullable String id, @Nullable String theRouting) {
			this.id = id;
			this.theRouting = theRouting;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getTheRouting() {
			return theRouting;
		}

		public void setTheRouting(@Nullable String theRouting) {
			this.theRouting = theRouting;
		}
	}

	@Document(indexName = "routing-resolver-test")
	@Routing(value = "@spelRouting.getRouting(#entity)")
	static class ValidSpelRoutingEntity {
		@Nullable @Id private String id;
		@Nullable private String theRouting;

		public ValidSpelRoutingEntity(@Nullable String id, @Nullable String theRouting) {
			this.id = id;
			this.theRouting = theRouting;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getTheRouting() {
			return theRouting;
		}

		public void setTheRouting(@Nullable String theRouting) {
			this.theRouting = theRouting;
		}
	}

	@Document(indexName = "routing-resolver-test")
	@Routing("unknownProperty")
	static class InvalidRoutingEntity {
		@Nullable @Id private String id;
		@Nullable private String theRouting;

		public InvalidRoutingEntity(@Nullable String id, @Nullable String theRouting) {
			this.id = id;
			this.theRouting = theRouting;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getTheRouting() {
			return theRouting;
		}

		public void setTheRouting(@Nullable String theRouting) {
			this.theRouting = theRouting;
		}
	}

	static class SpelRouting {

		@Nullable
		public String getRouting(Object o) {

			if (o instanceof ValidSpelRoutingEntity) {
				return ((ValidSpelRoutingEntity) o).getTheRouting();
			}

			return null;
		}
	}
}