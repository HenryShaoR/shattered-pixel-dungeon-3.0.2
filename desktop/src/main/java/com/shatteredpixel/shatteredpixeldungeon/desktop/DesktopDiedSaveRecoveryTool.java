package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SaveRecoveryManager;
import com.watabou.utils.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DesktopDiedSaveRecoveryTool {

	private static final String GAME_FILE_NAME = "game.dat";
	private static final String GAME_FOLDER_PREFIX = "game";
	private static final String HERO_KEY = "hero";
	private static final String HP_KEY = "HP";
	private static final String HT_KEY = "HT";

	public static void main(String[] args) throws Exception {
		if (args.length > 0 && isHelp(args[0])) {
			printUsage();
			return;
		}

		String title = System.getProperty("Specification-Title", "Shattered Pixel Dungeon");
		String implementationTitle = System.getProperty(
				"Implementation-Title",
				"com.shatteredpixel.shatteredpixeldungeon"
		);
		DesktopSavePaths.ResolvedSavePath savePath = DesktopSavePaths.resolve(title, implementationTitle);
		File baseDir = savePath.asFile();
		File diedDir = new File(baseDir, SaveRecoveryManager.DIED_DIR);

		System.out.println("Save root: " + baseDir.getAbsolutePath());

		File latestArchive = latestArchiveDir(diedDir);
		if (latestArchive == null) {
			System.out.println("No died save is currently available for recovery.");
			return;
		}

		int targetSlot = args.length > 0 ? parseTargetSlot(args[0]) : resolveTargetSlot(baseDir);
		if (targetSlot < 1 || targetSlot > GamesInProgress.MAX_SLOTS) {
			throw new IOException("No empty save slot is available for recovery.");
		}

		File targetDir = new File(baseDir, gameFolder(targetSlot));
		if (targetDir.exists()) {
			throw new IOException("Target slot " + targetSlot + " already exists: " + targetDir.getAbsolutePath());
		}

		Files.createDirectories(targetDir.getParentFile().toPath());
		Files.move(latestArchive.toPath(), targetDir.toPath(), StandardCopyOption.ATOMIC_MOVE);
		restoreHeroToFullHp(new File(targetDir, GAME_FILE_NAME));

		System.out.println(
				"Recovered died save into slot " + targetSlot +
				" at " + targetDir.getAbsolutePath() +
				" with hero HP restored to max."
		);
	}

	private static boolean isHelp(String arg) {
		return "help".equalsIgnoreCase(arg) || "--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg);
	}

	private static File latestArchiveDir(File archiveRoot) {
		List<File> archives = sortedArchiveDirs(archiveRoot);
		return archives.isEmpty() ? null : archives.get(0);
	}

	private static List<File> sortedArchiveDirs(File archiveRoot) {
		List<File> archives = new ArrayList<>();
		File[] files = archiveRoot.listFiles();
		if (files == null) {
			return archives;
		}

		for (File file : files) {
			if (file.isDirectory() && SaveRecoveryManager.archiveTimestamp(file.getName()) >= 0L) {
				archives.add(file);
			}
		}

		archives.sort(new Comparator<File>() {
			@Override
			public int compare(File left, File right) {
				return Long.compare(
						SaveRecoveryManager.archiveTimestamp(right.getName()),
						SaveRecoveryManager.archiveTimestamp(left.getName())
				);
			}
		});

		return archives;
	}

	private static int parseTargetSlot(String input) throws IOException {
		try {
			int slot = Integer.parseInt(input);
			if (slot < 1 || slot > GamesInProgress.MAX_SLOTS) {
				throw new IOException("Target slot must be between 1 and " + GamesInProgress.MAX_SLOTS + ".");
			}
			return slot;
		} catch (NumberFormatException e) {
			throw new IOException("Invalid target slot: " + input, e);
		}
	}

	private static int resolveTargetSlot(File baseDir) {
		for (int slot = 1; slot <= GamesInProgress.MAX_SLOTS; slot++) {
			if (!new File(baseDir, gameFolder(slot)).exists()) {
				return slot;
			}
		}
		return -1;
	}

	private static String gameFolder(int slot) {
		return GAME_FOLDER_PREFIX + slot;
	}

	private static void restoreHeroToFullHp(File gameFile) throws IOException {
		Bundle bundle;
		try (FileInputStream stream = new FileInputStream(gameFile)) {
			bundle = Bundle.read(stream);
		}

		Bundle heroBundle = bundle.getBundle(HERO_KEY);
		if (heroBundle == null || heroBundle.isNull()) {
			throw new IOException("Save file does not contain hero data: " + gameFile.getAbsolutePath());
		}

		heroBundle.put(HP_KEY, heroBundle.getInt(HT_KEY));
		bundle.put(HERO_KEY, heroBundle);

		try (FileOutputStream stream = new FileOutputStream(gameFile)) {
			if (!Bundle.write(bundle, stream)) {
				throw new IOException("Failed to write updated save file: " + gameFile.getAbsolutePath());
			}
		}
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("  ./gradlew desktop:recoverDiedSave");
		System.out.println("  ./gradlew desktop:recoverDiedSave --args=\"2\"");
		System.out.println("Run without args to recover into the first empty slot, or pass a target slot directly.");
	}
}
