package de.minebench.heads.provider;

/*
 * MbHeads
 * Copyright (c) 2019 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.minebench.heads.Head;
import de.minebench.heads.Heads;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.logging.Level;

public class JsonProvider implements HeadsProvider {

    private final Heads plugin;
    private final File file;

    public JsonProvider(Heads plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    @Override
    public void loadHeads(Consumer<Head> onLoad) {
        plugin.getLogger().log(Level.INFO, "Loading heads from " + file.getName() + "...");
        int i = 0;
        int failed = 0;
        if (file.exists() && file.isFile()) {
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                JsonElement json = new JsonParser().parse(reader);
                if (json.isJsonArray()) {
                    for (JsonElement element : json.getAsJsonArray()) {
                        if (element.isJsonObject()) {
                            JsonObject object = element.getAsJsonObject();
                            if (object.has("id") && object.has("name") && object.has("texture") && object.has("category")) {
                                Head head = new Head(
                                        object.get("id").getAsString(),
                                        object.get("name").getAsString(),
                                        object.get("category").getAsString(),
                                        object.get("texture").getAsString()
                                );
                                if (object.has("tags") && object.get("tags").isJsonArray()) {
                                    for (JsonElement tag : object.getAsJsonArray("tags")) {
                                        head.addTag(tag.getAsString());
                                    }
                                }
                                onLoad.accept(head);
                            } else {
                                plugin.getLogger().log(Level.WARNING, "Object " + element + " is missing required keys (id, name, and/or texture)!");
                                failed++;
                            }
                        } else {
                            plugin.getLogger().log(Level.WARNING, "Element " + element + " is not a json object!");
                            failed++;
                        }

                        i++;
                        if (i % 10000 == 0) {
                            plugin.getLogger().log(Level.INFO, "Processed " + i + " heads...");
                        }
                    }
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Content of " + file.getName() + " is not a json array!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().log(Level.SEVERE, "No heads database file at " + file.getPath() + " found!");
        }
        plugin.getLogger().log(Level.INFO, "Loaded " + (i - failed) + " heads!" + (failed > 0 ? " " + failed + " failed to load :(" : ""));
    }
}
