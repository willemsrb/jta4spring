package nl.futureedge.jta4spring.jms;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import nl.futureedge.jta4spring.JtaTransactionManager;

/**
 * XAConnectionFactory wrapper; Adapts the wrapped xa connection factory using a {@link XAConnectionFactoryAdapter}.
 */
public class XAConnectionFactoryWrapper implements FactoryBean<ConnectionFactory>, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(XAConnectionFactoryWrapper.class);

	private XAConnectionFactory xaConnectionFactory;
	private JtaTransactionManager transactionManager;
	private ConnectionFactory connectionFactory;

	/**
	 * Set the xa connection factory to wrap.
	 * @param xaConnectionFactory xa connection factory
	 */
	@Required
	public void setXaConnectionFactory(final XAConnectionFactory xaConnectionFactory)  {
		LOGGER.trace("setXaConnectionFactory(xaConnectionFactory={})", xaConnectionFactory);
		this.xaConnectionFactory = xaConnectionFactory;
	}

	/**
	 * Set the transaction manager to use.
	 * @param transactionManager transaction manager
	 */
	@Required
	@Autowired
	public void setTransactionManager(final JtaTransactionManager transactionManager) {
		LOGGER.trace("setTransactionManager(transactionManager={})", transactionManager);
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.trace("afterPropertiesSet()");
		connectionFactory = new XAConnectionFactoryAdapter(xaConnectionFactory, transactionManager);
	}

	@Override
	public ConnectionFactory getObject() throws Exception {
		LOGGER.trace("getObject()");
		return connectionFactory;
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
