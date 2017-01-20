package nl.futureedge.jta4spring.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.futureedge.jta4spring.JtaTransactionManager;

/**
 * XAConnectionFactory adapter; creates connections on the wrapped xa connection factory and adapts them using a {@link XAConnectionAdapter}.
 */
class XAConnectionFactoryAdapter implements ConnectionFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(XAConnectionFactoryAdapter.class);

	private final XAConnectionFactory xaConnectionFactory;
	private final JtaTransactionManager transactionManager;

	/**
	 * Constructor.
	 * @param xaConnectionFactory xa connection factory
	 * @param transactionManager transaction manager
	 */
	XAConnectionFactoryAdapter(final XAConnectionFactory xaConnectionFactory,
			final JtaTransactionManager transactionManager) {
		this.xaConnectionFactory =xaConnectionFactory;
		this.transactionManager = transactionManager;
	}

	@Override
	public Connection createConnection() throws JMSException {
		LOGGER.trace("getConnection()");
		final XAConnection xaConnection = xaConnectionFactory.createXAConnection();
		return new XAConnectionAdapter(xaConnection, transactionManager);
	}

	@Override
	public Connection createConnection(final String username, final String password) throws JMSException {
		LOGGER.trace("getConnection(username={}, password withheld)", username, password);
		final XAConnection xaConnection = xaConnectionFactory.createXAConnection(username, password);
		return new XAConnectionAdapter(xaConnection, transactionManager);
	}

}
