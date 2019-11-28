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

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedHashSet;

public class Category {
    private String id;
    private Collection<Head> heads = new LinkedHashSet<>();

    public Category(String id) {
        this.id = id.toLowerCase();
    }

    public String getId() {
        return id;
    }

    public Collection<Head> getHeads() {
        return heads;
    }

    public void addHead(Head head) {
        heads.add(head);
    }

    public ItemStack getIcon() {
        return heads.isEmpty() ? new ItemStack(Material.BARRIER) : heads.iterator().next().getItemStack();
    }
}
