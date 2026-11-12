package me.cropregions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DropService {

    private record CustomDrop(Material material, int min, int max, double chance,
                              String name, List<String> lore, Integer modelData,
                              List<String> commands) {}

    private final CropRegionsPlugin plugin;
    private final Map<Material, List<CustomDrop>> custom = new EnumMap<>(Material.class);

    public DropService(CropRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        custom.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("custom-drops");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            Material crop = Material.matchMaterial(key);
            if (crop == null) {
                plugin.getLogger().warning("custom-drops: material invalido " + key);
                continue;
            }
            List<CustomDrop> list = new ArrayList<>();
            for (Map<?, ?> m : sec.getMapList(key)) {
                Material mat = Material.matchMaterial(String.valueOf(m.get("material")));
                if (mat == null) {
                    plugin.getLogger().warning("custom-drops." + key + ": material invalido " + m.get("material"));
                    continue;
                }
                int min = num(m.get("min"), 1).intValue();
                int max = Math.max(min, num(m.get("max"), min).intValue());
                double chance = num(m.get("chance"), 100).doubleValue();
                String name = m.get("name") == null ? null : String.valueOf(m.get("name"));
                List<String> lore = strList(m.get("lore"));
                Integer cmd = m.get("custom-model-data") == null ? null : num(m.get("custom-model-data"), 0).intValue();
                List<String> commands = strList(m.get("commands"));
                list.add(new CustomDrop(mat, min, max, chance, name, lore, cmd, commands));
            }
            custom.put(crop, list);
        }
    }

    private static Number num(Object o, Number def) {
        return o instanceof Number n ? n : def;
    }

    private static List<String> strList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) out.add(String.valueOf(x));
        return out;
    }

    /**
     * Cosecha un cultivo: drops vanilla + custom, y replanta (o elimina si es single-crop).
     * @param player puede ser null (modo minion)
     * @param tool   puede ser null (modo minion)
     */
    public void harvest(Block block, ItemStack tool, Player player) {
        Material type = block.getType();
        World world = block.getWorld();
        Location center = block.getLocation().add(0.5, 0.3, 0.5);

        // Drops vanilla (respeta Fortuna de la herramienta)
        Collection<ItemStack> vanilla;
        if (tool != null && player != null) vanilla = block.getDrops(tool, player);
        else vanilla = block.getDrops();
        for (ItemStack item : vanilla) world.dropItemNaturally(center, item);

        // Drops custom
        List<CustomDrop> extras = custom.get(type);
        if (extras != null) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (CustomDrop cd : extras) {
                if (rnd.nextDouble() * 100.0 >= cd.chance()) continue;
                int amount = cd.min() == cd.max() ? cd.min() : rnd.nextInt(cd.min(), cd.max() + 1);

                if (cd.material() != Material.AIR && amount > 0) {
                    ItemStack stack = new ItemStack(cd.material(), amount);
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        if (cd.name() != null) meta.displayName(plugin.color(cd.name()));
                        if (!cd.lore().isEmpty()) {
                            meta.lore(cd.lore().stream().map(plugin::color).toList());
                        }
                        if (cd.modelData() != null) meta.setCustomModelData(cd.modelData());
                        stack.setItemMeta(meta);
                    }
                    world.dropItemNaturally(center, stack);
                }

                if (player != null) {
                    for (String c : cd.commands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                c.replace("%player%", player.getName()).replace("%amount%", String.valueOf(amount)));
                    }
                }
            }
        }

        // Replantar / eliminar
        if (plugin.isReplantCrop(type)) {
            BlockData data = block.getBlockData();
            if (data instanceof Ageable ageable) {
                ageable.setAge(0);
                block.setBlockData(ageable, false);
            }
        } else {
            block.setType(Material.AIR, false);
        }

        // Efectos
        if (plugin.getConfig().getBoolean("effects.particles", true)) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, center, 6, 0.25, 0.25, 0.25, 0);
        }
        if (plugin.getConfig().getBoolean("effects.sound", true)) {
            world.playSound(center, Sound.BLOCK_CROP_BREAK, 0.6f, 1.0f);
        }
    }
}
