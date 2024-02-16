/*
 * Copyright 2021-2023 the original author or authors.
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
package com.mawen.search.test.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.Version;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.mawen.search.test.client.support.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@SuppressWarnings("unused")
public final class ElasticsearchClients {

	private static final String X_SPRING_DATA_ELASTICSEARCH_CLIENT = "X-SpringDataElasticsearch-Client";
	public static final String IMPERATIVE_CLIENT = "imperative";
	public static final String REACTIVE_CLIENT = "reactive";

	private static final JsonpMapper DEFAULT_JSONP_MAPPER = new JacksonJsonpMapper();


	// region imperative client

	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration) {
		return createImperative(getRestClient(clientConfiguration), null, DEFAULT_JSONP_MAPPER);
	}

	public static ElasticsearchClient createImperative(ClientConfiguration clientConfiguration,
			TransportOptions transportOptions) {
		return createImperative(getRestClient(clientConfiguration), transportOptions, DEFAULT_JSONP_MAPPER);
	}

	public static ElasticsearchClient createImperative(RestClient restClient) {
		return createImperative(restClient, null, DEFAULT_JSONP_MAPPER);
	}

	public static ElasticsearchClient createImperative(RestClient restClient, @Nullable TransportOptions transportOptions,
			JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");

		ElasticsearchTransport transport = getElasticsearchTransport(restClient, IMPERATIVE_CLIENT, transportOptions,
				jsonpMapper);

		return createImperative(transport);
	}

	public static AutoCloseableElasticsearchClient createImperative(ElasticsearchTransport transport) {

		Assert.notNull(transport, "transport must not be null");

		return new AutoCloseableElasticsearchClient(transport);
	}
	// endregion

	// region low level RestClient
	private static RestClientOptions.Builder getRestClientOptionsBuilder(@Nullable TransportOptions transportOptions) {

		if (transportOptions instanceof RestClientOptions) {
			RestClientOptions restClientOptions = (RestClientOptions) transportOptions;
			return restClientOptions.toBuilder();
		}

		RestClientOptions.Builder builder = new RestClientOptions.Builder(RequestOptions.DEFAULT.toBuilder());

		if (transportOptions != null) {
			transportOptions.headers().forEach(header -> builder.addHeader(header.getKey(), header.getValue()));
			transportOptions.queryParameters().forEach(builder::setParameter);
			builder.onWarnings(transportOptions.onWarnings());
		}

		return builder;
	}

	public static RestClient getRestClient(ClientConfiguration clientConfiguration) {
		return getRestClientBuilder(clientConfiguration).build();
	}

	private static RestClientBuilder getRestClientBuilder(ClientConfiguration clientConfiguration) {
		HttpHost[] httpHosts = formattedHosts(clientConfiguration.getEndpoints(), clientConfiguration.useSsl()).stream()
				.map(HttpHost::create).toArray(HttpHost[]::new);
		RestClientBuilder builder = RestClient.builder(httpHosts);

		if (clientConfiguration.getPathPrefix() != null) {
			builder.setPathPrefix(clientConfiguration.getPathPrefix());
		}

		HttpHeaders headers = clientConfiguration.getDefaultHeaders();

		if (!headers.isEmpty()) {
			builder.setDefaultHeaders(toHeaderArray(headers));
		}

		builder.setHttpClientConfigCallback(clientBuilder -> {
			if (clientConfiguration.getCaFingerprint().isPresent()) {
				clientBuilder
						.setSSLContext(TransportUtils.sslContextFromCaFingerprint(clientConfiguration.getCaFingerprint().get()));
			}
			clientConfiguration.getSslContext().ifPresent(clientBuilder::setSSLContext);
			clientConfiguration.getHostNameVerifier().ifPresent(clientBuilder::setSSLHostnameVerifier);
			clientBuilder.addInterceptorLast(new CustomHeaderInjector(clientConfiguration.getHeadersSupplier()));

			RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
			Duration connectTimeout = clientConfiguration.getConnectTimeout();

			if (!connectTimeout.isNegative()) {
				requestConfigBuilder.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
			}

			Duration socketTimeout = clientConfiguration.getSocketTimeout();

			if (!socketTimeout.isNegative()) {
				requestConfigBuilder.setSocketTimeout(Math.toIntExact(socketTimeout.toMillis()));
				requestConfigBuilder.setConnectionRequestTimeout(Math.toIntExact(socketTimeout.toMillis()));
			}

			clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

			clientConfiguration.getProxy().map(HttpHost::create).ifPresent(clientBuilder::setProxy);

			for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
					.getClientConfigurers()) {
				if (clientConfigurer instanceof ElasticsearchHttpClientConfigurationCallback) {
					ElasticsearchHttpClientConfigurationCallback restClientConfigurationCallback = (ElasticsearchHttpClientConfigurationCallback) clientConfigurer;
					clientBuilder = restClientConfigurationCallback.configure(clientBuilder);
				}
			}

			return clientBuilder;
		});

		for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurationCallback : clientConfiguration
				.getClientConfigurers()) {
			if (clientConfigurationCallback instanceof ElasticsearchRestClientConfigurationCallback) {
				ElasticsearchRestClientConfigurationCallback configurationCallback = (ElasticsearchRestClientConfigurationCallback) clientConfigurationCallback;
				builder = configurationCallback.configure(builder);
			}
		}
		return builder;
	}
	// endregion

	// region Elasticsearch transport

	public static ElasticsearchTransport getElasticsearchTransport(RestClient restClient, String clientType,
			@Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");
		Assert.notNull(clientType, "clientType must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		TransportOptions.Builder transportOptionsBuilder = transportOptions != null ? transportOptions.toBuilder()
				: new RestClientOptions(RequestOptions.DEFAULT).toBuilder();

		RestClientOptions.Builder restClientOptionsBuilder = getRestClientOptionsBuilder(transportOptions);

		ContentType jsonContentType = Version.VERSION == null ? ContentType.APPLICATION_JSON
				: ContentType.create("application/vnd.elasticsearch+json",
				new BasicNameValuePair("compatible-with", String.valueOf(Version.VERSION.major())));

		Consumer<String> setHeaderIfNotPresent = header -> {
			if (restClientOptionsBuilder.build().headers().stream() //
					.noneMatch((h) -> h.getKey().equalsIgnoreCase(header))) {
				// need to add the compatibility header, this is only done automatically when not passing in custom options.
				// code copied from RestClientTransport as it is not available outside the package
				restClientOptionsBuilder.addHeader(header, jsonContentType.toString());
			}
		};

		setHeaderIfNotPresent.accept("Content-Type");
		setHeaderIfNotPresent.accept("Accept");

		restClientOptionsBuilder.addHeader(X_SPRING_DATA_ELASTICSEARCH_CLIENT, clientType);

		return new RestClientTransport(restClient, jsonpMapper, restClientOptionsBuilder.build());
	}
	// endregion

	private static List<String> formattedHosts(List<InetSocketAddress> hosts, boolean useSsl) {
		return hosts.stream().map(it -> (useSsl ? "https" : "http") + "://" + it.getHostString() + ':' + it.getPort())
				.collect(Collectors.toList());
	}

	private static org.apache.http.Header[] toHeaderArray(HttpHeaders headers) {
		return headers.entrySet().stream() //
				.flatMap(entry -> entry.getValue().stream() //
						.map(value -> new BasicHeader(entry.getKey(), value))) //
				.toArray(org.apache.http.Header[]::new);
	}

	@Getter
	@AllArgsConstructor
	private static class CustomHeaderInjector implements HttpRequestInterceptor {

		private Supplier<HttpHeaders> headersSupplier;

		@Override
		public void process(HttpRequest request, HttpContext context) {
			HttpHeaders httpHeaders = headersSupplier.get();

			if (httpHeaders != null && !httpHeaders.isEmpty()) {
				Arrays.stream(toHeaderArray(httpHeaders)).forEach(request::addHeader);
			}
		}
	}

	public interface ElasticsearchHttpClientConfigurationCallback
			extends ClientConfiguration.ClientConfigurationCallback<HttpAsyncClientBuilder> {

		static ElasticsearchHttpClientConfigurationCallback from(
				Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder> httpClientBuilderCallback) {

			Assert.notNull(httpClientBuilderCallback, "httpClientBuilderCallback must not be null");

			return httpClientBuilderCallback::apply;
		}
	}

	public interface ElasticsearchRestClientConfigurationCallback
			extends ClientConfiguration.ClientConfigurationCallback<RestClientBuilder> {

		static ElasticsearchRestClientConfigurationCallback from(
				Function<RestClientBuilder, RestClientBuilder> restClientBuilderCallback) {

			Assert.notNull(restClientBuilderCallback, "restClientBuilderCallback must not be null");

			return restClientBuilderCallback::apply;
		}
	}
}
