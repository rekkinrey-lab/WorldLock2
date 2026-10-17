package dev.worldlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldLock extends JavaPlugin implements Listener {

    private String worldName;
    private boolean blockBreakDisabled;
    private boolean blockPlaceDisabled;
    private boolean teleportOnJoin;
    private double x, y, z;
    private float yaw, pitch;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WorldLock enabled for world '" + worldName + "'.");
    }

    private void loadSettings() {
        reloadConfig();
        worldName = getConfig().getString("world", "world");
        blockBreakDisabled = getConfig().getBoolean("block-break-disabled", true);
        blockPlaceDisabled = getConfig().getBoolean("block-place-disabled", true);
        teleportOnJoin = getConfig().getBoolean("teleport-on-join", true);
        x = getConfig().getDouble("spawn.x", 2.5);
        y = getConfig().getDouble("spawn.y", 0.0);
        z = getConfig().getDouble("spawn.z", 0.5);
        yaw = (float) getConfig().getDouble("spawn.yaw", 0.0);
        pitch = (float) getConfig().getDouble("spawn.pitch", 0.0);
    }

    /** True only for the single configured world; every other world is left alone. */
    private boolean isLockedWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(worldName);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!blockBreakDisabled) return;
        if (isLockedWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!blockPlaceDisabled) return;
        if (isLockedWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!teleportOnJoin) return;
        Player player = event.getPlayer();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().warning("World '" + worldName + "' not found; cannot teleport " + player.getName());
            return;
        }
        Location target = new Location(world, x, y, z, yaw, pitch);
        // Teleport right away, then once more next tick so other join plugins can't override it.
        player.teleport(target);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) player.teleport(target);
        }, 1L);
    }
}
