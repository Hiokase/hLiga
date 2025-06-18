package hplugins.hliga.inventory.menus;

import hplugins.hliga.Main;
import hplugins.hliga.inventory.gui.BaseGui;
import hplugins.hliga.models.Season;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu de informações da temporada atual
 * Compatível com Minecraft 1.8 - 1.21+
 * 
 * @author hPlugins and Hokase
 * @version 2.0.0
 */
public class SeasonMenu extends BaseGui {
    
    /**
     * Construtor do menu de temporada
     * 
     * @param plugin Instância do plugin
     * @param player Jogador
     */
    public SeasonMenu(Main plugin, Player player) {
        super(plugin, player);
    }
    
    @Override
    public void open(int page) {
        buildInventory();
        player.openInventory(inventory);
        plugin.getInventoryManager().registerGui(player, this);
    }
    
    @Override
    protected void buildInventory() {
        ConfigurationSection config = plugin.getConfigManager().getMenusConfig().getConfigurationSection("menu_temporada");
        
        if (config == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessages().getMessage("erros.menu_temporada_config")));
            return;
        }
        
        String title = ensureCompatibleTitle(config.getString("titulo", "&8Temporada Atual"));
        int size = config.getInt("tamanho", 27);
        
        this.inventory = Bukkit.createInventory(null, size, title);
        this.clickActions.clear();
        
        
        createBorder(config);
        
        
        addSeasonInfoItem(config);
        
        
        addNavigationItems(config);
        
        
        fillEmptySlots(config);
    }
    
    /**
     * Adiciona o item principal com informações da temporada
     * 
     * @param config Configuração do menu
     */
    private void addSeasonInfoItem(ConfigurationSection config) {
        ConfigurationSection infoConfig = config.getConfigurationSection("info");
        if (infoConfig == null) return;
        
        int slot = infoConfig.getInt("slot", 13);
        Map<String, String> placeholders = getSeasonPlaceholders();
        
        ItemStack infoItem = createConfigItem(infoConfig, placeholders);
        if (infoItem != null) {
            inventory.setItem(slot, infoItem);
            
            
            addClickAction(slot, event -> {
                Season currentSeason = plugin.getSeasonManager().getCurrentSeason();
                if (currentSeason != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    long timeLeft = currentSeason.getTimeLeft();
                    String timeString = timeLeft > 0 ? formatTime(timeLeft) : 
                        plugin.getConfigManager().getMessages().getMessage("menu_temporada.tempo_finalizada");
                    int totalClans = plugin.getPointsManager().getTopClans(Integer.MAX_VALUE).size();
                    
                    
                    List<String> infoTemporada = plugin.getConfigManager().getMessages().getStringList("menu_temporada.info_temporada_ativa");
                    for (String linha : infoTemporada) {
                        String linhaFormatada = linha
                                .replace("{nome}", currentSeason.getName())
                                .replace("{inicio}", dateFormat.format(currentSeason.getStartDate()))
                                .replace("{termino}", dateFormat.format(currentSeason.getEndDate()))
                                .replace("{tempo_restante}", timeString)
                                .replace("{total_clans}", String.valueOf(totalClans));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getMessages().getMessage("menu_temporada.sem_temporada")));
                }
            });
        }
    }
    
    /**
     * Adiciona itens de navegação
     * 
     * @param config Configuração do menu
     */
    protected void addNavigationItems(ConfigurationSection config) {
        ConfigurationSection navConfig = config.getConfigurationSection("navegacao");
        if (navConfig == null) return;
        
        
        ConfigurationSection topConfig = navConfig.getConfigurationSection("top");
        if (topConfig != null) {
            int slot = topConfig.getInt("slot", 11);
            ItemStack topItem = createConfigItem(topConfig, null);
            
            if (topItem != null) {
                inventory.setItem(slot, topItem);
                addClickAction(slot, event -> {
                    new TopClansMenu(plugin, player).open(1);
                });
            }
        }
        
        
        ConfigurationSection historyConfig = navConfig.getConfigurationSection("historico");
        if (historyConfig != null) {
            int slot = historyConfig.getInt("slot", 15);
            ItemStack historyItem = createConfigItem(historyConfig, null);
            
            if (historyItem != null) {
                inventory.setItem(slot, historyItem);
                addClickAction(slot, event -> {
                    new SeasonHistoryMenu(plugin, player).open(1);
                });
            }
        }
        
        
        ConfigurationSection backConfig = navConfig.getConfigurationSection("voltar");
        if (backConfig != null) {
            int slot = backConfig.getInt("slot", 22);
            ItemStack backItem = createConfigItem(backConfig, null);
            
            if (backItem != null) {
                inventory.setItem(slot, backItem);
                addClickAction(slot, event -> {
                    new MainMenu(plugin, player).open(1);
                });
            }
        }
    }
    
    /**
     * Obtém placeholders da temporada atual
     * 
     * @return Map com placeholders
     */
    private Map<String, String> getSeasonPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        Season currentSeason = plugin.getSeasonManager().getCurrentSeason();
        if (currentSeason != null) {
            placeholders.put("{nome}", currentSeason.getName());
            placeholders.put("{inicio}", dateFormat.format(currentSeason.getStartDate()));
            placeholders.put("{termino}", dateFormat.format(currentSeason.getEndDate()));
            
            long timeLeft = currentSeason.getTimeLeft();
            if (timeLeft > 0) {
                placeholders.put("{restante}", formatTime(timeLeft));
            } else {
                placeholders.put("{restante}", "FINALIZADA");
            }
        } else {
            placeholders.put("{nome}", "Nenhuma temporada ativa");
            placeholders.put("{inicio}", "N/A");
            placeholders.put("{termino}", "N/A");
            placeholders.put("{restante}", "N/A");
        }
        
        int totalClans = plugin.getPointsManager().getTopClans(Integer.MAX_VALUE).size();
        placeholders.put("{total_clans}", String.valueOf(totalClans));
        
        return placeholders;
    }
    
    /**
     * Formata tempo em milissegundos para uma string legível
     * 
     * @param timeInMillis Tempo em milissegundos
     * @return String formatada
     */
    private String formatTime(long timeInMillis) {
        long seconds = timeInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}