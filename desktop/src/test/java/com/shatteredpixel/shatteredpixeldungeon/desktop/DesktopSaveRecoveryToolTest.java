package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DesktopSaveRecoveryToolTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private InputStream originalIn;
	private PrintStream originalOut;
	private PrintStream originalErr;

	@Before
	public void setUp() {
		originalIn = System.in;
		originalOut = System.out;
		originalErr = System.err;
	}

	@After
	public void tearDown() {
		System.setIn(originalIn);
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	@Test
	public void sortedRecoveryDirsFiltersInvalidDirectoriesAndSortsNewestFirst() throws Exception {
		File recoveryRoot = temp.newFolder("recovery");
		File oldest = new File(recoveryRoot, "1000");
		File newest = new File(recoveryRoot, "3000");
		File middle = new File(recoveryRoot, "2000");
		File invalidDir = new File(recoveryRoot, "deleted-slot3");
		File invalidFile = new File(recoveryRoot, "4000.txt");

		oldest.mkdirs();
		newest.mkdirs();
		middle.mkdirs();
		invalidDir.mkdirs();
		invalidFile.createNewFile();

		List<File> sorted = invokeSortedRecoveryDirs(recoveryRoot);

		assertEquals(3, sorted.size());
		assertEquals(newest.getAbsolutePath(), sorted.get(0).getAbsolutePath());
		assertEquals(middle.getAbsolutePath(), sorted.get(1).getAbsolutePath());
		assertEquals(oldest.getAbsolutePath(), sorted.get(2).getAbsolutePath());
	}

	@Test
	public void loadRecoveriesBuildsEntriesFromBundlesInSortedOrder() throws Exception {
		File recoveryRoot = temp.newFolder("recovery-load");
		createRecoveryArchive(recoveryRoot, "1000", HeroClass.WARRIOR, HeroSubClass.NONE, 3, 8);
		createRecoveryArchive(recoveryRoot, "3000", HeroClass.MAGE, HeroSubClass.BATTLEMAGE, 6, 12);

		List<?> recoveries = invokeLoadRecoveries(recoveryRoot);

		assertEquals(2, recoveries.size());
		assertEquals(1, getRecoveryField(recoveries.get(0), "id"));
		assertEquals("Battlemage", getRecoveryField(recoveries.get(0), "hero"));
		assertEquals(6, getRecoveryField(recoveries.get(0), "floor"));
		assertEquals(12, getRecoveryField(recoveries.get(0), "level"));
		assertEquals(3000L, getRecoveryField(recoveries.get(0), "deletedAt"));
		assertEquals(2, getRecoveryField(recoveries.get(1), "id"));
		assertEquals("Warrior", getRecoveryField(recoveries.get(1), "hero"));
	}

	@Test
	public void resolveTargetSlotReturnsFirstAvailableSlot() throws Exception {
		File baseDir = temp.newFolder("saves");
		new File(baseDir, "game1").mkdirs();
		new File(baseDir, "game2").mkdirs();
		new File(baseDir, "game4").mkdirs();

		int slot = invokeResolveTargetSlot(baseDir, new File(baseDir, "recovery/2000"));

		assertEquals(3, slot);
	}

	@Test
	public void resolveTargetSlotReturnsMinusOneWhenAllSlotsAreOccupied() throws Exception {
		File baseDir = temp.newFolder("full-saves");
		for (int slot = 1; slot <= GamesInProgress.MAX_SLOTS; slot++) {
			new File(baseDir, "game" + slot).mkdirs();
		}

		int slot = invokeResolveTargetSlot(baseDir, new File(baseDir, "recovery/2000"));

		assertEquals(-1, slot);
	}

	@Test
	public void buildRecoveryEntryFromBundlePrefersSubclassOverClassName() throws Exception {
		Object recovery = invokeBuildRecoveryEntryFromBundle(
				7,
				new File("3000"),
				3000L,
				createBundle(HeroClass.MAGE, HeroSubClass.BATTLEMAGE, 10, 15)
		);

		assertEquals(7, getRecoveryField(recovery, "id"));
		assertEquals("Battlemage", getRecoveryField(recovery, "hero"));
		assertEquals(10, getRecoveryField(recovery, "floor"));
		assertEquals(15, getRecoveryField(recovery, "level"));
	}

	@Test
	public void buildRecoveryEntryFromBundleUsesClassNameWhenSubclassIsNone() throws Exception {
		Object recovery = invokeBuildRecoveryEntryFromBundle(
				2,
				new File("2000"),
				2000L,
				createBundle(HeroClass.ROGUE, HeroSubClass.NONE, 4, 9)
		);

		assertEquals("Rogue", getRecoveryField(recovery, "hero"));
	}

	@Test
	public void formatEnumNameConvertsUnderscoreSeparatedNames() throws Exception {
		assertEquals("Battle Mage", invokeFormatEnumName("BATTLE_MAGE"));
		assertEquals("Rogue", invokeFormatEnumName("ROGUE"));
	}

	@Test
	public void gameFolderBuildsExpectedSlotName() throws Exception {
		assertEquals("game4", invokeGameFolder(4));
	}

	@Test
	public void isHelpRecognizesKnownFlags() throws Exception {
		assertTrue(invokeIsHelp("help"));
		assertTrue(invokeIsHelp("--help"));
		assertTrue(invokeIsHelp("-h"));
		assertFalse(invokeIsHelp("recover"));
	}

	@Test
	public void promptForRecoveryReturnsNullForBlankInput() throws Exception {
		System.setIn(new ByteArrayInputStream("\n".getBytes()));

		Object result = invokePromptForRecovery(new ArrayList<Object>());

		assertNull(result);
	}

	@Test
	public void promptForRecoveryReturnsSelectedRecoveryForValidId() throws Exception {
		File recoveryRoot = temp.newFolder("recovery-prompt");
		createRecoveryArchive(recoveryRoot, "1000", HeroClass.WARRIOR, HeroSubClass.NONE, 3, 8);
		createRecoveryArchive(recoveryRoot, "2000", HeroClass.ROGUE, HeroSubClass.NONE, 4, 9);
		List<?> recoveries = invokeLoadRecoveries(recoveryRoot);
		System.setIn(new ByteArrayInputStream("2\n".getBytes()));

		Object selected = invokePromptForRecovery(recoveries);

		assertSame(recoveries.get(1), selected);
	}

	@Test
	public void printRecoveriesOutputsTableHeadersAndData() throws Exception {
		File recoveryRoot = temp.newFolder("recovery-print");
		createRecoveryArchive(recoveryRoot, "1000", HeroClass.WARRIOR, HeroSubClass.NONE, 3, 8);
		List<?> recoveries = invokeLoadRecoveries(recoveryRoot);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output));

		invokePrintRecoveries(recoveries);

		String text = output.toString();
		assertTrue(text.contains("Deleted saves:"));
		assertTrue(text.contains("Hero"));
		assertTrue(text.contains("Warrior"));
	}

	@Test
	public void printUsageOutputsExpectedCommands() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output));

		invokePrintUsage();

		String text = output.toString();
		assertTrue(text.contains("./gradlew desktop:recoverSave"));
		assertTrue(text.contains("--args=\"2\""));
	}

	@Test
	public void mainWithHelpArgumentPrintsUsageAndReturns() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output));

		DesktopSaveRecoveryTool.main(new String[]{"--help"});

		assertTrue(output.toString().contains("Usage:"));
	}

	@SuppressWarnings("unchecked")
	private static List<?> invokeLoadRecoveries(File recoveryDir) throws Exception {
		return (List<?>) invokePrivateStaticMethod(
				"loadRecoveries",
				new Class[]{File.class},
				recoveryDir
		);
	}

	@SuppressWarnings("unchecked")
	private static List<File> invokeSortedRecoveryDirs(File recoveryDir) throws Exception {
		return (List<File>) invokePrivateStaticMethod(
				"sortedRecoveryDirs",
				new Class[]{File.class},
				recoveryDir
		);
	}

	private static Object invokeBuildRecoveryEntryFromBundle(int id, File archiveDir, long timestamp, Bundle bundle) throws Exception {
		return invokePrivateStaticMethod(
				"buildRecoveryEntryFromBundle",
				new Class[]{int.class, File.class, long.class, Bundle.class},
				id, archiveDir, timestamp, bundle
		);
	}

	private static int invokeResolveTargetSlot(File baseDir, File recoveryDir) throws Exception {
		return (Integer) invokePrivateStaticMethod(
				"resolveTargetSlot",
				new Class[]{File.class, File.class},
				baseDir,
				recoveryDir
		);
	}

	private static String invokeGameFolder(int slot) throws Exception {
		return (String) invokePrivateStaticMethod(
				"gameFolder",
				new Class[]{int.class},
				slot
		);
	}

	private static String invokeFormatEnumName(String enumName) throws Exception {
		return (String) invokePrivateStaticMethod(
				"formatEnumName",
				new Class[]{String.class},
				enumName
		);
	}

	private static boolean invokeIsHelp(String arg) throws Exception {
		return (Boolean) invokePrivateStaticMethod(
				"isHelp",
				new Class[]{String.class},
				arg
		);
	}

	private static Object invokePromptForRecovery(List<?> recoveries) throws Exception {
		return invokePrivateStaticMethod(
				"promptForRecovery",
				new Class[]{List.class},
				recoveries
		);
	}

	private static void invokePrintRecoveries(List<?> recoveries) throws Exception {
		invokePrivateStaticMethod(
				"printRecoveries",
				new Class[]{List.class},
				recoveries
		);
	}

	private static void invokePrintUsage() throws Exception {
		invokePrivateStaticMethod(
				"printUsage",
				new Class[0]
		);
	}

	private static Object getRecoveryField(Object recovery, String fieldName) throws Exception {
		java.lang.reflect.Field field = recovery.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(recovery);
	}

	private static Bundle createBundle(HeroClass heroClass, HeroSubClass subClass, int floor, int level) {
		Bundle bundle = new Bundle();
		bundle.put("depth", floor);
		Bundle hero = new Bundle();
		hero.put("class", heroClass);
		hero.put("subClass", subClass);
		hero.put("lvl", level);
		bundle.put("hero", hero);
		return bundle;
	}

	private static void createRecoveryArchive(
			File recoveryRoot,
			String archiveName,
			HeroClass heroClass,
			HeroSubClass subClass,
			int floor,
			int level
	) throws Exception {
		File archiveDir = new File(recoveryRoot, archiveName);
		assertTrue(archiveDir.mkdirs());
		File gameFile = new File(archiveDir, "game.dat");
		try (FileOutputStream output = new FileOutputStream(gameFile)) {
			assertTrue(Bundle.write(createBundle(heroClass, subClass, floor, level), output, false));
		}
	}

	private static Object invokePrivateStaticMethod(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
		Method method = DesktopSaveRecoveryTool.class.getDeclaredMethod(name, parameterTypes);
		method.setAccessible(true);
		try {
			return method.invoke(null, args);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			}
			throw e;
		}
	}
}
