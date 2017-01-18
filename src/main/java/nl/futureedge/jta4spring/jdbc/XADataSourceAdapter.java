package nl.futureedge.jta4spring.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import nl.futureedge.jta4spring.JtaTransactionManager;

/**
 * XADataSource adapter; delegates all calls to the wrapped xa datasource.
 *
 * Override the {@link #getConnection()} and {@link #getConnection(String, String)} methods to create a xa connection and enlist the XAResource to the transaction.
 */
class XADataSourceAdapter implements DataSource {

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
		return xaDataSource.getLogWriter();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return xaDataSource.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return xaDataSource.getParentLogger();
	}

	@Override
	public void setLogWriter(final PrintWriter out) throws SQLException {
		xaDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(final int seconds) throws SQLException {
		xaDataSource.setLoginTimeout(seconds);
	}

	@Override
	public boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(final Class<T> iface) throws SQLException {
		throw new SQLException("unwrap not supported");
	}

	@Override
	public Connection getConnection() throws SQLException {
		final XAConnection xaConnection = xaDataSource.getXAConnection();
		try {
			transactionManager.getTransaction().enlistResource(xaConnection.getXAResource());
		} catch (IllegalStateException | RollbackException | SystemException e) {
			throw new SQLException("Could not enlist connection to transaction", e);
		}
		return xaConnection.getConnection();
	}

	@Override
	public Connection getConnection(final String username, final String password) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
