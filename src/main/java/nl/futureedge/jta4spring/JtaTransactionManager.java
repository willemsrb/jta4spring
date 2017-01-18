package nl.futureedge.jta4spring;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public class JtaTransactionManager implements TransactionManager {

	private final JtaMonitor jtaMonitor;

	public JtaTransactionManager(final JtaMonitor jtaMonitor) {
		this.jtaMonitor=jtaMonitor;
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		jtaMonitor.begin();
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
	SecurityException, IllegalStateException, SystemException {
		getTransaction().commit();
	}

	@Override
	public int getStatus() throws SystemException {
		return jtaMonitor.getStatus();
	}

	@Override
	public JtaTransaction getTransaction() throws SystemException {
		return jtaMonitor.getTransaction();
	}

	@Override
	public void resume(final Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
		throw new UnsupportedOperationException("Transaction suspension is not supported");
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		getTransaction().rollback();

	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		getTransaction().setRollbackOnly();
	}

	@Override
	public void setTransactionTimeout(final int seconds) throws SystemException {
		getTransaction().setTransactionTimeout(seconds);

	}

	@Override
	public Transaction suspend() throws SystemException {
		throw new UnsupportedOperationException("Transaction suspension is not supported");
	}

}
