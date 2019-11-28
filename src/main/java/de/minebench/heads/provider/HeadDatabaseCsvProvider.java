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

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import de.minebench.heads.Head;
import de.minebench.heads.Heads;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class HeadDatabaseCsvProvider implements HeadsProvider {

    private final Heads plugin;

    private final Pattern tagPattern = Pattern.compile("\\|");

    public HeadDatabaseCsvProvider(Heads plugin) {
        this.plugin = plugin;
    }

    @Override
    public void loadHeads(Consumer<Head> onLoad) {
        int i = 0;
        int failed = 0;
        try (Stream<Path> files = Files.walk(plugin.getDataFolder().toPath())) {
            Optional<Path> csvFile = files
                    .filter(f -> f.getFileName().toString().startsWith("Custom-Head-DB") && f.getFileName().toString().endsWith(".csv"))
                    .max(Comparator.naturalOrder());
            if (csvFile.isPresent()) {
                CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(csvFile.get()))
                        .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                        .build();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length > 3) {
                        Head head = new Head(line[1], line[2], line[0], line[3]);
                        if (line.length > 5) {
                            for (String tag : tagPattern.split(line[5])) {
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
                plugin.getLogger().log(Level.SEVERE, "No heads database file found!");
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        plugin.getLogger().log(Level.INFO, "Loaded " + (i - failed) + " heads!" + (failed > 0 ? " " + failed + " failed to load :(" : ""));
    }
}
