package hplugins.hliga.inventory.gui;

import hplugins.hliga.Main;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI base com sistema de paginação avançado
 * Compatível com Minecraft 1.8 - 1.21+
 * 
 * @author hPlugins and Hokase
 * @version 2.0.0
 */
public abstract class PaginatedGui extends BaseGui {
    
    @Getter
    protected int itemsPerPage;
    
    @Getter
    protected List<Integer> contentSlots;
    
    protected ConfigurationSection menuConfig;
    protected String configKey;
    
    /**
     * Construtor da GUI paginada
     * 
     * @param plugin Instância do plugin
     * @param player Jogador
     * @param configKey Chave da configuração no menus.yml
     */
    public PaginatedGui(Main plugin, Player player, String configKey) {
        super(plugin, player);
        this.configKey = configKey;
        this.menuConfig = plugin.getConfigManager().getMenusConfig().getConfigurationSection(configKey);
        
        if (menuConfig != null) {
            this.itemsPerPage = menuConfig.getInt("itens_por_pagina", 21);
            this.contentSlots = menuConfig.getIntegerList("slots_clans");
            
            
            if (contentSlots.isEmpty()) {
                for (int i = 10; i <= 16; i++) contentSlots.add(i);
                for (int i = 19; i <= 25; i++) contentSlots.add(i);
                for (int i = 28; i <= 34; i++) contentSlots.add(i);
            }
        }
    }
    
    @Override
    public void open(int page) {
        if (menuConfig == null) {
            player.sendMessage(ChatColor.RED + "Erro: Configuração do menu não encontrada!");
            return;
        }
        
        this.currentPage = Math.max(1, page);
        
        
        List<ItemStack> allItems = getContent();
        this.totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / itemsPerPage));
        
        
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        
        plugin.getInventoryManager().setPlayerPage(player, currentPage);
        
        buildInventory();
        
        player.openInventory(inventory);
        plugin.getInventoryManager().registerGui(player, this);
    }
    
    @Override
    protected void buildInventory() {
        if (menuConfig == null) return;
        
        
        String title = menuConfig.getString("titulo", "&8Menu");
        if (totalPages > 1) {
            String pageFormat = menuConfig.getString("formato_pagina", " &7({pagina}/{total})");
            title += pageFormat.replace("{pagina}", String.valueOf(currentPage))
                             .replace("{total}", String.valueOf(totalPages));
        }
        
        title = ensureCompatibleTitle(title);
        int size = menuConfig.getInt("tamanho", 54);
        
        this.inventory = Bukkit.createInventory(null, size, title);
        this.clickActions.clear();
        
        
        createBorder(menuConfig);
        
        
        addPaginatedContent();
        
        
        ConfigurationSection navConfig = menuConfig.getConfigurationSection("navegacao");
        addNavigationItems(menuConfig);
        
        
        fillEmptySlots(menuConfig);
    }
    
    /**
     * Adiciona o conteúdo paginado ao inventário
     */
    protected void addPaginatedContent() {
        List<ItemStack> allItems = getContent();
        
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < contentSlots.size(); i++, slotIndex++) {
            int slot = contentSlots.get(slotIndex);
            ItemStack item = allItems.get(i);
            
            inventory.setItem(slot, item);
            
            
            addContentClickAction(slot, i, item);
        }
    }
    
    /**
     * Obtém todo o conteúdo que deve ser exibido na GUI
     * 
     * @return Lista de ItemStacks com o conteúdo
     */
    protected abstract List<ItemStack> getContent();
    
    /**
     * Adiciona ação de clique para um item de conteúdo
     * 
     * @param slot Slot do item
     * @param contentIndex Índice do item na lista de conteúdo
     * @param item ItemStack do item
     */
    protected void addContentClickAction(int slot, int contentIndex, ItemStack item) {
        
    }
    
    /**
     * Atualiza o conteúdo da GUI mantendo a página atual
     */
    public void refresh() {
        open(currentPage);
    }
    
    /**
     * Vai para a primeira página
     */
    public void goToFirstPage() {
        open(1);
    }
    
    /**
     * Vai para a última página
     */
    public void goToLastPage() {
        List<ItemStack> allItems = getContent();
        int lastPage = Math.max(1, (int) Math.ceil((double) allItems.size() / itemsPerPage));
        open(lastPage);
    }
    
    /**
     * Verifica se há página anterior
     * 
     * @return true se há página anterior
     */
    public boolean hasPreviousPage() {
        return currentPage > 1;
    }
    
    /**
     * Verifica se há próxima página
     * 
     * @return true se há próxima página
     */
    public boolean hasNextPage() {
        return currentPage < totalPages;
    }
    
    /**
     * Obtém informações sobre a paginação
     * 
     * @return String com informações da paginação
     */
    public String getPaginationInfo() {
        List<ItemStack> allItems = getContent();
        int totalItems = allItems.size();
        int startItem = (currentPage - 1) * itemsPerPage + 1;
        int endItem = Math.min(currentPage * itemsPerPage, totalItems);
        
        return String.format("Página %d/%d (%d-%d de %d itens)", 
                           currentPage, totalPages, startItem, endItem, totalItems);
    }
}