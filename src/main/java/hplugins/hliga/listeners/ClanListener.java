package hplugins.hliga.listeners;

import hplugins.hliga.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * Listener principal para eventos relacionados a clãs/guildas
 */
public class ClanListener implements Listener {
    
    private final Main plugin;
    
    public ClanListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Manipula o evento de criação de clã do SimpleClans
     * Este método não usa a anotação @EventHandler para evitar erros de ClassNotFound
     * É chamado via reflection quando o SimpleClans está disponível
     */
    public void onSimpleClansCreate(Object event) {
        try {
            
            Class<?> eventClass = event.getClass();
            Method getClanMethod = eventClass.getMethod("getClan");
            Object clan = getClanMethod.invoke(event);
            
            Method getTagMethod = clan.getClass().getMethod("getTag");
            String clanTag = (String) getTagMethod.invoke(clan);
            
            
            assignInitialPointsToClan(clanTag);
            
            
            int initialPoints = plugin.getConfig().getInt("pontos.iniciais", 0);
            if (initialPoints > 0) {
                String pointsName = initialPoints == 1 ? 
                        plugin.getConfig().getString("pontos.nome", "ponto") : 
                        plugin.getConfig().getString("pontos.nome_plural", "pontos");
                
                Method getOnlineMembersMethod = clan.getClass().getMethod("getOnlineMembers");
                List<?> onlineMembers = (List<?>) getOnlineMembersMethod.invoke(clan);
                
                for (Object clanPlayer : onlineMembers) {
                    Method toPlayerMethod = clanPlayer.getClass().getMethod("toPlayer");
                    Player player = (Player) toPlayerMethod.invoke(clanPlayer);
                    
                    if (player != null) {
                        player.sendMessage(plugin.getConfigManager().getMessages().getMessage("pontos.adicionado_clan", 
                                "{pontos}", String.valueOf(initialPoints),
                                "{pontos_nome}", pointsName));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao processar evento de criação de clã do SimpleClans", e);
        }
    }
    
    /**
     * Manipula o evento de dissolução de clã do SimpleClans
     * Este método não usa a anotação @EventHandler para evitar erros de ClassNotFound
     * É chamado via reflection quando o SimpleClans está disponível
     */
    public void onSimpleClansDisband(Object event) {
        try {
            
            Class<?> eventClass = event.getClass();
            Method getClanMethod = eventClass.getMethod("getClan");
            Object clan = getClanMethod.invoke(event);
            
            Method getTagMethod = clan.getClass().getMethod("getTag");
            String clanTag = (String) getTagMethod.invoke(clan);
            
            
            onClanRemoved(clanTag);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao processar evento de dissolução de clã do SimpleClans", e);
        }
    }
    
    /**
     * Método auxiliar para atribuir pontos iniciais a um clã recém-criado
     */
    private void assignInitialPointsToClan(String clanTag) {
        FileConfiguration config = plugin.getConfig();
        int initialPoints = config.getInt("pontos.iniciais", 0);
        
        if (initialPoints > 0) {
            
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getPointsManager().setClanPoints(clanTag, initialPoints);
            });
        }
    }
    
    /**
     * Método auxiliar para remover os pontos de um clã quando ele é removido
     */
    public void onClanRemoved(String clanTag) {
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getPointsManager().removeClanPoints(clanTag);
        });
    }
}
