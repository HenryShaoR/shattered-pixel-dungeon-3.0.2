package com.shatteredpixel.shatteredpixeldungeon.desktop;

import org.json.JSONException;
import org.json.JSONTokener;
import org.json.JSONObject;

import com.shatteredpixel.shatteredpixeldungeon.desktop.DesktopSavePaths.ResolvedSavePath;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    private static void recoverHP(HashMap<String, JSONObject> saveData) {

    }

    private static void addInvulnerability(HashMap<String, JSONObject> saveData) {

    }

    private static void clearBuffs(HashMap<String, JSONObject> saveData) {

    }

    private static void editDepth(HashMap<String, JSONObject> saveData) {

    }

    private static JSONObject gzip2Json(File gzipFile) {
        try (GZIPInputStream gis =
                     new GZIPInputStream(Files.newInputStream(gzipFile.toPath()));
             InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            return new JSONObject(new JSONTokener(sb.toString()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse gzip file: " + gzipFile, e);
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    private static HashMap<String, HashMap<String, JSONObject>> loadAllSaves() {
        String title = System.getProperty("Specification-Title", "Shattered Pixel Dungeon");
        String implementationTitle = System.getProperty(
                "Implementation-Title",
                "com.shatteredpixel.shatteredpixeldungeon"
        );
        ResolvedSavePath savePath = DesktopSavePaths.resolve(title, implementationTitle);
        File saveFilesDir = savePath.asFile();

        assert(saveFilesDir.exists() && saveFilesDir.isDirectory());

        File[] saves = saveFilesDir.listFiles();

        HashMap<String, HashMap<String, JSONObject>> retVal = new HashMap<>();

        if (saves != null) {
            for (File save : saves) {
                if (save.isDirectory() && save.getName().startsWith("game")) {
                    File[] files = save.listFiles();
                    if (files == null) {
                        continue;
                    }

                    HashMap<String, JSONObject> saveData = new HashMap<>();
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

    private static HashMap<String, JSONObject> selectSave() {
        HashMap<String, HashMap<String, JSONObject>> saves = loadAllSaves();
        System.out.println();
        String selectedSave;
        HashMap<String, JSONObject> selectedSaveData;
        do {
            System.out.println("Select a save to edit:");
            selectedSave = scanner.nextLine();
        } while ((selectedSaveData = saves.get(selectedSave)) == null);

        return selectedSaveData;
    }

    private static void updateSave(HashMap<String, JSONObject> saveData) {
        List<Map.Entry<String, Consumer<HashMap<String, JSONObject>>>> options = List.of(
            Map.entry("Recover to max HP", SaveEditor::recoverHP),
            Map.entry("Invulnerability buff", SaveEditor::addInvulnerability),
            Map.entry("Clear buffs", SaveEditor::clearBuffs),
            Map.entry("Edit depth", SaveEditor::editDepth),
            Map.entry("Exit", sd -> {})
        );

        while (true) {
            System.out.println();
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
        System.out.println("================================================================================");
        HashMap<String, JSONObject> selectedSave = selectSave();
        updateSave(selectedSave);
        System.out.println("================================================================================");

    }
}
