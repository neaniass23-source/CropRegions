package me.cropregions;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {
    private final CropRegionsPlugin plugin;
    private final File file;
    private final Map<String, Region> regions = new LinkedHashMap<>();
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public RegionManager(CropRegionsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
    }

    public void load() {
        regions.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("regions");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection r = sec.getConfigurationSection(key);
            if (r == null) continue;
            regions.put(key.toLowerCase(), new Region(key, r.getString("world"),
                    r.getInt("x1"), r.getInt("y1"), r.getInt("z1"),
                    r.getInt("x2"), r.getInt("y2"), r.getInt("z2")));
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Region r : regions.values()) {
            String p = "regions." + r.getName() + ".";
            yml.set(p + "world", r.getWorld());
            yml.set(p + "x1", r.getMinX());
            yml.set(p + "y1", r.getMinY());
            yml.set(p + "z1", r.getMinZ());
            yml.set(p + "x2", r.getMaxX());
            yml.set(p + "y2", r.getMaxY());
            yml.set(p + "z2", r.getMaxZ());
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar regions.yml: " + e.getMessage());
        }
    }

    public Region at(Location loc) {
        for (Region r : regions.values()) {
            if (r.contains(loc)) return r;
        }
        return null;
    }

    public Collection<Region> all() { return regions.values(); }
    public boolean exists(String name) { return regions.containsKey(name.toLowerCase()); }
    public void add(Region r) { regions.put(r.getName().toLowerCase(), r); save(); }
    public boolean remove(String name) {
        boolean ok = regions.remove(name.toLowerCase()) != null;
        if (ok) save();
        return ok;
    }

    public Map<UUID, Location> pos1() { return pos1; }
    public Map<UUID, Location> pos2() { return pos2; }
}
