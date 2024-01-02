/*
 * Copyright 2019-2023 the original author or authors.
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

import com.mawen.search.core.mapping.IndexCoordinates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class IndexCoordinatesUnitTests {

	@Test
	void cannotBeInitializedWithNullIndexName() {
		assertThatThrownBy(() -> IndexCoordinates.of(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void cannotBeInitializedWithNullIndexNames() {
		assertThatThrownBy(() -> IndexCoordinates.of((String[]) null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void cannotBeInitializedWithEmptyIndexNames() {
		assertThatThrownBy(() -> IndexCoordinates.of(new String[] {})).isInstanceOf(IllegalArgumentException.class);
	}
}
