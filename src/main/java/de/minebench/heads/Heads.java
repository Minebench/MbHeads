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

import de.minebench.heads.provider.CsvProvider;
import de.minebench.heads.provider.HeadsProvider;
import de.minebench.heads.provider.JsonProvider;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.apache.commons.text.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class Heads extends JavaPlugin {

    private HeadsManager manager;

    private static final String[] GUI_SETUP = {
            "hhhhhhhhh",
            "hhhhhhhhh",
            "hhhhhhhhh",
            "hhhhhhhhh",
            "hhhhhhhhh",
            "p f b l n"
    };

    private InventoryGui gui;
    private Function<Head, GuiElement> headElementCreator;

    @Override
    public void onEnable() {
        headElementCreator = head -> new StaticGuiElement('h', head.getItemStack(), click -> {
            if (click.getType() == ClickType.NUMBER_KEY || click.getType().isShiftClick()) {
                return true;
            }
            ItemStack clone = head.getItemStack().clone();
            ItemMeta cloneMeta = clone.getItemMeta();
            cloneMeta.setDisplayName(ChatColor.YELLOW + head.getName());
            if (click.getType() != ClickType.RIGHT && click.getType() != ClickType.CONTROL_DROP) {
                cloneMeta.setLore(Arrays.asList(
                        WordUtils.capitalizeFully(head.getCategory()),
                        String.join(", ", head.getTags())
                ));
            }
            clone.setItemMeta(cloneMeta);
            if (click.getType() == ClickType.DROP || click.getType() == ClickType.CONTROL_DROP) {
                click.getWhoClicked().getWorld()
                        .dropItemNaturally(click.getWhoClicked().getLocation(), clone)
                        .setThrower(click.getWhoClicked().getUniqueId());
            } else {
                click.getWhoClicked().getInventory().addItem(clone);
            }
            return true;
        },
                ChatColor.YELLOW + head.getName(),
                WordUtils.capitalizeFully(head.getCategory()),
                String.join(", ", head.getTags()),
                ChatColor.GRAY + "Left click to add head to inventory.",
                ChatColor.GRAY + "Right click to add without text."
        );

        loadConfig();
        getCommand("heads").setExecutor(this);
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            List<HeadsProvider> providers = new ArrayList<>();
            for (String provider : getConfig().getConfigurationSection("providers").getKeys(false)) {
                ConfigurationSection providerConfig = getConfig().getConfigurationSection("providers." + provider);
                try {
                    HeadsProvider.Type type = HeadsProvider.Type.valueOf(providerConfig.getString("type").toUpperCase());
                    switch (type) {
                        case JSON:
                            File jsonFile = new File(getDataFolder(), providerConfig.getString("file"));
                            if (!jsonFile.exists() && providerConfig.isString("url")) {
                                getLogger().log(Level.INFO, "Could not find " + jsonFile + ". Downloading one from configured URL...");
                                if (!downloadFile(providerConfig.getString("url"), jsonFile)) {
                                    continue;
                                }
                            }
                            providers.add(new JsonProvider(this, jsonFile));
                            break;
                        case CSV:
                            providers.add(new CsvProvider(this,
                                    new File(getDataFolder(), providerConfig.getString("file")),
                                    providerConfig.getString("separator", ";").charAt(0),
                                    providerConfig.getStringList("mapping"),
                                    providerConfig.getString("tag-splitter")
                            ));
                            break;
                        default:
                            getLogger().log(Level.SEVERE, "Unsupported provider " + type);
                    }
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, "Unknown provider " + getConfig().getString("provider"));
                }
            }
            getServer().getScheduler().runTask(Heads.this, () -> {
                manager = new HeadsManager(providers.toArray(new HeadsProvider[0]));
                gui = new InventoryGui(this, "Select category", GUI_SETUP, getNavBar());
                GuiElementGroup headGroup = new GuiElementGroup('h');
                gui.addElement(headGroup);
                for (Category category : manager.getCategories()) {
                    headGroup.addElement(getCategoryElement(category));
                }
            });
        });
    }

    private boolean downloadFile(String url, File file) {
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            long readSinceLastLog = 0;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                readSinceLastLog += bytesRead;
                if (readSinceLastLog >= 1024 * 1024) {
                    getLogger().log(Level.INFO, "Downloaded " + (file.length() / 1024 / 1024) + " MB...");
                    readSinceLastLog = 0;
                }
            }
            getLogger().log(Level.INFO, "Downloaded " + url + " to " + file.getAbsolutePath() + " (" + (file.length() / 1024 / 1024) + " MB)");
            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not download file from " + url + " to " + file, e);
            return false;
        }
    }

    private GuiElement getCategoryElement(Category category) {
        InventoryGui categoryGui = buildGui(WordUtils.capitalizeFully(category.getId()), category.getHeads());
        return new StaticGuiElement('h', category.getIcon(), click -> {
            categoryGui.show(click.getWhoClicked());
            return true;
        }, ChatColor.YELLOW + WordUtils.capitalizeFully(category.getId()), category.getHeads().size() + " Heads");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("mbheads.command.reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;
            } else if (sender instanceof Player && "search".equalsIgnoreCase(args[0]) && sender.hasPermission("mbheads.command.search")) {
                String query = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
                buildGui("Heads for '" + query + "':", manager.findHeads(query)).show((HumanEntity) sender);
                return true;
            }
        } else if (sender instanceof Player) {
            gui.show((HumanEntity) sender);
            return true;
        }
        return false;
    }

    private InventoryGui buildGui(String title, Collection<Head> heads) {
        InventoryGui newGui = new InventoryGui(this, title, GUI_SETUP, getNavBar());
        GuiElementGroup headGroup = new GuiElementGroup('h');
        newGui.addElement(headGroup);
        for (Head head : heads) {
            headGroup.addElement(head.getElement(headElementCreator));
        }
        return newGui;
    }

    private GuiElement[] getNavBar() {
        return new GuiElement[] {
                new GuiPageElement('f', new ItemStack(Material.ARROW), GuiPageElement.PageAction.FIRST, "First page"),
                new GuiPageElement('l', new ItemStack(Material.ARROW), GuiPageElement.PageAction.LAST, "Last page (%pages%)"),
                new GuiPageElement('p', new ItemStack(Material.PAPER), GuiPageElement.PageAction.PREVIOUS, "Previous page (%prevpage%)"),
                new GuiPageElement('n', new ItemStack(Material.PAPER), GuiPageElement.PageAction.NEXT, "Next page (%nextpage%)"),
                new StaticGuiElement('b', new ItemStack(Material.RED_WOOL), click -> {
                    if (!InventoryGui.goBack(click.getWhoClicked())) {
                        getServer().getScheduler().runTask(this, () -> click.getWhoClicked().closeInventory());
                    }
                    return true;
                }, "Back")
        };
    }
}
