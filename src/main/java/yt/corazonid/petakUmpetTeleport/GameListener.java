package yt.corazonid.petakUmpetTeleport;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GameListener implements Listener {
    private final PetakUmpetTeleport plugin;
    private final Set<UUID> eliminatedPlayers = new HashSet<>();
    private final Set<UUID> ghostPlayers = new HashSet<>();

    public GameListener(PetakUmpetTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameRunning()) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (event.getDrops() != null) {
            event.getDrops().removeIf(drop -> drop.getType() == Material.NETHERITE_SWORD); // Pastikan item hunter/ghost tidak jatuh ke tanah
        }

        // Hider mati
        if (gm.getParticipants().contains(victim) && !victim.equals(gm.getHunter())) {
            if (!eliminatedPlayers.contains(victim.getUniqueId())) {
                eliminatedPlayers.add(victim.getUniqueId());
                int penalty = gm.getNextDeathPenalty();
                gm.addScore(victim.getUniqueId(), penalty);
                Bukkit.broadcastMessage("§c" + victim.getName() + " tereliminasi! Skor: §l" + penalty);

                giveGhostGear(victim);
                ghostPlayers.add(victim.getUniqueId());
            }
        }

        // Scoring untuk killer
        if (killer != null && gm.getParticipants().contains(killer)) {
            // Hanya hunter dan ghost yang bisa mendapat poin dari kill
            if (killer.equals(gm.getHunter()) || ghostPlayers.contains(killer.getUniqueId())) {
                gm.addScore(killer.getUniqueId(), 1);
                if (ghostPlayers.contains(killer.getUniqueId())) {
                    killer.sendMessage("§7[GHOST] §a+1 Poin Kill!");
                } else {
                    killer.sendMessage("§a+1 Poin Kill!");
                }
            }
        }

        checkRoundEnd();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameRunning()) return;

        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // RULE 1: Ghost tidak bisa saling membunuh
        if (ghostPlayers.contains(attacker.getUniqueId()) && ghostPlayers.contains(victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage("§cGhost tidak bisa saling membunuh!");
            return;
        }

        // RULE 2: Ghost tidak bisa bunuh hunter
        if (ghostPlayers.contains(attacker.getUniqueId()) && victim.equals(gm.getHunter())) {
            event.setCancelled(true);
            attacker.sendMessage("§cGhost tidak bisa membunuh Hunter!");
            return;
        }

        // RULE 3: Hider alive tidak bisa bunuh ghost
        if (!ghostPlayers.contains(attacker.getUniqueId()) && !attacker.equals(gm.getHunter())
            && ghostPlayers.contains(victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage("§cHider tidak bisa membunuh Ghost!");
            return;
        }

        // RULE 4: Hider alive tidak bisa bunuh hunter
        if (!ghostPlayers.contains(attacker.getUniqueId()) && !attacker.equals(gm.getHunter())
            && victim.equals(gm.getHunter())) {
            event.setCancelled(true);
            attacker.sendMessage("§cHider tidak bisa menyakiti Hunter!");
            return;
        }

        // RULE 5: Hunter tidak bisa bunuh ghost
        if (attacker.equals(gm.getHunter()) && ghostPlayers.contains(victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage("§cGhost sudah aman dari Hunter!");
            return;
        }
    }

    private void checkRoundEnd() {
        GameManager gm = plugin.getGameManager();
        int totalHiders = gm.getParticipants().size() - 1;
        int currentDead = eliminatedPlayers.size();

        if (currentDead >= totalHiders) {
            gm.setGameRunning(false);
            Bukkit.broadcastMessage("§6§lRONDE SELESAI! §fSemua hider tertangkap.");
            org.bukkit.scoreboard.Team nameTeam = plugin.getNoNameTagTeam();
            // Hapus Strength effect dari semua pemain saat ronde selesai sebelum set dibersihkan
            for (Player p : gm.getParticipants()) {
                if (p != null && p.isOnline()) {
                    clearPlayerEffectsAndGear(p, nameTeam);
                    }
                }

            for (UUID ghostId : ghostPlayers) {
                Player ghostPlayer = Bukkit.getPlayer(ghostId);
                if (ghostPlayer != null && ghostPlayer.isOnline()) {
                    if (ghostPlayer.isDead()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (ghostPlayer.isOnline()) {
                                clearPlayerEffectsAndGear(ghostPlayer, nameTeam);
                            }
                        }, 10L);
                    } else {
                        clearPlayerEffectsAndGear(ghostPlayer, nameTeam);
                    }
                }
            }
            eliminatedPlayers.clear();
            ghostPlayers.clear();
        }
    }

    private void clearPlayerEffectsAndGear(Player p, org.bukkit.scoreboard.Team nameTeam) {
        p.removePotionEffect(PotionEffectType.STRENGTH);
        p.removePotionEffect(PotionEffectType.SATURATION);
        p.removePotionEffect(PotionEffectType.GLOWING);
        
        p.getInventory().remove(Material.NETHERITE_SWORD);

        if (nameTeam != null && nameTeam.hasEntry(p.getName())) {
            nameTeam.removeEntry(p.getName());
        }
    }

    public void giveHunterGear(Player p) {
        if (p == null || !p.isOnline()) {
            Bukkit.getLogger().warning("Cannot give hunter gear: Player " + (p != null ? p.getName() : "null") + " is not online!");
            return;
        }
        p.getInventory().clear();
        p.getInventory().addItem(new ItemStack(Material.NETHERITE_SWORD));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 99999, 255, true, false)); // STRENGTH II
        p.sendMessage("§e⚔ Kamu diberikan Netherite Sword & Kekuatan Maksimal!");
        Bukkit.broadcastMessage("§c§l" + p.getName() + " §csiap berburu!");
    }

    public void giveGhostGear(Player p) {
        if (p == null || !p.isOnline()) {
            Bukkit.getLogger().warning("Cannot give ghost gear: Player " + (p != null ? p.getName() : "null") + " is not online!");
            return;
        }
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }
        if (p.isDead()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> giveGhostGear(p), 5L);
            return;
        }
        p.getInventory().clear();
        p.getInventory().addItem(new ItemStack(Material.NETHERITE_SWORD));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 99999, 255, true, false)); // STRENGTH II (Amplifier 255 sering dibaca tingkat maksimal/tak terbatas di beberapa versi Spigot)
        p.sendMessage("§7[GHOST] §f⚔ Kamu menjadi ghost! Dapatkan §c+1 Poin §funtuk setiap kill!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop() != null && event.getItemDrop().getItemStack().getType() == Material.NETHERITE_SWORD) {
            event.setCancelled(true);
        }
    }

    public void onInventoryClick(InventoryClickEvent event) {
        // Blokir jika membuang dengan melepas item ke luar frame inventori
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
            if (event.getCursor().getType() == Material.NETHERITE_SWORD) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT) {
            if (event.getCurrentItem().getType() == Material.NETHERITE_SWORD) {
                event.setCancelled(true);
            }
        }

    }

    public void onPlayerSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        if ((event.getMainHandItem().getType() == Material.NETHERITE_SWORD) ||
            (event.getOffHandItem().getType() == Material.NETHERITE_SWORD)) {
            event.setCancelled(true);
        }
    }

    public Set<UUID> getGhostPlayers() {
        return ghostPlayers;
    }

    /**
     * Reset eliminated dan ghost players untuk ronde baru
     */
    public void resetForNewRound() {
        eliminatedPlayers.clear();
        ghostPlayers.clear();
    }
}