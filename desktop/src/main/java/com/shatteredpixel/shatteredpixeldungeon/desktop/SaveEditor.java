package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.shatteredpixel.shatteredpixeldungeon.desktop.DesktopSavePaths.ResolvedSavePath;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public class SaveEditor {
    private static final Scanner scanner = new Scanner(System.in);

    private SaveEditor() {}

    private static void recoverHP(HashMap<String, JsonNode> saveData) {

    }

    private static void addInvulnerability(HashMap<String, JsonNode> saveData) {

    }

    private static void clearBuffs(HashMap<String, JsonNode> saveData) {

    }

    private static void editDepth(HashMap<String, JsonNode> saveData) {

    }

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
        HashMap<String, JsonNode> selectedSaveData;
        do {
            System.out.println("Select a save to edit:");
            selectedSave = scanner.nextLine();
        } while ((selectedSaveData = saves.get(selectedSave)) == null);

        return selectedSaveData;
    }

    private static void updateSave(HashMap<String, JsonNode> saveData) {
        List<Map.Entry<String, Consumer<HashMap<String, JsonNode>>>> options = List.of(
            Map.entry("Recover to max HP", SaveEditor::recoverHP),
            Map.entry("Invulnerability buff", SaveEditor::addInvulnerability),
            Map.entry("Clear buffs", SaveEditor::clearBuffs),
            Map.entry("Edit depth", SaveEditor::editDepth),
            Map.entry("Exit", sd -> {})
        );

        while (true) {
            for (int i = 0; i < options.size(); i++) {
                System.out.println((i + 1) + ": " + options.get(i).getKey());
            }

            int choice = scanner.nextInt();

            if (choice < 1 || choice > options.size()) {
                System.out.println("Invalid choice");
                continue;
            }

            if (choice == options.size()) {
                return;
            }

            options.get(choice - 1).getValue().accept(saveData);
        }
    }

    public static void main(String[] args) {
        initOptions();
        System.out.println("================================================================================");
        HashMap<String, JsonNode> selectedSave = selectSave();
        updateSave(selectedSave);
        System.out.println("================================================================================");

    }
}
