package me.mats.bingo.game.ingame;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class BingoItem {
    //Not really needed here
    private Material item;
    private int[] position;
    private ItemMeta meta = null;

    BingoItem(Material item) {
        this.item = item;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    public void setMeta(ItemMeta meta) {
        this.meta = meta;
    }

    public ItemMeta getMeta() {
        return meta;
    }
}
