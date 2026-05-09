package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.SaveRecoveryManager;
import com.watabou.utils.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

public class SaveRecoveryIntegrationTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File tempRoot;

	@Before
	public void setUp() throws IOException {
		Gdx.files = new Lwjgl3Files();
		tempRoot = tempFolder.newFolder("save-recovery-root");
		setFileRoot(tempRoot);
	}

	@After
	public void tearDown() {
		FileUtils.setDefaultFileProperties(Files.FileType.Local, "");
	}

	@Test
	public void validPairwiseDeleteMovesSaveIntoRecoveryDirectory() throws IOException {
		PairwiseCase[] cases = new PairwiseCase[] {
				new PairwiseCase("game1", false, false),
				new PairwiseCase("game1", true, true),
				new PairwiseCase("game2", false, true),
				new PairwiseCase("game2", true, false)
		};

		for (int i = 0; i < cases.length; i++) {
			File caseRoot = new File(tempRoot, "pairwise-case-" + i);
			assertTrue(caseRoot.mkdirs());
			setFileRoot(caseRoot);

			PairwiseCase testCase = cases[i];

			if (testCase.hasInvalidRecoveryFolder) {
				FileHandle invalidFolder = recoveryRoot().child("not-a-timestamp");
				invalidFolder.mkdirs();
				invalidFolder.child("game.dat").writeString("invalid-folder-data", false);
			}

			String saveData = "pairwise-save-" + i;
			createFakeSaveFolder(testCase.gameFolder, saveData, testCase.hasExtraFile);

			boolean archived = SaveRecoveryManager.archiveDeletedSaveFolder(testCase.gameFolder);

			assertTrue(archived);
			assertFalse(fileHandle(testCase.gameFolder).exists());

			List<FileHandle> archives = validRecoveryArchives();

			assertEquals(1, archives.size());
			assertTrue(archives.get(0).child("game.dat").exists());
			assertEquals(saveData, archives.get(0).child("game.dat").readString());

			if (testCase.hasExtraFile) {
				assertTrue(archives.get(0).child("extra.txt").exists());
			} else {
				assertFalse(archives.get(0).child("extra.txt").exists());
			}

			if (testCase.hasInvalidRecoveryFolder) {
				assertTrue(recoveryRoot().child("not-a-timestamp").exists());
			}
		}
	}

	@Test
	public void onlyKeepsFiveMostRecentDeletedSaves() throws Exception {
		File caseRoot = new File(tempRoot, "max-five-case");
		assertTrue(caseRoot.mkdirs());
		setFileRoot(caseRoot);

		for (int slot = 1; slot <= 6; slot++) {
			String gameFolder = "game" + slot;
			createFakeSaveFolder(gameFolder, "save-" + slot, false);

			boolean archived = SaveRecoveryManager.archiveDeletedSaveFolder(gameFolder);

			assertTrue(archived);

			Thread.sleep(5);
		}

		List<FileHandle> archives = validRecoveryArchives();
		List<String> archivedContents = readGameDatContents(archives);

		assertEquals(SaveRecoveryManager.MAX_RECOVERED_SAVES, archives.size());

		assertFalse(archivedContents.contains("save-1"));
		assertTrue(archivedContents.contains("save-2"));
		assertTrue(archivedContents.contains("save-3"));
		assertTrue(archivedContents.contains("save-4"));
		assertTrue(archivedContents.contains("save-5"));
		assertTrue(archivedContents.contains("save-6"));
	}

	@Test
	public void invalidSaveFolderDoesNotCreateRecoveryArchive() throws IOException {
		File caseRoot = new File(tempRoot, "invalid-folder-case");
		assertTrue(caseRoot.mkdirs());
		setFileRoot(caseRoot);

		boolean archived = SaveRecoveryManager.archiveDeletedSaveFolder("game99");

		assertFalse(archived);
		assertEquals(0, validRecoveryArchives().size());
	}

	@Test
	public void invalidRecoveryFolderIsIgnoredDuringPruning() throws Exception {
		File caseRoot = new File(tempRoot, "invalid-recovery-folder-case");
		assertTrue(caseRoot.mkdirs());
		setFileRoot(caseRoot);

		FileHandle root = recoveryRoot();
		root.mkdirs();

		FileHandle invalidFolder = root.child("not-a-timestamp");
		invalidFolder.mkdirs();
		invalidFolder.child("game.dat").writeString("invalid-folder-data", false);

		for (int timestamp = 1000; timestamp <= 1004; timestamp++) {
			FileHandle archive = root.child(String.valueOf(timestamp));
			archive.mkdirs();
			archive.child("game.dat").writeString("old-" + timestamp, false);
		}

		createFakeSaveFolder("game1", "new-save", false);

		boolean archived = SaveRecoveryManager.archiveDeletedSaveFolder("game1");

		assertTrue(archived);
		assertTrue(invalidFolder.exists());

		List<FileHandle> archives = validRecoveryArchives();
		List<String> archivedContents = readGameDatContents(archives);

		assertEquals(SaveRecoveryManager.MAX_RECOVERED_SAVES, archives.size());
		assertFalse(archivedContents.contains("old-1000"));
		assertTrue(archivedContents.contains("new-save"));
	}

	private void setFileRoot(File root) {
		FileUtils.setDefaultFileProperties(
				Files.FileType.Absolute,
				root.getAbsolutePath() + File.separator
		);
	}

	private void createFakeSaveFolder(String gameFolder, String gameData, boolean includeExtraFile) {
		FileHandle folder = fileHandle(gameFolder);
		folder.mkdirs();

		folder.child("game.dat").writeString(gameData, false);

		if (includeExtraFile) {
			folder.child("extra.txt").writeString("extra save file", false);
		}
	}

	private FileHandle fileHandle(String path) {
		return FileUtils.getFileHandle(path);
	}

	private FileHandle recoveryRoot() {
		return FileUtils.getFileHandle(SaveRecoveryManager.RECOVERY_DIR);
	}

	private List<FileHandle> validRecoveryArchives() {
		List<FileHandle> archives = new ArrayList<FileHandle>();

		FileHandle root = recoveryRoot();
		if (!root.exists()) {
			return archives;
		}

		for (FileHandle file : root.list()) {
			if (file.isDirectory() && SaveRecoveryManager.archiveTimestamp(file.name()) >= 0L) {
				archives.add(file);
			}
		}

		archives.sort(new Comparator<FileHandle>() {
			@Override
			public int compare(FileHandle left, FileHandle right) {
				return Long.compare(
						SaveRecoveryManager.archiveTimestamp(right.name()),
						SaveRecoveryManager.archiveTimestamp(left.name())
				);
			}
		});

		return archives;
	}

	private List<String> readGameDatContents(List<FileHandle> archives) {
		List<String> contents = new ArrayList<String>();

		for (FileHandle archive : archives) {
			FileHandle gameFile = archive.child("game.dat");
			if (gameFile.exists()) {
				contents.add(gameFile.readString());
			}
		}

		return contents;
	}

	private static final class PairwiseCase {
		final String gameFolder;
		final boolean hasExtraFile;
		final boolean hasInvalidRecoveryFolder;

		PairwiseCase(String gameFolder, boolean hasExtraFile, boolean hasInvalidRecoveryFolder) {
			this.gameFolder = gameFolder;
			this.hasExtraFile = hasExtraFile;
			this.hasInvalidRecoveryFolder = hasInvalidRecoveryFolder;
		}
	}
}