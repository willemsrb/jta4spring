package nl.futureedge.jta4spring;

import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

public class JtaMonitor implements InitializingBean, DisposableBean, Synchronization {

	private static final Logger LOGGER = LoggerFactory.getLogger(JtaMonitor.class);

	private final AtomicLong TRANSACTION_ID = new AtomicLong(1);
	private final ThreadLocal<JtaTransaction> transaction = new ThreadLocal<>();

	private String uniqueName;

	@Required
	public void setUniqueName(final String uniqueName) {
		this.uniqueName = uniqueName;
	}

	/**
	 * Startup; read transaction store; rollback everything found.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
	}

	/**
	 * Shutdown; wait for all transactions to end.
	 */
	@Override
	public void destroy() throws Exception {

	}

	/**
	 * Create a new transaction and associate it with the current thread.
	 *
	 * @exception NotSupportedException Thrown if the thread is already
	 *    associated with a transaction and the Transaction Manager
	 *    implementation does not support nested transactions.
	 * @throws RollbackException
	 * @throws IllegalStateException
	 *
	 * @exception SystemException Thrown if the transaction manager
	 *    encounters an unexpected error condition.
	 *
	 */
	public void begin() throws NotSupportedException, SystemException {
		if(transaction.get() != null) {
			throw new NotSupportedException("Transaction already started");
		}

		final JtaTransaction result = new JtaTransaction(new JtaXid(uniqueName, TRANSACTION_ID.getAndIncrement()));
		try {
			result.registerSynchronization(this);
		} catch (IllegalStateException | RollbackException e) {
			final SystemException systemException = new SystemException("Could not register synchronization on transaction on begin");
			systemException.initCause(e);
			throw systemException;
		}
		transaction.set(result);
	}

	/**
	 * Get the transaction object that represents the transaction
	 * context of the calling thread.
	 *
	 * @return the <code>Transaction</code> object representing the
	 *	  transaction associated with the calling thread.
	 *
	 * @throws IllegalStateException if no transaction is active
	 * @exception SystemException Thrown if the transaction manager
	 *    encounters an unexpected error condition.
	 *
	 */
	public JtaTransaction getTransaction() {
		final JtaTransaction result = transaction.get();
		if(result == null) {
			throw new IllegalStateException("No transaction active");
		}
		return result;
	}

	/**
	 * Obtain the status of the transaction associated with the current thread.
	 *
	 * @return The transaction status. If no transaction is associated with
	 *    the current thread, this method returns the Status.NoTransaction
	 *    value.
	 *
	 * @exception SystemException Thrown if the transaction manager
	 *    encounters an unexpected error condition.
	 *
	 */
	public int getStatus() {
		final JtaTransaction result = transaction.get();
		if(result == null) {
			return Status.STATUS_NO_TRANSACTION;
		} else {
			return result.getStatus();
		}
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCompletion(final int status) {
		transaction.set(null);
	}


}
