package me.cropregions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CropRegionsPlugin extends JavaPlugin {

    public static NamespacedKey WAND_KEY;

    private RegionManager regions;
    private DropService drops;
    private BukkitTask autoTask;

    private final Set<Material> replantCrops = EnumSet.noneOf(Material.class);
    private final Set<Material> singleCrops = EnumSet.noneOf(Material.class);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        WAND_KEY = new NamespacedKey(this, "wand");

        regions = new RegionManager(this);
        regions.load();
        drops = new DropService(this);

        reloadAll();

        getServer().getPluginManager().registerEvents(new CropListener(this), this);

        PluginCommand cmd = getCommand("cropregion");
        if (cmd != null) {
            RegionCommand rc = new RegionCommand(this);
            cmd.setExecutor(rc);
            cmd.setTabCompleter(rc);
        }
    }

    @Override
    public void onDisable() {
        if (autoTask != null) autoTask.cancel();
        if (regions != null) regions.save();
    }

    public void reloadAll() {
        reloadConfig();
        loadMaterials("replant-crops", replantCrops);
        loadMaterials("single-crops", singleCrops);
        drops.reload();

        if (autoTask != null) {
            autoTask.cancel();
            autoTask = null;
        }
        if (getConfig().getBoolean("auto-harvest.enabled", false)) {
            long interval = Math.max(1, getConfig().getLong("auto-harvest.scan-interval-ticks", 20));
            autoTask = new AutoHarvestTask(this).runTaskTimer(this, interval, interval);
        }
    }

    private void loadMaterials(String path, Set<Material> target) {
        target.clear();
        List<String> list = getConfig().getStringList(path);
        for (String s : list) {
            Material m = Material.matchMaterial(s);
            if (m == null) getLogger().warning("Material invalido en " + path + ": " + s);
            else target.add(m);
        }
    }

    // ---------- helpers ----------
    public boolean isReplantCrop(Material m) { return replantCrops.contains(m); }
    public boolean isSingleCrop(Material m) { return singleCrops.contains(m); }
    public boolean isCrop(Material m) { return isReplantCrop(m) || isSingleCrop(m); }

    public boolean isMature(Block b) {
        if (isSingleCrop(b.getType())) return true;
        if (b.getBlockData() instanceof Ageable a) return a.getAge() >= a.getMaximumAge();
        return false;
    }

    public Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    public void msg(Player p, String key) {
        String text = getConfig().getString("messages." + key, "");
        if (!text.isEmpty()) p.sendActionBar(color(text));
    }

    public RegionManager regions() { return regions; }
    public DropService drops() { return drops; }
}
