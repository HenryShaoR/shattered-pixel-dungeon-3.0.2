package com.shatteredpixel.shatteredpixeldungeon.utils;
import com.badlogic.gdx.Input;
import com.watabou.input.KeyEvent;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class ScreenshotTest {
    @Test
    public void testHandle_inGame_screenshotCreated() throws Exception {
        Path dir = Files.createTempDirectory("shpd-screenshot-hotkey-test-");
        Path out = dir.resolve("shot.png");
        assertFalse(Files.exists(out));

        boolean handled = Screenshot.handle(
                new KeyEvent(Input.Keys.F12, true),
                true,
                () -> {
                    try {
                        Files.write(out, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        assertTrue(handled);
        assertTrue(Files.exists(out));
    }

    @Test
    public void testHandle_notInGame_screenshotNotCreated() throws Exception {
        Path dir = Files.createTempDirectory("shpd-screenshot-hotkey-test-");
        Path out = dir.resolve("shot.png");
        // noinspection ConstantConditions
        boolean handled = Screenshot.handle(
                new KeyEvent(Input.Keys.F12, true),
                false,
                () -> {
                    try {
                        Files.write(out, new byte[]{1});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        assertFalse(handled);
        assertFalse(Files.exists(out));
    }

    @Test
    public void testHandle_noEvent_screenshotNotCreated() throws Exception {
        Path dir = Files.createTempDirectory("shpd-screenshot-hotkey-test-");
        Path out = dir.resolve("shot.png");
        // noinspection ConstantConditions
        boolean handled = Screenshot.handle(
                null,
                true,
                () -> {
                    try {
                        Files.write(out, new byte[]{1});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        assertFalse(handled);
        assertFalse(Files.exists(out));
    }

    @Test
    public void testHandle_noAction_screenshotNotCreated() throws Exception {
        Path dir = Files.createTempDirectory("shpd-screenshot-hotkey-test-");
        Path out = dir.resolve("shot.png");
        // noinspection ConstantConditions
        boolean handled = Screenshot.handle(
                new KeyEvent(Input.Keys.F12, true),
                true,
                null
        );
        assertFalse(handled);
        assertFalse(Files.exists(out));
    }

    @Test
    public void testHandle_noEventNoAction_screenshotNotCreated() throws Exception {
        Path dir = Files.createTempDirectory("shpd-screenshot-hotkey-test-");
        Path out = dir.resolve("shot.png");
        // noinspection ConstantConditions
        boolean handled = Screenshot.handle(
                null,
                true,
                null
        );
        assertFalse(handled);
        assertFalse(Files.exists(out));
    }

    @Test
    public void testHandle_notPressed_screenshotNotCreated() throws Exception {
        Path dir = Files.createTempDirectory("shpd-screenshot-hotkey-test-");
        Path out = dir.resolve("shot.png");
        assertFalse(Files.exists(out));

        boolean handled = Screenshot.handle(
                new KeyEvent(Input.Keys.F12, false),
                true,
                () -> {
                    try {
                        Files.write(out, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        assertFalse(handled);
        assertFalse(Files.exists(out));
    }

    @Test
    public void testHandle_notF12_screenshotNotCreated() throws Exception {
        Path dir = Files.createTempDirectory("shpd-screenshot-hotkey-test-");
        Path out = dir.resolve("shot.png");
        assertFalse(Files.exists(out));

        boolean handled = Screenshot.handle(
                new KeyEvent(Input.Keys.F11, true),
                true,
                () -> {
                    try {
                        Files.write(out, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        assertFalse(handled);
        assertFalse(Files.exists(out));
    }
}