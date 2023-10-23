package me.mats.bingo.game.ingame;


import me.mats.bingo.game.BingoTeam;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;

public class BingoCollectListener implements Listener {

    private final IngameState state;


    public BingoCollectListener(IngameState state) {
        this.state = state;
    }


    public void checkItem(ItemStack item, Player p) {
        Material mat = item.getType();

        if (state.getBingoItemsMap().containsKey(mat)) {
            BingoItem bItem = state.getBingoItemsMap().get(mat);
            BingoTeam team = state.getManager().getTeam(p);

            // Check for special Items
            if (mat == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta itemMeta =(EnchantmentStorageMeta) item.getItemMeta();
                EnchantmentStorageMeta bItemMeta = (EnchantmentStorageMeta) bItem.getMeta();

                for (Enchantment ench : bItemMeta.getStoredEnchants().keySet()) {
                    if (itemMeta.getStoredEnchants().containsKey(ench) && itemMeta.getStoredEnchants().get(ench) == bItemMeta.getStoredEnchantLevel(ench)) {
                        team.checkForBingo(bItem.getPosition(), mat.toString().toLowerCase());
                    }
                }
            } else if (mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION || mat == Material.TIPPED_ARROW) {
                PotionMeta itemMeta = (PotionMeta) item.getItemMeta();
                PotionMeta bItemMeta = (PotionMeta) bItem.getMeta();

                PotionData itemData = itemMeta.getBasePotionData();
                PotionData bItemData = bItemMeta.getBasePotionData();

                if (itemData.getType() == bItemData.getType() && itemData.isExtended() == bItemData.isExtended() && itemData.isUpgraded() == bItemData.isUpgraded()) {
                    team.checkForBingo(bItem.getPosition(), mat.toString().toLowerCase());
                }
            } else {
                team.checkForBingo(bItem.getPosition(), mat.toString().toLowerCase());
            }
        }
    }



    @EventHandler
    public void onInventoryPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && state.getManager().getPlayers().contains(p)) {
            ItemStack item = e.getItem().getItemStack();
            if (item.getItemMeta() == null || (item.getItemMeta() != null && !item.getItemMeta().isUnbreakable())) {
                checkItem(item, p);
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        InventoryAction action = e.getAction();
        if (state.getManager().getPlayers().contains(p)) {
            if (e.getCurrentItem() != null && e.getClickedInventory() == p.getOpenInventory().getTopInventory() && (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF || action == InventoryAction.SWAP_WITH_CURSOR || action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME)) {
                ItemStack item = e.getCurrentItem();
                if (item.getItemMeta() == null || (item.getItemMeta() != null && !item.getItemMeta().isUnbreakable())) {
                    checkItem(item, p);
                }
            }
        }
    }
}
