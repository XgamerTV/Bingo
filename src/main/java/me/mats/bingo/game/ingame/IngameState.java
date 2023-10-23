package me.mats.bingo.game.ingame;


import me.mats.advancementinteraction.TeamAdvancements;
import me.mats.bingo.enums.Color;
import me.mats.bingo.game.BingoManager;
import me.mats.bingo.game.BingoTeam;
import me.mats.bingo.game.GameState;
import me.mats.bingo.game.finished.FinishedState;
import me.mats.bingo.message.Message;
import me.mats.bingo.message.MessageBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class IngameState extends GameState {

    private Map<Material, BingoItem> bingoItemsMap = null;
    // This is needed for Gapper Ability
    private Map<BingoItem, Material> reverseMap = null;

    // These are not changeable at this point
    private final int size;
    private final BingoLists.ListType setting;

    private final BingoCollectListener bingoCollectListener;
    private final BackpackListener backpackListener;
    private final SpawnListener spawnListener;
    private final AbilitiesMenuListener abilitiesMenuListener;
    private final AbilitiesListener abilitiesListener;
    private final int extraAbilityPoints;
    private final int spawnTime;

    // To later send final Advancement Screen
    private List<ItemStack> fieldItems;
    private float[][] fieldPositions;

    private final Abilities abilities = new Abilities();


    public IngameState(BingoManager manager, int size, BingoLists.ListType setting, int extraAbilityPoints, int spawnTime){
        super.manager = manager;
        this.size = size;
        this.setting = setting;
        this.extraAbilityPoints = extraAbilityPoints;
        this.spawnTime = spawnTime;

        this.bingoCollectListener = new BingoCollectListener(this);
        this.backpackListener = new BackpackListener(this);
        this.spawnListener = new SpawnListener(this);
        this.abilitiesMenuListener = new AbilitiesMenuListener(this);
        this.abilitiesListener = new AbilitiesListener(this);
        // Worldstuff here


    }

    public void newBingoField() {
        // Check if a Bingo Field has already been generated and if yes remove it
        List<String> removeAdvancements = getOldAdvancements();
        TeamAdvancements.revokeAdvancements(manager.getPlayers(), "bingo", removeAdvancements);

        int size2 = size*size;
        // Get the itemsCh
        bingoItemsMap = BingoLists.getRandoms(setting, size2);
        Material[] materialsList = bingoItemsMap.keySet().toArray(Material[]::new);

        // Generate field
        float[][] positions = new float[size2][2];

        boolean[][]  bingoField = new boolean[size][size];


        int n = 0;
        float x = 0;
        for (int i = 0; i < size; i++) {
            float y = 0;
            for (int j = 0; j < size; j++) {
                BingoItem item = bingoItemsMap.get(materialsList[n]);
                // Set bingoField to false
                bingoField[i][j] = false;
                // Set the position
                item.setPosition(new int[]{i,j});
                // Advancement Position
                positions[n] = new float[]{x, y};
                y += 0.9;
                n += 1;
            }
            x += 0.9;
        }

        // Add Field per Team (Deep Copy)
        for (BingoTeam team : manager.getBingoTeams()) {
            boolean[][] copiedField = new boolean[bingoField.length][];
            for (int i = 0; i < bingoField.length; i++) {
                copiedField[i] = bingoField[i].clone();
            }
            team.setBingoField(copiedField);
        }

        // Generate ItemStacks
        List<ItemStack> items = new ArrayList<>(size2);
        for (Material material : materialsList) {
            ItemStack item = new ItemStack(material);
            if (material == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                BingoLists.getRandomEnchant(meta);

                item.setItemMeta(meta);
                bingoItemsMap.get(material).setMeta(meta);

                Bukkit.getLogger().info(meta.getAsString());

            } else if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION || material == Material.TIPPED_ARROW) {
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                BingoLists.getRandomPotion(setting, meta);

                Bukkit.getLogger().info(meta.getAsString());

                item.setItemMeta(meta);
                bingoItemsMap.get(material).setMeta(meta);

                Bukkit.getLogger().info("Potion");
            }
            items.add(item);
        }

        // To later send final Advancement Screen
        fieldItems = items;
        fieldPositions = positions;
        // Send Packets
        for (BingoTeam bTeam : manager.getBingoTeams()) {
            bTeam.getAdvancement().sendAdvancements(bTeam.getPlayers(), items, positions, removeAdvancements);
        }

        // Create reversed Map
        reverseMap = new HashMap<>();
        for (Map.Entry<Material, BingoItem> entry : bingoItemsMap.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }
    }

    private List<String> getOldAdvancements() {
        // Check if a Bingo Field has already been generated and if yes remove it
        List<String> removeAdvancements = new ArrayList<>();
        if (bingoItemsMap != null) {
            Material[] oldMaterialsList = bingoItemsMap.keySet().toArray(Material[]::new);
            for (Material m : oldMaterialsList) {
                removeAdvancements.add(m.toString().toLowerCase());
            }
        }
        return removeAdvancements;
    }

    public void validateNewBingoPosition(int[] position) {
        boolean winnerExists = false;
        for (BingoTeam b : manager.getBingoTeams()) {
            if (!b.getPlayers().isEmpty() && abilities.getThiefAbilityList().contains(b.getPlayers().get(0))) {
                boolean isWinner = b.hasWonWithNewPosition(position);
                if (isWinner) {
                    winnerExists = true;
                }

            }
        }
        if (winnerExists)
            stop();
    }

    public Material getMaterialFromPosition(int[] position) {
        for (BingoItem b : bingoItemsMap.values()) {
            if (Arrays.equals(b.getPosition(), position)) {
                return reverseMap.get(b);
            }
        }
        // Should never happen
        return null;
    }


    @Override
    public void start() {
        newBingoField();

        // Register required Listeners for Spawn
        Bukkit.getPluginManager().registerEvents(spawnListener, manager.getPlugin());
        Bukkit.getPluginManager().registerEvents(abilitiesMenuListener, manager.getPlugin());

        ItemStack abilities = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemMeta meta = abilities.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        meta.displayName(Component.text("Abilities", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        abilities.setItemMeta(meta);

        ItemStack crafting = new ItemStack(Material.CRAFTING_TABLE);
        meta = crafting.getItemMeta();
        meta.displayName(Component.text("Crafting Recipes", TextColor.color(0xFCCB00)).decoration(TextDecoration.ITALIC, false));
        crafting.setItemMeta(meta);

        ItemStack furnace = new ItemStack(Material.FURNACE);
        meta = furnace.getItemMeta();
        meta.displayName(Component.text("Smelting Recipes", TextColor.color(0xFCCB00)).decoration(TextDecoration.ITALIC, false));
        furnace.setItemMeta(meta);

        List<NamespacedKey> recipeNames = new ArrayList<>();
        Bukkit.recipeIterator().forEachRemaining(r -> recipeNames.add(((Keyed) r).getKey()));

        for (Player p : manager.getPlayers()) {
            Team playerTeam = manager.getBoard().getPlayerTeam(p);
            p.playerListName(playerTeam.prefix().append(Component.text(p.getName(), playerTeam.color())).append(Component.text(" ")).append(Message.O_BRACKET.getComponent()).append(Component.text("O", Color.OVERWORLD.getTextColor())).append(Message.C_BRACKET.getComponent()));
            p.getInventory().setItem(8, abilities);
            p.getInventory().setItem(0, crafting);
            p.getInventory().setItem(1, furnace);

            // Give Player all recipes
            p.discoverRecipes(recipeNames);
        }
        SpawnCountdown countdown = new SpawnCountdown(manager, this);
        countdown.start(spawnTime);
    }

    @Override
    public void stop() {
        // 1. Determine Winner
        List<BingoTeam> winners = new ArrayList<>();
        for (BingoTeam b : manager.getBingoTeams()) {
            if (b.isWinner())
                winners.add(b);
        }
        BingoTeam finalWinner = null;
        if (winners.size() == 1) {
            finalWinner = winners.get(0);
            for (Player p : manager.getPlayers()) {
                p.sendMessage(MessageBuilder.bingo(Component.text("Team "+finalWinner.getName(), TextColor.color(finalWinner.getColorCode())).append(Component.text(" got a ", Color.STD_COLOR.getTextColor()).append(Message.BINGO.getComponent()))));            }
        } else {
            int maxCollectedBingoItems = 0;
            for (BingoTeam b : winners) {
                if (b.getCollectedBingoItems() > maxCollectedBingoItems) {
                    finalWinner = b;
                    maxCollectedBingoItems = b.getCollectedBingoItems();

                }
            }
            for (Player p : manager.getPlayers()) {
                p.sendMessage(MessageBuilder.bingo(Component.text("Multiple Teams got a ", Color.STD_COLOR.getTextColor()).append(Message.BINGO.getComponent()).append(MessageBuilder.buildMsg(List.of(" but ", "Team "+finalWinner.getName(), " has ", Integer.toString(maxCollectedBingoItems), " Bingo Advancements"), List.of(Color.STD_COLOR.getColorCode(), finalWinner.getColorCode(), Color.STD_COLOR.getColorCode(), NamedTextColor.YELLOW.value(), Color.STD_COLOR.getColorCode())))));
            }
        }

        // 2. Get Winner Advancements and send them with entire new field without toasts
        List<String> winField = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (finalWinner.getBingoField()[i][j]) {
                    winField.add(getMaterialFromPosition(new int[]{i,j}).toString().toLowerCase());
                }
            }
        }

        finalWinner.getAdvancement().sendFinalField(manager.getPlayers(), getOldAdvancements(), winField, fieldItems, fieldPositions);


        // 3. Teleport
        FinishedState finishedState = new FinishedState(manager, finalWinner);
        for (Player p : manager.getPlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(finishedState.getWaitingWorld().getWorld().getSpawnLocation());
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1F, 1F);
        }

        // Delete World and unregister Listeners
        HandlerList.unregisterAll(bingoCollectListener);
        HandlerList.unregisterAll(backpackListener);
        HandlerList.unregisterAll(spawnListener);
        HandlerList.unregisterAll(abilitiesListener);

        // Start Finished State
        manager.setGameState(finishedState);
        manager.getGameState().start();
    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(manager.getWorld().getSpawnLocation());
        p.setGameMode(GameMode.SPECTATOR);
        p.sendMessage(MessageBuilder.bingo("You are now spectating"));
    }

    public int getSize() {
        return size;
    }

    public Map<Material, BingoItem> getBingoItemsMap() {
        return bingoItemsMap;
    }

    public BackpackListener getBackpackListener() {
        return backpackListener;
    }

    public SpawnListener getSpawnListener() {
        return spawnListener;
    }

    public AbilitiesListener getAbilitiesListener() {
        return abilitiesListener;
    }

    public BingoCollectListener getBingoCollectListener() {
        return bingoCollectListener;
    }

    public AbilitiesMenuListener getAbilitiesMenuListener() {
        return abilitiesMenuListener;
    }

    public Abilities getAbilities() {
        return abilities;
    }

    public BingoLists.ListType getSetting() {
        return setting;
    }

    public int getExtraAbilityPoints() {
        return extraAbilityPoints;
    }
}
