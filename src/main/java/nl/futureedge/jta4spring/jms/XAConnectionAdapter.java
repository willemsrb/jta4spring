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
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.futureedge.jta4spring.JtaTransactionManager;

/**
 * XAConnection adapter; delegates all calls to the wrapped XAConnection.
 *
 * Overrides the {@link #createSession(boolean, int)} method to start a XASession and enlist the XAResource to the transaction.
 */
class XAConnectionAdapter implements Connection, Synchronization {

	private static final Logger LOGGER = LoggerFactory.getLogger(XAConnectionAdapter.class);

	private final XAConnection xaConnection;
	private final JtaTransactionManager transactionManager;

	private boolean closeAfterCompletion=false;

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
		LOGGER.trace("createSession(transacted={},acknowledgeMode={})",transacted,acknowledgeMode);
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
	public void beforeCompletion() {
		// Nothing

	}

	@Override
	public void afterCompletion(final int status) {
		if(closeAfterCompletion) {
			try {
				LOGGER.debug("Closing connection after completion of transaction");
				xaConnection.close();
			} catch (final JMSException e) {
				LOGGER.warn("Could not close connection after completion of transaction", e);
			}
		}
	}

	@Override
	public String getClientID() throws JMSException {
		LOGGER.trace("getClientID()");
		return xaConnection.getClientID();
	}

	@Override
	public void setClientID(final String clientID) throws JMSException {
		LOGGER.trace("setClientID(clientID={})",clientID);
		xaConnection.setClientID(clientID);
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		LOGGER.trace("getMetaData()");
		return xaConnection.getMetaData();
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		LOGGER.trace("getExceptionListener()");
		return xaConnection.getExceptionListener();
	}

	@Override
	public void setExceptionListener(final ExceptionListener listener) throws JMSException {
		LOGGER.trace("setExceptionListener(listener={})",listener);
		xaConnection.setExceptionListener(listener);
	}

	@Override
	public void start() throws JMSException {
		LOGGER.trace("start()");
		xaConnection.start();
	}

	@Override
	public void stop() throws JMSException {
		LOGGER.trace("stop()");
		xaConnection.stop();
	}

	@Override
	public void close() throws JMSException {
		LOGGER.trace("close()");
		if(Status.STATUS_NO_TRANSACTION == transactionManager.getStatus()) {
			xaConnection.close();
		} else {
			LOGGER.debug("Registering connection as closed; keeping until completion of transaction");
			closeAfterCompletion = true;
		}
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(final Destination destination, final String messageSelector,
			final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
		LOGGER.trace("createConnectionConsumer(destination={},messageSelector={},sessionPool={},maxMessages={})",destination,messageSelector,sessionPool,maxMessages);
		throw new UnsupportedOperationException("Create connection consumer not supported");
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(final Topic topic, final String subscriptionName,
			final String messageSelector, final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
		LOGGER.trace("createConnectionConsumer(topic={},subscriptionName={},messageSelector={},sessionPool={},maxMessages={})",topic,subscriptionName,messageSelector,sessionPool,maxMessages);
		throw new UnsupportedOperationException("Create durable connection consumer not supported");
	}


}
