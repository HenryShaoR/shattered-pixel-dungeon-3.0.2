package com.shatteredpixel.shatteredpixeldungeon;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class testLogger {

    private static final File LOG_FILE = new File("mob_log.txt");

    private PrintStream originalOut;
    private PrintStream originalErr;

    @Before
    public void setUp() {

        //delete the log text so we can verify whether content has been properly appended
        if (LOG_FILE.exists()) {

            if (!LOG_FILE.delete()){
                System.err.println("mog_log.txt cannot be deleted!!!");
                System.exit(1);
            }

        }

        originalOut = System.out;
        originalErr = System.err;
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);

        if (LOG_FILE.exists()) {
            if (!LOG_FILE.delete()){
                System.err.println("mog_log.txt cannot be deleted!!!");
                System.exit(1);
            }
        }
    }

    // Test the positive path where by logging some event, they will appear in mob.txt
    @Test
    public void testLog_validEvent_writesToFile() throws Exception {

        // Arrange
        String eventType = "EVENT";
        String message = "SOME EVENT";

        // Act
        Logger.log(eventType, message);

        // Assert
        assertTrue(LOG_FILE.exists());

        List<String> lines = Files.readAllLines(LOG_FILE.toPath());

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("EVENT"));
        assertTrue(lines.get(0).contains("SOME EVENT"));
    }

    // Test the negative path where opening "mob.txt" throws IOException
    @Test
    public void testLog_invalidLogFile_printsErrorMessage() {

        // Arrange
        File invalidFile = new File("mob_log.txt");
        invalidFile.mkdir();

        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        String eventType = "EVENT";
        String message = "SOME EVENT";

        // Act
        Logger.log(eventType, message);

        // Assert
        String errorOutput = errContent.toString();

        assertTrue(errorOutput.contains("Failed to write log file."));

        invalidFile.delete();
    }

    @Test
    public void testLogSpawn_validMob_writesSpawnEventToFile() throws Exception {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;

        // Act
        Logger.logSpawn(mobName, mobId);

        // Assert
        assertTrue(LOG_FILE.exists());

        List<String> lines = Files.readAllLines(LOG_FILE.toPath());

        assertEquals(1, lines.size());

        assertTrue(lines.get(0).contains("[SPAWN]"));
        assertTrue(lines.get(0).contains("mob=Rat"));
        assertTrue(lines.get(0).contains("id=1"));
    }


    @Test
    public void testLogStateChange_differentStates_writesStateChangeToFile() throws Exception {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        String oldState = "SLEEPING";
        String newState = "HUNTING";

        // Act
        Logger.logStateChange(
                mobName,
                mobId,
                oldState,
                newState
        );

        // Assert
        assertTrue(LOG_FILE.exists());

        List<String> lines = Files.readAllLines(LOG_FILE.toPath());

        assertEquals(1, lines.size());

        assertTrue(lines.get(0).contains("[STATE_CHANGE]"));
        assertTrue(lines.get(0).contains("mob=Rat"));
        assertTrue(lines.get(0).contains("id=1"));
        assertTrue(lines.get(0).contains("from=SLEEPING"));
        assertTrue(lines.get(0).contains("to=HUNTING"));
    }

    @Test
    public void testLogStateChange_sameStates_doesNotWriteToFile() {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        String oldState = "HUNTING";
        String newState = "HUNTING";

        // Act
        Logger.logStateChange(
                mobName,
                mobId,
                oldState,
                newState
        );

        // Assert
        assertFalse(LOG_FILE.exists());
    }

    @Test
    public void testLogAlert_differentAlertStatus_writesAlertEventToFile() throws Exception {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        boolean oldAlert = false;
        boolean newAlert = true;

        // Act
        Logger.logAlert(mobName, mobId, oldAlert, newAlert);

        // Assert
        assertTrue(LOG_FILE.exists());

        List<String> lines = Files.readAllLines(LOG_FILE.toPath());

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("[ALERT]"));
        assertTrue(lines.get(0).contains("mob=Rat"));
        assertTrue(lines.get(0).contains("id=1"));
        assertTrue(lines.get(0).contains("from= false"));
        assertTrue(lines.get(0).contains("to= true"));
    }

    @Test
    public void testLogAlert_sameAlertStatus_doesNotWriteToFile() {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        boolean oldAlert = true;
        boolean newAlert = true;

        // Act
        Logger.logAlert(mobName, mobId, oldAlert, newAlert);

        // Assert
        assertFalse(LOG_FILE.exists());
    }

    @Test
    public void testLogTargetChange_differentTargets_writesTargetChangeToFile() throws Exception {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        int oldTarget = 12;
        int newTarget = 42;

        // Act
        Logger.logTargetChange(mobName, mobId, oldTarget, newTarget);

        // Assert
        assertTrue(LOG_FILE.exists());

        List<String> lines = Files.readAllLines(LOG_FILE.toPath());

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("[TARGET_CHANGE]"));
        assertTrue(lines.get(0).contains("mob=Rat"));
        assertTrue(lines.get(0).contains("id=1"));
        assertTrue(lines.get(0).contains("from= 12"));
        assertTrue(lines.get(0).contains("to= 42"));
    }

    @Test
    public void testLogTargetChange_sameTarget_doesNotWriteToFile() {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        int oldTarget = 42;
        int newTarget = 42;

        // Act
        Logger.logTargetChange(mobName, mobId, oldTarget, newTarget);

        // Assert
        assertFalse(LOG_FILE.exists());
    }

    @Test
    public void testLogTargetChange_oldTargetIsNone_writesOldTargetAsNoTarget() throws Exception {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        int oldTarget = -1;
        int newTarget = 42;

        // Act
        Logger.logTargetChange(mobName, mobId, oldTarget, newTarget);

        // Assert
        assertTrue(LOG_FILE.exists());

        List<String> lines = Files.readAllLines(LOG_FILE.toPath());

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("[TARGET_CHANGE]"));
        assertTrue(lines.get(0).contains("mob=Rat"));
        assertTrue(lines.get(0).contains("id=1"));
        assertTrue(lines.get(0).contains("from= No Target"));
        assertTrue(lines.get(0).contains("to= 42"));
    }

    @Test
    public void testLogTargetChange_newTargetIsNone_writesNewTargetAsNoTarget() throws Exception {

        // Arrange
        String mobName = "Rat";
        int mobId = 1;
        int oldTarget = 42;
        int newTarget = -1;

        // Act
        Logger.logTargetChange(mobName, mobId, oldTarget, newTarget);

        // Assert
        assertTrue(LOG_FILE.exists());

        List<String> lines = Files.readAllLines(LOG_FILE.toPath());

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("[TARGET_CHANGE]"));
        assertTrue(lines.get(0).contains("mob=Rat"));
        assertTrue(lines.get(0).contains("id=1"));
        assertTrue(lines.get(0).contains("from= 42"));
        assertTrue(lines.get(0).contains("to= No Target"));
    }

}
