package gov.nsa.ia.util;

import gov.nsa.ia.drbg.EntropySource;

/**
 * The LousyEntropySource is completely deterministic. It returns exactly the
 * minimum requested bytes, all 'a'. It is used only for performing self-test of
 * this class.
 *
 * @author nziring
 */
public class LousyEntropySource implements EntropySource {

	@Override
	public byte[] getEntropy(int requestedEntropyBits, int minOutputBytes, int maxOutputBytes) {
		byte[] ret;
		ret = new byte[minOutputBytes];

		for (int i = 0; i < minOutputBytes; i++) {
			ret[i] = (byte) 'a';
		}
		return ret;
	}

	@Override
	public boolean performSelfTest() {
		return true;
	}

	@Override
	public byte[] getSelfTestEntropy() {
		return null;
	}

	@Override
	public void dispose() {
		return;
	}
}
