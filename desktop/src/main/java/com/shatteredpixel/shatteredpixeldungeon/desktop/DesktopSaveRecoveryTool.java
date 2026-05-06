package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SaveRecoveryManager;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.watabou.utils.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class DesktopSaveRecoveryTool {

	private static final String GAME_FILE_NAME = "game.dat";
	private static final String GAME_FOLDER_PREFIX = "game";
	private static final String HERO_KEY = "hero";
	private static final String DEPTH_KEY = "depth";
	private static final String CLASS_KEY = "class";
	private static final String SUBCLASS_KEY = "subClass";
	private static final String LEVEL_KEY = "lvl";

	public static void main(String[] args) throws Exception {
		// if user type "-h", "help", "-help", print the helper document
		if (args.length > 0 && isHelp(args[0])) {
			printUsage();
			return;
		}

		// find the target path
		String title = System.getProperty("Specification-Title", "Shattered Pixel Dungeon");
		String implementationTitle = System.getProperty(
				"Implementation-Title",
				"com.shatteredpixel.shatteredpixeldungeon"
		);
		DesktopSavePaths.ResolvedSavePath savePath = DesktopSavePaths.resolve(title, implementationTitle);
		File baseDir = savePath.asFile();
		File recoveryDir = new File(baseDir, SaveRecoveryManager.RECOVERY_DIR);

		// find the recovery path
		List<RecoveryEntry> recoveries = loadRecoveries(recoveryDir);

		// print root and handle the empty case
		System.out.println("Save root: " + baseDir.getAbsolutePath());
		if (recoveries.isEmpty()) {
			System.out.println("No deleted saves are currently available for recovery.");
			return;
		}

		// print games as a table
		printRecoveries(recoveries);

		// listen the selected index or its given
		RecoveryEntry selected = args.length > 0
				? selectRecoveryById(recoveries, args[0])
				: promptForRecovery(recoveries);
		if (selected == null) {
			System.out.println("Recovery cancelled.");
			return;
		}

		// find a slot number
		int targetSlot = resolveTargetSlot(baseDir, selected.archiveDir);
		if (targetSlot < 1 || targetSlot > GamesInProgress.MAX_SLOTS) {
			System.err.println("No empty save slot is available for recovery.");
			System.exit(1);
		}

		// move it back to game directory
		File targetDir = new File(baseDir, gameFolder(targetSlot));
		if (targetDir.exists()) {
			System.err.println("Target slot " + targetSlot + " already exists: " + targetDir.getAbsolutePath());
			System.exit(1);
		}

		Files.createDirectories(targetDir.getParentFile().toPath());
		Files.move(selected.archiveDir.toPath(), targetDir.toPath(), StandardCopyOption.ATOMIC_MOVE);

		System.out.println(
				"Recovered save " + selected.id + " into slot " + targetSlot +
				" at " + targetDir.getAbsolutePath()
		);
	}

	/*
		this method is used to determine wether user enter a help command
	 */
	private static boolean isHelp(String arg) {
		return "help".equalsIgnoreCase(arg) || "--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg);
	}

	/*
		return all the recoverable element as a list
	 */
	private static List<RecoveryEntry> loadRecoveries(File recoveryDir) throws IOException {
		List<File> recoveryDirs = sortedRecoveryDirs(recoveryDir);
		List<RecoveryEntry> entries = new ArrayList<>();
		for (int i = 0; i < recoveryDirs.size(); i++) {
			File archiveDir = recoveryDirs.get(i);
			entries.add(buildRecoveryEntry(i + 1, archiveDir));
		}
		return entries;
	}

	/*
		construct a recoverable element
	 */
	private static RecoveryEntry buildRecoveryEntry(int id, File archiveDir) throws IOException {
		long timestamp = SaveRecoveryManager.archiveTimestamp(archiveDir.getName());

		File gameFile = new File(archiveDir, GAME_FILE_NAME);
		try (FileInputStream stream = new FileInputStream(gameFile)) {
			Bundle bundle = Bundle.read(stream);
			return buildRecoveryEntryFromBundle(id, archiveDir, timestamp, bundle);
		}
	}

	/*
		read the key information for target game save that will be shown to user 
	 */
	private static RecoveryEntry buildRecoveryEntryFromBundle(
			int id,
			File archiveDir,
			long timestamp,
			Bundle bundle
	) {
		int floor = bundle.getInt(DEPTH_KEY);
		Bundle heroBundle = bundle.getBundle(HERO_KEY);

		HeroClass heroClass = heroBundle.getEnum(CLASS_KEY, HeroClass.class);
		HeroSubClass subClass = heroBundle.getEnum(SUBCLASS_KEY, HeroSubClass.class);
		int level = heroBundle.getInt(LEVEL_KEY);

		String hero = subClass != null && subClass != HeroSubClass.NONE
				? formatEnumName(subClass.name())
				: (heroClass != null ? formatEnumName(heroClass.name()) : "Unknown");

		return new RecoveryEntry(id, archiveDir, timestamp, hero, floor, level);
	}

	/*
		print the table to stdout
	 */
	private static void printRecoveries(List<RecoveryEntry> recoveries) {
		System.out.println("Deleted saves:");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
		System.out.printf(
				Locale.ROOT,
				"%-4s %-18s %-7s %-7s %-19s%n",
				"ID", "Hero", "Floor", "Level", "Deleted Time"
		);
		System.out.println("---------------------------------------------------------------");
		for (RecoveryEntry recovery : recoveries) {
			String deletedAt = recovery.deletedAt >= 0L ? format.format(new Date(recovery.deletedAt)) : "unknown";
			System.out.printf(
					Locale.ROOT,
					"%-4d %-18s %-7d %-7d %-19s%n",
					recovery.id,
					recovery.hero,
					recovery.floor,
					recovery.level,
					deletedAt
			);
		}
	}

	/*
		Scanner to listen a input from user
	 */
	private static RecoveryEntry promptForRecovery(List<RecoveryEntry> recoveries) {
		System.out.print("Enter the numeric id to recover, or press Enter to cancel: ");
		Scanner scanner = new Scanner(System.in);
		if (!scanner.hasNextLine()) {
			System.out.println();
			System.out.println("No interactive input detected. Re-run with stdin attached or pass an id directly.");
			return null;
		}
		String input = scanner.nextLine().trim();
		if (input.isEmpty() || "q".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
			return null;
		}

		int selectedId;
		try {
			selectedId = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid id: " + input);
			System.exit(1);
			return null;
		}

		return selectRecoveryById(recoveries, selectedId);
	}

	/*
		parse the all the element in the list to int
	 */
	private static RecoveryEntry selectRecoveryById(List<RecoveryEntry> recoveries, String input) {
		int selectedId;
		try {
			selectedId = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid id: " + input);
			System.exit(1);
			return null;
		}
		return selectRecoveryById(recoveries, selectedId);
	}

	/*
		return a RecoveryEntry base on the given id
	 */
	private static RecoveryEntry selectRecoveryById(List<RecoveryEntry> recoveries, int selectedId) {
		for (RecoveryEntry recovery : recoveries) {
			if (recovery.id == selectedId) {
				return recovery;
			}
		}

		System.err.println("No recovery entry exists for id " + selectedId);
		System.exit(1);
		return null;
	}

	/*
		find the frist avalible game index
	 */
	private static int resolveTargetSlot(File baseDir, File recovery) {
		for (int slot = 1; slot <= GamesInProgress.MAX_SLOTS; slot++) {
			if (!new File(baseDir, gameFolder(slot)).exists()) {
				return slot;
			}
		}
		return -1;
	}

	/*
		find and sort the File instance, return a File list
	 */
	private static List<File> sortedRecoveryDirs(File recoveryDir) {
		List<File> recoveries = new ArrayList<>();
		File[] files = recoveryDir.listFiles();
		if (files == null) {
			return recoveries;
		}

		for (File file : files) {
			if (file.isDirectory() && SaveRecoveryManager.archiveTimestamp(file.getName()) >= 0L) {
				recoveries.add(file);
			}
		}

		recoveries.sort(new Comparator<File>() {
			@Override
			public int compare(File left, File right) {
				return Long.compare(
						SaveRecoveryManager.archiveTimestamp(right.getName()),
						SaveRecoveryManager.archiveTimestamp(left.getName())
				);
			}
		});
		return recoveries;
	}

	/*
		name the game folder as gameN
	 */
	private static String gameFolder(int slot) {
		return GAME_FOLDER_PREFIX + slot;
	}

	/*
		transfer the enumerate variable name to printed version
	 */
	private static String formatEnumName(String enumName) {
		String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < words.length; i++) {
			if (words[i].isEmpty()) {
				continue;
			}
			if (i > 0) {
				result.append(' ');
			}
			result.append(Character.toUpperCase(words[i].charAt(0)));
			if (words[i].length() > 1) {
				result.append(words[i].substring(1));
			}
		}
		return result.toString();
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("  ./gradlew desktop:recoverSave");
		System.out.println("  ./gradlew desktop:recoverSave --args=\"2\"");
		System.out.println("Run without args for interactive selection, or pass a numeric id directly.");
	}

	/*
		container for the key info
	 */
	private static final class RecoveryEntry {
		final int id;
		final File archiveDir;
		final long deletedAt;
		final String hero;
		final int floor;
		final int level;

		RecoveryEntry(int id, File archiveDir, long deletedAt, String hero, int floor, int level) {
			this.id = id;
			this.archiveDir = archiveDir;
			this.deletedAt = deletedAt;
			this.hero = hero;
			this.floor = floor;
			this.level = level;
		}
	}
}
