package yt.corazonid.petakUmpetTeleport;

import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
public class GameLoopTask extends BukkitRunnable {
    private final PetakUmpetTeleport plugin;
    private int totalSeconds = 300; // 5 Menit

    public GameLoopTask(PetakUmpetTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameRunning() || totalSeconds <= 0) {
            this.cancel();
            Bukkit.broadcastMessage("§6§lWAKTU HABIS! Game Selesai.");
            
            org.bukkit.scoreboard.Team nameTeam = plugin.getNoNameTagTeam();
            for (Player p : gm.getParticipants()) {
                if (p != null && p.isOnline()) {
                    p.removePotionEffect(PotionEffectType.STRENGTH);
                    p.removePotionEffect(PotionEffectType.SATURATION);
                    p.getInventory().remove(org.bukkit.Material.NETHERITE_SWORD);

                    if (nameTeam != null && nameTeam.hasEntry(p.getName())) {
                        nameTeam.removeEntry(p.getName());
                    }
                }
            }
            return;
        }

        // Tampilkan Timer di Action Bar setiap detik
        String timeFormatted = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        Bukkit.getOnlinePlayers().forEach(p ->
                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent("§6§lWAKTU BERMAIN: §e" + timeFormatted)));

        // Trigger teleport setiap menit (5:00, 4:00, 3:00, 2:00, 1:00)
        if (totalSeconds % 60 == 0 && totalSeconds > 0) {
            triggerTeleport();
        }

        // Glowing Effect untuk hider jika game kurang dari 45 detik sebelum berakhir
        if (totalSeconds == 45) {
            Bukkit.broadcastMessage("§9§lPERINGATAN] §fSisa waktu 45 detik, semua Hider yang tersisa akan diberi efek glowing!");
            java.util.Set<UUID> ghosts = plugin.getGameListener().getGhostPlayers();

            for (Player p : gm.getParticipants()) {
                if (p != null && p.isOnline() && !p.isDead()) {
                    if (!p.equals(gm.getHunter()) && !ghosts.contains(p.getUniqueId())) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 140, 0, true, false));
                    }
                }
            }
        }
        totalSeconds--;
    }

    private void triggerTeleport() {
        TeleportManager tm = plugin.getTeleportManager();
        GameManager gm = plugin.getGameManager();

        // Broadcast warning
        Bukkit.broadcastMessage("§9§l[TELEPORT] §fDimulai dalam 3 detik...");

        // 3-second countdown
        new BukkitRunnable() {
            int count = 3;
            @Override
            public void run() {
                if (count > 0) {
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        p.sendTitle("§c" + count, "", 0, 21, 0);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
                    });
                    count--;
                } else {
                    this.cancel();
                    // EXECUTE TELEPORT - Random type
                    int randomType = new Random().nextInt(5);
                    String typeInfo = switch (randomType) {
                        case 0 -> "Swap Semua Player";
                        case 1 -> "Random Swap Hiders Only";
                        case 2 -> "Mix Swap (4 random, 2 stay)";
                        case 3 -> "Fake Swap (No Teleport)";
                        case 4 -> "Fixed Swap Hiders Pattern";
                        default -> "Unknown";
                    };
                    tm.executeTeleport(randomType, gm.getParticipants(), gm.getHunter(), plugin.getGameListener().getGhostPlayers());
                    plugin.getLogger().info("Teleport Type #" + randomType + ": " + typeInfo);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}

