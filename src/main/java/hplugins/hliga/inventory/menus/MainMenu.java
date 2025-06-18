package hplugins.hliga.inventory.menus;

import com.cryptomorin.xseries.XMaterial;
import hplugins.hliga.Main;
import hplugins.hliga.inventory.gui.BaseGui;
import hplugins.hliga.inventory.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Menu principal do sistema hLiga
 * Compatível com Minecraft 1.8 - 1.21+
 * 
 * @author hPlugins and Hokase
 * @version 2.0.0
 */
public class MainMenu extends BaseGui {
    
    /**
     * Construtor do menu principal
     * 
     * @param plugin Instância do plugin
     * @param player Jogador
     */
    public MainMenu(Main plugin, Player player) {
        super(plugin, player);
    }
    
    @Override
    public void open(int page) {
        buildInventory();
        if (inventory != null) {
            player.openInventory(inventory);
            plugin.getInventoryManager().registerGui(player, this);
        } else {
            player.sendMessage(ChatColor.RED + "Erro interno: Não foi possível abrir o menu!");
        }
    }
    
    @Override
    protected void buildInventory() {
        ConfigurationSection config = null;
        
        try {
            config = plugin.getConfigManager().getMenusConfig().getConfigurationSection("menu_principal");
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar configuração do menu: " + e.getMessage());
        }
        
        
        String title = "&6hLiga &8- &7Menu";
        int size = 27;
        
        if (config != null) {
            title = config.getString("titulo", title);
            size = config.getInt("tamanho", size);
        }
        
        title = ensureCompatibleTitle(title);
        this.inventory = Bukkit.createInventory(null, size, title);
        this.clickActions.clear();
        
        
        if (config != null) {
            createBorder(config);
        }
        
        
        if (config != null) {
            ConfigurationSection itemsConfig = config.getConfigurationSection("itens");
            if (itemsConfig != null) {
                for (String key : itemsConfig.getKeys(false)) {
                    ConfigurationSection itemConfig = itemsConfig.getConfigurationSection(key);
                    if (itemConfig != null) {
                        addMenuItem(itemConfig, key);
                    }
                }
            }
        } else {
            
            createDefaultItems();
        }
        
        
        if (config != null) {
            fillEmptySlots(config);
        }
    }
    
    /**
     * Adiciona um item do menu
     * 
     * @param itemConfig Configuração do item
     * @param actionKey Chave da ação
     */
    private void addMenuItem(ConfigurationSection itemConfig, String actionKey) {
        int slot = itemConfig.getInt("slot", 0);
        ItemStack item = createConfigItem(itemConfig, getPlaceholders());
        
        if (item != null) {
            inventory.setItem(slot, item);
            
            
            String action = itemConfig.getString("acao", actionKey);
            addClickAction(slot, event -> handleMenuAction(action, event));
        }
    }
    
    /**
     * Manipula as ações dos itens do menu
     * 
     * @param action Ação a ser executada
     * @param event Evento de clique
     */
    private void handleMenuAction(String action, InventoryClickEvent event) {
        switch (action.toLowerCase()) {
            case "clans":
                new ClanListMenu(plugin, player).open(1);
                break;
                
            case "top_clans":
                new TopClansMenu(plugin, player).open(1);
                break;
                
            case "temporada":
                new SeasonMenu(plugin, player).open(1);
                break;
                
            case "historico":
                new SeasonHistoryMenu(plugin, player).open(1);
                break;
                
            case "fechar":
                player.closeInventory();
                break;
                
            default:
                player.sendMessage(ChatColor.YELLOW + "Funcionalidade em desenvolvimento!");
                break;
        }
    }
    
    /**
     * Cria itens padrão quando a configuração não está disponível
     */
    private void createDefaultItems() {
        
        ItemStack clansItem = new ItemBuilder(XMaterial.IRON_CHESTPLATE)
                .name("&e⚔ &6Lista de Clãs &e⚔")
                .lore("", "&7Ver todos os clãs registrados", "&7e suas respectivas pontuações", "", "&8➥ &aClique para abrir")
                .build();
        inventory.setItem(10, clansItem);
        addClickAction(10, event -> new ClanListMenu(plugin, player).open(1));
        
        
        ItemStack topItem = new ItemBuilder(XMaterial.GOLDEN_HELMET)
                .name("&e🏆 &6Top Clãs &e🏆")
                .lore("", "&7Ver o ranking dos clãs", "&7com maior pontuação", "", "&8➥ &aClique para abrir")
                .build();
        inventory.setItem(12, topItem);
        addClickAction(12, event -> new TopClansMenu(plugin, player).open(1));
        
        
        ItemStack seasonItem = new ItemBuilder(XMaterial.CLOCK)
                .name("&e⏰ &6Temporada Atual &e⏰")
                .lore("", "&7Ver informações detalhadas", "&7sobre a temporada em andamento", "", "&8➥ &aClique para abrir")
                .build();
        inventory.setItem(14, seasonItem);
        addClickAction(14, event -> new SeasonMenu(plugin, player).open(1));
        
        
        ItemStack historyItem = new ItemBuilder(XMaterial.BOOK)
                .name("&e📚 &6Histórico &e📚")
                .lore("", "&7Ver o histórico completo", "&7de temporadas anteriores", "", "&8➥ &aClique para abrir")
                .build();
        inventory.setItem(16, historyItem);
        addClickAction(16, event -> new SeasonHistoryMenu(plugin, player).open(1));
        
        
        ItemStack closeItem = new ItemBuilder(XMaterial.BARRIER)
                .name("&c✘ Fechar Menu")
                .lore("", "&7Clique para fechar este menu")
                .build();
        inventory.setItem(22, closeItem);
        addClickAction(22, event -> player.closeInventory());
    }
    
    /**
     * Obtém placeholders para substituição nos itens
     * 
     * @return Map com placeholders
     */
    private Map<String, String> getPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        
        
        placeholders.put("{player}", player.getName());
        
        
        if (plugin.getSeasonManager() != null && plugin.getSeasonManager().getCurrentSeason() != null) {
            String seasonName = plugin.getSeasonManager().getCurrentSeason().getName();
            placeholders.put("{temporada_atual}", seasonName);
        } else {
            placeholders.put("{temporada_atual}", "Nenhuma");
        }
        
        
        if (plugin.getPointsManager() != null) {
            int totalClans = plugin.getPointsManager().getTopClans(Integer.MAX_VALUE).size();
            placeholders.put("{total_clans}", String.valueOf(totalClans));
        } else {
            placeholders.put("{total_clans}", "0");
        }
        
        return placeholders;
    }
}