package com.mawen.search.client.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ScrollState {
	private final Object lock = new Object();

	private final Set<String> pastIds = new LinkedHashSet<>();
	@Nullable
	private String scrollId;

	public ScrollState() {
	}

	public ScrollState(String scrollId) {
		updateScrollId(scrollId);
	}

	@Nullable
	public String getScrollId() {
		return scrollId;
	}

	public List<String> getScrollIds() {

		synchronized (lock) {
			List<String> copyOf = new ArrayList<>(pastIds.size());
			copyOf.addAll(pastIds);
			return copyOf;
		}
	}

	public void updateScrollId(@Nullable String scrollId) {

		if (StringUtils.hasText(scrollId)) {

			synchronized (lock) {

				this.scrollId = scrollId;
				pastIds.add(scrollId);
			}
		}
	}
}
