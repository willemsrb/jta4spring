package nl.futureedge.jta4spring;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

public class JtaTransactionManager implements TransactionManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(JtaTransactionManager.class);

	private JtaMonitor jtaMonitor;

	@Required
	@Autowired
	public void setJtaMonitor(final JtaMonitor jtaMonitor) {
		this.jtaMonitor=jtaMonitor;
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		LOGGER.trace("begin()");
		jtaMonitor.begin();
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
	SecurityException, IllegalStateException, SystemException {
		LOGGER.trace("commit()");
		getTransaction().commit();
	}

	@Override
	public int getStatus() {
		LOGGER.trace("getStatus()");
		return jtaMonitor.getStatus();
	}

	@Override
	public JtaTransaction getTransaction() throws SystemException {
		LOGGER.trace("getTransaction()");
		return jtaMonitor.getTransaction();
	}

	@Override
	public void resume(final Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException {
		LOGGER.trace("resume(transaction="+transaction+")");
		throw new UnsupportedOperationException("Transaction suspension is not supported");
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		LOGGER.trace("rollback()");
		getTransaction().rollback();

	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		LOGGER.trace("setRollbackOnly()");
		getTransaction().setRollbackOnly();
	}

	@Override
	public void setTransactionTimeout(final int seconds) throws SystemException {
		LOGGER.trace("setTransactionTimeout(seconds="+seconds+")");
		getTransaction().setTransactionTimeout(seconds);
	}

	@Override
	public Transaction suspend() throws SystemException {
		LOGGER.trace("suspend()");
		throw new UnsupportedOperationException("Transaction suspension is not supported");
	}

}
