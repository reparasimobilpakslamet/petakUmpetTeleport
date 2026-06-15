package yt.corazonid.petakUmpetTeleport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TeleportManager {
    private final PetakUmpetTeleport plugin;

    public TeleportManager(PetakUmpetTeleport plugin) {
        this.plugin = plugin;
    }

    public void executeTeleport(int type, List<Player> allPlayers, Player hunter, Set<UUID> ghostPlayers) {
        switch (type) {
            case 0 -> swapAllPlayers(allPlayers, ghostPlayers);
            case 1 -> swapHidersRandom(allPlayers, hunter, ghostPlayers);
            case 2 -> swapMix(allPlayers, ghostPlayers);
            case 3 -> fakeSwap(allPlayers);
            case 4 -> fixedSwapHiders(allPlayers, hunter, ghostPlayers);
        }
    }

    // TYPE 1: Swap SEMUA Player ke Posisi Masing2 (Hunter + Ghost + Hider ALL)
    private void swapAllPlayers(List<Player> players, Set<UUID> ghostPlayers) {
        List<Player> allCanTeleport = new ArrayList<>(players);

        if (allCanTeleport.size() < 2) {
            Bukkit.broadcastMessage("§9[TELEPORT] §cTidak cukup player! Teleport dibatalkan.");
            return;
        }

        Collections.shuffle(allCanTeleport);

        List<Location> locations = new ArrayList<>();
        for (Player p : allCanTeleport) {
            locations.add(p.getLocation().clone());
        }

        Collections.rotate(locations, 1);

        for (int i = 0; i < allCanTeleport.size(); i++) {
            Player currentPlayer = allCanTeleport.get(i);
            Location targetLocation = locations.get(i);

            currentPlayer.teleport(targetLocation);
            currentPlayer.playSound(currentPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }

        Bukkit.broadcastMessage("§9[TELEPORT] §f🌪️ SEMUA PEMAIN BERTUKAR POSISI!");
    }

    // TYPE 2: Swap Hiders Alive Only (Each Other - Ghost & Hunter TIDAK ikut)
    private void swapHidersRandom(List<Player> allPlayers, Player hunter, Set<UUID> ghostPlayers) {
        List<Player> liveHiders = new ArrayList<>(allPlayers.stream()
                .filter(p -> !p.equals(hunter))
                .filter(p -> !ghostPlayers.contains(p.getUniqueId()))
                .toList());
        if (liveHiders.size() < 2) {
            Bukkit.broadcastMessage("§b[TELEPORT] §cTidak cukup hider alive! Teleport dibatalkan.");
            return;
        }

        Collections.shuffle(liveHiders);

        List<Location> locations = new ArrayList<>();
        for (Player p : liveHiders) {
            locations.add(p.getLocation().clone());
        }

        Collections.rotate(locations, 1);

        for (int i = 0; i < liveHiders.size(); i++) {
            Player currentHider = liveHiders.get(i);
            Location targetLocation = locations.get(i);

            currentHider.teleport(targetLocation);
            currentHider.playSound(currentHider.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }

        Bukkit.broadcastMessage("§b[TELEPORT] §fHiders bertukar posisi satu sama lain! Ghost & Hunter tetap ditempat.");
    }

    // TYPE 3: Swap Mix - SEMUA (Dynamic - 2/3 swap, 1/3 stay)
    private void swapMix(List<Player> allPlayers, Set<UUID> ghostPlayers) {
        List<Player> allCanTeleport = new ArrayList<>(allPlayers);

        if (allCanTeleport.size() < 2) {
            Bukkit.broadcastMessage("§c[TELEPORT] §cTidak cukup player! Teleport dibatalkan.");
            return;
        }

        int swapCount = Math.max(2, (allCanTeleport.size() * 2 / 3));
        swapCount = Math.min(swapCount, allCanTeleport.size() - 1);

        Collections.shuffle(allCanTeleport);
        List<Player> toSwap = new ArrayList<>(allCanTeleport.subList(0, swapCount));

        // Buat list lokasi target kloningan dari toSwap sebelum di-rotate/shuffle
        List<Location> targetLocations = new ArrayList<>();
        for (Player p : toSwap) {
            targetLocations.add(p.getLocation().clone());
        }

        // Amankan rotasi posisi lokasi agar adil
        Collections.rotate(targetLocations, 1);

        for (int i = 0; i < toSwap.size(); i++) {
            Player player = toSwap.get(i);
            player.teleport(targetLocations.get(i));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }

        int stayCount = allCanTeleport.size() - swapCount;
        StringBuilder sb = new StringBuilder("§c[TELEPORT] §f⚡ Swap " + swapCount + " player, " + stayCount + " stay: ");
        toSwap.forEach(p -> sb.append(p.getName()).append(", "));
        // Memotong koma terakhir agar rapi
        if (!toSwap.isEmpty()) sb.setLength(sb.length() - 2); 
        
        Bukkit.broadcastMessage(sb.toString());
    }

    // TYPE 4: Fake Swap (No teleport, just effects)
    private void fakeSwap(List<Player> allPlayers) {
        for (Player p : allPlayers) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0, false, false));
        }

        Bukkit.broadcastMessage("§e[TELEPORT] §fTelah dimulai...");

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("§9[TELEPORT] §c❌ PRENK!");
                Bukkit.broadcastMessage("§9[TELEPORT] §fTidak ada teleport kali ini!");
            }
        }.runTaskLater(plugin, 40L);
    }

    // TYPE 5: Fixed Swap HIDERS ALIVE Pattern (Ghost TIDAK ikut)
    private void fixedSwapHiders(List<Player> allPlayers, Player hunter, Set<UUID> ghostPlayers) {
        List<Player> liveHiders = new ArrayList<>(allPlayers.stream()
                .filter(p -> !p.equals(hunter))
                .filter(p -> !ghostPlayers.contains(p.getUniqueId()))
                .toList());

        if (liveHiders.size() < 2) {
            Bukkit.broadcastMessage("§2[TELEPORT] §cTidak cukup hider alive! Teleport dibatalkan.");
            return;
        }

        // FIX BUG: Ambil semua lokasi awal hider SEBELUM ada yang berteleportasi
        List<Location> fixedLocations = new ArrayList<>();
        for (Player p : liveHiders) {
            fixedLocations.add(p.getLocation().clone());
        }

        // Rotasikan list lokasi (menggantikan logika loop manual yang rentan bug desync)
        Collections.rotate(fixedLocations, 1);

        // Eksekusi teleportasi terstruktur
        for (int i = 0; i < liveHiders.size(); i++) {
            Player currentHider = liveHiders.get(i);
            Location targetLoc = fixedLocations.get(i);
            
            currentHider.teleport(targetLoc);
            currentHider.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }

        Bukkit.broadcastMessage("§2[TELEPORT] §fHiders bertukar posisi (cycle)! Ghost & Hunter tetap ditempat.");
    }
}