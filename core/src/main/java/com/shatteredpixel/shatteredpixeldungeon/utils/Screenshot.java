package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.badlogic.gdx.Input;
import com.watabou.input.KeyEvent;

public final class Screenshot {

	private Screenshot() {}

	public static boolean handle(KeyEvent event, boolean inGameScene, Runnable takeScreenshot) {
		if (!inGameScene) return false;
		if (event == null || takeScreenshot == null) return false;

		if (event.pressed && event.code == Input.Keys.F12) {
			// Decouple the take screenshot logics to ensure testability
			takeScreenshot.run();
			return true;
		}

		return false;
	}
}

