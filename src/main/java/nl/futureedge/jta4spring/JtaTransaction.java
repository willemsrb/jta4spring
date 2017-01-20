package nl.futureedge.jta4spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JtaTransaction implements Transaction {

	private static final Logger LOGGER = LoggerFactory.getLogger(JtaTransaction.class);

	private static final List<Integer> XA_ROLLBACK_CODES = Arrays.asList(
			XAException.XA_RBCOMMFAIL,
			XAException.XA_RBDEADLOCK,
			XAException.XA_RBEND,
			XAException.XA_RBINTEGRITY,
			XAException.XA_RBOTHER,
			XAException.XA_RBPROTO,
			XAException.XA_RBROLLBACK,
			XAException.XA_RBTIMEOUT,
			XAException.XA_RBTRANSIENT
			);

	private static final int DEFAULT_TIMEOUT_IN_SECONDS = 30;

	private final Xid xid;
	private int timeoutInSeconds = DEFAULT_TIMEOUT_IN_SECONDS;
	private int status = Status.STATUS_ACTIVE;
	private final List<XAResource> xaResources = new ArrayList<>();
	private final Map<Synchronization,Void> synchronizations = new WeakHashMap<>();

	JtaTransaction(final Xid xid) {
		this.xid = xid;
	}

	/* ***************************** */
	/* *** RESOURCES *************** */
	/* ***************************** */

	@Override
	public boolean delistResource(final XAResource xaResource, final int flag) throws IllegalStateException, SystemException {
		LOGGER.trace("delistResource(xaResource={}, flag={})", xaResource, flag);
		throw new UnsupportedOperationException("delist resource not supported");
	}

	@Override
	public boolean enlistResource(final XAResource xaResource) throws RollbackException, IllegalStateException, SystemException {
		LOGGER.trace("enlistResource(xaResource={})", xaResource);
		if(Status.STATUS_MARKED_ROLLBACK == status) {
			LOGGER.debug("Transaction is marked for rollback; enlist resource not possible.");
			throw new RollbackException("Transaction is marked for rollback; enlist resource not possible.");
		}
		if(Status.STATUS_ACTIVE != status) {
			LOGGER.debug("Transaction status is not active (but " + status + "); enlist resource not possible.");
			throw new IllegalStateException("Transaction status is not active (but " + status + "); enlist resource not possible.");
		}

		for(final XAResource enlistedResource : xaResources) {
			if( enlistedResource == xaResource) {
				LOGGER.debug("XA resource already enlisted; enlist resource not possible.");
				throw new IllegalStateException("XA resource already enlisted; enlist resource not possible.");
			}
		}

		// Start transaction on XA resource
		try {
			xaResource.setTransactionTimeout(timeoutInSeconds);
			xaResource.start(xid, XAResource.TMNOFLAGS);
		} catch (final XAException e) {
			LOGGER.warn("Could not start transaction on XA resource", e);
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
		LOGGER.trace("commit()");
		if(Status.STATUS_ACTIVE != status) {
			LOGGER.debug("Transaction status is not active (but " + status + "); commit not possible.");
			throw new RollbackException("Transaction status is not active (but " + status + "); commit not possible.");
		}

		doBeforeCompletion();

		doCommit();

		doAfterCompletion();
	}

	private void doCommit() throws RollbackException, SystemException {
		LOGGER.trace("doCommit()");

		// Prepare
		LOGGER.trace("Starting prepare of 2-phase commit");
		status = Status.STATUS_PREPARING;
		boolean ok = true;

		final List<XAResource> preparedXaResources = new ArrayList<>();
		for(final XAResource xaResource : xaResources) {
			try {
				LOGGER.debug("Calling xa_end on {}", xaResource);
				xaResource.end(xid, XAResource.TMSUCCESS);
				LOGGER.debug("Calling xa_prepare on {}", xaResource);
				final int prepareResult = xaResource.prepare(xid);
				if(prepareResult == XAResource.XA_OK) {
					LOGGER.debug("xa_prepare on {}; result ok; adding xaResource to list of prepared resources.", xaResource);
					preparedXaResources.add(xaResource);
				} else if(prepareResult != XAResource.XA_RDONLY) {
					ok = false;
					LOGGER.error("Unknown result from xaResource.prepare: " + prepareResult);
				} else {
					LOGGER.debug("xa_prepare on {}; result read-only. Skipping xa resource for commit.", xaResource);
				}
			} catch (final XAException e) {
				ok = false;
				if(XA_ROLLBACK_CODES.contains(e.errorCode)) {
					LOGGER.debug("XA exception during prepare; xa resource is rolled back", e);
				} else {
					LOGGER.warn("XA exception during prepare; xa resource had an error; adding xa resource for rollback", e);
					preparedXaResources.add(xaResource);
				}
			}
		}
		LOGGER.debug("Prepare of 2-phase commit completed; result = {}", ok);

		// Set resources to prepared resources
		xaResources.clear();
		xaResources.addAll(preparedXaResources);

		// Rollback if necessary
		if(!ok) {
			LOGGER.debug("Transaction could not be prepared. Executing rollback.");
			doRollback();
			throw new RollbackException("Transaction could not be prepared. View log for previous error(s).");
		}
		status = Status.STATUS_PREPARED;

		// Commit
		LOGGER.trace("Starting commit of 2-phase commit");
		status = Status.STATUS_COMMITTING;
		for(final XAResource xaResource : xaResources) {
			try {
				LOGGER.debug("Calling xa_commit on {}", xaResource);
				xaResource.commit(xid, false);
			} catch (final XAException e) {
				ok = false;
				LOGGER.error("XA exception during commit", e);
			}
		}

		LOGGER.debug("Commit of 2-phase commit completed; result = {}", ok);
		status = Status.STATUS_COMMITTED;

		if(!ok) {
			LOGGER.error("Transaction could not be commited completely (after succesfull preparation). DATA CAN BE INCONSISTENT! View log for previous error(s).");
			throw new SystemException("Transaction could not be commited completely (after succesfull preparation). DATA CAN BE INCONSISTENT! View log for previous error(s).");
		}
	}

	@Override
	public void rollback() throws IllegalStateException, SystemException {
		LOGGER.trace("rollback()");
		if(Status.STATUS_ACTIVE != status && Status.STATUS_MARKED_ROLLBACK != status) {
			LOGGER.debug("Transaction status is not active or marked for rollback (but " + status + "); rollback not possible.");
			throw new IllegalStateException("Transaction status is not active or marked for rollback (but " + status + "); rollback not possible.");
		}

		doRollback();

		doAfterCompletion();
	}

	private void doRollback() throws SystemException {
		LOGGER.trace("rollback()");

		// Rollback
		LOGGER.debug("Starting rollback");
		status = Status.STATUS_ROLLING_BACK;

		boolean ok = true;
		for(final XAResource xaResource : xaResources) {
			try {
				LOGGER.debug("Calling xa_end on {}", xaResource);
				xaResource.end(xid, XAResource.TMFAIL);
				LOGGER.debug("Calling xa_rollback on {}", xaResource);
				xaResource.rollback(xid);
			} catch (final XAException e) {
				ok = false;
				LOGGER.warn("XA exception during rollback", e);
			}
		}

		LOGGER.debug("Rollback completed; result = {}", ok);
		status = Status.STATUS_ROLLEDBACK;

		if(!ok) {
			LOGGER.warn("Transaction could not be rollbacked completely (after succesfull preparation). DATA CAN BE INCONSISTENT! View log for previous error(s).");
			throw new SystemException("Transaction could not be rollbacked completely (view log for previous error(s)).");
		}
	}

	/* ***************************** */
	/* *** SYNCHRONIZATIONS ******** */
	/* ***************************** */

	@Override
	public void registerSynchronization(final Synchronization synchronization)
			throws RollbackException, IllegalStateException, SystemException {
		LOGGER.trace( "registerSynchronization(synchronization={}", synchronization);
		if(Status.STATUS_MARKED_ROLLBACK == status) {
			LOGGER.debug("Transaction is marked for rollback; register synchronization not possible");
			throw new RollbackException("Transaction is marked for rollback; register synchronization not possible");
		}
		if(Status.STATUS_ACTIVE != status) {
			LOGGER.debug("Transaction status is not active (but " + status + "); register synchronization not possible.");
			throw new IllegalStateException("Transaction status is not active (but " + status + "); register synchronization not possible.");
		}
		synchronizations.put(synchronization, null);
	}

	private void doBeforeCompletion() {
		LOGGER.trace("doBeforeCompletion()");
		for(final Synchronization synchronization : synchronizations.keySet()) {
			synchronization.beforeCompletion();
		}
	}

	private void doAfterCompletion() {
		LOGGER.trace("doAfterCompletion()");
		for(final Synchronization synchronization : synchronizations.keySet()) {
			synchronization.afterCompletion(status);
		}
	}

	/* ***************************** */
	/* *** STATUS ****************** */
	/* ***************************** */

	@Override
	public int getStatus() {
		LOGGER.trace("getStatus()");
		return status;
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		LOGGER.trace("setRollbackOnly()");
		if(Status.STATUS_ACTIVE != status && Status.STATUS_MARKED_ROLLBACK != status) {
			LOGGER.debug("Transaction status is not active or marked for rollback (but " + status + "); set rollback only not possible.");
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
		LOGGER.trace("setTransactionTimeout(seconds={})", seconds);
		if(seconds < 0) {
			LOGGER.debug("Timeout may not be a negative value");
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
				LOGGER.warn("Could not set timeout on XA resource");
				final SystemException systemException = new SystemException("Could not set timeout on XA resource");
				systemException.initCause(e);
				throw systemException;
			}
		}
	}
}
