package me.cropregions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class CropListener implements Listener {

    private final CropRegionsPlugin plugin;

    public CropListener(CropRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- Romper: SOLO cultivos ----------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (plugin.regions().at(b.getLocation()) == null) return;

        Player p = e.getPlayer();
        if (p.hasPermission("cropregions.bypass")) return;

        // Siempre cancelamos el evento vanilla y manejamos todo nosotros
        e.setCancelled(true);

        Material type = b.getType();
        if (!plugin.isCrop(type)) {
            plugin.msg(p, "deny");
            return;
        }
        if (!plugin.isMature(b)) {
            plugin.msg(p, "immature");
            return;
        }
        plugin.drops().harvest(b, p.getInventory().getItemInMainHand(), p);
    }

    // ---------- Colocar: bloqueado ----------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (plugin.regions().at(e.getBlock().getLocation()) == null) return;
        if (e.getPlayer().hasPermission("cropregions.bypass")) return;
        e.setCancelled(true);
        plugin.msg(e.getPlayer(), "deny");
    }

    // ---------- Varita de seleccion + anti-pisoteo ----------
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        // Varita
        ItemStack item = e.getItem();
        if (item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(CropRegionsPlugin.WAND_KEY, PersistentDataType.BYTE)
                && e.getClickedBlock() != null
                && e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {

            if (!p.hasPermission("cropregions.admin")) return;
            Location loc = e.getClickedBlock().getLocation();
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.regions().pos1().put(p.getUniqueId(), loc);
                p.sendMessage(plugin.color("&aPosicion 1: &f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                e.setCancelled(true);
                e.setUseInteractedBlock(Event.Result.DENY);
                return;
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.regions().pos2().put(p.getUniqueId(), loc);
                p.sendMessage(plugin.color("&aPosicion 2: &f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
                e.setCancelled(true);
                e.setUseInteractedBlock(Event.Result.DENY);
                return;
            }
        }

        // No pisotear la tierra de cultivo
        if (e.getAction() == Action.PHYSICAL && e.getClickedBlock() != null
                && e.getClickedBlock().getType() == Material.FARMLAND
                && plugin.regions().at(e.getClickedBlock().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    // ---------- Mobs / endermans / pisoteo por entidades ----------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent e) {
        if (plugin.regions().at(e.getBlock().getLocation()) != null) e.setCancelled(true);
    }

    // ---------- Explosiones ----------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.regions().at(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.regions().at(b.getLocation()) != null);
    }
}
