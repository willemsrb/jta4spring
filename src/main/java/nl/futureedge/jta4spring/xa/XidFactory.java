package nl.futureedge.jta4spring.xa;

import javax.transaction.xa.Xid;

public class XidFactory {

	public static final Xid createXid() {
		return XidImpl.getUniqueXid((int)Thread.currentThread().getId());
	}
}
