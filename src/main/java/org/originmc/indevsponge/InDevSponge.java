package org.originmc.indevsponge;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Material.*;

public final class InDevSponge extends JavaPlugin implements Listener {

    private static final int RADIUS = 3;

    private static final List<Material> LIQUIDS = ImmutableList.of(WATER, STATIONARY_WATER, LAVA, STATIONARY_LAVA);

    private Map<UUID, Long> invulnerable = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin has been disabled!");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void clearLiquids(BlockPlaceEvent event) {
        // Do nothing if placed block is not sponge
        Block block = event.getBlock();
        if (block.getType() != Material.SPONGE) return;

        // Iterate through radius around the block
        Location loc = block.getLocation();
        for (int x = -RADIUS; x < RADIUS; ++x) {
            for (int y = -RADIUS; y < RADIUS; ++y) {
                for (int z = -RADIUS; z < RADIUS; ++z) {
                    // Do nothing if y distance is further than the radius
                    Block b = loc.getWorld().getBlockAt(loc.clone().add(x, y, z));
                    double distanceY = loc.getY() > b.getY() ? loc.getY() - b.getY() : b.getY() - loc.getY();
                    if (distanceY > RADIUS) continue;

                    // Do nothing if block is not a liquid
                    if (!LIQUIDS.contains(b.getType())) continue;

                    // Remove the liquid
                    b.setType(Material.AIR);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void blockLiquids(BlockFromToEvent event) {
        // Do nothing if event happened within the same location
        Block f = event.getBlock();
        Block t = event.getToBlock();
        if (t.getLocation().equals(f.getLocation())) return;

        // Do nothing if this is having no affect on liquids
        if (!LIQUIDS.contains(f.getType())) return;

        // Do nothing if block to change is already liquid
        if (LIQUIDS.contains(t.getType())) return;

        // Iterate through all blocks surrounding the liquid
        Location loc = t.getLocation();
        Location loc1 = loc.clone().add(RADIUS, RADIUS, RADIUS);
        Location loc2 = loc.clone().subtract(RADIUS, RADIUS, RADIUS);

        int topBlockX = loc1.getBlockX() < loc2.getBlockX() ? loc2.getBlockX() : loc1.getBlockX();
        int bottomBlockX = loc1.getBlockX() > loc2.getBlockX() ? loc2.getBlockX() : loc1.getBlockX();

        int topBlockY = loc1.getBlockY() < loc2.getBlockY() ? loc2.getBlockY() : loc1.getBlockY();
        int bottomBlockY = loc1.getBlockY() > loc2.getBlockY() ? loc2.getBlockY() : loc1.getBlockY();

        if (bottomBlockY < 0) bottomBlockY = 0;
        if (topBlockY > 256) topBlockY = 256;

        int topBlockZ = loc1.getBlockZ() < loc2.getBlockZ() ? loc2.getBlockZ() : loc1.getBlockZ();
        int bottomBlockZ = loc1.getBlockZ() > loc2.getBlockZ() ? loc2.getBlockZ() : loc1.getBlockZ();

        for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int y = bottomBlockY; y <= topBlockY; y++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                    // Cancel event if sponge is found
                    if (loc.getWorld().getBlockAt(x, y, z).getType().equals(SPONGE)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void breakSponge(BlockDamageEvent event) {
        // Do nothing if block is not sponge
        if (event.getBlock().getType() != SPONGE) return;

        // Instantly break the sponge
        event.setInstaBreak(true);

        // Grant player a small invulnerability to fall damage
        invulnerable.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void protectPlayer(EntityDamageEvent event) {
        // Do nothing if cause is null
        if (event.getCause() == null) return;

        // Do nothing if damage cause is not via falling
        if (!event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) return;

        // Do nothing if damaged entity is not a player
        if (!(event.getEntity() instanceof Player)) return;

        // Do nothing if player is not on the invulnerability list
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        if (!invulnerable.containsKey(uuid)) return;

        // Do nothing if the invulnerability was granted too long ago
        if (System.currentTimeMillis() - invulnerable.get(uuid) > 100) return;

        // Cancel the event
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void removeInvulnerability(PlayerQuitEvent event) {
        // Remove player from invulnerable list
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        invulnerable.remove(uuid);
    }

}
