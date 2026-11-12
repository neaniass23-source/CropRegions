package me.cropregions;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Modo "minion": recorre las regiones por tandas y cosecha/replanta los cultivos maduros.
 */
public class AutoHarvestTask extends BukkitRunnable {

    private final CropRegionsPlugin plugin;

    public AutoHarvestTask(CropRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int budget = Math.max(100, plugin.getConfig().getInt("auto-harvest.blocks-per-scan", 4000));

        for (Region r : plugin.regions().all()) {
            World world = Bukkit.getWorld(r.getWorld());
            if (world == null) continue;

            long volume = r.volume();
            if (volume <= 0) continue;

            long cursor = r.getCursor();
            long steps = Math.min(budget, volume);

            for (long i = 0; i < steps; i++) {
                long idx = (cursor + i) % volume;
                int sx = r.sizeX();
                int sz = r.sizeZ();

                int dx = (int) (idx % sx);
                int dz = (int) ((idx / sx) % sz);
                int dy = (int) (idx / ((long) sx * sz));

                int x = r.getMinX() + dx;
                int y = r.getMinY() + dy;
                int z = r.getMinZ() + dz;

                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;

                Block b = world.getBlockAt(x, y, z);
                if (!plugin.isReplantCrop(b.getType())) continue; // el minion solo replanta
                if (!plugin.isMature(b)) continue;

                plugin.drops().harvest(b, null, null);
            }
            r.setCursor((cursor + steps) % volume);
        }
    }
}
