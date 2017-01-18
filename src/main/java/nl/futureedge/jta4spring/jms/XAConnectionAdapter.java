package nl.futureedge.jta4spring.jms;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import nl.futureedge.jta4spring.JtaTransactionManager;

/**
 * XAConnection adapter; delegates all calls to the wrapped XAConnection.
 *
 * Overrides the {@link #createSession(boolean, int)} method to start a XASession and enlist the XAResource to the transaction.
 */
class XAConnectionAdapter implements Connection {

	private final XAConnection xaConnection;
	private final JtaTransactionManager transactionManager;

	/**
	 * Constructor.
	 * @param xaConnection xa connection
	 * @param transactionManager transaction manager
	 */
	XAConnectionAdapter(final XAConnection xaConnection, final JtaTransactionManager transactionManager) {
		this.xaConnection = xaConnection;
		this.transactionManager = transactionManager;
	}

	@Override
	public Session createSession(final boolean transacted, final int acknowledgeMode) throws JMSException {
		if(transacted) {
			final XASession xaSession = xaConnection.createXASession();
			try {
				transactionManager.getTransaction().enlistResource(xaSession.getXAResource());
			} catch (IllegalStateException | RollbackException | SystemException e) {
				final JMSException jmsException = new JMSException("Could not enlist connection to transaction");
				jmsException.initCause(e);
				throw jmsException;
			}
			return xaSession;

		} else {
			return xaConnection.createSession(false, acknowledgeMode);
		}
	}

	@Override
	public String getClientID() throws JMSException {
		return xaConnection.getClientID();
	}

	@Override
	public void setClientID(final String clientID) throws JMSException {
		xaConnection.setClientID(clientID);
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		return xaConnection.getMetaData();
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return xaConnection.getExceptionListener();
	}

	@Override
	public void setExceptionListener(final ExceptionListener listener) throws JMSException {
		xaConnection.setExceptionListener(listener);
	}

	@Override
	public void start() throws JMSException {
		xaConnection.start();
	}

	@Override
	public void stop() throws JMSException {
		xaConnection.stop();
	}

	@Override
	public void close() throws JMSException {
		xaConnection.close();
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(final Destination destination, final String messageSelector,
			final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
		throw new UnsupportedOperationException("Create connection consumer not supported");
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(final Topic topic, final String subscriptionName,
			final String messageSelector, final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
		throw new UnsupportedOperationException("Create durable connection consumer not supported");
	}

}
