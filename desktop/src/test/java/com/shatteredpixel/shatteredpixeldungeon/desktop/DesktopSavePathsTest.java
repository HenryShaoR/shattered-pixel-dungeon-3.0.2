package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class DesktopSavePathsTest {

	private boolean wasWindows;
	private boolean wasMac;
	private boolean wasLinux;
	private String originalOsName;
	private String originalUserHome;
	private String originalUserDir;

	@Before
	public void setUp() {
		wasWindows = SharedLibraryLoader.isWindows;
		wasMac = SharedLibraryLoader.isMac;
		wasLinux = SharedLibraryLoader.isLinux;
		originalOsName = System.getProperty("os.name");
		originalUserHome = System.getProperty("user.home");
		originalUserDir = System.getProperty("user.dir");
		System.setProperty("user.home", "/Users/tester");
		System.setProperty("user.dir", "/workspace/project");
	}

	@After
	public void tearDown() {
		SharedLibraryLoader.isWindows = wasWindows;
		SharedLibraryLoader.isMac = wasMac;
		SharedLibraryLoader.isLinux = wasLinux;
		restoreProperty("os.name", originalOsName);
		restoreProperty("user.home", originalUserHome);
		restoreProperty("user.dir", originalUserDir);
	}

	@Test
	public void resolveWindowsXpUsesApplicationDataPath() {
		SharedLibraryLoader.isWindows = true;
		SharedLibraryLoader.isMac = false;
		SharedLibraryLoader.isLinux = false;
		System.setProperty("os.name", "Windows XP");

		DesktopSavePaths.ResolvedSavePath resolved =
				DesktopSavePaths.resolve("Shattered Pixel Dungeon", "com.shatteredpixel.shatteredpixeldungeon");

		assertEquals("Application Data/.shatteredpixel/Shattered Pixel Dungeon/", resolved.basePath);
		assertEquals(Files.FileType.External, resolved.fileType);
		assertEquals(
				new File("/Users/tester", "Application Data/.shatteredpixel/Shattered Pixel Dungeon/"),
				resolved.asFile()
		);
	}

	@Test
	public void resolveWindowsNonXpUsesRoamingPath() {
		SharedLibraryLoader.isWindows = true;
		SharedLibraryLoader.isMac = false;
		SharedLibraryLoader.isLinux = false;
		System.setProperty("os.name", "Windows 11");

		DesktopSavePaths.ResolvedSavePath resolved =
				DesktopSavePaths.resolve("Shattered Pixel Dungeon", "shatteredpixel");

		assertEquals("AppData/Roaming/.shatteredpixel/Shattered Pixel Dungeon/", resolved.basePath);
		assertEquals(Files.FileType.External, resolved.fileType);
	}

	@Test
	public void resolveMacUsesApplicationSupportPath() {
		SharedLibraryLoader.isWindows = false;
		SharedLibraryLoader.isMac = true;
		SharedLibraryLoader.isLinux = false;

		DesktopSavePaths.ResolvedSavePath resolved =
				DesktopSavePaths.resolve("Shattered Pixel Dungeon", "com.shatteredpixel.shatteredpixeldungeon");

		assertEquals("Library/Application Support/Shattered Pixel Dungeon/", resolved.basePath);
		assertEquals(Files.FileType.External, resolved.fileType);
		assertEquals(
				new File("/Users/tester", "Library/Application Support/Shattered Pixel Dungeon/"),
				resolved.asFile()
		);
	}

	@Test
	public void resolveLinuxUsesLowercaseHyphenatedTitle() {
		SharedLibraryLoader.isWindows = false;
		SharedLibraryLoader.isMac = false;
		SharedLibraryLoader.isLinux = true;

		DesktopSavePaths.ResolvedSavePath resolved =
				DesktopSavePaths.resolve("Shattered Pixel Dungeon", "com.shatteredpixel.shatteredpixeldungeon");

		assertEquals(
				"/Users/tester/.local/share/.shatteredpixel/shattered-pixel-dungeon/",
				resolved.basePath
		);
		assertEquals(Files.FileType.Absolute, resolved.fileType);
		assertEquals(
				new File("/Users/tester/.local/share/.shatteredpixel/shattered-pixel-dungeon/"),
				resolved.asFile()
		);
	}

	@Test
	public void asFileUsesWorkingDirectoryForLocalPaths() {
		DesktopSavePaths.ResolvedSavePath resolved =
				new DesktopSavePaths.ResolvedSavePath("saves/", Files.FileType.Local);

		assertEquals(new File("/workspace/project", "saves/"), resolved.asFile());
	}

	@Test
	public void asFileFallsBackToPlainFileWhenTypeIsUnknown() {
		DesktopSavePaths.ResolvedSavePath resolved =
				new DesktopSavePaths.ResolvedSavePath("plain-path", null);

		assertEquals(new File("plain-path"), resolved.asFile());
	}

	private static void restoreProperty(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}
}
