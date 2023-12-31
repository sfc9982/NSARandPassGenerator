package gov.nsa.ia.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nsa.ia.drbg.AbstractDRBG;
import gov.nsa.ia.drbg.EntropySource;
import gov.nsa.ia.drbg.JavaSecRandEntropySource;
import gov.nsa.ia.gen.CharacterSet;
import gov.nsa.ia.pass.RandManager;

public class BashGenerator {

	private static final Logger debugLog = Log.getLogger(BashGenerator.class.getName());

	public static void generateBashFormat(int strength, String userList) {
		BashGenerator bg = new BashGenerator();
		String[] users = userList.split(" ");
		int count = users.length;

		bg.usernames = new ArrayList<>(Arrays.asList(users));
		int retCount = bg.generatePasswords(count, strength);

		if (retCount != count) {
			debugLog.warning("Password number generated doesn't match the input");
		}

		bg.printCredentials();
		bg.printBash();
	}

	public static void main(String[] args) {
		debugLog.log(Level.INFO, "Start Bash generation test");
		generateBashFormat(160, "Tom Jerry UncleSam foo bar");
	}

	private static void message(String s) {
		debugLog.info(s);
	}

	private final RandManager randman;

	private ArrayList<String> usernames;

	private ArrayList<String> passwords;

	/**
	 * Initialize this BashGenerator and its RandManager object.
	 */
	public BashGenerator() {
		randman = new RandManager("RandPassGenToBash", Log.getMute());

		EntropySource primarysrc = null;

		try {
			primarysrc = new JavaSecRandEntropySource();

			randman.setPrimarySource(primarysrc);
			randman.setStartupSource(primarysrc);

			if (!randman.performDRBGKATest()) {
				debugLog.severe("Underlying DRBG implementation known answer self-test failed.");
				System.exit(1);
			}
			if (!randman.initialize()) {
				message("Initialization failled.");
				System.exit(1);
			}
			if (!randman.performSelfTest()) {
				message("Randomness self-test failed.");
				System.exit(1);
			}

		} catch (Exception e) {
			debugLog.severe("Exception in RandPassGenerator setup, fatal.");
			message("Error in Bash Generator startup: " + e);
		}
	}

	private int generatePasswords(int count, int strength) {
		CharacterSet cs = new CharacterSet(debugLog);
		AbstractDRBG drbg;

		if (count < 1) {
			debugLog.warning("RandPassGen - count for passwords < 1, error");
			message("Error - number of passwords must be > 0");
			return -1;
		}

		drbg = randman.getDRBG();

		if (!(drbg.isOkay())) {
			debugLog.severe("DRBG is not ok, error");
			return -1;
		}

		if (strength > (drbg.getStrength() * 2)) {
			debugLog.warning(
					"Requested password strength " + strength + " is greater than hash size of underlying DRBG.");
			return -1;
		}

		message("RandPassGen - initialized password character set, size=" + cs.size());

		int i;
		String pass;
		ArrayList<String> passes = new ArrayList<>(count);
		cs.addSet(CharacterSet.DEFAULT_USABLE);
		for (i = 0; i < count; i++) {
			pass = cs.getRandomStringByEntropy(strength, drbg);
			if (pass == null) {
				debugLog.warning("Character set password generator failed.");
				return -1;
			} else {
				passes.add(pass);
			}
		}

		message("Generated " + passes.size() + " passwords at strength " + strength);

		passwords = passes;

		for (String p : passwords) {
			debugLog.info(p);
		}
		message("Save passwords to designated object member");

		return passes.size();
	}

	private void printBash() {
		String bashUpper;
		String bashLower;

		StringBuilder userlist = new StringBuilder(" ");
		StringBuilder passlist = new StringBuilder(" ");

		bashUpper = "\r\n" + "#!/bin/bash" + "\r\n";
		bashLower = "num_users=${#user_list[@]}\r\n" + "num_passwords=${#password_list[@]}" + "\r\n"
				+ "if [ $num_users -ne $num_passwords ]\r\n" + "then\r\n"
				+ "    echo \"Error: Number of users and passwords does not match\"\r\n" + "    exit 1\r\n" + "fi"
				+ "\r\n" + "for (( i=0; i<${num_users}; i++ ))\r\n" + "do\r\n" + "    useradd ${user_list[$i]}\r\n"
				+ "    echo \"${user_list[$i]}:${password_list[$i]}\" | chpasswd\r\n" + "done" + "\r\n" + "\r\n";

		message("Print credentials to console");

		if (usernames.size() != passwords.size()) {
			debugLog.warning("username and password list's size don't match");
			return;
		}

		for (String user : usernames) {
			userlist.append("\"").append(user).append("\"").append(" ");
		}

		for (String pass : passwords) {
			passlist.append("\"").append(pass).append("\"").append(" ");
		}

		System.out.print(bashUpper);
		System.out.printf("user_list=(%s)%n", userlist);
		System.out.printf("password_list=(%s)%n", passlist);
		System.out.print(bashLower);
	}

	private void printCredentials() {
		if (usernames.size() != passwords.size()) {
			debugLog.warning("Credential size doesn't match!");
			return;
		}

		for (int i = 0; i < usernames.size(); i++) {
			System.out.printf("%s:%s%n", usernames.get(i), passwords.get(i));
		}
	}

}
