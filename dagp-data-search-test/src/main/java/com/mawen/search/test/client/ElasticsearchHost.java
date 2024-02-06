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

package com.mawen.search.test.client;

import java.net.InetSocketAddress;
import java.time.Instant;

import org.springframework.util.Assert;

public class ElasticsearchHost {

	public static final int DEFAULT_PORT = 9200;

	private final InetSocketAddress endpoint;
	private final State state;
	private final Instant timestamp;

	public ElasticsearchHost(InetSocketAddress endpoint, State state) {

		Assert.notNull(endpoint, "Host must not be null");
		Assert.notNull(state, "State must not be null");

		this.endpoint = endpoint;
		this.state = state;
		this.timestamp = Instant.now();
	}

	public static ElasticsearchHost online(InetSocketAddress host) {
		return new ElasticsearchHost(host, State.ONLINE);
	}

	public static ElasticsearchHost offline(InetSocketAddress host) {
		return new ElasticsearchHost(host, State.OFFLINE);
	}

	public static InetSocketAddress parse(String hostAndPort) {
		return InetSocketAddressParser.parse(hostAndPort, DEFAULT_PORT);
	}

	public boolean isOnline() {
		return State.ONLINE.equals(state);
	}

	public InetSocketAddress getEndpoint() {
		return endpoint;
	}

	public State getState() {
		return state;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "ElasticsearchHost(" + endpoint + ", " + state.name() + ')';
	}

	public enum State {
		ONLINE, OFFLINE, UNKNOWN
	}
}
