package hplugins.hliga.inventory.gui;

import com.cryptomorin.xseries.XMaterial;
import hplugins.hliga.Main;
import hplugins.hliga.inventory.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Classe base para todas as GUIs do sistema
 * Compatível com Minecraft 1.8 - 1.21+
 * 
 * @author hPlugins and Hokase
 * @version 2.0.0
 */
public abstract class BaseGui {
    
    protected final Main plugin;
    protected final Player player;
    
    @Getter
    protected Inventory inventory;
    
    @Getter
    protected int currentPage = 1;
    
    @Getter
    protected int totalPages = 1;
    
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickActions = new HashMap<>();
    
    /**
     * Garante que o título seja compatível com todas as versões do Minecraft
     * 
     * @param title Título original
     * @return Título truncado se necessário
     */
    protected String ensureCompatibleTitle(String title) {
        if (title == null) {
            return "Menu";
        }
        
        
        String coloredTitle = ChatColor.translateAlternateColorCodes('&', title);
        
        
        if (coloredTitle.length() > 32) {
            return coloredTitle.substring(0, 32);
        }
        
        return coloredTitle;
    }
    
    /**
     * Construtor base da GUI
     * 
     * @param plugin Instância principal do plugin
     * @param player Jogador que abrirá a GUI
     */
    public BaseGui(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    
    /**
     * Abre a GUI para o jogador
     * 
     * @param page Página a ser exibida
     */
    public abstract void open(int page);
    
    /**
     * Constrói o inventário com todos os itens
     */
    protected abstract void buildInventory();
    
    /**
     * Registra uma ação de clique para um slot específico
     * 
     * @param slot Slot do inventário
     * @param action Ação a ser executada
     */
    protected void addClickAction(int slot, Consumer<InventoryClickEvent> action) {
        clickActions.put(slot, action);
    }
    
    /**
     * Cria um item baseado na configuração
     * 
     * @param config Seção de configuração
     * @param placeholders Placeholders para substituição
     * @return ItemStack criado
     */
    protected ItemStack createConfigItem(ConfigurationSection config, Map<String, String> placeholders) {
        if (config == null) return null;
        
        try {
            String materialName = config.getString("material", "STONE");
            XMaterial material = XMaterial.matchXMaterial(materialName).orElse(XMaterial.STONE);
            
            String displayName = config.getString("nome", "");
            List<String> lore = config.getStringList("lore");
            
            
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    displayName = displayName.replace(entry.getKey(), entry.getValue());
                    lore.replaceAll(line -> line.replace(entry.getKey(), entry.getValue()));
                }
            }
            
            ItemBuilder builder = new ItemBuilder(material)
                    .name(ChatColor.translateAlternateColorCodes('&', displayName))
                    .lore(lore.stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                            .toArray(String[]::new));
            
            return builder.build();
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao criar item da configuração: " + e.getMessage());
            return new ItemBuilder(XMaterial.STONE).name(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessages().getMessage("erros.item_erro"))).build();
        }
    }
    
    /**
     * Cria itens de navegação padrão
     * 
     * @param config Configuração do menu
     */
    protected void addNavigationItems(ConfigurationSection config) {
        if (config == null) return;
        
        ConfigurationSection navConfig = config.getConfigurationSection("navegacao");
        if (navConfig == null) return;
        
        
        ConfigurationSection prevConfig = navConfig.getConfigurationSection("anterior");
        if (prevConfig != null && currentPage > 1) {
            int slot = prevConfig.getInt("slot", 48);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{pagina}", String.valueOf(currentPage));
            placeholders.put("{total_paginas}", String.valueOf(totalPages));
            
            ItemStack prevItem = createConfigItem(prevConfig, placeholders);
            inventory.setItem(slot, prevItem);
            
            addClickAction(slot, event -> {
                open(currentPage - 1);
            });
        }
        
        
        ConfigurationSection nextConfig = navConfig.getConfigurationSection("proxima");
        if (nextConfig != null && currentPage < totalPages) {
            int slot = nextConfig.getInt("slot", 50);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{pagina}", String.valueOf(currentPage));
            placeholders.put("{total_paginas}", String.valueOf(totalPages));
            
            ItemStack nextItem = createConfigItem(nextConfig, placeholders);
            inventory.setItem(slot, nextItem);
            
            addClickAction(slot, event -> {
                open(currentPage + 1);
            });
        }
        
        
        ConfigurationSection backConfig = navConfig.getConfigurationSection("voltar");
        if (backConfig != null) {
            int slot = backConfig.getInt("slot", 49);
            ItemStack backItem = createConfigItem(backConfig, null);
            inventory.setItem(slot, backItem);
            
            addClickAction(slot, event -> {
                
                new hplugins.hliga.inventory.menus.MainMenu(plugin, player).open(1);
            });
        }
    }
    
    /**
     * Preenche slots vazios com um item decorativo
     * 
     * @param config Configuração do menu
     */
    protected void fillEmptySlots(ConfigurationSection config) {
        if (config == null || !config.getBoolean("preencher_slots_vazios", false)) return;
        
        ConfigurationSection itemConfig = config.getConfigurationSection("item_vazio");
        if (itemConfig == null) return;
        
        ItemStack fillItem = createConfigItem(itemConfig, null);
        if (fillItem == null) return;
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillItem);
            }
        }
    }
    
    /**
     * Cria uma borda decorativa no inventário
     * 
     * @param config Configuração do menu
     */
    protected void createBorder(ConfigurationSection config) {
        if (config == null || !config.getBoolean("criar_borda", false)) return;
        
        String materialName = config.getString("material_borda", "BLACK_STAINED_GLASS_PANE");
        XMaterial borderMaterial = XMaterial.matchXMaterial(materialName).orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
        ItemStack borderItem = new ItemBuilder(borderMaterial).name(" ").build();
        
        int size = inventory.getSize();
        int rows = size / 9;
        String borderType = config.getString("tipo_borda", "completa");
        
        if ("completa".equalsIgnoreCase(borderType)) {
            
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, borderItem); 
                inventory.setItem(size - 9 + i, borderItem); 
            }
            
            for (int i = 1; i < rows - 1; i++) {
                inventory.setItem(i * 9, borderItem); 
                inventory.setItem(i * 9 + 8, borderItem); 
            }
        } else if ("cantos".equalsIgnoreCase(borderType)) {
            
            inventory.setItem(0, borderItem); 
            inventory.setItem(8, borderItem); 
            inventory.setItem(size - 9, borderItem); 
            inventory.setItem(size - 1, borderItem); 
        }
    }
    
    /**
     * Processa cliques no inventário
     * 
     * @param event Evento de clique
     */
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        
        Consumer<InventoryClickEvent> action = clickActions.get(slot);
        if (action != null) {
            try {
                action.accept(event);
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao executar ação do slot " + slot + ": " + e.getMessage());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessages().getMessage("erros.erro_clique")));
            }
        }
    }
    
    /**
     * Processa fechamento do inventário
     * 
     * @param event Evento de fechamento
     */
    public void handleClose(InventoryCloseEvent event) {
        
    }
}