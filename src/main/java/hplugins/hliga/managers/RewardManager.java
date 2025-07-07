package hplugins.hliga.managers;

import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.models.Reward;
import hplugins.hliga.models.Season;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RewardManager {

    private final Main plugin;

    public RewardManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Carrega as recompensas da configuração
     *
     * @return Lista de recompensas
     */
    public List<Reward> loadRewards() {
        List<Reward> rewards = new ArrayList<>();
        FileConfiguration premiacoesConfig = plugin.getConfigManager().getPremiacoesConfig();

        ConfigurationSection rewardsSection = premiacoesConfig.getConfigurationSection("recompensas");
        if (rewardsSection == null) {
            return rewards;
        }

        for (String key : rewardsSection.getKeys(false)) {
            try {
                int position = Integer.parseInt(key);
                List<String> commands = rewardsSection.getStringList(key);

                if (!commands.isEmpty()) {
                    rewards.add(new Reward(position, commands));
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Posição inválida na configuração de recompensas: " + key);
            }
        }

        return rewards;
    }

    /**
     * Distribui recompensas para os clãs vencedores
     *
     * @return true se as recompensas foram distribuídas, false caso contrário
     */
    public boolean distributeRewards() {
        FileConfiguration premiacoesConfig = plugin.getConfigManager().getPremiacoesConfig();

        if (!premiacoesConfig.getBoolean("ativado", true)) {
            return false;
        }

        List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(10);

        if (topClans.isEmpty()) {
            plugin.getLogger().warning("Não foi possível distribuir recompensas: Não há clãs com pontos.");
            return false;
        }

        List<Reward> rewards = loadRewards();

        if (rewards.isEmpty()) {
            plugin.getLogger().warning("Não foi possível distribuir recompensas: Não há recompensas configuradas.");
            return false;
        }

        for (Reward reward : rewards) {
            int position = reward.position;

            if (topClans.size() >= position) {
                ClanPoints clanPoints = topClans.get(position - 1);
                GenericClan clan = plugin.getClansManager().getClanByTag(clanPoints.getClanTag());

                if (clan != null) {
                    executeReward(reward, clan, position, clanPoints.getPoints());
                }
            }
        }

        Bukkit.broadcastMessage(plugin.getConfigManager().getMessages().getMessage("premiacao.distribuida"));

        distributeSpecialRewards();

        distributeParticipationRewards();

        // Isso é importante caso o RewardManager seja chamado independentemente
        ensureSeasonTagsDistributed();


        return true;
    }

    /**
     * Executa os comandos de recompensa para um clã
     *
     * @param reward Recompensa a ser executada
     * @param clan Clã vencedor
     * @param position Posição do clã
     * @param points Pontos do clã
     */
    private void executeReward(Reward reward, GenericClan clan, int position, int points) {
        List<String> commands = reward.commands;

        List<String> processedCommands = new ArrayList<>();

        for (String command : commands) {
            String processed = command
                    .replace("{clan}", clan.getTag())
                    .replace("{pontos}", String.valueOf(points))
                    .replace("{posicao}", String.valueOf(position));

            processedCommands.add(processed);
        }

        for (Player player : clan.getOnlineMembers()) {
            if (player != null) {
                for (String command : processedCommands) {
                    String finalCommand = command.replace("{player}", player.getName());

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    });
                }
            }
        }
    }

    /**
     * Verifica se um clã está entre os vencedores
     *
     * @param clanTag Tag do clã
     * @return Posição do clã no ranking (1 = primeiro, 2 = segundo, etc.) ou 0 se não for um vencedor
     */
    public int getWinnerPosition(String clanTag) {
        List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(10);

        for (int i = 0; i < topClans.size(); i++) {
            if (topClans.get(i).getClanTag().equals(clanTag)) {
                return i + 1;
            }
        }

        return 0;
    }

    /**
     * Executa uma recompensa específica para um clã
     *
     * @param position Posição da recompensa
     * @param clanTag Tag do clã
     * @return true se a recompensa foi executada, false caso contrário
     */
    public boolean executeSpecificReward(int position, String clanTag) {
        GenericClan clan = plugin.getClansManager().getClanByTag(clanTag);

        if (clan == null) {
            return false;
        }

        FileConfiguration premiacoesConfig = plugin.getConfigManager().getPremiacoesConfig();
        List<String> commands = premiacoesConfig.getStringList("recompensas." + position);

        if (commands.isEmpty()) {
            return false;
        }

        int points = plugin.getPointsManager().getClanPoints(clanTag);
        Reward reward = new Reward(position, commands);

        executeReward(reward, clan, position, points);
        return true;
    }

    /**
     * Distribui recompensas de participação para todos os clãs
     *
     * @return true se qualquer recompensa foi distribuída, false caso contrário
     */
    public boolean distributeParticipationRewards() {
        FileConfiguration premiacoesConfig = plugin.getConfigManager().getPremiacoesConfig();
        List<String> commands = premiacoesConfig.getStringList("recompensas_especiais.participacao");

        if (commands.isEmpty()) {
            return false;
        }

        boolean anyRewardGiven = false;

        List<ClanPoints> allClans = plugin.getPointsManager().getAllClanPoints();

        Reward participationReward = new Reward(0, commands);

        for (ClanPoints clanPoints : allClans) {
            GenericClan clan = plugin.getClansManager().getClanByTag(clanPoints.getClanTag());
            if (clan == null) {
                continue;
            }

            executeReward(participationReward, clan, 0, clanPoints.getPoints());
            anyRewardGiven = true;
        }

        return anyRewardGiven;
    }

    /**
     * Distribui recompensas especiais baseadas na pontuação dos clãs
     *
     * @return true se qualquer recompensa foi distribuída, false caso contrário
     */
    public boolean distributeSpecialRewards() {
        FileConfiguration premiacoesConfig = plugin.getConfigManager().getPremiacoesConfig();

        ConfigurationSection pointRewardsSection = premiacoesConfig.getConfigurationSection("recompensas_especiais.pontuacao");
        if (pointRewardsSection == null) {
            return false;
        }

        boolean anyRewardGiven = false;

        List<ClanPoints> allClans = plugin.getPointsManager().getAllClanPoints();

        for (ClanPoints clanPoints : allClans) {
            GenericClan clan = plugin.getClansManager().getClanByTag(clanPoints.getClanTag());
            if (clan == null) {
                continue;
            }

            int points = clanPoints.getPoints();

            List<Integer> pointThresholds = new ArrayList<>();
            for (String key : pointRewardsSection.getKeys(false)) {
                try {
                    pointThresholds.add(Integer.parseInt(key));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Pontuação inválida na configuração de recompensas especiais: " + key);
                }
            }

            pointThresholds.sort((a, b) -> b - a);

            for (int threshold : pointThresholds) {
                if (points >= threshold) {
                    List<String> commands = premiacoesConfig.getStringList("recompensas_especiais.pontuacao." + threshold);
                    if (!commands.isEmpty()) {
                        Reward reward = new Reward(0, commands);
                        executeReward(reward, clan, 0, points);
                        anyRewardGiven = true;
                    }

                    break;
                }
            }
        }

        return anyRewardGiven;
    }

    /**
     * Garante que as tags permanentes sejam distribuídas junto com os prêmios
     * CRÍTICO: Previne situação onde prêmios são dados mas tags não
     */
    private void ensureSeasonTagsDistributed() {
        try {
            Season currentSeason = plugin.getSeasonManager().getCurrentSeason();
            if (currentSeason == null) {
                plugin.getLogger().info("RewardManager: Nenhuma temporada ativa - não distribuindo tags");
                return;
            }

            if (plugin.getTagManager() == null || !plugin.getTagManager().isSystemEnabled()) {
                plugin.getLogger().info("RewardManager: Sistema de tags desabilitado - não distribuindo tags");
                return;
            }

            int positionsRewarded = plugin.getConfigManager().getTagsConfig().getInt("tags_temporada.posicoes_premiadas", 3);
            List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(positionsRewarded);

            boolean hasValidWinners = false;
            for (int i = 0; i < Math.min(topClans.size(), positionsRewarded); i++) {
                ClanPoints cp = topClans.get(i);
                if (cp != null && cp.getPoints() > 0) {
                    hasValidWinners = true;
                    break;
                }
            }

            if (!hasValidWinners) {
                plugin.getLogger().info("RewardManager: Nenhum ganhador válido encontrado - não distribuindo tags");
                return;
            }

            plugin.getLogger().info("RewardManager: Garantindo distribuição de tags permanentes para temporada: " + currentSeason.name);
            plugin.getTagManager().distributeSeasonTags(currentSeason);

        } catch (Exception e) {
            plugin.getLogger().warning("RewardManager: Erro ao garantir distribuição de tags: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Anuncia que não há ganhadores para uma temporada
     *
     * @param season Temporada sem ganhadores
     */
    public void announceNoWinners(Season season) {
        hplugins.hliga.config.Messages config = plugin.getConfigManager().getMessages();

        List<String> noWinnersMessages = config.getStringList("temporada.sem_ganhadores.chat");

        if (noWinnersMessages.isEmpty()) {
            noWinnersMessages.add("&e[hLiga] &7A temporada &6{nome}&7 terminou sem ganhadores.");
            noWinnersMessages.add("&e[hLiga] &7Nenhum clã conseguiu pontuar durante esta temporada.");
        }

        for (String linha : noWinnersMessages) {
            String linhaFormatada = linha
                    .replace("{nome}", season.name)
                    .replace("{duracao}", String.valueOf(season.getDurationDays()));

            Bukkit.broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', linhaFormatada));
        }
    }
}
