package me.mats.bingo.game;

import me.mats.advancementinteraction.AdvancementInteraction;
import me.mats.bingo.Bingo;
import me.mats.bingo.enums.Color;
import me.mats.bingo.game.ingame.IngameState;
import me.mats.bingo.game.waiting.WaitingState;
import me.mats.bingo.message.Message;
import me.mats.bingo.game.waiting.WaitingCountdown;
import me.mats.bingo.world.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BingoManager {

    // Tracks all running Bingo games
    public static List<BingoManager> runningGames = new ArrayList<>();
    public static List<World> worlds = new ArrayList<>();

    // Static number tracker for naming
    private static int num = 0;

    // Simple getter for all games
    public static List<BingoManager> getRunningGames() {
        return runningGames;
    }

    // Check if a Player is in a Bingo game
    public static boolean inBingo(Player p) {
        for (BingoManager bm : runningGames) {
            if (bm.getPlayers().contains(p)) {
                return true;
            }
        }
        return false;
    }

    // Get the Bingo game of a player
    public static BingoManager getBingo(Player p) {
        for (BingoManager bm : runningGames) {
            if (bm.getPlayers().contains(p)) {
                return bm;
            }
        }
        return null;
    }


    // Non-static stuff

    // Unique Identifier at Runtime
    private final String name;

    // The plugin
    private final Bingo plugin;

    // All Players in this Bingo Game
    private final List<Player> players = new ArrayList<>();

    // Scoreboard
    private final Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    private final Team defaultTeam = board.registerNewTeam("default");

    // All the bingo teams
    private final List<BingoTeam> bingoTeams = new ArrayList<>();

    // The GameState Subclasses handle the specific implementation
    private GameState gameState;

    // The world where the game gets played
    private final World world;
    private World netherWorld;
    private World endWorld;

    public void configWorld(World world) {
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setPVP(false);
        world.setKeepSpawnInMemory(false);
    }

    public World getNetherWorld() {
        if (netherWorld == null) {
            WorldCreator bingoWorldCreator = new WorldCreator(name+"_nether");
            bingoWorldCreator.environment(World.Environment.NETHER);
            netherWorld = bingoWorldCreator.createWorld();
            configWorld(netherWorld);
            worlds.add(netherWorld);
        }
        return netherWorld;

    }

    public boolean isNetherNotNull() {
        return netherWorld != null;
    }

    public World getEndWorld() {
        if (endWorld == null) {
            WorldCreator bingoWorldCreator = new WorldCreator(name+"_the_end");
            bingoWorldCreator.environment(World.Environment.THE_END);
            endWorld = bingoWorldCreator.createWorld();
            configWorld(endWorld);
            worlds.add(endWorld);
        }
        return endWorld;
    }

    public boolean isEndNotNull() {
        return endWorld != null;
    }

    public void deleteWorlds() {
        WorldManager.deleteWorld(world);
        if (netherWorld != null) {
            WorldManager.deleteWorld(netherWorld);
        }
        if (endWorld != null) {
            WorldManager.deleteWorld(endWorld);
        }
    }

    public BingoManager(Bingo plugin) {
        defaultTeam.color(NamedTextColor.GRAY);
        this.plugin = plugin;
        num++;
        name = "Bingo"+ num;

        // World Stuff here to prevent lag for now
        WorldCreator bingoWorldCreator = new WorldCreator(name);
        world = bingoWorldCreator.createWorld();

        InputStream nbtFile = plugin.getResource("bingospawn.nbt");
        if  (nbtFile != null) {
            try {
                Structure bingoSpawn = Bukkit.getStructureManager().loadStructure(nbtFile);
                bingoSpawn.place(world, new BlockVector(-15,world.getSpawnLocation().getBlockY()+100,-15), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random());
            } catch (IOException e) {
                Bukkit.getLogger().warning("Couldn't load bingospawn.nbt: "+e.getMessage());
            }

        } else {
            Bukkit.getLogger().warning("Couldn't load bingospawn.nbt: ");
        }
        configWorld(world);
        world.setSpawnLocation(0,world.getSpawnLocation().getBlockY()+100, 0);
        worlds.add(world);

        gameState = new WaitingState(this);
        gameState.start();
        runningGames.add(this);
    }


    public void addPlayer(Player p) {
        AdvancementInteraction.getInstance().addBingoPlayer(p); // This makes sure that our AdvancementPacket ProtocolLib Handler works
        defaultTeam.addPlayer(p);
        p.displayName(Component.text(p.getName(), NamedTextColor.GRAY));
        p.setScoreboard(board);
        p.sendPlayerListFooter(Component.newline().append(Component.text("Playing ", NamedTextColor.GRAY)).append(Message.BINGO.getComponent()).append(Component.text(name.charAt(name.length()-1), TextColor.color(0xc2f4f9))));
        players.add(p);
        gameState.addPlayer(p);

        for (Player p2 : Bukkit.getOnlinePlayers()) {
            // Show Bingo Players, hide the rest
            if (players.contains(p2)) {
                p.showPlayer(plugin, p2);
                p2.showPlayer(plugin, p);
            } else {
                p.hidePlayer(plugin, p2);
                p2.hidePlayer(plugin, p);
            }
        }

    }

    public void invitePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!inBingo(p)) {
                p.sendMessage(Message.BINGO_PREFIX.getComponent().append(Component.text("[CLICK]", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/bingo join "+name)).hoverEvent(HoverEvent.showText(Component.text("Click to join this Game", Color.STD_COLOR.getTextColor())))).append(Component.text(" to join "+name, Color.STD_COLOR.getTextColor())));
            }
        }
    }

    public boolean inWaitingState() {
        return gameState instanceof WaitingState;
    }
    public boolean inIngameState() {
        return gameState instanceof IngameState;
    }

    // Get BingoTeam
    public BingoTeam getTeam(Material material) {
        for (BingoTeam bTeam : bingoTeams) {
            if (bTeam.getWaitingTeam().getItem().getType().equals(material)) {
                return bTeam;
            }
        }
        return null;
    }

    public BingoTeam getTeam(Player p) {
        for (BingoTeam bTeam : bingoTeams) {
            if (bTeam.getPlayers().contains(p)) {
                return bTeam;
            }
        }
        return null;
    }

    public void sendChat(Component comp) {
        for (Player p : players) {
            p.sendMessage(comp);
        }
    }

    public void endBingoGame() {
        Bukkit.getLogger().info("Ended "+name);
        runningGames.remove(this);
    }


    // Getters and Setters
    public List<BingoTeam> getBingoTeams() {
        return bingoTeams;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public String getName() {
        return name;
    }

    public Bingo getPlugin() {
        return plugin;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public WaitingCountdown getWaitingCountdown() {
        if (inWaitingState()) {
            return ((WaitingState) gameState).getWaitingCountdown();
        } else {
            return null;
        }
    }

    public Scoreboard getBoard() {
        return board;
    }

    public World getWorld() {
        return world;
    }

    public Team getDefaultTeam() {
        return defaultTeam;
    }

    public static List<World> getWorlds() {
        return worlds;
    }
}
