package nl.futureedge.jta4spring.jdbc;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import nl.futureedge.jta4spring.JtaTransactionManager;

public class XADataSourceWrapper implements FactoryBean<DataSource>, InitializingBean {

	private XADataSource xaDataSource;
	private JtaTransactionManager transactionManager;
	private DataSource dataSource;

	@Required
	public void setXaDataSource(final XADataSource xaDataSource) {
		this.xaDataSource = xaDataSource;
	}
	@Required
	public void setTransactionManager(final JtaTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		dataSource = new XADataSourceAdapter(xaDataSource, transactionManager);
	}

	@Override
	public DataSource getObject() throws Exception {
		return dataSource;
	}

	@Override
	public Class<?> getObjectType() {
		return DataSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
