package com.newscraft.endportal;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndPortalFrame;
import org.bukkit.block.data.type.EndPortalFrame.Eye;
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

/**
 * Listener responsável por toda a lógica do plugin.
 * Monitora eventos de quebrar e colocar blocos do Portal do End.
 */
public class EndPortalListener implements Listener {

    // Permissão necessária para usar o recurso
    private static final String PERMISSION = "newscraft.endportal";

    // Nome personalizado que aparece no item coletado
    private static final String ITEM_NAME = "§5§lBloco do Portal do End";

    private final EndPortalPlugin plugin;

    public EndPortalListener(EndPortalPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────
    // EVENTO: Jogador quebra um bloco
    // ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Verifica se o bloco é o Portal do End (Frame)
        if (block.getType() != Material.END_PORTAL_FRAME) {
            return;
        }

        // Verifica se o jogador tem a permissão necessária
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para coletar blocos do Portal do End!");
            event.setCancelled(true);
            return;
        }

        // Jogadores em modo criativo não precisam de Silk Touch
        if (player.getGameMode() == GameMode.CREATIVE) {
            // No criativo apenas cancela o drop padrão, não entrega item
            event.setDropItems(false);
            return;
        }

        // Verifica se a ferramenta tem Silk Touch
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!hasSilkTouch(tool)) {
            player.sendMessage("§cVocê precisa de uma picareta com §lToque Suave§c para coletar este bloco!");
            event.setCancelled(true);
            return;
        }

        // Captura os dados do bloco ANTES de quebrá-lo
        // (se o bloco tem o olho do End colocado ou não)
        boolean hasEye = false;
        if (block.getBlockData() instanceof org.bukkit.block.data.type.EndPortalFrame frameData) {
            hasEye = frameData.hasEye();
        }

        // Cancela o drop padrão do bloco
        event.setDropItems(false);

        // Cria o item personalizado para dar ao jogador
        ItemStack portalFrame = createPortalFrameItem(hasEye);

        // Entrega o item no inventário do jogador
        // Se não couber no inventário, dropa no chão
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(portalFrame);
            player.sendMessage("§aBloco do Portal do End coletado com sucesso!");
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), portalFrame);
            player.sendMessage("§eInventário cheio! O bloco foi dropado no chão.");
        }
    }

    // ─────────────────────────────────────────────
    // EVENTO: Jogador coloca um bloco
    // ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        // Verifica se o item sendo colocado é um End Portal Frame
        if (item.getType() != Material.END_PORTAL_FRAME) {
            return;
        }

        // Verifica se o jogador tem permissão
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para colocar blocos do Portal do End!");
            event.setCancelled(true);
            return;
        }

        // Recupera se o item carregava o olho do End
        boolean hasEye = isEyeFrame(item);

        // Aguarda o próximo tick para aplicar os dados ao bloco já colocado
        // (necessário pois o bloco ainda está sendo colocado neste momento)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Block placedBlock = event.getBlockPlaced();

            // Confirma que o bloco foi realmente colocado
            if (placedBlock.getType() != Material.END_PORTAL_FRAME) {
                return;
            }

            // Aplica o estado do olho ao bloco colocado
            if (placedBlock.getBlockData() instanceof org.bukkit.block.data.type.EndPortalFrame frameData) {
                frameData.setEye(hasEye);
                placedBlock.setBlockData(frameData, true);
            }

            player.sendMessage("§aBloco do Portal do End colocado com sucesso!");
        });
    }

    // ─────────────────────────────────────────────
    // MÉTODOS AUXILIARES
    // ─────────────────────────────────────────────

    /**
     * Verifica se uma ferramenta possui o encantamento Silk Touch.
     */
    private boolean hasSilkTouch(ItemStack tool) {
        if (tool == null || tool.getType() == Material.AIR) {
            return false;
        }
        return tool.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    /**
     * Cria o ItemStack do End Portal Frame com nome personalizado.
     * Se hasEye for true, registra na lore que o bloco contém o olho.
     */
    private ItemStack createPortalFrameItem(boolean hasEye) {
        ItemStack item = new ItemStack(Material.END_PORTAL_FRAME, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Nome colorido no item
            meta.setDisplayName(ITEM_NAME);

            // Lore (descrição) do item
            List<String> lore = new ArrayList<>();
            lore.add("§7Servidor: §5NewsCraft");
            lore.add("§7Use com cuidado!");

            if (hasEye) {
                // Marca que este frame tem o olho do End
                lore.add("§6✦ Contém o Olho do End");
                lore.add("§8[eye:true]"); // Tag interna para identificação
            } else {
                lore.add("§8[eye:false]"); // Tag interna para identificação
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Verifica se um ItemStack de End Portal Frame contém o olho do End,
     * lendo a tag interna que salvamos na lore do item.
     */
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

        // Procura a tag interna na lore do item
        for (String line : lore) {
            if (line.contains("[eye:true]")) {
                return true;
            }
        }

        return false;
    }
}
