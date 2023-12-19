package com.mawen.search.repository.support;

import java.io.Serializable;

import com.mawen.search.core.ElasticsearchOperations;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends RepositoryFactoryBeanSupport<T, S, ID> {

	@Nullable
	private ElasticsearchOperations operations;

	public ElasticsearchRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}


	public void setElasticsearchOperations(ElasticsearchOperations operations) {

		Assert.notNull(operations, "ElasticsearchOperations must not be null!");

		setMappingContext(operations.getElasticsearchConverter().getMappingContext());
		this.operations = operations;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(operations, "ElasticsearchOperations must be configured!");
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {

		Assert.notNull(operations, "operations are not initialized");

		return new ElasticsearchRepositoryFactory(operations);
	}
}
