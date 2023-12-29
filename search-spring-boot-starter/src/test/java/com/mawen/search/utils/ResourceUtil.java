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
package com.mawen.search.utils;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.mawen.search.ResourceNotFoundException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

public abstract class ResourceUtil {

	/**
	 * Read a {@link ClassPathResource} into a {@link String}.
	 *
	 * @param url url the resource
	 * @return the contents of the resource
	 */
	public static String readFileFromClasspath(String url) {

		Assert.notNull(url, "url must not be null");

		try (InputStream is = new ClassPathResource(url).getInputStream()) {
			return StreamUtils.copyToString(is, Charset.defaultCharset());
		} catch (Exception e) {
			throw new ResourceNotFoundException("Could not load resource from " + url, e);
		}
	}

	// Utility constructor
	private ResourceUtil() {}
}
