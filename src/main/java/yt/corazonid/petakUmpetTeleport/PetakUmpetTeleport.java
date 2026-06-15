package yt.corazonid.petakUmpetTeleport;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class PetakUmpetTeleport extends JavaPlugin implements Listener {
    private GameManager gameManager;
    private TeleportManager teleportManager;
    private GameListener gameListener;

    private Team noNameTagTeam;

    @Override
    public void onEnable() {
        this.gameManager = new GameManager();
        this.teleportManager = new TeleportManager(this);
        this.gameListener = new GameListener(this);

        setupScoreboardTeam();
        // register Commands
        getCommand("reg").setExecutor(new AdminCommands(this));
        getCommand("unreg").setExecutor(new AdminCommands(this));
        getCommand("listplayer").setExecutor(new AdminCommands(this));
        getCommand("gacha").setExecutor(new GameCommands(this));
        getCommand("start").setExecutor(new GameCommands(this));
        getCommand("nextround").setExecutor(new GameCommands(this));
        if (getCommand("resetgame") != null) getCommand("resetgame").setExecutor(new AdminCommands(this));
        if (getCommand("endgame") != null) getCommand("endgame").setExecutor(new AdminCommands(this));
        if (getCommand("listscore") != null) getCommand("listscore").setExecutor(new AdminCommands(this));
        if (getCommand("tpinfo") != null) getCommand("tpinfo").setExecutor(new AdminCommands(this));

        this.gameListener = new GameListener(this);
        getServer().getPluginManager().registerEvents(gameListener, this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("PetakUmpet Teleport Enabled!");
    }

    private void setupScoreboardTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team oldTeam = scoreboard.getTeam("Hide Nametag");
        if (oldTeam != null) {
            oldTeam.unregister();
        }
        noNameTagTeam = scoreboard.registerNewTeam("Hide Nametag");
        noNameTagTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    public GameManager getGameManager() { return gameManager; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public GameListener getGameListener() { return gameListener; }
    public org.bukkit.scoreboard.Team getNoNameTagTeam() { 
        return this.noNameTagTeam; 
    }

    @Override
    public void onDisable() {
        // fix bug permanent potion effect
        if (gameManager != null && gameManager.getParticipants() != null) {
            for (Player p : gameManager.getParticipants()) {
                if (p != null && p.isOnline()) {
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.SATURATION);
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH);
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
                }
            }
            gameManager.setGameRunning(false);
        }
        if (noNameTagTeam != null) {
            noNameTagTeam.unregister();
        }
    }
}
