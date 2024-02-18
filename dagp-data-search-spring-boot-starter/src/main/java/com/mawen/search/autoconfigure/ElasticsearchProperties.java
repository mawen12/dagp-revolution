package com.mawen.search.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Setter
@Getter
@ConfigurationProperties("spring.elasticsearch")
public class ElasticsearchProperties {
	private final Restclient restclient = new Restclient();
	/**
	 * Comma-separated list of the Elasticsearch instances to use.
	 */
	private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));
	/**
	 * Username for authentication with Elasticsearch.
	 */
	private String username;
	/**
	 * Password for authentication with Elasticsearch.
	 */
	private String password;
	/**
	 * Connection timeout used when communicating with Elasticsearch.
	 */
	private Duration connectionTimeout = Duration.ofSeconds(1);
	/**
	 * Socket timeout used when communicating with Elasticsearch.
	 */
	private Duration socketTimeout = Duration.ofSeconds(30);
	/**
	 * Whether to enable socket keep alive between client and Elasticsearch.
	 */
	private boolean socketKeepAlive = false;
	/**
	 * Prefix added to the path of every request sent to Elasticsearch.
	 */
	private String pathPrefix;

	public static class Restclient {

		private final Sniffer sniffer = new Sniffer();

		public Sniffer getSniffer() {
			return this.sniffer;
		}

		public static class Sniffer {

			/**
			 * Interval between consecutive ordinary sniff executions.
			 */
			private Duration interval = Duration.ofMinutes(5);

			/**
			 * Delay of a sniff execution scheduled after a failure.
			 */
			private Duration delayAfterFailure = Duration.ofMinutes(1);

			public Duration getInterval() {
				return this.interval;
			}

			public void setInterval(Duration interval) {
				this.interval = interval;
			}

			public Duration getDelayAfterFailure() {
				return this.delayAfterFailure;
			}

			public void setDelayAfterFailure(Duration delayAfterFailure) {
				this.delayAfterFailure = delayAfterFailure;
			}
		}
	}
}
