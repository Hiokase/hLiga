package hplugins.hliga.hooks;

import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.TagType;
import hplugins.hliga.utils.TimeUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Plugin de integração com o PlaceholderAPI para fornecer placeholders
 * Atualizado para usar nossa nova abstração GenericClan
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    
    private final Main plugin;
    
    public PlaceholderAPIHook(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "hliga";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }
        
        
        if (identifier.equals("tag_ranking") || identifier.equals("tag_permanentes")) {
            if (plugin.getTagManager() == null || !plugin.getTagManager().isSystemEnabled()) {
                return "";
            }
            
            
            if (!plugin.getTagManager().isTagsEnabledForPlayer(player.getUniqueId())) {
                return "";
            }
            
            try {
                if (identifier.equals("tag_ranking")) {
                    
                    return plugin.getTagManager().getPlayerRankingTag(player.getUniqueId());
                    
                } else if (identifier.equals("tag_permanentes")) {
                    
                    return plugin.getTagManager().getPlayerPermanentTag(player.getUniqueId());
                }
            } catch (Exception e) {
                return "";
            }
        }
        
        
        if (identifier.equals("clan_points")) {
            if (player == null || !player.isOnline()) {
                return "0";
            }
            
            Player onlinePlayer = player.getPlayer();
            GenericClan clan = plugin.getClansManager().getPlayerClan(onlinePlayer);
            
            if (clan == null) {
                return "0";
            }
            
            String clanTag = clan.getTag();
            
            return String.valueOf(plugin.getPointsManager().getClanPoints(clanTag));
        }
        
        
        if (identifier.startsWith("top_")) {
            try {
                int position = Integer.parseInt(identifier.substring(4)) - 1;
                if (position < 0) {
                    return "";
                }
                
                List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(position + 1);
                
                if (topClans.size() <= position) {
                    return "";
                }
                
                ClanPoints clanPoints = topClans.get(position);
                return clanPoints.clanTag;
            } catch (NumberFormatException e) {
                return "";
            }
        }
        
        
        if (identifier.startsWith("top_points_")) {
            try {
                int position = Integer.parseInt(identifier.substring(11)) - 1;
                if (position < 0) {
                    return "";
                }
                
                List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(position + 1);
                
                if (topClans.size() <= position) {
                    return "";
                }
                
                ClanPoints clanPoints = topClans.get(position);
                return String.valueOf(clanPoints.points);
            } catch (NumberFormatException e) {
                return "";
            }
        }
        
        
        if (identifier.equals("season_active")) {
            return plugin.getSeasonManager().isSeasonActive() ? "true" : "false";
        }
        
        
        if (identifier.equals("season_name")) {
            Optional<Season> optionalSeason = plugin.getSeasonManager().getActiveSeason();
            return optionalSeason.isPresent() ? optionalSeason.get().name : "";
        }
        
        
        if (identifier.equals("season_start")) {
            Optional<Season> optionalSeason = plugin.getSeasonManager().getActiveSeason();
            return optionalSeason.isPresent() ? TimeUtils.formatDate(optionalSeason.get().startDate) : "";
        }
        
        
        if (identifier.equals("season_end")) {
            Optional<Season> optionalSeason = plugin.getSeasonManager().getActiveSeason();
            return optionalSeason.isPresent() ? TimeUtils.formatDate(optionalSeason.get().endDate) : "";
        }
        
        
        if (identifier.equals("season_remaining")) {
            Optional<Season> optionalSeason = plugin.getSeasonManager().getActiveSeason();
            if (!optionalSeason.isPresent()) {
                return "";
            }
            
            Season season = optionalSeason.get();
            long remaining = season.endDate - System.currentTimeMillis();
            
            if (remaining <= 0) {
                return "Encerrada";
            }
            
            return TimeUtils.formatTimeLeft(remaining);
        }
        
        
        if (identifier.equals("clan_position")) {
            if (player == null || !player.isOnline()) {
                return "";
            }
            
            Player onlinePlayer = player.getPlayer();
            GenericClan clan = plugin.getClansManager().getPlayerClan(onlinePlayer);
            
            if (clan == null) {
                return "";
            }
            
            String clanTag = clan.getTag();
            
            List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(Integer.MAX_VALUE);
            
            for (int i = 0; i < topClans.size(); i++) {
                if (topClans.get(i).clanTag.equals(clanTag)) {
                    return String.valueOf(i + 1);
                }
            }
            
            return "";
        }
        
        
        if (identifier.equals("provider_name")) {
            return plugin.getClansManager().getActiveProvider().getProviderName();
        }
        
        
        if (identifier.equals("clan_leader")) {
            if (player == null || !player.isOnline()) {
                return "";
            }
            
            Player onlinePlayer = player.getPlayer();
            GenericClan clan = plugin.getClansManager().getPlayerClan(onlinePlayer);
            
            if (clan == null) {
                return "";
            }
            
            return clan.getLeaderName() != null ? clan.getLeaderName() : "";
        }
        
        
        if (identifier.equals("clan_members")) {
            if (player == null || !player.isOnline()) {
                return "0";
            }
            
            Player onlinePlayer = player.getPlayer();
            GenericClan clan = plugin.getClansManager().getPlayerClan(onlinePlayer);
            
            if (clan == null) {
                return "0";
            }
            
            return String.valueOf(clan.getMemberCount());
        }
        
        
        if (identifier.equals("clan_online")) {
            if (player == null || !player.isOnline()) {
                return "0";
            }
            
            Player onlinePlayer = player.getPlayer();
            GenericClan clan = plugin.getClansManager().getPlayerClan(onlinePlayer);
            
            if (clan == null) {
                return "0";
            }
            
            return String.valueOf(clan.getOnlineMemberCount());
        }
        
        return null;
    }
    

}
