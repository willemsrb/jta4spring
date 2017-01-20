package nl.futureedge.jta4spring;

import javax.transaction.xa.Xid;

public class JtaXid implements Xid {

	private static final int DEFAULT_FORMAT_ID = 0x1ee3;

	private final byte[] globalTransactionId;
	private final byte[] branchQualifier;

	public JtaXid(final String uniqueName, final long transactionId) {
		branchQualifier = new byte[] { (byte)0 };
		globalTransactionId = (uniqueName + "-" + transactionId).getBytes();
	}

	@Override
	public byte[] getBranchQualifier() {
		return branchQualifier;
	}

	@Override
	public int getFormatId() {
		return DEFAULT_FORMAT_ID;
	}

	@Override
	public byte[] getGlobalTransactionId() {
		return globalTransactionId;
	}
}
