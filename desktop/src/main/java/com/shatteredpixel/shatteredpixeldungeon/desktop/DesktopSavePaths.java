package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.utils.SharedLibraryLoader;

import java.io.File;
import java.util.Locale;

/*
	this class is a utility class that should not be instantiate
	this class is reponsible for claculate the file path according to the
	current platform that the game is running on
 */
final class DesktopSavePaths {

	private DesktopSavePaths() {
	}

	// this method is used to generate a legal path given the vendor and opretating evnironment
	static ResolvedSavePath resolve(String title, String implementationTitleOrVendor) {
		String vendor = implementationTitleOrVendor;
		if (implementationTitleOrVendor.contains(".")) {
			String[] parts = implementationTitleOrVendor.split("\\.");
			if (parts.length > 1) {
				vendor = parts[1];
			}
		}

		String basePath = "";
		Files.FileType baseFileType = null;
		if (SharedLibraryLoader.isWindows) {
			if (System.getProperties().getProperty("os.name").equals("Windows XP")) {
				basePath = "Application Data/." + vendor + "/" + title + "/";
			} else {
				basePath = "AppData/Roaming/." + vendor + "/" + title + "/";
			}
			baseFileType = Files.FileType.External;
		} else if (SharedLibraryLoader.isMac) {
			basePath = "Library/Application Support/" + title + "/";
			baseFileType = Files.FileType.External;
		} else if (SharedLibraryLoader.isLinux) {
			String xdgHome = System.getenv("XDG_DATA_HOME");
			if (xdgHome == null) xdgHome = System.getProperty("user.home") + "/.local/share";

			String titleLinux = title.toLowerCase(Locale.ROOT).replace(" ", "-");
			basePath = xdgHome + "/." + vendor + "/" + titleLinux + "/";

			baseFileType = Files.FileType.Absolute;
		}

		return new ResolvedSavePath(basePath, baseFileType);
	}

	// container for the return value
	static final class ResolvedSavePath {
		final String basePath;
		final Files.FileType fileType;

		ResolvedSavePath(String basePath, Files.FileType fileType) {
			this.basePath = basePath;
			this.fileType = fileType;
		}

		// return the file instantiated with the valid path
		File asFile() {
			if (fileType == Files.FileType.Absolute) {
				return new File(basePath);
			} else if (fileType == Files.FileType.External) {
				return new File(System.getProperty("user.home"), basePath);
			} else if (fileType == Files.FileType.Local) {
				return new File(System.getProperty("user.dir"), basePath);
			} else {
				return new File(basePath);
			}
		}
	}
}
