package me.mats.bingo.game.finished;

import me.mats.bingo.game.BingoManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

public class FinishedCountdown {
    private final BingoManager manager;
    private final FinishedState state;

    // Keeps track of actual Countdown
    private float countdown;

    private int taskId;


    public FinishedCountdown(BingoManager manager, FinishedState state) {
        this.manager = manager;
        this.state = state;

    }

    public void start(int countdownTime) {
        countdown = countdownTime;

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(manager.getPlugin(), () ->  {
            if (countdown == countdownTime) {
                for (Player p : manager.getPlayers()) {
                    p.clearActivePotionEffects();
                    p.getInventory().clear();
                }

            } else if (countdown > 0) {
                for (Player p : manager.getPlayers()) {
                    p.setExp(countdown / countdownTime);
                }

            } else if (countdown < 0.05F) {
                state.stop();
                Bukkit.getScheduler().cancelTask(taskId);
            }
            countdown = countdown - 0.05F;
        },0,1);

    }




}
