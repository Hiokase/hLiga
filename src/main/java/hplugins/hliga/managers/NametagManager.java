package hplugins.hliga.managers;

import hplugins.hliga.Main;
import hplugins.hliga.utils.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Gerenciador de nametags para exibir tags acima da cabeça dos jogadores
 */
public class NametagManager implements Listener {
    
    private final Main plugin;
    private final Map<UUID, String> playerNametags = new HashMap<>();
    private final Scoreboard scoreboard;
    
    public NametagManager(Main plugin) {
        this.plugin = plugin;
        this.scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        LogUtils.debug("NametagManager inicializado");
    }
    
    /**
     * Atualiza a nametag de um jogador
     */
    public void updatePlayerNametag(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        try {
            String tag = plugin.getTagManager().getPlayerActiveTag(player.getUniqueId());
            String currentTag = playerNametags.get(player.getUniqueId());
            
            
            if ((tag == null || tag.isEmpty()) && (currentTag == null || currentTag.isEmpty())) {
                return;
            }
            if (tag != null && tag.equals(currentTag)) {
                return;
            }
            
            
            removeFromAllTeams(player);
            
            
            if (tag != null && !tag.isEmpty()) {
                String teamName = generateUniqueTeamName(player);
                Team team = scoreboard.getTeam(teamName);
                
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                }
                
                
                String cleanTag = ChatColor.translateAlternateColorCodes('&', tag);
                if (cleanTag.length() > 16) {
                    cleanTag = cleanTag.substring(0, 16); 
                }
                
                team.setPrefix(cleanTag + " ");
                team.addEntry(player.getName());
                
                playerNametags.put(player.getUniqueId(), tag);
                LogUtils.debug("Nametag atualizada para " + player.getName() + ": " + cleanTag);
            } else {
                playerNametags.remove(player.getUniqueId());
                LogUtils.debug("Nametag removida para " + player.getName());
            }
            
        } catch (Exception e) {
            LogUtils.error("Erro ao atualizar nametag do jogador " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Remove jogador de todas as equipes
     */
    private void removeFromAllTeams(Player player) {
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
                
                
                if (team.getSize() == 0 && team.getName().startsWith("hl_")) {
                    team.unregister();
                }
            }
        }
    }
    
    /**
     * Atualiza nametags de todos os jogadores online
     */
    public void updateAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerNametag(player);
        }
        LogUtils.debug("Nametags atualizadas para todos os jogadores online");
    }
    
    /**
     * Limpa todas as nametags do plugin
     */
    public void clearAllNametags() {
        
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("hliga_")) {
                team.unregister();
            }
        }
        
        playerNametags.clear();
        LogUtils.debug("Todas as nametags foram limpas");
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updatePlayerNametag(event.getPlayer());
        }, 20L);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeFromAllTeams(player);
        playerNametags.remove(player.getUniqueId());
    }
    
    /**
     * Gera um nome único para a equipe respeitando o limite de 16 caracteres
     */
    private String generateUniqueTeamName(Player player) {
        String playerName = player.getName().toLowerCase();
        String prefix = "hl_";
        
        
        int maxNameLength = 16 - prefix.length();
        
        if (playerName.length() <= maxNameLength) {
            return prefix + playerName;
        } else {
            
            String truncatedName = playerName.substring(0, maxNameLength - 4);
            int hash = Math.abs(player.getUniqueId().hashCode()) % 1000;
            return prefix + truncatedName + String.format("%03d", hash);
        }
    }
    
    public void disable() {
        clearAllNametags();
        LogUtils.debug("NametagManager desabilitado");
    }
}