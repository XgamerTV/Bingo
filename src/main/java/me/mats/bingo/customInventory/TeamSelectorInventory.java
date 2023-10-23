package me.mats.bingo.customInventory;

import me.mats.bingo.game.BingoManager;
import me.mats.bingo.game.BingoTeam;
import me.mats.bingo.game.waiting.WaitingTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TeamSelectorInventory extends CustomInventory<Inventory>{

    private final BingoManager manager;

    public TeamSelectorInventory(BingoManager manager, Player p) {
        this.manager = manager;
        BingoTeam pBTeam = manager.getTeam(p);
        WaitingTeam pWTeam = null;
        if (pBTeam != null) {
            pWTeam = pBTeam.getWaitingTeam();
        }

       inventory = Bukkit.createInventory(null, 9,Component.text("Select a Team", NamedTextColor.DARK_GRAY));
        for (BingoTeam bingoTeam : manager.getBingoTeams()) {
            WaitingTeam wTeam = bingoTeam.getWaitingTeam();
            if (pWTeam != null && pWTeam == wTeam) {
                ItemStack item = pWTeam.getItem();
                ItemMeta itemMeta = item.getItemMeta().clone();
                itemMeta.addEnchant(Enchantment.LUCK, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                ItemStack newItem = new ItemStack(Material.valueOf(bingoTeam.getName().toUpperCase()+"_GLAZED_TERRACOTTA"));
                newItem.setItemMeta(itemMeta);
                inventory.addItem(newItem);
            } else {
                inventory.addItem(wTeam.getItem());
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item != null) {
            BingoTeam bTeam = manager.getTeam(item.getType());

            if (bTeam != null && manager.getTeam(p) != bTeam) {
                bTeam.addPlayer(p);
                ItemStack bed = p.getInventory().getItem(0);
                assert bed != null;
                bed.setType(Material.valueOf(bTeam.getName().toUpperCase()+"_BED"));

                for (Player t : manager.getPlayers()) {
                    if (CustomInventoryManager.getInventory(t.getOpenInventory()) instanceof TeamSelectorInventory) {
                        CustomInventoryManager.openInventory(t, new TeamSelectorInventory(manager,t));
                    }
                }
            }
        }
    }
}