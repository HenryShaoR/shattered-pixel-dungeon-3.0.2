package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.shatteredpixel.shatteredpixeldungeon.desktop.DesktopSavePaths.ResolvedSavePath;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

public class SaveEditor {
    private SaveEditor() {}

    private static final Scanner scanner = new Scanner(System.in);

    private static JsonNode gzip2Json(File gzipFile) {
        ObjectMapper mapper = new ObjectMapper();

        try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(gzipFile.toPath()))) {
            return mapper.readTree(gis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse gzip JSON file: " + gzipFile, e);
        }
    }

    private static HashMap<String, HashMap<String, JsonNode>> loadAllSaves() {
        String title = System.getProperty("Specification-Title", "Shattered Pixel Dungeon");
        String implementationTitle = System.getProperty(
                "Implementation-Title",
                "com.shatteredpixel.shatteredpixeldungeon"
        );
        ResolvedSavePath savePath = DesktopSavePaths.resolve(title, implementationTitle);
        File saveFilesDir = savePath.asFile();

        assert(saveFilesDir.exists() && saveFilesDir.isDirectory());

        File[] saves = saveFilesDir.listFiles();

        HashMap<String, HashMap<String, JsonNode>> retVal = new HashMap<>();

        if (saves != null) {
            for (File save : saves) {
                if (save.isDirectory() && save.getName().startsWith("game")) {
                    File[] files = save.listFiles();
                    if (files == null) {
                        continue;
                    }

                    HashMap<String, JsonNode> saveData = new HashMap<>();
                    System.out.println(" - " + save.getName());

                    for (File file : files) {
                        saveData.put(file.getName(), gzip2Json(file));
                        System.out.println("   - " + file.getName());
                    }
                    retVal.put(save.getName(), saveData);
                }
            }
        }
        return retVal;
    }

    private static HashMap<String, JsonNode> selectSave() {
        HashMap<String, HashMap<String, JsonNode>> saves = loadAllSaves();
        System.out.println("\n");
        String selectedSave;
        HashMap<String, JsonNode> selectedSaveData = null;
        do {
            System.out.println("Select a save to edit:");
            selectedSave = scanner.nextLine();
        } while ((selectedSaveData = saves.get(selectedSave)) == null);

        return selectedSaveData;
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        HashMap<String, JsonNode> selectedSave = selectSave();
        System.out.println("================================================================================");

    }
}
