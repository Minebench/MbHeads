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

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.themoep.inventorygui.GuiElement;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class Head implements Comparable<Head> {
    private final String id;
    private final String name;
    private final String category;
    private final String textureUrl;

    private final Set<String> tags = new LinkedHashSet<>();

    // Caching
    private ItemStack cachedItem = null;
    private GuiElement cachedElement = null;

    public Head(String id, String name, String category, String textureUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.textureUrl = textureUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    @Override
    public int hashCode() {
        return textureUrl.hashCode();
    }

    @Override
    public int compareTo(Head o) {
        return getName().compareToIgnoreCase(o.getName());
    }

    public ItemStack getItemStack() {
        if (cachedItem == null) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(("Head:" + getId()).getBytes()), "Head_" + getId());
            String encodedTexture = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + textureUrl + "\"}}}").getBytes(StandardCharsets.UTF_8));
            profile.setProperty(new ProfileProperty("textures", encodedTexture));
            meta.setPlayerProfile(profile);
            skull.setItemMeta(meta);
            cachedItem = skull;
        }
        return cachedItem;
    }

    public GuiElement getElement(Function<Head, GuiElement> creator) {
        if (cachedElement == null) {
            cachedElement = creator.apply(this);
        }
        return cachedElement;
    }
}
