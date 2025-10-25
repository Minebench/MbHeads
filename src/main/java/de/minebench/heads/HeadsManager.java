package de.minebench.heads;

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

import de.minebench.heads.provider.HeadsProvider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class HeadsManager {

    private Map<String, Head> heads = new LinkedHashMap<>();
    private Map<String, Category> categories = new TreeMap<>();

    public HeadsManager(HeadsProvider... providers) {
        for (HeadsProvider provider : providers) {
            provider.loadHeads(head -> {
                heads.put(head.getId(), head);
                Category category = categories.get(head.getCategory().toLowerCase());
                if (category == null) {
                    category = new Category(head.getCategory());
                    categories.put(category.getId().toLowerCase(), category);
                }
                category.getHeads().add(head);
            });
        }
    }

    public Collection<Category> getCategories() {
        return categories.values();
    }

    public Category getCategory(String id) {
        return categories.get(id.toLowerCase());
    }

    public Map<String, Head> getHeads() {
        return heads;
    }

    public Collection<Head> findHeads(String query) {
        query = query.toLowerCase();
        boolean isId = query.startsWith("id:");
        Set<Head> heads = new TreeSet<>();
        if (isId) {
            Head head = getHeads().get(query.substring(3));
            if (head != null) {
                heads.add(head);
            }
            return heads;
        }

        for (Category category : categories.values()) {
            if (category.getId().toLowerCase().contains(query)) {
                heads.addAll(category.getHeads());
            }
        }

        for (Head head : getHeads().values()) {
            if (head.getName().toLowerCase().contains(query) || head.getId().toLowerCase().contains(query)) {
                heads.add(head);
                continue;
            }
            for (String tag : head.getTags()) {
                if (tag.toLowerCase().contains(query)) {
                    heads.add(head);
                    break;
                }
            }
        }

        return heads;
    }
}
