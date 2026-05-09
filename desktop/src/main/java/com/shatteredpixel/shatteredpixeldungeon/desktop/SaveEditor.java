package com.shatteredpixel.shatteredpixeldungeon.desktop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.json.JSONObject;

import com.shatteredpixel.shatteredpixeldungeon.desktop.DesktopSavePaths.ResolvedSavePath;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SaveEditor {
    private static final Scanner scanner = new Scanner(System.in);

    private SaveEditor() {}

    private static void recoverHP(HashMap<String, JSONObject> saveData) {
        JSONObject game = saveData.get("game.dat");
        JSONObject hero = game.getJSONObject("hero");
        hero.put("HP", hero.getInt("HT"));
    }

    private static void addInvulnerability(HashMap<String, JSONObject> saveData) {
        JSONObject game = saveData.get("game.dat");
        JSONObject hero = game.getJSONObject("hero");
        JSONArray buffs = hero.getJSONArray("buffs");
        // Construct the buff:
        JSONObject invulnerability = new JSONObject();
        invulnerability.put("id", Integer.MAX_VALUE);
        invulnerability.put("time", 0);
        invulnerability.put("__className", "com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InvulnerabilityForever");
        // Add buff
        buffs.put(invulnerability);
    }

    private static void clearBuffs(HashMap<String, JSONObject> saveData) {
        JSONObject game = saveData.get("game.dat");
        JSONObject hero = game.getJSONObject("hero");
        hero.remove("buffs");
        hero.put("buffs", new JSONArray());
    }

    private static void editDepth(HashMap<String, JSONObject> saveData) {
        System.out.print("\nEnter the new depth: ");
        int newDepth = scanner.nextInt();
        JSONObject game = saveData.get("game.dat");
        int oldMaxDepth = game.getInt("maxDepth");
        game.put("depth", newDepth);
        game.put("maxDepth", Math.max(newDepth, oldMaxDepth));
    }

    private static void saveChange(HashMap<String, JSONObject> saveData) {
        for (String filename : saveData.keySet()) {
            Path targetPath = Paths.get(filename);
            JSONObject json = saveData.get(filename);

            try {
                // 1. Backup existing file if it exists
                if (Files.exists(targetPath)) {
                    String backupName = filename + "-" + Instant.now().toEpochMilli() + ".old";
                    Path backupPath = Paths.get(backupName);
                    Files.move(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // 2. Create temp file
                Path tempPath = Files.createTempFile("json-save-", ".gz");

                try (OutputStream fos = Files.newOutputStream(tempPath);
                     GZIPOutputStream gos = new GZIPOutputStream(fos);
                     OutputStreamWriter osw = new OutputStreamWriter(gos);
                     BufferedWriter writer = new BufferedWriter(osw)) {

                    writer.write(json.toString());
                }

                // 3. Atomically move temp -> target
                Files.move(
                        tempPath,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException e) {
                throw new RuntimeException("Failed to save file: " + filename, e);
            }
        }
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
            Map.entry("Save and exit", SaveEditor::saveChange)
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
