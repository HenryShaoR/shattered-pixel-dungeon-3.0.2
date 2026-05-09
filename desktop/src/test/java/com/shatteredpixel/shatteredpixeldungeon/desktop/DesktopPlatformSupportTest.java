package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DesktopPlatformSupportTest {

	@Test
	public void takeScreenshot_writesPngToPicturesFolder() throws Exception {
		// Arrange
		Pixmap source = mock(Pixmap.class);
		Pixmap flipped = mock(Pixmap.class);

		when(source.getWidth()).thenReturn(4);
		when(source.getHeight()).thenReturn(3);
		when(source.getFormat()).thenReturn(Pixmap.Format.RGBA8888);

		doNothing().when(flipped).drawPixmap(source, 0, 0, 4, 1, 0, 2, 4, 1);
		doNothing().when(flipped).drawPixmap(source, 0, 1, 4, 1, 0, 1, 4, 1);
		doNothing().when(flipped).drawPixmap(source, 0, 2, 4, 1, 0, 0, 4, 1);

		File tempDir = Files.createTempDirectory("shpd-desktop-screenshot-test-").toFile();
		String screenshotDirPath = new File(tempDir, "Pictures/ShatteredPixelDungeon").getAbsolutePath() + File.separator;

		// Act
		FakeDesktopPlatformSupport support = new FakeDesktopPlatformSupport(source, flipped, screenshotDirPath, "2026-05-09_17-16-00");
		String path = support.takeScreenshot();

		// Assert
		assertEquals(screenshotDirPath, path);
		assertNotNull(support.writtenFile);
		assertEquals("screenshot_2026-05-09_17-16-00.png", support.writtenFile.name());
		verify(source).dispose();
		verify(flipped).dispose();
	}

	// The fake class to provide minimal functionalities needed to test the behaviour
	private static class FakeDesktopPlatformSupport extends DesktopPlatformSupport {
		private final Pixmap source;
		private final Pixmap flipped;
		private final String screenshotDirPath;
		private final String timestamp;
		private FileHandle writtenFile;

		private FakeDesktopPlatformSupport(Pixmap source, Pixmap flipped, String screenshotDirPath, String timestamp) {
			this.source = source;
			this.flipped = flipped;
			this.screenshotDirPath = screenshotDirPath;
			this.timestamp = timestamp;
		}

		@Override
		protected Pixmap getFrameBufferPixmap() {
			return source;
		}

		@Override
		protected Pixmap createPixmap(int width, int height, Pixmap.Format format) {
			return flipped;
		}

		@Override
		protected String timestamp() {
			return timestamp;
		}

		@Override
		protected String screenshotDirectoryPath() {
			return screenshotDirPath;
		}

		@Override
		protected FileHandle absoluteFile(String path) {
			return new FileHandle(path);
		}

		@Override
		protected void writePng(FileHandle file, Pixmap pixmap) {
			this.writtenFile = file;
		}
	}
}
