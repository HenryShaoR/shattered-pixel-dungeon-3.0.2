package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.GameSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SaveRecoveryManagerTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private Files previousFiles;

	@Before
	public void setUp() throws IOException {
		previousFiles = Gdx.files;
		File baseDir = temp.newFolder("saves-root");
		Gdx.files = new TestFiles();
		GameSettings.set(new TestPreferences());
		FileUtils.setDefaultFileProperties(Files.FileType.Absolute, baseDir.getAbsolutePath() + File.separator);
	}

	@After
	public void tearDown() {
		Gdx.files = previousFiles;
	}

	@Test
	public void buildArchiveNameReturnsTimestampString() {
		assertEquals("12345", SaveRecoveryManager.buildArchiveName(12345L));
	}

	@Test
	public void archiveTimestampParsesValidNamesAndRejectsInvalidOnes() {
		assertEquals(9876L, SaveRecoveryManager.archiveTimestamp("9876"));
		assertEquals(-1L, SaveRecoveryManager.archiveTimestamp("deleted-9876-slot1"));
	}

	@Test
	public void archiveDeletedSaveReturnsFalseWhenSourceDirectoryDoesNotExist() throws Exception {
		assertFalse(SaveRecoveryManager.archiveDeletedSave(1));
	}

	@Test
	public void archiveDeletedSaveMovesSaveDirectoryIntoRecovery() throws Exception {
		File baseDir = new File(temp.getRoot(), "saves-root");
		File sourceDir = new File(baseDir, "game1");
		assertTrue(sourceDir.mkdirs());
		File sourceFile = new File(sourceDir, "game.dat");
		assertTrue(sourceFile.createNewFile());

		boolean archived = SaveRecoveryManager.archiveDeletedSave(1);

		assertTrue(archived);
		assertFalse(sourceDir.exists());

		File recoveryDir = new File(baseDir, SaveRecoveryManager.RECOVERY_DIR);
		File[] archivedDirs = recoveryDir.listFiles(File::isDirectory);
		assertNotNull(archivedDirs);
		assertEquals(1, archivedDirs.length);
		assertTrue(new File(archivedDirs[0], "game.dat").exists());
	}

	@Test
	public void archiveDeletedSaveReturnsFalseWhenSourceIsAFile() throws Exception {
		File baseDir = new File(temp.getRoot(), "saves-root");
		File sourceFile = new File(baseDir, "game1");
		assertTrue(sourceFile.createNewFile());

		assertFalse(SaveRecoveryManager.archiveDeletedSave(1));
	}

	@Test
	public void archiveDiedSaveKeepsOnlyMostRecentArchive() throws Exception {
		File baseDir = new File(temp.getRoot(), "saves-root");
		File firstSourceDir = new File(baseDir, "game1");
		assertTrue(firstSourceDir.mkdirs());
		File firstGameFile = new File(firstSourceDir, "game.dat");
		assertTrue(firstGameFile.createNewFile());
		java.nio.file.Files.write(firstGameFile.toPath(), "first".getBytes(StandardCharsets.UTF_8));

		assertTrue(SaveRecoveryManager.archiveDiedSave(1));

		File diedDir = new File(baseDir, SaveRecoveryManager.DIED_DIR);
		File[] firstArchivedDirs = diedDir.listFiles(File::isDirectory);
		assertNotNull(firstArchivedDirs);
		assertEquals(1, firstArchivedDirs.length);
		File firstArchivedDir = firstArchivedDirs[0];
		assertTrue(new File(firstArchivedDir, "game.dat").exists());

		File secondSourceDir = new File(baseDir, "game1");
		assertTrue(secondSourceDir.mkdirs());
		File secondGameFile = new File(secondSourceDir, "game.dat");
		assertTrue(secondGameFile.createNewFile());
		java.nio.file.Files.write(secondGameFile.toPath(), "second".getBytes(StandardCharsets.UTF_8));

		assertTrue(SaveRecoveryManager.archiveDiedSave(1));

		File[] secondArchivedDirs = diedDir.listFiles(File::isDirectory);
		assertNotNull(secondArchivedDirs);
		assertEquals(1, secondArchivedDirs.length);
		File archivedGameFile = new File(secondArchivedDirs[0], "game.dat");
		assertTrue(archivedGameFile.exists());
		assertEquals("second", new String(java.nio.file.Files.readAllBytes(archivedGameFile.toPath()), StandardCharsets.UTF_8));
	}

	@Test
	public void pruneOldArchivesSupportsCustomLimit() throws Exception {
		File recoveryRoot = temp.newFolder("recovery-limit");
		for (int i = 1; i <= 4; i++) {
			assertTrue(new File(recoveryRoot, String.valueOf(i)).mkdirs());
		}

		invokePruneOldArchives(new FileHandle(recoveryRoot), 2);

		String[] remaining = recoveryRoot.list();
		assertNotNull(remaining);
		Arrays.sort(remaining);
		assertArrayEquals(new String[]{"3", "4"}, remaining);
	}

	@Test
	public void pruneOldArchivesKeepsNewestFiveNumericDirectories() throws Exception {
		File recoveryRoot = temp.newFolder("recovery");
		for (int i = 1; i <= 7; i++) {
			assertTrue(new File(recoveryRoot, String.valueOf(i)).mkdirs());
		}
		assertTrue(new File(recoveryRoot, "deleted-slot3").mkdirs());

		invokePruneOldArchives(new FileHandle(recoveryRoot), SaveRecoveryManager.MAX_RECOVERED_SAVES);

		String[] remaining = recoveryRoot.list();
		assertNotNull(remaining);
		Arrays.sort(remaining);
		assertArrayEquals(
				new String[]{"3", "4", "5", "6", "7", "deleted-slot3"},
				remaining
		);
	}

	private static void invokePruneOldArchives(FileHandle recoveryRoot, int maxArchives) throws Exception {
		Method method = SaveRecoveryManager.class.getDeclaredMethod("pruneOldArchives", FileHandle.class, int.class);
		method.setAccessible(true);
		method.invoke(null, recoveryRoot, maxArchives);
	}

	private static final class TestFiles implements Files {

		@Override
		public FileHandle getFileHandle(String path, FileType type) {
			return new FileHandle(path);
		}

		@Override
		public FileHandle classpath(String path) {
			return new FileHandle(path);
		}

		@Override
		public FileHandle internal(String path) {
			File cwd = new File(System.getProperty("user.dir"));
			File assetsRoot = new File(cwd, "src/main/assets");
			if (!assetsRoot.exists()) {
				assetsRoot = new File(cwd, "core/src/main/assets");
			}
			return new FileHandle(new File(assetsRoot, path));
		}

		@Override
		public FileHandle external(String path) {
			return new FileHandle(path);
		}

		@Override
		public FileHandle absolute(String path) {
			return new FileHandle(path);
		}

		@Override
		public FileHandle local(String path) {
			return new FileHandle(path);
		}

		@Override
		public String getExternalStoragePath() {
			return "";
		}

		@Override
		public boolean isExternalStorageAvailable() {
			return true;
		}

		@Override
		public String getLocalStoragePath() {
			return "";
		}

		@Override
		public boolean isLocalStorageAvailable() {
			return true;
		}
	}

	private static final class TestPreferences implements Preferences {

		private final Map<String, Object> values = new HashMap<>();

		@Override
		public Preferences putBoolean(String key, boolean val) {
			values.put(key, val);
			return this;
		}

		@Override
		public Preferences putInteger(String key, int val) {
			values.put(key, val);
			return this;
		}

		@Override
		public Preferences putLong(String key, long val) {
			values.put(key, val);
			return this;
		}

		@Override
		public Preferences putFloat(String key, float val) {
			values.put(key, val);
			return this;
		}

		@Override
		public Preferences putString(String key, String val) {
			values.put(key, val);
			return this;
		}

		@Override
		public Preferences put(Map<String, ?> vals) {
			values.putAll(vals);
			return this;
		}

		@Override
		public boolean getBoolean(String key) {
			return getBoolean(key, false);
		}

		@Override
		public int getInteger(String key) {
			return getInteger(key, 0);
		}

		@Override
		public long getLong(String key) {
			return getLong(key, 0L);
		}

		@Override
		public float getFloat(String key) {
			return getFloat(key, 0f);
		}

		@Override
		public String getString(String key) {
			return getString(key, "");
		}

		@Override
		public boolean getBoolean(String key, boolean defValue) {
			Object value = values.get(key);
			return value instanceof Boolean ? (Boolean) value : defValue;
		}

		@Override
		public int getInteger(String key, int defValue) {
			Object value = values.get(key);
			return value instanceof Integer ? (Integer) value : defValue;
		}

		@Override
		public long getLong(String key, long defValue) {
			Object value = values.get(key);
			return value instanceof Long ? (Long) value : defValue;
		}

		@Override
		public float getFloat(String key, float defValue) {
			Object value = values.get(key);
			return value instanceof Float ? (Float) value : defValue;
		}

		@Override
		public String getString(String key, String defValue) {
			Object value = values.get(key);
			return value instanceof String ? (String) value : defValue;
		}

		@Override
		public Map<String, ?> get() {
			return values;
		}

		@Override
		public boolean contains(String key) {
			return values.containsKey(key);
		}

		@Override
		public void clear() {
			values.clear();
		}

		@Override
		public void remove(String key) {
			values.remove(key);
		}

		@Override
		public void flush() {
			// no-op for tests
		}
	}
}
