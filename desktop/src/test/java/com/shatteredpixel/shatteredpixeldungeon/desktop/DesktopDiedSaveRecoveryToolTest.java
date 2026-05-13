package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.shatteredpixel.shatteredpixeldungeon.SaveRecoveryManager;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DesktopDiedSaveRecoveryToolTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File tempRoot;
	private String oldUserHome;
	private String oldSpecificationTitle;
	private String oldImplementationTitle;

	@Before
	public void setUp() throws IOException {
		Gdx.files = new Lwjgl3Files();

		tempRoot = tempFolder.newFolder("died-recovery-root");
		FileUtils.setDefaultFileProperties(Files.FileType.Absolute, tempRoot.getAbsolutePath() + File.separator);

		oldUserHome = System.getProperty("user.home");
		oldSpecificationTitle = System.getProperty("Specification-Title");
		oldImplementationTitle = System.getProperty("Implementation-Title");

		System.setProperty("user.home", tempRoot.getAbsolutePath());
		System.setProperty("Specification-Title", "Shattered Pixel Dungeon");
		System.setProperty("Implementation-Title", "com.shatteredpixel.shatteredpixeldungeon");
	}

	@After
	public void tearDown() {
		FileUtils.setDefaultFileProperties(Files.FileType.Local, "");
		restoreProperty("user.home", oldUserHome);
		restoreProperty("Specification-Title", oldSpecificationTitle);
		restoreProperty("Implementation-Title", oldImplementationTitle);
	}

	@Test
	public void recoverDiedSaveMovesLatestArchiveAndRestoresHeroHpToMax() throws Exception {
		File baseDir = desktopBaseDir();
		File diedArchive = new File(new File(baseDir, SaveRecoveryManager.DIED_DIR), "12345");
		assertTrue(diedArchive.mkdirs());

		File gameFile = new File(diedArchive, "game.dat");
		writeSave(gameFile, 3, 17);

		DesktopDiedSaveRecoveryTool.main(new String[0]);

		File recoveredDir = new File(baseDir, "game1");
		assertTrue(recoveredDir.exists());
		assertFalse(diedArchive.exists());

		Bundle recovered = readBundle(new File(recoveredDir, "game.dat"));
		Bundle hero = recovered.getBundle("hero");
		assertEquals(17, hero.getInt("HT"));
		assertEquals(17, hero.getInt("HP"));
	}

	private File desktopBaseDir() {
		DesktopSavePaths.ResolvedSavePath savePath =
				DesktopSavePaths.resolve(
						"Shattered Pixel Dungeon",
						"com.shatteredpixel.shatteredpixeldungeon"
				);
		return savePath.asFile();
	}

	private void writeSave(File file, int hp, int ht) throws IOException {
		Bundle hero = new Bundle();
		hero.put("HP", hp);
		hero.put("HT", ht);

		Bundle save = new Bundle();
		save.put("hero", hero);

		try (FileOutputStream stream = new FileOutputStream(file)) {
			if (!Bundle.write(save, stream)) {
				throw new IOException("Failed to write test save");
			}
		}
	}

	private Bundle readBundle(File file) throws IOException {
		try (FileInputStream stream = new FileInputStream(file)) {
			return Bundle.read(stream);
		}
	}

	private void restoreProperty(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}
}
