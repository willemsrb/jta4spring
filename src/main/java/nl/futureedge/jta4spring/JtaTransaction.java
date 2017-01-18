package nl.futureedge.jta4spring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import nl.futureedge.jta4spring.xa.XidFactory;

public class JtaTransaction implements Transaction {

	private static final Logger LOGGER = Logger.getLogger(JtaTransaction.class.getName());

	private static final int DEFAULT_TIMEOUT_IN_SECONDS = 30;

	private static final Xid xid = XidFactory.createXid();

	private int timeoutInSeconds = DEFAULT_TIMEOUT_IN_SECONDS;
	private int status = Status.STATUS_ACTIVE;
	private final List<XAResource> xaResources = new ArrayList<>();
	private final Map<Synchronization,Void> synchronizations = new WeakHashMap<>();


	/* ***************************** */
	/* *** RESOURCES *************** */
	/* ***************************** */

	@Override
	public boolean delistResource(final XAResource xaResource, final int flag) throws IllegalStateException, SystemException {
		throw new UnsupportedOperationException("delist resource not supported");
	}

	@Override
	public boolean enlistResource(final XAResource xaResource) throws RollbackException, IllegalStateException, SystemException {
		if(Status.STATUS_MARKED_ROLLBACK == status) {
			throw new RollbackException("Transaction is marked for rollback; enlist resource not possible.");
		}
		if(Status.STATUS_ACTIVE != status) {
			throw new IllegalStateException("Transaction status is not active (but " + status + "); enlist resource not possible.");
		}

		for(final XAResource enlistedResource : xaResources) {
			if( enlistedResource == xaResource) {
				throw new IllegalStateException("XA resource already enlisted; enlist resource not possible.");
			}
		}

		// Start transaction on XA resource
		try {
			xaResource.setTransactionTimeout(timeoutInSeconds);
			xaResource.start(xid, XAResource.TMNOFLAGS);
		} catch (final XAException e) {
			final SystemException systemException = new SystemException("Could not start transaction on XA resource");
			systemException.initCause(e);
			throw systemException;
		}

		xaResources.add(xaResource);
		return true;
	}


	/* ***************************** */
	/* *** COMMIT/ROLLBACK ********* */
	/* ***************************** */

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
	SecurityException, IllegalStateException, SystemException {
		if(Status.STATUS_ACTIVE != status) {
			throw new RollbackException("Transaction status is not active (but " + status + "); commit not possible.");
		}

		doBeforeCompletion();

		doCommit();

		doAfterCompletion();
	}

	private void doCommit() throws RollbackException, SystemException {
		// Prepare
		status = Status.STATUS_PREPARING;
		boolean ok = true;

		final List<XAResource> preparedXaResources = new ArrayList<>();
		for(final XAResource xaResource : xaResources) {
			try {
				xaResource.end(xid, XAResource.TMSUCCESS);
				final int prepareResult = xaResource.prepare(xid);
				if(prepareResult == XAResource.XA_OK) {
					preparedXaResources.add(xaResource);
				} else if(prepareResult != XAResource.XA_RDONLY) {
					ok = false;
					LOGGER.log(Level.SEVERE, "Unknown result from xaResource.prepare: " + prepareResult);
				}
			} catch (final XAException e) {
				ok = false;
				LOGGER.log(Level.FINE, "XA exception during prepare", e);
			}
		}

		// Set resources to prepared resources
		xaResources.clear();
		xaResources.addAll(preparedXaResources);

		// Rollback if necessary
		if(!ok) {
			LOGGER.log(Level.INFO, "Transaction could not be prepared. Executing rollback.");
			doRollback();
			throw new RollbackException("Transaction could not be prepared. View log for previous error(s).");
		}
		status = Status.STATUS_PREPARED;

		// Commit
		status = Status.STATUS_COMMITTING;
		for(final XAResource xaResource : xaResources) {
			try {
				xaResource.commit(xid, false);
			} catch (final XAException e) {
				ok = false;
				LOGGER.log(Level.SEVERE, "XA exception during commit", e);
			}
		}

		if(ok) {
			status = Status.STATUS_COMMITTED;
		} else {
			LOGGER.log(Level.SEVERE, "Transaction could not be commited completely (after succesfull preparation). DATA CAN BE INCONSISTENT! View log for previous error(s).");
			throw new SystemException("Transaction could not be commited completely (after succesfull preparation). DATA CAN BE INCONSISTENT! View log for previous error(s).");
		}

	}

	@Override
	public void rollback() throws IllegalStateException, SystemException {
		if(Status.STATUS_ACTIVE != status && Status.STATUS_MARKED_ROLLBACK != status) {
			throw new IllegalStateException("Transaction status is not active or marked for rollback (but " + status + "); rollback not possible.");
		}

		doRollback();

		doAfterCompletion();
	}

	private void doRollback() throws SystemException {
		// Rollback
		status = Status.STATUS_ROLLING_BACK;

		boolean ok = true;
		for(final XAResource xaResource : xaResources) {
			try {
				//xaResource.end(xid, XAResource.TMFAIL);
				xaResource.rollback(xid);
			} catch (final XAException e) {
				ok = false;
				LOGGER.log(Level.SEVERE, "XA exception during rollback", e);
			}
		}

		status = Status.STATUS_ROLLEDBACK;

		if(!ok) {
			LOGGER.log(Level.SEVERE, "Transaction could not be rollbacked completely (after succesfull preparation). DATA CAN BE INCONSISTENT! View log for previous error(s).");
			throw new SystemException("Transaction could not be rollbacked completely (view log for previous error(s)).");
		}
	}

	/* ***************************** */
	/* *** SYNCHRONIZATIONS ******** */
	/* ***************************** */


	@Override
	public void registerSynchronization(final Synchronization synchronization)
			throws RollbackException, IllegalStateException, SystemException {
		if(Status.STATUS_MARKED_ROLLBACK == status) {
			throw new RollbackException("Transaction is marked for rollback; register synchronization not possible");
		}
		if(Status.STATUS_ACTIVE != status) {
			throw new IllegalStateException("Transaction status is not active (but " + status + "); register synchronization not possible.");
		}
		synchronizations.put(synchronization, null);
	}

	private void doBeforeCompletion() {
		for(final Synchronization synchronization : synchronizations.keySet()) {
			synchronization.beforeCompletion();
		}
	}

	private void doAfterCompletion() {
		for(final Synchronization synchronization : synchronizations.keySet()) {
			synchronization.afterCompletion(status);
		}
	}

	/* ***************************** */
	/* *** STATUS ****************** */
	/* ***************************** */

	@Override
	public int getStatus() throws SystemException {
		return status;
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		if(Status.STATUS_ACTIVE != status && Status.STATUS_MARKED_ROLLBACK != status) {
			throw new IllegalStateException("Transaction status is not active or marked for rollback (but " + status + "); set rollback only not possible.");
		}
		status = Status.STATUS_MARKED_ROLLBACK;
	}

	/* ***************************** */
	/* *** TIMEOUT ***************** */
	/* ***************************** */

	/**
	 * Modify the timeout value that is associated with transactions started
	 * by the current thread with the begin method.
	 *
	 * <p> If an application has not called this method, the transaction
	 * service uses some default value for the transaction timeout.
	 *
	 * @param seconds The value of the timeout in seconds. If the value is zero,
	 *        the transaction service restores the default value. If the value
	 *        is negative a SystemException is thrown.
	 *
	 * @exception SystemException Thrown if the transaction manager
	 *    encounters an unexpected error condition.
	 *
	 */
	public void setTransactionTimeout(final int seconds) throws SystemException {
		if(seconds < 0) {
			throw new SystemException("Timeout may not be a negative value");
		}
		if(seconds == 0) {
			timeoutInSeconds = DEFAULT_TIMEOUT_IN_SECONDS;
		} else {
			timeoutInSeconds = seconds;
		}

		// Update all enlisted xa resources
		for(final XAResource xaResource: xaResources) {
			try {
				xaResource.setTransactionTimeout(timeoutInSeconds);
			} catch (final XAException e) {
				final SystemException systemException = new SystemException("Could not set timeout on XA resource");
				systemException.initCause(e);
				throw systemException;
			}
		}
	}

}
