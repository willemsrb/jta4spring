package nl.futureedge.jta4spring.xa;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import javax.transaction.xa.Xid;

import org.hsqldb.jdbc.pool.JDBCXID;

public class XidImpl implements Xid {

	int formatID;
	byte[] txID;
	byte[] txBranch;
	//
	int hash;
	boolean hashComputed;

	@Override
	public int getFormatId() {
		return formatID;
	}

	@Override
	public byte[] getGlobalTransactionId() {
		return txID;
	}

	@Override
	public byte[] getBranchQualifier() {
		return txBranch;
	}

	public XidImpl(final int formatID, final byte[] txID, final byte[] txBranch) {

		this.formatID = formatID;
		this.txID = txID;
		this.txBranch = txBranch;
	}

	@Override
	public int hashCode() {
		if (!hashComputed) {
			hash = 7;
			hash = 83 * hash + formatID;
			hash = 83 * hash + Arrays.hashCode(txID);
			hash = 83 * hash + Arrays.hashCode(txBranch);
			hashComputed = true;
		}
		return hash;
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Xid) {
			final Xid o = (Xid) other;

			return formatID == o.getFormatId()
					&& Arrays.equals(txID, o.getGlobalTransactionId())
					&& Arrays.equals(txBranch, o.getBranchQualifier());
		}

		return false;
	}

	// inherit doc
	@Override
	public String toString() {

		final StringBuffer sb = new StringBuffer(512);
		//
		sb.append("formatId=").append(getFormatId());
		//
		sb.append(" globalTransactionId(").append(txID.length).append(")={0x");
		for (final byte element : txID) {
			final int hexVal = element & 0xFF;
			if (hexVal < 0x10) {
				sb.append("0").append(Integer.toHexString(element & 0xFF));
			}
			sb.append(Integer.toHexString(element & 0xFF));
		}
		//
		sb.append("} branchQualifier(").append(txBranch.length).append("))={0x");
		for (final byte element : txBranch) {
			final int hexVal = element & 0xFF;
			if (hexVal < 0x10) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(element & 0xFF));
		}
		sb.append("}");
		//
		return sb.toString();
	}

	private static byte[] s_localIp = null;
	private static int s_txnSequenceNumber = 0;
	//
	private static final int UXID_FORMAT_ID = 0xFEED;

	private static int nextTxnSequenceNumber() {
		s_txnSequenceNumber++;
		return  s_txnSequenceNumber;
	}

	private static byte[] getLocalIp() {
		if (null == s_localIp) {
			try {
				s_localIp = InetAddress.getLocalHost().getAddress();
			} catch (final Exception ex) {
				s_localIp = new byte[]{0x7F, 0x00, 0x00, 0x01};
			}
		}
		return s_localIp;
	}

	/**
	 * Retrieves a randomly generated JDBCXID.
	 *
	 * The newly generated object is based on the local IP address, the given
	 * <tt>threadId</tt> and a randomly generated number using the current time
	 * in milliseconds as the random seed.
	 *
	 * Note that java.util.Random is used, not java.security.SecureRandom.
	 *
	 * @param threadId can be a real thread id or just some convenient
	 *        tracking value.
	 *
	 * @return a randomly generated JDBCXID
	 */
	public static Xid getUniqueXid(final int threadId) {
		final Random random = new Random(System.currentTimeMillis());
		//
		int txnSequenceNumberValue = nextTxnSequenceNumber();
		int threadIdValue = threadId;
		int randomValue = random.nextInt();
		//
		final byte[] globalTransactionId = new byte[MAXGTRIDSIZE];
		final byte[] branchQualifier = new byte[MAXBQUALSIZE];
		final byte[] localIp = getLocalIp();

		System.arraycopy(localIp, 0, globalTransactionId, 0, 4);
		System.arraycopy(localIp, 0, branchQualifier, 0, 4);

		// Bytes 4 -> 7 - unique transaction id.
		// Bytes 8 ->11 - thread id.
		// Bytes 12->15 - random.
		for (int i = 0; i <= 3; i++) {
			globalTransactionId[i + 4] = (byte) (txnSequenceNumberValue % 0x100);
			branchQualifier[i + 4] = (byte) (txnSequenceNumberValue % 0x100);
			txnSequenceNumberValue >>= 8;
		globalTransactionId[i + 8] = (byte) (threadIdValue % 0x100);
		branchQualifier[i + 8] = (byte) (threadIdValue % 0x100);
		threadIdValue >>= 8;
			globalTransactionId[i + 12] = (byte) (randomValue % 0x100);
			branchQualifier[i + 12] = (byte) (randomValue % 0x100);
			randomValue >>= 8;
		}

		return new JDBCXID(UXID_FORMAT_ID, globalTransactionId, branchQualifier);
	}
}
