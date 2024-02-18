package com.mawen.search.microbenchmark.support;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import org.springframework.util.StringUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/2/18
 */
public abstract class RestClientHelper {

	private static final String LOCAL_DEFAULT = "http://127.0.0.1:9200";

	private static final String USERNAME_DEFAULT = "elastic";
	private static final String PASSWORD_DEFAULT = "elastic";
	private static final int CONNECT_TIMEOUT_DEFAULT = 20000;
	private static final int SOCKET_TIMEOUT_DEFAULT = 60000;
	private static final int KEEP_ALIVE_DEFAULT = 3600000;


	public static RestClient localDefault() {
		HttpHost host = createHttpHost(LOCAL_DEFAULT);
		RestClientBuilder builder = RestClient.builder(host);

		// http client config
		builder.setHttpClientConfigCallback((httpClientBuilder -> {
			httpClientBuilder.setDefaultCredentialsProvider(createProvider(URI.create(LOCAL_DEFAULT), USERNAME_DEFAULT, PASSWORD_DEFAULT));
//			httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build());
			return httpClientBuilder;
		}));

		// request config
		builder.setRequestConfigCallback((requestConfigBuilder -> {
			requestConfigBuilder.setConnectTimeout(CONNECT_TIMEOUT_DEFAULT);
			requestConfigBuilder.setSocketTimeout(SOCKET_TIMEOUT_DEFAULT);
			return requestConfigBuilder;
		}));

		return builder.build();
	}

	private static HttpHost createHttpHost(String uri) {
		try {
			return createHttpHost(URI.create(uri));
		}
		catch (IllegalArgumentException ex) {
			return HttpHost.create(uri);
		}
	}

	private static HttpHost createHttpHost(URI uri) {
		if (!StringUtils.hasLength(uri.getUserInfo())) {
			return HttpHost.create(uri.toString());
		}
		try {
			return HttpHost.create(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
					uri.getQuery(), uri.getFragment()).toString());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static BasicCredentialsProvider createProvider(URI uri, String username, String password) {
		BasicCredentialsProvider provider = new BasicCredentialsProvider();

		AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort());
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		provider.setCredentials(authScope, credentials);

		return provider;
	}

}
