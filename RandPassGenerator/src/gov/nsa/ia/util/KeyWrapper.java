package gov.nsa.ia.util;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * KeyWrapper is a class that encrypts a generated hexadecimal key using 256 bit
 * AES encryption key derived from random password. Saves encrypted key as .enc
 * file.
 *
 * @author amsagos
 */

public class KeyWrapper {

	/**
	 * Magic bytes identifying the encrypted file header. Followed by a 16-byte
	 * random salt, then the AES-wrapped key bytes. Format:
	 * [2 bytes magic "NS"] [16 bytes salt] [wrapped key].
	 */
	private static final byte[] MAGIC = new byte[] { 'N', 'S' };

	/**
	 * Length in bytes of the random salt written to each encrypted file header.
	 */
	private static final int SALT_LENGTH = 16;

	public static void fileProcessor(char[] pass, String inputKey, File encryptedFile) {
		try {

			while (pass.length < 16) {
				System.out.println("Password must be at least 16 characters, failed to encrypt key");
				System.out.print("Provide a random password of at leaset 16 characters: ");
				// BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				Console br = System.console();
				pass = br.readPassword();
			}
			// DPKDF2 NIST SP 800-132
			// salt value, freshly generated per encryption so it is unique
			byte[] salt = new byte[SALT_LENGTH];
			new SecureRandom().nextBytes(salt);

			// iteration count
			int iterCount = 100000;

			int derivedKeyLength = 256; // Should be at least 256 bits.

			KeySpec spec = new PBEKeySpec(pass, salt, iterCount, derivedKeyLength);
			SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

			SecretKey secretKey = f.generateSecret(spec);
			SecretKey cipherKey = new SecretKeySpec(secretKey.getEncoded(), "AES");

			// RFC 3394
			int cipherMode = Cipher.WRAP_MODE;
			Cipher cipher = Cipher.getInstance("AESWrap", "SunJCE");

			// setup the key encryption key and the to-be-wrapped key

			byte[] newKey = inputKey.getBytes(StandardCharsets.UTF_8);
			SecretKey WrapThisKey = new SecretKeySpec(newKey, "AES");

			if (pass.length < 16) {
				System.out.println("Password must be at least 16 characters, failed to encrypt key");
				return;
			} else {
				cipher.init(cipherMode, cipherKey);
			}
			byte[] outputBytes = cipher.wrap(WrapThisKey);
			FileOutputStream outputStream = new FileOutputStream(encryptedFile);
			outputStream.write(MAGIC);
			outputStream.write(salt);
			outputStream.write(outputBytes);

			outputStream.close();
		} catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException
				| IOException | NoSuchProviderException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}

}
