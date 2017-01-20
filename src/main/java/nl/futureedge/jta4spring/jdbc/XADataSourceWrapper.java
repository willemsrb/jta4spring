package nl.futureedge.jta4spring.jdbc;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import nl.futureedge.jta4spring.JtaTransactionManager;

public class XADataSourceWrapper implements FactoryBean<DataSource>, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(XADataSourceWrapper.class);

	private XADataSource xaDataSource;
	private JtaTransactionManager transactionManager;
	private DataSource dataSource;

	@Required
	public void setXaDataSource(final XADataSource xaDataSource) {
		LOGGER.trace("setXaDataSource(xaDataSource={})", xaDataSource);
		this.xaDataSource = xaDataSource;
	}
	@Required
	public void setTransactionManager(final JtaTransactionManager transactionManager) {
		LOGGER.trace("setTransactionManager(transactionManager={})", transactionManager);
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.trace("afterPropertiesSet()");
		dataSource = new XADataSourceAdapter(xaDataSource, transactionManager);
	}

	@Override
	public DataSource getObject() throws Exception {
		LOGGER.trace("getObject()");
		return dataSource;
	}

	@Override
	public Class<?> getObjectType() {
		LOGGER.trace("getObjectType()");
		return DataSource.class;
	}

	@Override
	public boolean isSingleton() {
		LOGGER.trace("isSingleton()");
		return true;
	}

}
