package hplugins.hliga.hooks;

import hplugins.hliga.Main;
import hplugins.hliga.hooks.providers.LeafGuildsHook;
import hplugins.hliga.hooks.providers.NullClanProvider;
import hplugins.hliga.hooks.providers.SimpleClansHook;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.utils.LogUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gerenciador central de todos os provedores de clãs
 * Esta classe detecta automaticamente qual provedor está disponível e o utiliza
 * Com a abstração GenericClan, é possível usar qualquer plugin de clãs de forma transparente
 */
public class ClansManager {

    private final Main plugin;
    private final Map<String, ClanProvider> providers = new HashMap<>();
    private ClanProvider activeProvider = null;

    public ClansManager(Main plugin) {
        this.plugin = plugin;
        registerProviders();
        selectProvider();
    }

    /**
     * Registra todos os provedores de clãs disponíveis
     */
    private void registerProviders() {
        registerProvider(new SimpleClansHook(plugin));

        registerProvider(new LeafGuildsHook(plugin));

        // Por exemplo: registerProvider(new SuperClansHook(plugin));
    }

    /**
     * Registra um provedor de clãs
     *
     * @param provider Provedor a ser registrado
     */
    public void registerProvider(ClanProvider provider) {
        providers.put(provider.getProviderName().toLowerCase(), provider);
    }

    /**
     * Seleciona o provedor ativo com base na disponibilidade e configuração
     */
    private void selectProvider() {
        String preferredProvider = plugin.getConfig().getString("clans.preferido", "auto").toLowerCase();

        List<String> availableProviders = new ArrayList<>();
        for (ClanProvider provider : providers.values()) {
            if (provider.isAvailable()) {
                availableProviders.add(provider.getProviderName());
            }
        }

        if (!availableProviders.isEmpty()) {
            sendConsoleMessage("&7Provedores: &f" + String.join("&7, &f", availableProviders));
        }

        if (!preferredProvider.equals("auto")) {
            for (ClanProvider provider : providers.values()) {
                try {
                    boolean available = provider.isAvailable();

                    if (provider.getProviderName().toLowerCase().equals(preferredProvider)) {
                        if (available) {
                            activeProvider = provider;
                            sendConsoleMessage("&aIntegração com &f" + provider.getProviderName() + " &aativada com sucesso!");
                            sendConsoleMessage("&ePlugins e recursos relacionados a clãs estarão disponíveis.");
                            return;
                        } else {
                            sendConsoleMessage("&eProvedor preferido &f" + provider.getProviderName() +
                                    "&e encontrado, mas não está disponível. Verificando alternativas...");
                        }
                    }
                } catch (Exception e) {
                    sendConsoleMessage("&cErro ao verificar disponibilidade do provedor &f" +
                            provider.getProviderName() + "&c: " + e.getMessage());
                }
            }
            sendConsoleMessage("&eProvedor de clãs preferido &f'" + preferredProvider +
                    "'&e não está disponível. Buscando alternativas...");
        }

        for (ClanProvider provider : providers.values()) {
            try {
                boolean available = provider.isAvailable();

                if (available) {
                    activeProvider = provider;
                    sendConsoleMessage("&a✓ " + provider.getProviderName() + " integrado");
                    return;
                }
            } catch (Exception e) {
                sendConsoleMessage("&cErro ao verificar disponibilidade do provedor &f" +
                        provider.getProviderName() + "&c: " + e.getMessage());
            }
        }

        sendConsoleMessage("&cNenhum plugin de clãs compatível encontrado! &fFuncionalidades relacionadas a clãs estarão indisponíveis.");
        sendConsoleMessage("&ePlugins de clãs suportados: &fSimpleClans, LeafGuilds");

        activeProvider = new NullClanProvider(plugin);
    }

    /**
     * Envia mensagem colorida para o console
     */
    private void sendConsoleMessage(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(colorize("&8[&2hLiga&8] " + message));
    }

    /**
     * Transforma códigos de cores (&) em cores
     */
    private String colorize(String text) {
        return text.replace('&', '§');
    }

    /**
     * Obtém o provedor ativo
     *
     * @return Provedor ativo ou NullClanProvider se nenhum estiver disponível
     */
    public ClanProvider getActiveProvider() {
        return activeProvider;
    }

    /**
     * Verifica se algum provedor de clãs está disponível
     *
     * @return true se há um provedor disponível, false caso contrário
     */
    public boolean hasClanProvider() {
        return !(activeProvider instanceof NullClanProvider);
    }

    /**
     * Alias para hasClanProvider() por questões de compatibilidade
     *
     * @return true se há um provedor disponível, false caso contrário
     */
    public boolean hasAvailableProvider() {
        return hasClanProvider();
    }

    /**
     * Obtém um provedor específico pelo nome
     *
     * @param name Nome do provedor
     * @return Provedor solicitado ou null se não existir
     */
    public ClanProvider getProvider(String name) {
        return providers.get(name.toLowerCase());
    }

    /**
     * Obtém todos os provedores registrados
     *
     * @return Lista de todos os provedores
     */
    public List<ClanProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }

    /*
     * Métodos de conveniência que delegam para o provedor ativo
     */

    /**
     * Obtém um clã pelo seu tag como objeto GenericClan
     *
     * @param tag Tag do clã
     * @return Objeto GenericClan ou null se não existir
     */
    public GenericClan getClan(String tag) {
        try {
            return activeProvider.getClan(tag);
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter clã por tag: " + tag, e);
            return null;
        }
    }

    /**
     * Alias para getClan() por questões de compatibilidade
     *
     * @param tag Tag do clã
     * @return Objeto GenericClan ou null se não existir
     */
    public GenericClan getClanByTag(String tag) {
        return getClan(tag);
    }

    /**
     * Obtém o clã de um jogador como objeto GenericClan
     *
     * @param player Jogador
     * @return Objeto GenericClan ou null se o jogador não estiver em um clã
     */
    public GenericClan getPlayerClan(Player player) {
        try {
            return activeProvider.getPlayerClan(player);
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter clã do jogador: " + player.getName(), e);
            return null;
        }
    }

    /**
     * Obtém a tag do clã de um jogador
     *
     * @param player Jogador
     * @return Tag do clã ou null se o jogador não estiver em um clã
     */
    public String getPlayerClanTag(Player player) {
        try {
            return activeProvider.getPlayerClanTag(player);
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter tag do clã do jogador: " + player.getName(), e);
            return null;
        }
    }

    /**
     * Verifica se um clã existe pelo tag
     *
     * @param tag Tag do clã
     * @return true se o clã existir, false caso contrário
     */
    public boolean clanExists(String tag) {
        try {
            return activeProvider.clanExists(tag);
        } catch (Exception e) {
            LogUtils.warning("Erro ao verificar existência do clã: " + tag, e);
            return false;
        }
    }

    /**
     * Obtém o nome completo de um clã
     *
     * @param tag Tag do clã
     * @return Nome completo do clã ou a própria tag se o clã não existir
     */
    public String getClanName(String tag) {
        try {
            return activeProvider.getClanName(tag);
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter nome do clã: " + tag, e);
            return tag;
        }
    }

    /**
     * Obtém a tag colorida de um clã
     *
     * @param tag Tag do clã
     * @return Tag colorida do clã ou a própria tag se o clã não existir
     */
    public String getColoredClanTag(String tag) {
        try {
            return activeProvider.getColoredClanTag(tag);
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter tag colorida do clã: " + tag, e);
            return tag;
        }
    }

    /**
     * Obtém o número de membros em um clã
     *
     * @param tag Tag do clã
     * @return Número de membros ou 0 se o clã não existir
     */
    public int getClanMemberCount(String tag) {
        try {
            return activeProvider.getClanMemberCount(tag);
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter quantidade de membros do clã: " + tag, e);
            return 0;
        }
    }

    /**
     * Obtém todos os jogadores online de um clã
     *
     * @param tag Tag do clã
     * @return Lista de jogadores online ou lista vazia se o clã não existir
     */
    public List<Player> getOnlineClanMembers(String tag) {
        try {
            return activeProvider.getOnlineClanMembers(tag);
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter membros online do clã: " + tag, e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtém todos os tags de clãs/guildas disponíveis
     *
     * @return Lista de tags de clãs
     */
    public List<String> getAllClanTags() {
        try {
            return activeProvider.getAllClanTags();
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter tags de todos os clãs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtém todos os clãs disponíveis
     *
     * @return Lista de objetos GenericClan
     */
    public List<GenericClan> getAllClans() {
        try {
            return activeProvider.getAllClans();
        } catch (Exception e) {
            LogUtils.warning("Erro ao obter todos os clãs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Verifica se um jogador pertence a um clã
     *
     * @param player Jogador
     * @return true se o jogador pertencer a um clã, false caso contrário
     */
    public boolean isPlayerInClan(Player player) {
        try {
            return activeProvider.isPlayerInClan(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao verificar se jogador está em um clã", e);
            return false;
        }
    }

    /**
     * Obtém todos os membros de um clã (UUIDs)
     *
     * @param tag Tag do clã
     * @return Lista de UUIDs dos membros ou lista vazia se o clã não existir
     */
    public List<UUID> getClanMembers(String tag) {
        try {
            return activeProvider.getClanMembers(tag);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter membros do clã", e);
            return new ArrayList<>();
        }
    }



    /**
     * Verifica se um jogador é líder de um clã
     *
     * @param player Jogador
     * @return true se o jogador for líder, false caso contrário
     */
    public boolean isPlayerLeader(Player player) {
        try {
            return activeProvider.isPlayerLeader(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao verificar se jogador é líder", e);
            return false;
        }
    }

    /**
     * Obtém o nome do líder de um clã
     *
     * @param tag Tag do clã
     * @return Nome do líder ou null se o clã não existir
     */
    public String getClanLeaderName(String tag) {
        try {
            return activeProvider.getClanLeaderName(tag);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter nome do líder do clã", e);
            return null;
        }
    }

    /**
     * Verifica se o provedor ativo suporta criação de clãs via API
     *
     * @return true se a criação for suportada, false caso contrário
     */
    public boolean supportsClanCreation() {
        try {
            return activeProvider.supportsClanCreation();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao verificar suporte para criação de clãs", e);
            return false;
        }
    }

    /**
     * Sincroniza os clãs do provedor ativo com o banco de dados do hLiga
     */
    public void syncClansWithDatabase() {
        try {
            List<String> allClanTags = getAllClanTags();

            LogUtils.debugHigh("Sincronizando " + allClanTags.size() + " clãs do provedor " + activeProvider.getProviderName());

            int syncCount = 0;

            for (String clanTag : allClanTags) {
                int currentPoints = plugin.getPointsManager().getClanPoints(clanTag);

                if (!plugin.getDatabaseManager().getAdapter().clanExists(clanTag)) {
                    plugin.getPointsManager().setClanPoints(clanTag, 0);
                    syncCount++;
                    LogUtils.debug("Clã sincronizado com 0 pontos: " + clanTag);
                }
            }

            LogUtils.debug("Sincronização concluída - " + syncCount + " clãs adicionados ao banco");

            if (allClanTags.isEmpty()) {
                LogUtils.debugMedium("Nenhum clã encontrado para sincronizar. Verificando provedor ativo: " + activeProvider.getProviderName());

                if (activeProvider.getProviderName().equals("LeafGuilds")) {
                    sendConsoleMessage("&eDica: Verifique se o plugin LeafGuilds está carregado corretamente e se há clãs criados.");
                } else if (activeProvider.getProviderName().equals("SimpleClans")) {
                    sendConsoleMessage("&eDica: Verifique se o plugin SimpleClans está carregado corretamente e se há clãs criados.");
                }
            }

            if (syncCount > 0 || plugin.getConfig().getBoolean("debug", false)) {
                sendConsoleMessage("&aSincronização concluída: &f" + syncCount + " &anovos clãs adicionados.");
            }
        } catch (Exception e) {
            sendConsoleMessage("&cErro ao sincronizar clãs com o banco de dados: &f" + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
}