package me.cropregions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegionCommand implements CommandExecutor, TabCompleter {

    private final CropRegionsPlugin plugin;

    public RegionCommand(CropRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    private void say(CommandSender s, String msg) {
        s.sendMessage(plugin.color(msg));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("cropregions.admin")) {
            say(sender, "&cNo tienes permiso.");
            return true;
        }
        if (args.length == 0) {
            say(sender, "&e/cr wand &7- Varita de seleccion");
            say(sender, "&e/cr create <nombre> &7- Crear region con la seleccion");
            say(sender, "&e/cr delete <nombre> &7- Borrar region");
            say(sender, "&e/cr list &7- Listar regiones");
            say(sender, "&e/cr reload &7- Recargar config");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> {
                if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                ItemStack wand = new ItemStack(Material.BLAZE_ROD);
                ItemMeta meta = wand.getItemMeta();
                meta.displayName(plugin.color("&6Varita de CropRegions"));
                meta.lore(List.of(plugin.color("&7Click izq: pos1"), plugin.color("&7Click der: pos2")));
                meta.getPersistentDataContainer().set(CropRegionsPlugin.WAND_KEY, PersistentDataType.BYTE, (byte) 1);
                wand.setItemMeta(meta);
                p.getInventory().addItem(wand);
                say(sender, "&aTe di la varita.");
            }
            case "create" -> {
                if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                if (args.length < 2) { say(sender, "&cUso: /cr create <nombre>"); return true; }
                UUID id = p.getUniqueId();
                Location a = plugin.regions().pos1().get(id);
                Location b = plugin.regions().pos2().get(id);
                if (a == null || b == null) { say(sender, "&cPrimero selecciona pos1 y pos2 con la varita."); return true; }
                if (!a.getWorld().equals(b.getWorld())) { say(sender, "&cLas posiciones estan en mundos distintos."); return true; }
                if (plugin.regions().exists(args[1])) { say(sender, "&cYa existe una region con ese nombre."); return true; }
                Region r = new Region(args[1], a.getWorld().getName(),
                        a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                        b.getBlockX(), b.getBlockY(), b.getBlockZ());
                plugin.regions().add(r);
                say(sender, "&aRegion &f" + r.getName() + " &acreada (" + r.volume() + " bloques).");
            }
            case "delete" -> {
                if (args.length < 2) { say(sender, "&cUso: /cr delete <nombre>"); return true; }
                say(sender, plugin.regions().remove(args[1]) ? "&aRegion eliminada." : "&cNo existe esa region.");
            }
            case "list" -> {
                if (plugin.regions().all().isEmpty()) { say(sender, "&7No hay regiones."); return true; }
                for (Region r : plugin.regions().all()) {
                    say(sender, "&e" + r.getName() + " &7- " + r.getWorld() + " ["
                            + r.getMinX() + "," + r.getMinY() + "," + r.getMinZ() + "] -> ["
                            + r.getMaxX() + "," + r.getMaxY() + "," + r.getMaxZ() + "]");
                }
            }
            case "reload" -> {
                plugin.regions().load();
                plugin.reloadAll();
                say(sender, "&aConfiguracion recargada.");
            }
            default -> say(sender, "&cSubcomando desconocido.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("cropregions.admin")) return out;
        if (args.length == 1) {
            for (String s : List.of("wand", "create", "delete", "list", "reload"))
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            for (Region r : plugin.regions().all())
                if (r.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(r.getName());
        }
        return out;
    }
}
