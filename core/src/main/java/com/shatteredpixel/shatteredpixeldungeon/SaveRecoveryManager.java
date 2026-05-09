package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// this class is a utility class that should not be instantiate
public final class SaveRecoveryManager {

	public static final String RECOVERY_DIR = "recovery";
	public static final String DIED_DIR = "died";
	// define the max number of the archive
	public static final int MAX_RECOVERED_SAVES = 5;

	private SaveRecoveryManager() {
	}

	public static boolean archiveDeletedSave(int slot) throws IOException {
		return archiveSave(slot, RECOVERY_DIR, MAX_RECOVERED_SAVES, false);
	}

	public static boolean archiveDiedSave(int slot) throws IOException {
		return archiveSave(slot, DIED_DIR, 1, true);
	}

	private static boolean archiveSave(int slot, String archiveDirName, int maxArchives, boolean clearExisting) throws IOException {
		FileHandle source = FileUtils.getFileHandle(GamesInProgress.gameFolder(slot));
		// return flase if the slot is not found
		if (source == null || !source.exists() || !source.isDirectory()) {
			return false;
		}

		try {
			FileHandle archiveRoot = FileUtils.getFileHandle(archiveDirName);
			archiveRoot.mkdirs();

			if (clearExisting) {
				clearArchives(archiveRoot);
			}

			long now = System.currentTimeMillis();
			String archiveName = buildArchiveName(now);
			// find the most avaliable path
			FileHandle target = FileUtils.getFileHandle(archiveDirName + "/" + archiveName);
			while (target.exists()) {
				now++;
				archiveName = buildArchiveName(now);
				target = FileUtils.getFileHandle(archiveDirName + "/" + archiveName);
			}

			// move to target place instead of the deletion
			source.moveTo(target);
			// delete the oldest one if there the number is over the max number
			pruneOldArchives(archiveRoot, maxArchives);
			return true;
		} catch (GdxRuntimeException e) {
			throw new IOException(e);
		}
	}

	public static String buildArchiveName(long timestamp) {
		return Long.toString(timestamp);
	}

	public static long archiveTimestamp(String archiveName) {
		try {
			return Long.parseLong(archiveName);
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	private static void clearArchives(FileHandle archiveRoot) {
		for (FileHandle file : archiveRoot.list()) {
			if (file.isDirectory()) {
				file.deleteDirectory();
			}
		}
	}

	private static void pruneOldArchives(FileHandle recoveryRoot, int maxArchives) {
		ArrayList<FileHandle> archives = new ArrayList<>();
		// find all the legal archive game
		for (FileHandle file : recoveryRoot.list()) {
			if (file.isDirectory() && archiveTimestamp(file.name()) >= 0L) {
				archives.add(file);
			}
		}

		// sort the game by their deletion time
		Collections.sort(archives, new Comparator<FileHandle>() {
			@Override
			public int compare(FileHandle left, FileHandle right) {
				return Long.compare(archiveTimestamp(right.name()), archiveTimestamp(left.name()));
			}
		});

		// delete extra file
		for (int i = maxArchives; i < archives.size(); i++) {
			archives.get(i).deleteDirectory();
		}
	}
}
