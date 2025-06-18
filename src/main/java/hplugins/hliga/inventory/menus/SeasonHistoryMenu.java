package hplugins.hliga.inventory.menus;

import com.cryptomorin.xseries.XMaterial;
import hplugins.hliga.Main;
import hplugins.hliga.inventory.gui.PaginatedGui;
import hplugins.hliga.inventory.utils.ItemBuilder;
import hplugins.hliga.models.Season;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Menu de histórico de temporadas com paginação
 * Compatível com Minecraft 1.8 - 1.21+
 * 
 * @author hPlugins and Hokase
 * @version 2.0.0
 */
public class SeasonHistoryMenu extends PaginatedGui {
    
    /**
     * Construtor do menu de histórico
     * 
     * @param plugin Instância do plugin
     * @param player Jogador
     */
    public SeasonHistoryMenu(Main plugin, Player player) {
        super(plugin, player, "menu_historico");
        
        
        if (contentSlots.isEmpty()) {
            
            int[] defaultSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            };
            
            for (int slot : defaultSlots) {
                contentSlots.add(slot);
            }
        }
    }
    
    @Override
    protected List<ItemStack> getContent() {
        List<ItemStack> items = new ArrayList<>();
        
        try {
            if (plugin.getSeasonManager() == null) {
                return items;
            }
            
            List<Season> allSeasons = plugin.getSeasonManager().getAllSeasons();
            ConfigurationSection formatConfig = menuConfig != null ? menuConfig.getConfigurationSection("formato_temporada") : null;
            
            if (formatConfig == null) {
                
                return createDefaultSeasonItems(allSeasons);
            }
            
            
            for (Season season : allSeasons) {
                if (season.isFinished()) {
                    ItemStack seasonItem = createSeasonItem(season, formatConfig);
                    if (seasonItem != null) {
                        items.add(seasonItem);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar histórico de temporadas: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Cria itens padrão para temporadas quando não há configuração
     */
    private List<ItemStack> createDefaultSeasonItems(List<Season> allSeasons) {
        List<ItemStack> items = new ArrayList<>();
        
        for (Season season : allSeasons) {
            if (season.isFinished()) {
                ItemStack seasonItem = new ItemBuilder(XMaterial.BOOK)
                        .name("&6Temporada: &e" + season.getName())
                        .lore(
                            "&7Status: &cFinalizada",
                            "&7Data: &f" + season.getStartDate(),
                            "",
                            "&8➥ &7Clique para mais detalhes"
                        )
                        .build();
                items.add(seasonItem);
            }
        }
        
        return items;
    }
    
    /**
     * Cria um item representando uma temporada
     * 
     * @param season Dados da temporada
     * @param formatConfig Configuração de formato
     * @return ItemStack da temporada
     */
    private ItemStack createSeasonItem(Season season, ConfigurationSection formatConfig) {
        Map<String, String> placeholders = createSeasonPlaceholders(season);
        
        
        ConfigurationSection itemConfig = formatConfig;
        if (season.getWinner() != null && !season.getWinner().isEmpty()) {
            String winnerMaterial = formatConfig.getString("material_vencedor");
            if (winnerMaterial != null) {
                itemConfig.set("material", winnerMaterial);
            }
        }
        
        ItemStack item = createConfigItem(itemConfig, placeholders);
        
        
        if (formatConfig.getBoolean("adicionar_clique_info", true) && item != null && item.getItemMeta() != null) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8➥ &7Clique para mais detalhes"));
            
            item.getItemMeta().setLore(lore);
            item.setItemMeta(item.getItemMeta());
        }
        
        return item;
    }
    
    /**
     * Cria placeholders para uma temporada
     * 
     * @param season Temporada
     * @return Map com placeholders
     */
    private Map<String, String> createSeasonPlaceholders(Season season) {
        Map<String, String> placeholders = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        
        placeholders.put("{nome}", season.getName());
        placeholders.put("{inicio}", dateFormat.format(season.getStartDate()));
        placeholders.put("{termino}", dateFormat.format(season.getEndDate()));
        
        
        long durationMillis = season.getEndDate() - season.getStartDate();
        long durationDays = TimeUnit.MILLISECONDS.toDays(durationMillis);
        placeholders.put("{duracao}", String.valueOf(durationDays));
        
        
        String winner = season.getWinner();
        if (winner != null && !winner.isEmpty()) {
            placeholders.put("{vencedor}", winner);
            
            
            int winnerPoints = getSeasonWinnerPoints(season, winner);
            placeholders.put("{pontos}", String.valueOf(winnerPoints));
        } else {
            placeholders.put("{vencedor}", "Não definido");
            placeholders.put("{pontos}", "0");
        }
        
        return placeholders;
    }
    
    /**
     * Obtém a pontuação do vencedor de uma temporada
     * 
     * @param season Temporada
     * @param winner Tag do vencedor
     * @return Pontuação do vencedor
     */
    private int getSeasonWinnerPoints(Season season, String winner) {
        try {
            
            return season.getWinnerPoints();
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter pontos do vencedor " + winner + " da temporada " + season.getName());
        }
        return 0;
    }
    
    @Override
    protected void addContentClickAction(int slot, int contentIndex, ItemStack item) {
        addClickAction(slot, event -> {
            List<Season> finishedSeasons = new ArrayList<>();
            
            
            for (Season season : plugin.getSeasonManager().getAllSeasons()) {
                if (season.isFinished()) {
                    finishedSeasons.add(season);
                }
            }
            
            if (contentIndex < finishedSeasons.size()) {
                Season season = finishedSeasons.get(contentIndex);
                showSeasonDetails(season);
            }
        });
    }
    
    /**
     * Mostra detalhes de uma temporada para o jogador
     * 
     * @param season Temporada
     */
    private void showSeasonDetails(Season season) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        player.sendMessage(ChatColor.GOLD + "═══════════ DETALHES DA TEMPORADA ═══════════");
        player.sendMessage(ChatColor.YELLOW + "Nome: " + ChatColor.WHITE + season.getName());
        player.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.RED + "FINALIZADA");
        player.sendMessage(ChatColor.YELLOW + "Início: " + ChatColor.WHITE + dateFormat.format(season.getStartDate()));
        player.sendMessage(ChatColor.YELLOW + "Término: " + ChatColor.WHITE + dateFormat.format(season.getEndDate()));
        
        long durationMillis = season.getEndDate() - season.getStartDate();
        long durationDays = TimeUnit.MILLISECONDS.toDays(durationMillis);
        player.sendMessage(ChatColor.YELLOW + "Duração: " + ChatColor.WHITE + durationDays + " dias");
        
        String winner = season.getWinner();
        if (winner != null && !winner.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "🏆 Vencedor: " + ChatColor.GOLD + winner);
            
            int winnerPoints = getSeasonWinnerPoints(season, winner);
            if (winnerPoints > 0) {
                player.sendMessage(ChatColor.YELLOW + "Pontuação Final: " + ChatColor.WHITE + winnerPoints);
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + "Vencedor: " + ChatColor.GRAY + "Não definido");
        }
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
    }
}