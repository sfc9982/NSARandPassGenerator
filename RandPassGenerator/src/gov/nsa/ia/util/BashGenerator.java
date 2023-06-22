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
		bg.generatePasswords(count, strength);

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

	private RandManager randman;

	private ArrayList<String> usernames;

	private ArrayList<String> passwords;

	/**
	 * Initialize this RandPassGenerator using the supplied log file and the
	 * supplied entropy startup file.
	 *
	 * @param logfile Path to a writeable log file, null to log to stderr
	 * @param pw      A usable printwriter for output, may not be null
	 * @param verbose if true, print verbose messages
	 * @return
	 */
	public BashGenerator() {
		boolean die = false;

		randman = new RandManager("RandPassGenToBash", Log.getMute());

		EntropySource primarysrc = null;

		try {
			primarysrc = new JavaSecRandEntropySource();

			randman.setPrimarySource(primarysrc);
			randman.setStartupSource(primarysrc);

			message("RandPassGen - created randomness manager, about to initialize and perform self-test");

			if (randman.performDRBGKATest()) {
				message("Random generator code known answers self-test passed.");
				message("RandPassGen - underlying DRBG implementation known answer self-test passed.");
			} else {
				debugLog.severe(
						"RandPassGen - underlying DRBG implementation known answer self-test failed!  This should never happen.  JVM broken?  Exiting.");
				System.exit(1);
			}

			message("Initializing randomness; this will take some time if system entropy isn't full.  Be patient, or go do something else on this computer to help the system gather more entropy.  Thank you.");

			if (randman.initialize()) {
				message("RandPassGen - initialization succeeded, proceeding to self-test.");
				if (randman.performSelfTest()) {
					message("Random generation manager self-tests passed.");
				} else {
					message("Randomness self-test failed.  Exiting.");
				}
			}

		} catch (Exception e) {
			debugLog.severe("Exception in RandPassGenerator setup, fatal.");
			message("Error in RandPassGen startup: " + e);
			die = true;
		}

		if (die) {
			throw new RuntimeException("Error in RandPassGen startup.");
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
			debugLog.severe("RandPassGen - DRBG is not ok, error");
			message("Error - DRBG is not usable, sorry.");
			return -1;
		}

		if (strength > (drbg.getStrength() * 2)) {
			debugLog.warning("RandPassGen - requested password strength " + strength
					+ " is greater than hash size of underlying DRBG.  Error.");
			message("Error - requested password strength of " + strength
					+ " is greater than the hash size of the underlying DRBG");
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
				debugLog.warning("RandPassGen - character set password generator failed!  Error.");
				return -1;
			} else {
				passes.add(pass);
			}
		}

		message("RandPassGen - generated " + passes.size() + " passwords at strength " + strength);

		passwords = passes;

		for (String p : passwords) {
			debugLog.info(p);
		}
		message("RandPassGen - save passwords to designated object member");

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
			userlist.append("\"" + user + "\"" + " ");
		}

		for (String pass : passwords) {
			passlist.append("\"" + pass + "\"" + " ");
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
