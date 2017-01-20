package nl.futureedge.jta4spring.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.sql.XAConnection;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.futureedge.jta4spring.JtaTransactionManager;

public class XAConnectionAdapter implements Connection, Synchronization {

	private static final Logger LOGGER = LoggerFactory.getLogger(XAConnectionAdapter.class);

	private final XAConnection xaConnection;
	private final Connection connection;
	private final JtaTransactionManager transactionManager;

	private boolean closeAfterCompletion=false;

	public XAConnectionAdapter(final XAConnection xaConnection, final JtaTransactionManager transactionManager) throws SQLException {
		this.xaConnection=xaConnection;
		connection=xaConnection.getConnection();
		this.transactionManager=transactionManager;
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
			} catch (final SQLException e) {
				LOGGER.warn("Could not close connection after completion of transaction", e);
			}
		}
	}

	private void checkNotClosed() throws SQLException {
		if(closeAfterCompletion || connection.isClosed()) {
			throw new SQLException("Connection is closed");
		}
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
	public void abort(final Executor executor) throws SQLException {
		connection.abort(executor);

	}

	@Override
	public void clearWarnings() throws SQLException {
		checkNotClosed();
		connection.clearWarnings();
	}


	@Override
	public void close() throws SQLException {
		if(Status.STATUS_NO_TRANSACTION == transactionManager.getStatus()) {
			xaConnection.close();
		} else {
			LOGGER.debug("Registering connection as closed; keeping until completion of transaction");
			closeAfterCompletion = true;
		}
	}

	@Override
	public void commit() throws SQLException {
		checkNotClosed();
		connection.commit();

	}

	@Override
	public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
		checkNotClosed();
		return	connection.createArrayOf(typeName, elements);
	}

	@Override
	public Blob createBlob() throws SQLException {
		checkNotClosed();
		return connection.createBlob();
	}

	@Override
	public Clob createClob() throws SQLException {
		checkNotClosed();
		return connection.createClob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		checkNotClosed();
		return connection.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		checkNotClosed();
		return connection.createSQLXML();
	}

	@Override
	public Statement createStatement() throws SQLException {
		checkNotClosed();
		return connection.createStatement();
	}

	@Override
	public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
		checkNotClosed();
		return connection.createStatement(resultSetType, resultSetConcurrency);
	}

	@Override
	public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability)
			throws SQLException {
		checkNotClosed();
		return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
		checkNotClosed();
		return connection.createStruct(typeName, attributes);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		checkNotClosed();
		return connection.getAutoCommit();
	}

	@Override
	public String getCatalog() throws SQLException {
		checkNotClosed();
		return connection.getCatalog();
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		checkNotClosed();
		return connection.getClientInfo();
	}

	@Override
	public String getClientInfo(final String name) throws SQLException {
		checkNotClosed();
		return connection.getClientInfo(name);
	}

	@Override
	public int getHoldability() throws SQLException {
		checkNotClosed();
		return connection.getHoldability();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		checkNotClosed();
		return connection.getMetaData();
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		checkNotClosed();
		return connection.getNetworkTimeout();
	}

	@Override
	public String getSchema() throws SQLException {
		checkNotClosed();
		return connection.getSchema();
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		checkNotClosed();
		return connection.getTransactionIsolation();
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		checkNotClosed();
		return connection.getTypeMap();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		checkNotClosed();
		return connection.getWarnings();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return closeAfterCompletion || connection.isClosed();
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		checkNotClosed();
		return connection.isReadOnly();
	}

	@Override
	public boolean isValid(final int timeout) throws SQLException {
		checkNotClosed();
		return connection.isValid(timeout);
	}

	@Override
	public String nativeSQL(final String sql) throws SQLException {
		checkNotClosed();
		return connection.nativeSQL(sql);
	}

	@Override
	public CallableStatement prepareCall(final String sql) throws SQLException {
		checkNotClosed();
		return connection.prepareCall(sql);
	}

	@Override
	public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
		checkNotClosed();
		return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		checkNotClosed();
		return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql) throws SQLException {
		checkNotClosed();
		return connection.prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
		checkNotClosed();
		return connection.prepareStatement(sql, autoGeneratedKeys);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
		checkNotClosed();
		return connection.prepareStatement(sql, columnIndexes);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
		checkNotClosed();
		return connection.prepareStatement(sql, columnNames);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
			throws SQLException {
		checkNotClosed();
		return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		checkNotClosed();
		return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
		checkNotClosed();
		connection.releaseSavepoint(savepoint);
	}

	@Override
	public void rollback() throws SQLException {
		checkNotClosed();
		connection.rollback();
	}

	@Override
	public void rollback(final Savepoint savepoint) throws SQLException {
		checkNotClosed();
		connection.rollback(savepoint);
	}

	@Override
	public void setAutoCommit(final boolean autoCommit) throws SQLException {
		checkNotClosed();
		connection.setAutoCommit(autoCommit);
	}

	@Override
	public void setCatalog(final String catalog) throws SQLException {
		checkNotClosed();
		connection.setCatalog(catalog);
	}

	@Override
	public void setClientInfo(final Properties properties) throws SQLClientInfoException {
		connection.setClientInfo(properties);
	}

	@Override
	public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
		connection.setClientInfo(name, value);
	}

	@Override
	public void setHoldability(final int holdability) throws SQLException {
		checkNotClosed();
		connection.setHoldability(holdability);
	}

	@Override
	public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
		checkNotClosed();
		connection.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public void setReadOnly(final boolean readOnly) throws SQLException {
		checkNotClosed();
		connection.setReadOnly(readOnly);
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		checkNotClosed();
		return connection.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(final String name) throws SQLException {
		checkNotClosed();
		return connection.setSavepoint(name);
	}

	@Override
	public void setSchema(final String schema) throws SQLException {
		checkNotClosed();
		connection.setSchema(schema);
	}

	@Override
	public void setTransactionIsolation(final int level) throws SQLException {
		checkNotClosed();
		connection.setTransactionIsolation(level);
	}

	@Override
	public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
		checkNotClosed();
		connection.setTypeMap(map);
	}
}
