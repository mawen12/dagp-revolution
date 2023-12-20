package com.mawen.search.repository.support;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.stream.Stream;

import com.mawen.search.core.domain.SearchHit;
import lombok.Getter;

import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class ElasticsearchRepositoryMetadata extends DefaultRepositoryMetadata {

	public ElasticsearchRepositoryMetadata(Class<?> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	public Class<?> getReturnedDomainClass(Method method) {
		Class<?> returnedDomainClass = super.getReturnedDomainClass(method);
		if (SearchHit.class.isAssignableFrom(returnedDomainClass)) {
			try {
				// dealing with Collection<SearchHit<T>> or Flux<SearchHit<T>>, getting to T
				ParameterizedType methodGenericReturnType = ((ParameterizedType) method.getGenericReturnType());
				if (isAllowedGenericType(methodGenericReturnType)) {
					ParameterizedType collectionTypeArgument = (ParameterizedType) methodGenericReturnType
							.getActualTypeArguments()[0];
					if (SearchHit.class.isAssignableFrom((Class<?>) collectionTypeArgument.getRawType())) {
						returnedDomainClass = (Class<?>) collectionTypeArgument.getActualTypeArguments()[0];
					}
				}
			}
			catch (Exception ignored) {}
		}
		return returnedDomainClass;
	}

	protected boolean isAllowedGenericType(ParameterizedType methodGenericReturnType) {
		return Collection.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType())
				|| Stream.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType());
	}
}