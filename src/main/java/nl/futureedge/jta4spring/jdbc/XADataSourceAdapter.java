package nl.futureedge.jta4spring.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.futureedge.jta4spring.JtaTransactionManager;

/**
 * XADataSource adapter; delegates all calls to the wrapped xa datasource.
 *
 * Override the {@link #getConnection()} and {@link #getConnection(String, String)} methods to create a xa connection and enlist the XAResource to the transaction.
 */
class XADataSourceAdapter implements DataSource {

	private static final Logger LOGGER = LoggerFactory.getLogger(XADataSourceAdapter.class);

	private final XADataSource xaDataSource;
	private final JtaTransactionManager transactionManager;

	/**
	 * Constructor.
	 * @param xaDataSource xa data source
	 * @param transactionManager transaction manager
	 */
	XADataSourceAdapter(final XADataSource xaDataSource, final JtaTransactionManager transactionManager) {
		this.xaDataSource=xaDataSource;
		this.transactionManager=transactionManager;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		LOGGER.trace("getLogWriter()");
		return xaDataSource.getLogWriter();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		LOGGER.trace("getLoginTimeout()");
		return xaDataSource.getLoginTimeout();
	}

	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		LOGGER.trace("getParentLogger()");
		return xaDataSource.getParentLogger();
	}

	@Override
	public void setLogWriter(final PrintWriter out) throws SQLException {
		LOGGER.trace("setLogWriter(out={})", out);
		xaDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(final int seconds) throws SQLException {
		LOGGER.trace("setLoginTimeout(seconds={})", seconds);
		xaDataSource.setLoginTimeout(seconds);
	}

	@Override
	public boolean isWrapperFor(final Class<?> iface) throws SQLException {
		LOGGER.trace("isWrapperFor(iface={})", iface);
		return false;
	}

	@Override
	public <T> T unwrap(final Class<T> iface) throws SQLException {
		LOGGER.trace("unwrap(iface={})", iface);
		throw new SQLException("unwrap not supported");
	}

	@Override
	public Connection getConnection() throws SQLException {
		LOGGER.trace("getConnection()");
		final XAConnection xaConnection = xaDataSource.getXAConnection();
		enlistResource(xaConnection);

		return adaptConnection(xaConnection);
	}

	@Override
	public Connection getConnection(final String username, final String password) throws SQLException {
		LOGGER.trace("getConnection(username={}, password withheld)", username, password);
		final  XAConnection xaConnection  = xaDataSource.getXAConnection(username, password);
		enlistResource(xaConnection);
		return adaptConnection(xaConnection);
	}

	private Connection adaptConnection(final XAConnection xaConnection) throws SQLException {
		final XAConnectionAdapter result = new XAConnectionAdapter(xaConnection, transactionManager);
		try {
			transactionManager.getTransaction().registerSynchronization(result);
		} catch (IllegalStateException | RollbackException | SystemException e) {
			LOGGER.debug("Could not register connection adapter to transaction", e);
			throw new SQLException("Could not register connection adapter to transaction", e);
		}

		return result;
	}

	private void enlistResource(final XAConnection xaConnection) throws SQLException {
		try {
			transactionManager.getTransaction().enlistResource(xaConnection.getXAResource());
		} catch (IllegalStateException | RollbackException | SystemException e) {
			LOGGER.debug("Could not enlist connection to transaction", e);
			throw new SQLException("Could not enlist connection to transaction", e);
		}
	}

}
