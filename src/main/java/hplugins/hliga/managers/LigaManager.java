package hplugins.hliga.managers;

import hplugins.hliga.Main;
import hplugins.hliga.hooks.DiscordWebhook;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.models.Season;
import hplugins.hliga.utils.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LigaManager {

    private final Main plugin;

    public LigaManager(Main plugin) {
        this.plugin = plugin;
    }
    private DiscordWebhook discordWebhook;

    /**
     * Obtém a instância do webhook do Discord, inicializando se necessário
     *
     * @return Instância do webhook
     */
    public DiscordWebhook getDiscordWebhook() {
        if (discordWebhook == null) {
            discordWebhook = new DiscordWebhook(plugin);
        }
        return discordWebhook;
    }

    /**
     * Envia notificação para o Discord quando pontos são adicionados a um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos adicionados
     * @param totalPoints Total de pontos do clã
     */
    public void sendDiscordPointsNotification(String clanTag, int points, int totalPoints) {
        sendDiscordPointsNotification(clanTag, points, totalPoints, null);
    }

    /**
     * Envia notificação para o Discord quando pontos são adicionados a um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos adicionados
     * @param totalPoints Total de pontos do clã
     * @param description Descrição opcional da operação
     */
    public void sendDiscordPointsNotification(String clanTag, int points, int totalPoints, String description) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("discord.ativado", true) ||
                !config.getBoolean("discord.anunciar_pontos", true)) {
            return;
        }

        if (points > 0 && points < config.getInt("discord.minimo_pontos_anuncio", 100)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getDiscordWebhook().sendClanPointsNotification(clanTag, points, totalPoints, description);
        });
    }

    /**
     * Envia notificação para o Discord quando uma temporada é iniciada
     *
     * @param season Temporada iniciada
     */
    public void sendDiscordSeasonStart(Season season) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("discord.ativado", true) ||
                !config.getBoolean("discord.anunciar_temporadas", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getDiscordWebhook().sendSeasonStartNotification(season);
        });
    }

    /**
     * Envia notificação para o Discord quando uma temporada é encerrada
     *
     * @param season Temporada encerrada
     * @param allClans Lista de todos os clãs (será filtrada internamente)
     */
    public void sendDiscordSeasonEnd(Season season, List<ClanPoints> allClans) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("discord.ativado", true) ||
                !config.getBoolean("discord.anunciar_temporadas", true)) {
            LogUtils.debug("Discord desabilitado ou notificações de temporada desabilitadas");
            return;
        }

        if (season == null) {
            LogUtils.warning("Temporada nula ao tentar enviar notificação Discord");
            return;
        }

        List<ClanPoints> validClans = new ArrayList<>();
        if (allClans != null) {
            for (ClanPoints clan : allClans) {
                if (clan != null && clan.getPoints() > 0) {
                    validClans.add(clan);
                }
            }
        }

        int topLimit = config.getInt("discord.top_resultados", 5);
        List<ClanPoints> topClans = new ArrayList<>();

        for (int i = 0; i < Math.min(topLimit, validClans.size()); i++) {
            topClans.add(validClans.get(i));
        }

        if (topClans.isEmpty()) {
            LogUtils.info("Nenhum clã com pontos válidos - enviando notificação Discord de fim de temporada sem ganhadores");
        } else {
            LogUtils.info("Enviando webhook Discord de fim de temporada com " + topClans.size() + " clãs válidos");
        }

        final List<ClanPoints> finalTopClans = topClans;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = getDiscordWebhook().sendSeasonEndNotification(season, finalTopClans);
            if (success) {
                LogUtils.debug("Webhook Discord de fim de temporada enviado com sucesso");
            } else {
                LogUtils.warning("Erro ao enviar webhook Discord de fim de temporada");
            }
        });
    }

    /**
     * Verifica se um clã está entre os vencedores de uma temporada
     *
     * @param clanTag Tag do clã
     * @param position Posição desejada (1 = primeiro, 2 = segundo, etc.)
     * @return true se o clã estiver na posição especificada, false caso contrário
     */
    public boolean isClanInPosition(String clanTag, int position) {
        if (position <= 0) {
            return false;
        }

        List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(position);

        if (topClans.size() < position) {
            return false;
        }

        ClanPoints clanPoints = topClans.get(position - 1);
        return clanPoints.getClanTag().equals(clanTag);
    }

    /**
     * Obtém o prêmio configurado para uma posição
     *
     * @param position Posição (1 = primeiro, 2 = segundo, etc.)
     * @return Lista de comandos para executar ou uma lista vazia se não houver prêmio
     */
    public List<String> getRewardCommands(int position) {
        FileConfiguration config = plugin.getConfig();
        return config.getStringList("premiacao.recompensas." + position);
    }

    /**
     * Verifica se um clã existe
     *
     * @param clanTag Tag do clã
     * @return true se o clã existir, false caso contrário
     */
    public boolean clanExists(String clanTag) {
        return plugin.getClansManager().clanExists(clanTag);
    }

    /**
     * Obtém o nome completo de um clã
     *
     * @param clanTag Tag do clã
     * @return Nome completo do clã ou a própria tag se o clã não existir
     */
    public String getClanName(String clanTag) {
        return plugin.getClansManager().getClanName(clanTag);
    }

    /**
     * Obtém a tag colorida de um clã
     *
     * @param clanTag Tag do clã
     * @return Tag colorida do clã ou a própria tag se o clã não existir
     */
    public String getColoredClanTag(String clanTag) {
        return plugin.getClansManager().getColoredClanTag(clanTag);
    }

    /**
     * Obtém o clan a partir da tag
     *
     * @param clanTag Tag do clã
     * @return GenericClan ou null se não existir
     */
    public GenericClan getClan(String clanTag) {
        return plugin.getClansManager().getClanByTag(clanTag);
    }

    /**
     * Mostra detalhes de um clã para um jogador
     *
     * @param player Jogador
     * @param clanTag Tag do clã
     */
    public void showClanDetails(Player player, String clanTag) {
        // Neste caso, vamos abrir o menu de pontos de clãs para o jogador
        try {
            if (!clanExists(clanTag)) {
                player.sendMessage(ChatColor.RED + "Esse clã não existe!");
                return;
            }

            String clanName = getClanName(clanTag);
            int points = plugin.getPointsManager().getClanPoints(clanTag);

            player.sendMessage(ChatColor.GREEN + "Detalhes do clã " + getColoredClanTag(clanTag) + ":");
            player.sendMessage(ChatColor.YELLOW + "Nome: " + ChatColor.WHITE + clanName);
            player.sendMessage(ChatColor.YELLOW + "Pontos: " + ChatColor.WHITE + plugin.getPointsManager().formatPoints(points));

            new hplugins.hliga.inventory.menus.ClanListMenu(plugin, player).open(1);
        } catch (Exception e) {
            LogUtils.severe("Erro ao mostrar detalhes do clã: " + e.getMessage(), e);
            player.sendMessage(ChatColor.RED + "Ocorreu um erro ao exibir detalhes do clã.");
        }
    }
}
