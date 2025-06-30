package hplugins.hliga.inventory;

import hplugins.hliga.Main;
import hplugins.hliga.inventory.gui.BaseGui;
import hplugins.hliga.utils.LogUtils;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gerenciador central do sistema de inventários
 * Sistema robusto compatível com Minecraft 1.8 - 1.21+
 *
 * @author hLiga Plugin Team
 * @version 2.0.0
 */
public class InventoryManager implements Listener {

    private final Main plugin;

    @Getter
    private final Map<UUID, BaseGui> openGuis = new HashMap<>();

    @Getter
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    /**
     * Construtor do gerenciador de inventários
     *
     * @param plugin Instância principal do plugin
     */
    public InventoryManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Registra uma GUI aberta para um jogador
     *
     * @param player Jogador
     * @param gui GUI a ser registrada
     */
    public void registerGui(Player player, BaseGui gui) {
        if (player == null || gui == null) {
            plugin.getLogger().warning("Tentativa de registrar GUI inválida!");
            return;
        }

        openGuis.put(player.getUniqueId(), gui);
        plugin.getLogger().log(Level.FINE, "GUI registrada para jogador " + player.getName());
    }

    /**
     * Remove o registro de uma GUI
     *
     * @param player Jogador
     */
    public void unregisterGui(Player player) {
        if (player != null) {
            openGuis.remove(player.getUniqueId());
            playerPages.remove(player.getUniqueId());
            plugin.getLogger().log(Level.FINE, "GUI removida para jogador " + player.getName());
        }
    }

    /**
     * Obtém a GUI atualmente aberta pelo jogador
     *
     * @param player Jogador
     * @return GUI aberta ou null se não houver
     */
    public BaseGui getOpenGui(Player player) {
        return player != null ? openGuis.get(player.getUniqueId()) : null;
    }

    /**
     * Define a página atual do jogador
     *
     * @param player Jogador
     * @param page Página
     */
    public void setPlayerPage(Player player, int page) {
        if (player != null) {
            playerPages.put(player.getUniqueId(), Math.max(1, page));
        }
    }

    /**
     * Obtém a página atual do jogador
     *
     * @param player Jogador
     * @return Página atual (padrão: 1)
     */
    public int getPlayerPage(Player player) {
        return player != null ? playerPages.getOrDefault(player.getUniqueId(), 1) : 1;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        BaseGui gui = getOpenGui(player);

        if (gui != null) {
            event.setCancelled(true);

            try {
                LogUtils.debug("Clique detectado - Slot: " + event.getSlot() + " | Jogador: " + player.getName());

                gui.handleClick(event);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao processar clique na GUI para " + player.getName(), e);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getMessages().getMessage("erros.erro_interno")));
                player.closeInventory();
                unregisterGui(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        BaseGui gui = getOpenGui(player);

        if (gui != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        BaseGui gui = getOpenGui(player);

        if (gui != null) {
            try {
                gui.handleClose(event);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao processar fechamento da GUI para " + player.getName(), e);
            } finally {
                unregisterGui(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        unregisterGui(event.getPlayer());
    }

    /**
     * Limpa todos os registros de GUIs abertas
     */
    public void clearAll() {
        openGuis.clear();
        playerPages.clear();
        LogUtils.debugMedium("Todos os registros de GUI foram limpos.");
    }

    /**
     * Atualiza todos os menus abertos forçando recarregamento dos dados
     * Usado após reset de temporada para limpar cache obsoleto
     */
    public void refreshAllMenus() {
        LogUtils.info("Iniciando refresh de todos os menus abertos...");

        for (Map.Entry<UUID, BaseGui> entry : openGuis.entrySet()) {
            try {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.closeInventory();
                    LogUtils.debug("Menu fechado para jogador: " + player.getName());
                }
            } catch (Exception e) {
                LogUtils.warning("Erro ao fechar menu para jogador: " + e.getMessage());
            }
        }

        clearAll();
        LogUtils.info("Cache de menus limpo após reset de temporada");
    }

    /**
     * Obtém estatísticas do gerenciador
     *
     * @return String com estatísticas
     */
    public String getStats() {
        return String.format("GUIs abertas: %d, Páginas registradas: %d",
                openGuis.size(), playerPages.size());
    }
}