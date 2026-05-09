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
	// define the max number of the archive
	public static final int MAX_RECOVERED_SAVES = 5;

	private SaveRecoveryManager() {
	}

	public static boolean archiveDeletedSave(int slot) throws IOException {
		return archiveDeletedSaveFolder(GamesInProgress.gameFolder(slot));
	}

	public static boolean archiveDeletedSaveFolder(String gameFolder) throws IOException {
		FileHandle source = FileUtils.getFileHandle(gameFolder);
		if (source == null || !source.exists() || !source.isDirectory()) {
			return false;
		}

		try {
			FileHandle recoveryRoot = FileUtils.getFileHandle(RECOVERY_DIR);
			recoveryRoot.mkdirs();

			long now = System.currentTimeMillis();
			String archiveName = buildArchiveName(now);
			FileHandle target = FileUtils.getFileHandle(RECOVERY_DIR + "/" + archiveName);
			while (target.exists()) {
				now++;
				archiveName = buildArchiveName(now);
				target = FileUtils.getFileHandle(RECOVERY_DIR + "/" + archiveName);
			}

			// move to target place instead of the deletion
			source.moveTo(target);
			// delete the oldest one if there the number is over the max number
			pruneOldArchives(recoveryRoot);
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

	private static void pruneOldArchives(FileHandle recoveryRoot) {
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
		for (int i = MAX_RECOVERED_SAVES; i < archives.size(); i++) {
			archives.get(i).deleteDirectory();
		}
	}
}
