package me.mats.bingo;


import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.mats.bingo.game.BingoManager;
import me.mats.bingo.message.Message;
import me.mats.bingo.message.MessageBuilder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static me.mats.bingo.enums.Color.*;


public class GeneralListener implements Listener {

    private static final Scoreboard standardBoard = Bukkit.getScoreboardManager().getNewScoreboard();
    private static final Team standardTeam = standardBoard.registerNewTeam("Standard");
    private static final Team adminTeam = standardBoard.registerNewTeam("Owner");
    private static final List<UUID> admins = List.of(UUID.fromString("ff7e1b97-3ffe-420e-9027-c1464eeab17b"));

    public GeneralListener() {
        standardTeam.color(NamedTextColor.GRAY);
        standardTeam.prefix(Message.O_BRACKET.getComponent().append(Component.text("Player", PLAYER.getTextColor())).append(Message.C_BRACKET.getComponent()));

        adminTeam.color(NamedTextColor.GRAY);
        adminTeam.prefix(Message.O_BRACKET.getComponent().append(Component.text("Admin", ADMIN.getTextColor())).append(Message.C_BRACKET.getComponent()));
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.setScoreboard(standardBoard);
        p.sendPlayerListHeaderAndFooter(Message.O_BRACKET.getComponent().append(Message.PLAYERS.getComponent()).append(Message.C_BRACKET.getComponent()).appendNewline(), Component.newline().append(Component.text("Playing ", NamedTextColor.GRAY)).append(Component.text("LOBBY1", NamedTextColor.YELLOW)));

        if (admins.contains(p.getUniqueId())) {
            p.displayName(Component.text(p.getName(), ADMIN.getTextColor()));
            adminTeam.addPlayer(p);
        } else {
            p.displayName(Component.text(p.getName(), PLAYER.getTextColor()));
            standardTeam.addPlayer(p);
        }

        // Hide Bingo Players
        for (Player p2 : Bukkit.getOnlinePlayers()) {
            if (BingoManager.inBingo(p2)) {
                p.hidePlayer(Bingo.getInstance(), p2);
                p2.hidePlayer(Bingo.getInstance(), p);
            }
        }

        e.joinMessage(MessageBuilder.buildMsg(List.of("[", "+", "] "), List.of(NamedTextColor.GREEN.value(), NamedTextColor.DARK_GREEN.value(), NamedTextColor.GREEN.value())).append(Component.text(p.getName(), NamedTextColor.GRAY)));

    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(MessageBuilder.buildMsg(List.of("[", "-", "] "), List.of(NamedTextColor.RED.value(), NamedTextColor.DARK_RED.value(), NamedTextColor.RED.value())).append(Component.text(p.getName(), NamedTextColor.GRAY)));
    }


    // Only send to Bingo Members
    @EventHandler(priority = EventPriority.HIGH)
    public void onChatInBingo(AsyncChatEvent e) {
        if (BingoManager.inBingo(e.getPlayer())) {
            BingoManager bm = BingoManager.getBingo(e.getPlayer());
            Set<Audience> viewers =  e.viewers();
            viewers.clear();
            viewers.addAll(bm.getPlayers());
        } // Could add here that Lobby messages also only reach Lobby players (Bad Performance)
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {

        e.renderer(new ChatRenderer() {
            @Override
            public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
                return Component.text("[", NamedTextColor.DARK_GRAY).append(sourceDisplayName).append(Component.text("]", NamedTextColor.DARK_GRAY)).append(Component.text("> ").decorate(TextDecoration.BOLD)).append(message.color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false));
            }
        });
    }

    // TODO: Non global Death Messages
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        TextReplacementConfig config = TextReplacementConfig.builder().matchLiteral(p.getName()).replacement(Component.text("† ", TextColor.color(0x3f3d3d)).append(e.getPlayer().displayName())).build();
        e.deathMessage(e.deathMessage().replaceText(config).color(NamedTextColor.GRAY));
    }

    public static void setDefaults(Player p) {
        p.sendPlayerListHeaderAndFooter(Message.O_BRACKET.getComponent().append(Message.PLAYERS.getComponent()).append(Message.C_BRACKET.getComponent()).appendNewline(), Component.newline().append(Component.text("Playing ", NamedTextColor.GRAY)).append(Component.text("LOBBY1", NamedTextColor.YELLOW)));
        p.setScoreboard(standardBoard);
        if (admins.contains(p.getUniqueId())) {
            p.playerListName(adminTeam.prefix().append(Component.text(p.getName(), adminTeam.color())));
            p.displayName(Component.text(p.getName(), ADMIN.getTextColor()));
        } else {
            p.playerListName(standardTeam.prefix().append(Component.text(p.getName(), standardTeam.color())));
            p.displayName(Component.text(p.getName(), PLAYER.getTextColor()));
        }
    }

    public static Scoreboard getStandardBoard() {
        return standardBoard;
    }

    public static Team getStandardTeam() {
        return standardTeam;
    }

    public static Team getAdminTeam() {
        return adminTeam;
    }


}
