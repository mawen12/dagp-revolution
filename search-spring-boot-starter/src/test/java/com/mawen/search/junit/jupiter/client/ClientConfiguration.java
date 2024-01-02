/*
 * Copyright 2018-2023 the original author or authors.
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
package com.mawen.search.junit.jupiter.client;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import com.mawen.search.junit.jupiter.client.support.HttpHeaders;

import org.springframework.lang.Nullable;


public interface ClientConfiguration {

	static ClientConfigurationBuilderWithRequiredEndpoint builder() {
		return new ClientConfigurationBuilder();
	}

	static ClientConfiguration localhost() {
		return new ClientConfigurationBuilder().connectedToLocalhost().build();
	}

	static ClientConfiguration create(String hostAndPort) {
		return new ClientConfigurationBuilder().connectedTo(hostAndPort).build();
	}

	static ClientConfiguration create(InetSocketAddress socketAddress) {
		return new ClientConfigurationBuilder().connectedTo(socketAddress).build();
	}

	List<InetSocketAddress> getEndpoints();

	HttpHeaders getDefaultHeaders();

	boolean useSsl();

	Optional<SSLContext> getSslContext();

	Optional<String> getCaFingerprint();

	Optional<HostnameVerifier> getHostNameVerifier();

	Duration getConnectTimeout();

	Duration getSocketTimeout();

	@Nullable
	String getPathPrefix();

	Optional<String> getProxy();

	<T> List<ClientConfigurationCallback<?>> getClientConfigurers();

	Supplier<HttpHeaders> getHeadersSupplier();


	interface ClientConfigurationBuilderWithRequiredEndpoint {

		default MaybeSecureClientConfigurationBuilder connectedTo(String hostAndPort) {
			return connectedTo(new String[] { hostAndPort });
		}

		MaybeSecureClientConfigurationBuilder connectedTo(String... hostAndPorts);

		default MaybeSecureClientConfigurationBuilder connectedTo(InetSocketAddress endpoint) {
			return connectedTo(new InetSocketAddress[] { endpoint });
		}

		MaybeSecureClientConfigurationBuilder connectedTo(InetSocketAddress... endpoints);

		default MaybeSecureClientConfigurationBuilder connectedToLocalhost() {
			return connectedTo("localhost:9200");
		}
	}

	interface MaybeSecureClientConfigurationBuilder extends TerminalClientConfigurationBuilder {

		TerminalClientConfigurationBuilder usingSsl();

		TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext);

		TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext, HostnameVerifier hostnameVerifier);

		TerminalClientConfigurationBuilder usingSsl(String caFingerprint);
	}


	interface TerminalClientConfigurationBuilder {

		TerminalClientConfigurationBuilder withDefaultHeaders(HttpHeaders defaultHeaders);

		default TerminalClientConfigurationBuilder withConnectTimeout(long millis) {
			return withConnectTimeout(Duration.ofMillis(millis));
		}

		TerminalClientConfigurationBuilder withConnectTimeout(Duration timeout);

		default TerminalClientConfigurationBuilder withSocketTimeout(long millis) {
			return withSocketTimeout(Duration.ofMillis(millis));
		}

		TerminalClientConfigurationBuilder withSocketTimeout(Duration timeout);

		TerminalClientConfigurationBuilder withBasicAuth(String username, String password);

		TerminalClientConfigurationBuilder withPathPrefix(String pathPrefix);

		TerminalClientConfigurationBuilder withProxy(String proxy);

		TerminalClientConfigurationBuilder withClientConfigurer(ClientConfigurationCallback<?> clientConfigurer);

		TerminalClientConfigurationBuilder withHeaders(Supplier<HttpHeaders> headers);

		ClientConfiguration build();
	}

	@FunctionalInterface
	interface ClientConfigurationCallback<T> {
		T configure(T clientConfigurer);
	}
}
