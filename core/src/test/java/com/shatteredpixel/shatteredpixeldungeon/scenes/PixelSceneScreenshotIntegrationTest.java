package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.watabou.input.KeyEvent;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.junit.Test;

import static com.badlogic.gdx.Input.Keys.F12;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PixelSceneScreenshotIntegrationTest {

	@Test
	public void handleScreenshotHotkey_callsPlatformScreenshotInGame() {
		// Arrange
		PlatformSupport previous = Game.platform;
		PlatformSupport platform = mock(PlatformSupport.class);
		Game.platform = platform;
		try {
			PixelScene scene = new PixelScene();
			scene.inGameScene = true;

			// Act
			boolean handled = scene.handleScreenshotHotkey(new KeyEvent(F12, true));

			// Assert
			assertTrue(handled);
			verify(platform, times(1)).takeScreenshot();
		} finally {
			Game.platform = previous;
		}
	}
}
