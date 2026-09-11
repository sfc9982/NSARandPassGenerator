package gov.nsa.ia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * KeyUnwrapper is a class that decrypts an encrypted generated hexadecimal key
 * using 256 bit AES key derived from random password. Saves unencrypted key as
 * .txt file.
 *
 * @author amsagos
 */

public class KeyUnwrapper {

	/**
	 * Magic bytes identifying the encrypted file header. Matches KeyWrapper's
	 * MAGIC. Format: [2 bytes magic "NS"] [16 bytes salt] [wrapped key].
	 */
	private static final byte[] MAGIC = new byte[] { 'N', 'S' };

	/**
	 * Length in bytes of the random salt stored in the encrypted file header.
	 */
	private static final int SALT_LENGTH = 16;

	/**
	 * Total length of the header, magic bytes plus salt.
	 */
	private static final int HEADER_LENGTH = MAGIC.length + SALT_LENGTH;

	public static void fileProcessor(char[] PW, File encryptedFile, File decryptedFile) {
		try {

			// DPKDF2 NIST SP 800-132
			// salt value, read from the encrypted file header
			byte[] inputBytes = new byte[(int) encryptedFile.length()];
			try (FileInputStream inputStream = new FileInputStream(encryptedFile)) {
				inputStream.read(inputBytes);
			}

			// validate the header magic bytes, otherwise the format is unsupported
			if (inputBytes.length < HEADER_LENGTH || inputBytes[0] != MAGIC[0] || inputBytes[1] != MAGIC[1]) {
				System.out.println("Not a valid encrypted key file format, failed to decrypt key");
				return;
			}
			byte[] salt = new byte[SALT_LENGTH];
			System.arraycopy(inputBytes, MAGIC.length, salt, 0, SALT_LENGTH);

			// iteration count
			int iterCount = 100000;

			int derivedKeyLength = 256; // Should be at least 256 bits.

			KeySpec spec = new PBEKeySpec(PW, salt, iterCount, derivedKeyLength);
			SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

			SecretKey secretKey = f.generateSecret(spec);

			SecretKey cipherKey = new SecretKeySpec(secretKey.getEncoded(), "AES");

			// RFC 3394
			int cipherMode = Cipher.UNWRAP_MODE;
			Cipher cipher = Cipher.getInstance("AESWrap", "SunJCE");

			// unwrap key

			if (PW.length < 16) {
				System.out.println("Password must be at least 16 characters, failed to decrypt key");
				return;
			} else {
				cipher.init(cipherMode, cipherKey, cipher.getParameters());
			}
			byte[] wrappedBytes = new byte[inputBytes.length - HEADER_LENGTH];
			System.arraycopy(inputBytes, HEADER_LENGTH, wrappedBytes, 0, wrappedBytes.length);
			Key outputKey = cipher.unwrap(wrappedBytes, "AES", Cipher.SECRET_KEY);

			byte[] outputBytes = outputKey.getEncoded();

			FileOutputStream outputStream = new FileOutputStream(decryptedFile);
			outputStream.write(outputBytes);

			outputStream.close();

		} catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IOException
				| NoSuchProviderException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
	}

}
