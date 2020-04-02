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

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import de.minebench.heads.Head;
import de.minebench.heads.Heads;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class CsvProvider implements HeadsProvider {

    private final Heads plugin;
    private final File file;
    private final char separator;
    private final Map<String, Integer> mapping;

    private final Pattern tagPattern;

    public CsvProvider(Heads plugin, File file, char separator, List<String> mapping, String tagPattern) {
        this.plugin = plugin;
        this.file = file;
        this.separator = separator;
        this.mapping = new LinkedHashMap<>();
        for (int i = 0; i < mapping.size(); i++) {
            this.mapping.put(mapping.get(i).toLowerCase(), i);
        }
        this.tagPattern = Pattern.compile(tagPattern);
    }

    @Override
    public void loadHeads(Consumer<Head> onLoad) {
        int i = 0;
        int failed = 0;
        try {
            if (file.exists() && file.isFile()) {
                CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file.toPath()))
                        .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                        .build();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length > 3) {
                        Head head = new Head(line[mapping.get("id")], line[mapping.get("name")], line[mapping.get("category")], line[mapping.get("texture")]);
                        if (line.length > mapping.getOrDefault("tags", mapping.size())) {
                            for (String tag : tagPattern.split(line[mapping.getOrDefault("tags", mapping.size() + 1)])) {
                                head.addTag(tag);
                            }
                        }
                        onLoad.accept(head);
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Line " + Arrays.toString(line) + " is invalid!");
                        failed++;
                    }
                    i++;
                    if (i % 10000 == 0) {
                        plugin.getLogger().log(Level.INFO, "Processed " + i + " heads...");
                    }
                }
            } else {
                plugin.getLogger().log(Level.SEVERE, "No heads database file at " + file.getPath() + " found!");
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        plugin.getLogger().log(Level.INFO, "Loaded " + (i - failed) + " heads!" + (failed > 0 ? " " + failed + " failed to load :(" : ""));
    }
}
