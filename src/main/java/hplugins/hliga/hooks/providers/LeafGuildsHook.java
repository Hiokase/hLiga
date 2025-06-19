package hplugins.hliga.hooks.providers;

import com.leafplugins.products.leafguilds.platform.commons.LeafGuildsAPI;
import com.leafplugins.products.leafguilds.platform.commons.objects.Guild;
import com.leafplugins.products.leafguilds.platform.commons.objects.Member;
import hplugins.hliga.Main;
import hplugins.hliga.hooks.BaseClanProvider;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.utils.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Implementação do provedor de clãs para o LeafGuilds
 */
public class LeafGuildsHook extends BaseClanProvider {

    private final Main plugin;
    private boolean initialized = false;
    private static final String PROVIDER_NAME = "LeafPlugins";
    LeafGuildsAPI leafGuildsAPI = LeafGuildsAPI.getApi();

    public LeafGuildsHook(Main plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        try {
            LogUtils.debug("LeafGuildsHook - initialize: Tentando inicializar hook do LeafGuilds...");
            boolean pluginPresent = Bukkit.getPluginManager().getPlugin("LeafPlugins") != null;
            LogUtils.debugMedium("LeafGuildsHook - initialize: Plugin LeafGuilds está carregado? " + pluginPresent);

            if (!pluginPresent) {
                LogUtils.debugMedium("LeafGuildsHook - initialize: LeafGuilds não encontrado. A integração não será carregada.");
                return;
            }
            if (leafGuildsAPI == null) {
               LogUtils.debugMedium("LeafGuildsHook - initialize: API do LeafGuilds não disponível. A integração não será carregada.");
                return;
            }

            initialized = true;
            plugin.getLogger().info("LeafGuildsHook - initialize: Integração com LeafGuilds inicializada com sucesso!");
            try {
                Collection<Guild> guilds = leafGuildsAPI.getStoredGuilds();
                int guildCount = guilds != null ? guilds.size() : 0;
                LogUtils.debugMedium("LeafGuildsHook - initialize: Número de guildas detectadas: " + guildCount);
            } catch (Exception e) {
                LogUtils.debugMedium("LeafGuildsHook - initialize: Erro ao obter guildas iniciais: " + e.getMessage());
            }

        } catch (Exception e) {
            LogUtils.debugMedium("LeafGuildsHook - initialize: Falha ao inicializar integração com LeafGuilds" + e);
            e.printStackTrace();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return initialized && leafGuildsAPI != null;
    }

    @Override
    public String getPlayerClanTag(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }

        try {
            Member member = leafGuildsAPI.getMember(player.getUniqueId());
            if (member != null) {
                Guild guild = member.getGuild();
                if (guild != null) {
                    return guild.getTag();
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                LogUtils.debugMedium("Erro ao obter tag da guilda do jogador: " + player.getName() + e);
            }
        }

        return null;
    }

    @Override
    public List<String> getAllClanTags() {
        if (!isAvailable()) {
            LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Hook não está disponível");
            return new ArrayList<>();
        }

        try {

            LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Obtendo todas as guildas...");

            Collection<Guild> guilds = null;

            try {
                guilds = leafGuildsAPI.getStoredGuilds();
                LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: getStoredGuilds() foi chamado com sucesso");
            } catch (Exception e) {
               LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Erro ao chamar getStoredGuilds(): " + e.getMessage());
                LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Tentando método alternativo...");
                return getGuildsFromOnlinePlayers();
            }

            if (guilds == null) {
                LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: A coleção de guildas retornou nula, tentando método alternativo");
                return getGuildsFromOnlinePlayers();
            }

            if (guilds.isEmpty()) {
                LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: A coleção de guildas está vazia, tentando método alternativo");
                List<String> alternativeTags = getGuildsFromOnlinePlayers();
                if (!alternativeTags.isEmpty()) {
                    return alternativeTags;
                }
            }

            plugin.getLogger().info("LeafGuildsHook - getAllClanTags: Número de guildas encontradas: " + guilds.size());

            List<String> tags = new ArrayList<>();
            for (Guild guild : guilds) {
                if (guild == null) {
                    LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Encontrada uma guilda nula");
                    continue;
                }

                String tag = guild.getTag();
                if (tag != null && !tag.isEmpty()) {
                    tags.add(tag);
                    LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Adicionada guilda com tag: " + tag);
                } else {
                    LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Guilda sem tag válida");
                }
            }

            
            if (tags.isEmpty()) {
                LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Nenhuma tag válida encontrada, tentando método alternativo");
                return getGuildsFromOnlinePlayers();
            }

            LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Total de tags válidas: " + tags.size());
            return tags;
        } catch (Exception e) {
           LogUtils.debugMedium( "Erro ao obter todas as guildas" + e);
            e.printStackTrace();


            LogUtils.debugMedium("LeafGuildsHook - getAllClanTags: Devido ao erro, tentando método alternativo");
            return getGuildsFromOnlinePlayers();
        }
    }

    /**
     * Método alternativo para obter todas as guildas
     * Usamos jogadores online para tentar encontrar guildas
     */
    private List<String> getGuildsFromOnlinePlayers() {
        LogUtils.debugMedium("LeafGuildsHook - getGuildsFromOnlinePlayers: Tentando obter guildas através de jogadores online");
        List<String> tags = new ArrayList<>();

        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    Member member = leafGuildsAPI.getMember(player.getUniqueId());
                    if (member != null) {
                        Guild guild = member.getGuild();
                        if (guild != null) {
                            String tag = guild.getTag();
                            if (tag != null && !tag.isEmpty() && !tags.contains(tag)) {
                                tags.add(tag);
                                LogUtils.debugMedium("LeafGuildsHook - getGuildsFromOnlinePlayers: Adicionada guilda: " + tag);
                            }
                        }
                    }
                } catch (Exception e) {
                   LogUtils.debugMedium("LeafGuildsHook - getGuildsFromOnlinePlayers: Erro ao processar jogador " + player.getName() + ": " + e.getMessage());
                }
            }

            LogUtils.debugMedium("LeafGuildsHook - getGuildsFromOnlinePlayers: Total de guildas encontradas: " + tags.size());
        } catch (Exception e) {
           LogUtils.debugMedium("LeafGuildsHook - getGuildsFromOnlinePlayers: Erro geral"+ e);
        }

        return tags;
    }

    @Override
    public List<GenericClan> getAllClans() {
        if (!isAvailable()) {
            return new ArrayList<>();
        }

        List<GenericClan> clans = new ArrayList<>();

        try {
            Collection<Guild> guilds = leafGuildsAPI.getStoredGuilds();

            if (guilds == null || guilds.isEmpty()) {
                
                List<String> guildTags = getGuildsFromOnlinePlayers();
                for (String tag : guildTags) {
                    Guild guild = getGuildByTag(tag);
                    if (guild != null) {
                        GenericClan genericClan = convertToGenericClan(guild);
                        if (genericClan != null) {
                            clans.add(genericClan);
                        }
                    }
                }
            } else {
                
                for (Guild guild : guilds) {
                    GenericClan genericClan = convertToGenericClan(guild);
                    if (genericClan != null) {
                        clans.add(genericClan);
                    }
                }
            }
        } catch (Exception e) {
           LogUtils.debugMedium("Erro ao obter todos os clãs do LeafGuilds"+ e);
        }

        return clans;
    }

    @Override
    public GenericClan getPlayerClan(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }

        try {
            Member member = leafGuildsAPI.getMember(player.getUniqueId());
            if (member != null) {
                Guild guild = member.getGuild();
                if (guild != null) {
                    return convertToGenericClan(guild);
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
               LogUtils.debugMedium("Erro ao obter guilda do jogador: " + player.getName() + e);
            }
        }

        return null;
    }

    /**
     * Obtém uma guilda pelo ID/Tag
     */
    private Guild getGuildByTag(String tag) {
        if (!isAvailable() || tag == null || tag.isEmpty()) {
            return null;
        }

        try {
            
            Guild guild = leafGuildsAPI.getGuild(tag);
            if (guild != null) {
                return guild;
            }

            
            Collection<Guild> guilds = leafGuildsAPI.getStoredGuilds();
            if (guilds != null) {
                for (Guild g : guilds) {
                    String guildTag = g.getTag();
                    if (tag.equalsIgnoreCase(guildTag)) {
                        return g;
                    }
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
               LogUtils.debugMedium("Erro ao obter guilda pela tag: " + tag+ e);
            }
        }

        return null;
    }

    @Override
    public GenericClan getClan(String tag) {
        if (!isAvailable() || tag == null || tag.isEmpty()) {
            return null;
        }

        Guild guild = getGuildByTag(tag);
        return guild != null ? convertToGenericClan(guild) : null;
    }

    @Override
    public boolean isPlayerLeader(Player player) {
        if (!isAvailable() || player == null) {
            return false;
        }

        try {
            Member member = leafGuildsAPI.getMember(player.getUniqueId());
            if (member != null && member.getRole() != null) {
                try {
                    
                    return member.getRole().getEnum().name().equalsIgnoreCase("LEADER");
                } catch (Exception e) {
                    
                    try {
                        return member.getGuild().getLeader().isOnline();
                    } catch (Exception ex) {
                       LogUtils.debugMedium( "Erro ao verificar se jogador é líder usando ambos os métodos"+ ex);
                    }
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
               LogUtils.debugMedium("Erro ao verificar se jogador é líder: " + player.getName()+ e);
            }
        }

        return false;
    }

    @Override
    public List<UUID> getClanMembers(String tag) {
        if (!isAvailable() || tag == null || tag.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Guild guild = getGuildByTag(tag);
            if (guild != null) {
                Collection<Member> members = guild.getMembers();
                List<UUID> uuids = new ArrayList<>();

                for (Member member : members) {
                    if (member != null) {
                        UUID uuid = member.getUniqueId();
                        if (uuid != null) {
                            uuids.add(uuid);
                        }
                    }
                }

                return uuids;
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
               LogUtils.debugMedium( "Erro ao obter membros da guilda: " + tag+ e);
            }
        }

        return new ArrayList<>();
    }

    @Override
    public String getClanLeaderName(String tag) {
        if (!isAvailable() || tag == null || tag.isEmpty()) {
            return null;
        }

        try {
            Guild guild = getGuildByTag(tag);
            if (guild != null) {
                Member leader = guild.getLeader();
                if (leader != null) {
                    return leader.getName();
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
               LogUtils.debugMedium( "Erro ao obter líder da guilda: " + tag + e);
            }
        }

        return null;
    }

    @Override
    public boolean supportsClanCreation() {
        return false; 
    }

    @Override
    public boolean supportsClanDissolution() {
        return false; 
    }

    @Override
    public boolean supportsMemberEditing() {
        return false; 
    }

    @Override
    public GenericClan convertToGenericClan(Object nativeObject) {
        if (nativeObject == null) {
            return null;
        }

        try {
            Guild guild = (Guild) nativeObject;

            
            List<UUID> memberUUIDs = new ArrayList<>();
            Collection<Member> members = guild.getMembers();

            for (Member member : members) {
                UUID uuid = member.getUniqueId();
                memberUUIDs.add(uuid);
            }

            
            List<Player> onlineMembers = new ArrayList<>();
            Collection<Member> onlineMemberObjects = guild.getOnlineMembers();

            for (Member member : onlineMemberObjects) {
                if (member.isOnline()) {
                    UUID uuid = member.getUniqueId();
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        onlineMembers.add(player);
                    }
                }
            }

            
            String leaderName = null;
            Member leader = guild.getLeader();
            if (leader != null) {
                leaderName = leader.getName();
            }

            
            String tag = guild.getTag();
            String name = guild.getName();
            String coloredTag;

            try {
                coloredTag = guild.getColorTag();
                if (coloredTag == null || coloredTag.isEmpty()) {
                    coloredTag = tag;
                }
            } catch (Exception e) {
                coloredTag = tag;
            }

            
            return new GenericClan(
                    tag, 
                    name, 
                    coloredTag, 
                    memberUUIDs, 
                    onlineMembers, 
                    leaderName, 
                    guild, 
                    PROVIDER_NAME 
            );
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
               LogUtils.debugMedium("Erro ao converter guilda para GenericClan"+ e);
            }
            return null;
        }
    }

    @Override
    public boolean clanExists(String clanTag) {
        if (!isAvailable()) {
            return false;
        }

        try {
            Guild guild = getGuildByTag(clanTag);
            return guild != null;
        } catch (Exception e) {
           LogUtils.debugMedium( "Erro ao verificar existência da guilda: " + clanTag+ e);
            return false;
        }
    }

    @Override
    public String getClanName(String clanTag) {
        if (!isAvailable()) {
            return clanTag;
        }

        try {
            Guild guild = getGuildByTag(clanTag);
            if (guild != null) {
                return guild.getName();
            }
            return clanTag;
        } catch (Exception e) {
           LogUtils.debugMedium( "Erro ao obter nome da guilda: " + clanTag+ e);
            return clanTag;
        }
    }

    @Override
    public String getColoredClanTag(String clanTag) {
        if (!isAvailable()) {
            return clanTag;
        }

        try {
            Guild guild = getGuildByTag(clanTag);
            if (guild != null) {
                
                try {
                    String colorTag = guild.getColorTag();
                    if (colorTag != null && !colorTag.isEmpty()) {
                        return colorTag;
                    }
                } catch (Exception e) {
                   LogUtils.debugMedium( "Erro ao obter colorTag: " + e.getMessage());
                }

                
                return guild.getTag();
            }
            return clanTag;
        } catch (Exception e) {
           LogUtils.debugMedium( "Erro ao obter tag colorida da guilda: " + clanTag+ e);
            return clanTag;
        }
    }

    @Override
    public int getClanMemberCount(String clanTag) {
        if (!isAvailable()) {
            return 0;
        }

        try {
            Guild guild = getGuildByTag(clanTag);
            if (guild != null) {
                Collection<Member> members = guild.getMembers();
                return members != null ? members.size() : 0;
            }
            return 0;
        } catch (Exception e) {
           LogUtils.debugMedium( "Erro ao obter número de membros da guilda: " + clanTag+ e);
            return 0;
        }
    }

    @Override
    public List<Player> getOnlineClanMembers(String clanTag) {
        if (!isAvailable()) {
            return new ArrayList<>();
        }

        try {
            Guild guild = getGuildByTag(clanTag);
            if (guild == null) {
                return new ArrayList<>();
            }

            Collection<Member> onlineMembers = guild.getOnlineMembers();
            if (onlineMembers == null) {
                return new ArrayList<>();
            }

            List<Player> players = new ArrayList<>();
            for (Member member : onlineMembers) {
                if (member == null) continue;

                if (member.isOnline()) {
                    UUID uuid = member.getUniqueId();
                    if (uuid != null) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            players.add(player);
                        }
                    }
                }
            }

            return players;
        } catch (Exception e) {
           LogUtils.debugMedium( "Erro ao obter membros online da guilda: " + clanTag+ e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isPlayerInClan(Player player) {
        if (!isAvailable() || player == null) {
            return false;
        }

        try {
            Member member = leafGuildsAPI.getMember(player.getUniqueId());
            if (member != null) {
                Guild guild = member.getGuild();
                return guild != null;
            }
        } catch (Exception e) {
           LogUtils.debugMedium( "Erro ao verificar se jogador está em guilda: " + player.getName()+ e);
        }

        return false;
    }
}