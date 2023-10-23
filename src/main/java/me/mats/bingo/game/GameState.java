package me.mats.bingo.game;

import org.bukkit.entity.Player;

public abstract class GameState {

    protected BingoManager manager;

    public abstract void start();
    public abstract void stop();

    public abstract void addPlayer(Player p);

    public BingoManager getManager() {
        return manager;
    }
}
