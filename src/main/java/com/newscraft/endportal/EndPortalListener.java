package com.newscraft.endportal;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EndPortalListener implements Listener {

    private static final String PERMISSION = "newscraft.endportal";
    private static final String ITEM_NAME = "§5§lBloco do Portal do End";

    private final EndPortalPlugin plugin;

    public EndPortalListener(EndPortalPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────
    // EVENTO: Quebrar bloco
    // ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.END_PORTAL_FRAME) {
            return;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para coletar este bloco!");
            event.setCancelled(true);
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            event.setDropItems(false);
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!hasSilkTouch(tool)) {
            player.sendMessage("§cVocê precisa de uma picareta com §lToque Suave§c!");
            event.setCancelled(true);
            return;
        }

        // Captura se o frame tem o olho usando BlockData diretamente
        boolean hasEye = false;
        BlockData data = block.getBlockData();
        if (data instanceof EndPortalFrame frameData) {
            hasEye = frameData.hasEye();
        }

        event.setDropItems(false);

        ItemStack portalFrame = createPortalFrameItem(hasEye);

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(portalFrame);
            player.sendMessage("§aBloco do Portal do End coletado!");
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), portalFrame);
            player.sendMessage("§eInventário cheio! O bloco caiu no chão.");
        }
    }

    // ─────────────────────────────────────────────
    // EVENTO: Colocar bloco
    // ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (item.getType() != Material.END_PORTAL_FRAME) {
            return;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para colocar este bloco!");
            event.setCancelled(true);
            return;
        }

        boolean hasEye = isEyeFrame(item);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Block placedBlock = event.getBlockPlaced();

            if (placedBlock.getType() != Material.END_PORTAL_FRAME) {
                return;
            }

            BlockData data = placedBlock.getBlockData();
            if (data instanceof EndPortalFrame frameData) {
                frameData.setEye(hasEye);
                placedBlock.setBlockData(frameData, true);
            }

            player.sendMessage("§aBloco do Portal do End colocado!");
        });
    }

    // ─────────────────────────────────────────────
    // MÉTODOS AUXILIARES
    // ─────────────────────────────────────────────
    private boolean hasSilkTouch(ItemStack tool) {
        if (tool == null || tool.getType() == Material.AIR) {
            return false;
        }
        return tool.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    private ItemStack createPortalFrameItem(boolean hasEye) {
        ItemStack item = new ItemStack(Material.END_PORTAL_FRAME, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ITEM_NAME);

            List<String> lore = new ArrayList<>();
            lore.add("§7Servidor: §5NewsCraft");
            lore.add("§7Use com cuidado!");

            if (hasEye) {
                lore.add("§6✦ Contém o Olho do End");
                lore.add("§8[eye:true]");
            } else {
                lore.add("§8[eye:false]");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean isEyeFrame(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return false;
        }

        for (String line : lore) {
            if (line.contains("[eye:true]")) {
                return true;
            }
        }

        return false;
    }
}
