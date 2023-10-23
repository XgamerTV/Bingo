package me.mats.bingo.customInventory;

import me.mats.bingo.game.BingoManager;
import me.mats.bingo.game.BingoTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class BackpackInventory extends CustomInventory<Inventory> {

    private final BingoManager manager;
    private final BingoTeam bingoTeam;

    public BackpackInventory(BingoManager manager, BingoTeam bingoTeam) {
        this.bingoTeam = bingoTeam;
        this.manager = manager;
        inventory =  Bukkit.createInventory(null, 27, Component.text("Backpack", NamedTextColor.DARK_GRAY).decoration(TextDecoration.UNDERLINED, true));//.color(TextColor.color(bingoTeam.getColorCode())));
    }


    @Override
    public void onClick(InventoryClickEvent e) {
        // Here we want to always update the Backpack item and forbid any interaction with it
        //Bukkit.getLogger().info(e.getClick().name()+" "+e.getAction().name());
        if ((e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.BUNDLE) || (e.getClick() == ClickType.NUMBER_KEY && e.getView().getBottomInventory().getItem(e.getHotbarButton()) != null && e.getView().getBottomInventory().getItem(e.getHotbarButton()).getType() == Material.BUNDLE)) {
            e.setCancelled(true);
        } else {
            // The event still goes through the other handlers so removing and adding to backpack is still possible
            Bukkit.getScheduler().runTask(manager.getPlugin(), bingoTeam::updateBackPack);
        }

    }
}
