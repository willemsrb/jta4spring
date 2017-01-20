package nl.futureedge.jta4spring.it;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import nl.futureedge.jta4spring.JtaXid;

public class XaDataSourceTest extends AbstractIT {

	@Autowired
	private XADataSource xaDataSource;

	@Test
	@Ignore
	public void test() throws XAException, SQLException {
		final Xid xid = new JtaXid("test2", 4);

		System.out.println("Connection 1");
		final XAConnection xaConnection1 = xaDataSource.getXAConnection();
		final XAResource xaResource1 = xaConnection1.getXAResource();
		final Connection connection1 = xaConnection1.getConnection();

		System.out.println("Connection 2");
		final XAConnection xaConnection2 = xaDataSource.getXAConnection();
		final XAResource xaResource2 = xaConnection2.getXAResource();
		final Connection connection2 = xaConnection2.getConnection();

		System.out.println("Start 1");
		xaResource1.start(xid, XAResource.TMNOFLAGS);
		if(xaResource2.isSameRM(xaResource1)) {
			System.out.println("Join 2");
			xaResource2.start(xid, XAResource.TMJOIN);
		} else {
			System.out.println("Start 2");
			xaResource2.start(xid, XAResource.TMNOFLAGS);
		}

		System.out.println("Statement 1");
		final Statement statement1 = connection1.createStatement();
		statement1.execute("insert into test(id, description) values(1, 'first')");
		statement1.close();

		System.out.println("Statement 2 - create");
		final Statement statement2 = connection2.createStatement();
		System.out.println("Statement 2 - execute");
		statement2.execute("insert into test(id, description) values(2, 'second')");
		System.out.println("Statement 2 - close");
		statement2.close();

		System.out.println("Prepare 1");
		xaResource1.end(xid, XAResource.TMSUCCESS);
		xaResource1.prepare(xid);

		System.out.println("Prepare 2");
		xaResource2.end(xid, XAResource.TMSUCCESS);
		xaResource2.prepare(xid);

		System.out.println("Commit");
		xaResource1.commit(xid, false);
		xaResource2.commit(xid, false);

		System.out.println("Close");
		connection1.close();
		connection2.close();


	}

}
