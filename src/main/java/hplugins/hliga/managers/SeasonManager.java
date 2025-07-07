package hplugins.hliga.managers;

import hplugins.hliga.Main;
import hplugins.hliga.models.Season;
import hplugins.hliga.utils.LogUtils;
import hplugins.hliga.utils.NotificationUtils;
import hplugins.hliga.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

public class SeasonManager {

    private final Main plugin;

    public SeasonManager(Main plugin) {
        this.plugin = plugin;
    }
    private BukkitTask endSeasonTask;
    private Map<Integer, BukkitTask> warningTasks = new HashMap<>();

    /**
     * Verifica se existe uma temporada ativa
     *
     * @return true se existir uma temporada ativa, false caso contrário
     */
    public boolean isSeasonActive() {
        return plugin.getDatabaseManager().getAdapter().getActiveSeason().isPresent();
    }

    /**
     * Obtém a temporada ativa atual
     *
     * @return Temporada ativa ou Optional vazio se não houver
     */
    public Optional<Season> getActiveSeason() {
        return plugin.getDatabaseManager().getAdapter().getActiveSeason();
    }

    /**
     * Inicia uma nova temporada
     *
     * @param name Nome da temporada
     * @param durationDays Duração em dias
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean startSeason(String name, int durationDays) {
        return startSeason(name, durationDays, 0, 0);
    }

    /**
     * Inicia uma nova temporada com data e hora específicas
     *
     * @param name Nome da temporada
     * @param durationDays Duração em dias
     * @param hour Hora específica para encerramento (0-23)
     * @param minute Minuto específico para encerramento (0-59)
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean startSeason(String name, int durationDays, int hour, int minute) {
        if (isSeasonActive()) {
            return false;
        }

        Season season = new Season();
        season.name = name;
        season.startDate = System.currentTimeMillis();

        FileConfiguration config = plugin.getConfig();
        String timezone = config.getString("temporada.fuso_horario", "America/Sao_Paulo");

        if (hour >= 0 && minute >= 0) {
            season.endDate = TimeUtils.calculateEndDateWithTime(durationDays, hour, minute, timezone);
        } else {
            season.endDate = TimeUtils.calculateEndDate(durationDays, timezone);
        }

        season.active = true;

        plugin.getPointsManager().syncClansWithDatabase();

        boolean success = plugin.getDatabaseManager().getAdapter().saveSeason(season);

        if (success) {
            scheduleSeasonEnd(season);

            int topClansToShow = config.getInt("temporada.top_clans_anuncio", 3);
            NotificationUtils.announceSeasonStart(plugin, season);
        }

        return success;
    }

    /**
     * Inicia uma nova temporada com data específica de término
     *
     * @param name Nome da temporada
     * @param targetDate Data específica de término
     * @param hour Hora de término (-1 para usar 23:59)
     * @param minute Minuto de término (-1 para usar 59)
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean startSeasonWithSpecificDate(String name, java.time.LocalDate targetDate, int hour, int minute) {
        FileConfiguration config = plugin.getConfig();
        String timezone = config.getString("temporada.timezone", "America/Sao_Paulo");

        if (isSeasonActive()) {
            return false;
        }

        Season season = new Season();
        season.name = name;
        season.startDate = System.currentTimeMillis();

        season.endDate = TimeUtils.calculateEndDateForSpecificDate(targetDate, hour, minute, timezone);
        season.active = true;

        plugin.getPointsManager().syncClansWithDatabase();

        boolean success = plugin.getDatabaseManager().getAdapter().saveSeason(season);

        if (success) {
            scheduleSeasonEnd(season);

            NotificationUtils.announceSeasonStart(plugin, season);
        }

        return success;
    }

    /**
     * Encerra a temporada ativa
     *
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean endSeason() {
        if (!isSeasonActive()) {
            return false; // Não há temporada ativa para fechar
        }

        Optional<Season> activeSeasonOpt = getActiveSeason();
        if (!activeSeasonOpt.isPresent()) {
            return false;
        }

        Season activeSeason = activeSeasonOpt.get();
        FileConfiguration config = plugin.getConfig();

        LogUtils.info("Iniciando finalização da temporada: " + activeSeason.name);

        List<hplugins.hliga.models.ClanPoints> finalRanking = plugin.getPointsManager().getTopClans(10);
        LogUtils.debug("Ranking final obtido: " + finalRanking.size() + " clãs");

        for (int i = 0; i < finalRanking.size(); i++) {
            hplugins.hliga.models.ClanPoints cp = finalRanking.get(i);
            if (cp != null) {
                LogUtils.debug("  " + (i+1) + "º lugar: " + cp.getClanTag() + " com " + cp.getPoints() + " pontos");
            }
        }

        int positionsRewarded = plugin.getConfigManager().getTagsConfig().getInt("tags_temporada.posicoes_premiadas", 3);
        boolean hasValidWinners = false;

        LogUtils.info("=== VERIFICAÇÃO DE GANHADORES VÁLIDOS ===");
        LogUtils.info("Posições premiadas configuradas: " + positionsRewarded);
        LogUtils.info("Total de clãs no ranking: " + finalRanking.size());

        for (int i = 0; i < Math.min(finalRanking.size(), positionsRewarded); i++) {
            hplugins.hliga.models.ClanPoints cp = finalRanking.get(i);
            if (cp != null) {
                LogUtils.info("Verificando posição " + (i+1) + ": Clã " + cp.getClanTag() + " com " + cp.getPoints() + " pontos");
                if (cp.getPoints() > 0) {
                    hasValidWinners = true;
                    LogUtils.info("✓ Ganhador válido encontrado na posição " + (i+1) + ": " + cp.getClanTag());
                }
            } else {
                LogUtils.warning("ClanPoints nulo encontrado na posição " + (i+1));
            }
        }

        final boolean hasValidWinnersFinaal = hasValidWinners; // Para usar em lambda
        LogUtils.info("RESULTADO: Ganhadores válidos encontrados: " + hasValidWinnersFinaal);
        LogUtils.info("============================================");

        if (plugin.getTagManager() != null && plugin.getTagManager().isSystemEnabled()) {
            if (hasValidWinners) {
                plugin.getTagManager().distributeSeasonTags(activeSeason);
                LogUtils.info("Tags de temporada distribuídas para a temporada: " + activeSeason.name);
            } else {
                LogUtils.info("Nenhum ganhador válido encontrado - tags de temporada NÃO distribuídas para: " + activeSeason.name);
            }
        }

        LogUtils.info("Enviando notificações de fim de temporada...");

        NotificationUtils.announceSeasonEnd(plugin, activeSeason, 5);
        LogUtils.info("Notificação de fim de temporada enviada para Minecraft");

        plugin.getLigaManager().sendDiscordSeasonEnd(activeSeason, finalRanking);
        LogUtils.info("Notificação de fim de temporada enviada para Discord");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (hasValidWinnersFinaal) {
                boolean rewardsDistributed = plugin.getRewardManager().distributeRewards();
                if (rewardsDistributed) {
                    LogUtils.info("Recompensas distribuídas com sucesso");
                }
            } else {
                plugin.getRewardManager().announceNoWinners(activeSeason);
                LogUtils.info("Nenhum ganhador válido encontrado - anúncio de temporada sem participantes enviado");
            }
        }, 60L); // 3 segundos

        if (config.getBoolean("temporada.resetar_pontos", true)) {
            LogUtils.info("Iniciando reset completo da temporada...");

            plugin.getDatabaseManager().getAdapter().resetAllClanPoints();
            LogUtils.info("Pontos de todos os clãs resetados");

            if (plugin.getTagManager() != null) {
                plugin.getTagManager().resetSeasonComplete();
                LogUtils.info("Sistema de tags resetado - tags temporárias removidas");
            }

            if (plugin.getNpcManager() != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getNpcManager().resetAllNPCsToDefault();
                    LogUtils.info("NPCs resetados para valores padrão");
                }, 20L);
            }

            if (plugin.getInventoryManager() != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getInventoryManager().refreshAllMenus();
                    LogUtils.info("Cache dos menus limpo e atualizado");
                }, 40L);
            }

            LogUtils.info("Reset completo da temporada finalizado");
        }

        cancelEndTasks();

        createSeasonArchiveFile(activeSeason, finalRanking, hasValidWinners);

        boolean success = plugin.getDatabaseManager().getAdapter().endActiveSeason();

        if (success) {
            LogUtils.info("Temporada '" + activeSeason.name + "' finalizada com sucesso!");
        } else {
            LogUtils.error("Erro ao finalizar temporada no banco de dados");
        }

        return success;
    }

    /**
     * Obtém o histórico de temporadas
     *
     * @return Lista de temporadas anteriores
     */
    public List<Season> getSeasonHistory() {
        return plugin.getDatabaseManager().getAdapter().getSeasonHistory();
    }

    /**
     * Obtém a temporada ativa atual
     *
     * @return Temporada atual ou null se não houver
     */
    public Season getCurrentSeason() {
        Optional<Season> activeSeason = getActiveSeason();
        return activeSeason.orElse(null);
    }

    /**
     * Obtém todas as temporadas
     *
     * @return Lista de todas as temporadas
     */
    public List<Season> getAllSeasons() {
        return getSeasonHistory();
    }

    /**
     * Verifica e programa a finalização de uma temporada ativa
     */
    /**
     * Obtém o clan na posição específica do ranking
     *
     * @param position Posição no ranking (1-based)
     * @return ClanPoints do clan na posição ou null se não encontrado
     */
    public hplugins.hliga.models.ClanPoints getClanAtPosition(int position) {
        List<hplugins.hliga.models.ClanPoints> clans = plugin.getDatabaseManager().getAdapter().getTopClans(position);
        if (!clans.isEmpty() && position > 0 && position <= clans.size()) {
            return clans.get(position - 1); // position é 1-based, list é 0-based
        }
        return null;
    }

    public void checkActiveSeason() {
        Optional<Season> optionalSeason = getActiveSeason();

        if (optionalSeason.isPresent()) {
            Season season = optionalSeason.get();

            if (season.endDate <= System.currentTimeMillis()) {
                LogUtils.debug("Temporada " + season.name + " já expirou! Finalizando...");
                endSeason();
                plugin.getRewardManager().distributeRewards();
            } else {
                scheduleSeasonEnd(season);
            }
        }
    }

    /**
     * Programa a finalização automática de uma temporada
     *
     * @param season Temporada a ser finalizada
     */
    private void scheduleSeasonEnd(Season season) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("temporada.fechamento_automatico", true)) {
            return;
        }

        cancelEndTasks();

        long endTimeMillis = season.endDate;
        long currentTimeMillis = System.currentTimeMillis();
        long delayMillis = endTimeMillis - currentTimeMillis;

        if (delayMillis <= 0) {
            LogUtils.debug("Temporada " + season.name + " já expirou! Finalizando...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                endSeason();
                plugin.getRewardManager().distributeRewards();
            });
            return;
        }

        long delayTicks = delayMillis / 50; // Usar 50ms em vez de 1000ms/20 para compensar atrasos do servidor

        endSeasonTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            LogUtils.debug("Finalizando temporada " + season.name + " automaticamente...");
            endSeason();

            // Não chamar NotificationUtils.announceSeasonEnd() aqui para evitar duplicação
        }, delayTicks);

        scheduleWarnings(season);

        LogUtils.debug("Temporada " + season.name + " programada para finalizar em " +
                TimeUtils.formatTimeLeft(delayMillis));
    }

    /**
     * Programa avisos prévios de finalização de temporada
     *
     * @param season Temporada
     */
    private void scheduleWarnings(Season season) {
        FileConfiguration config = plugin.getConfig();
        List<Integer> warningMinutes = config.getIntegerList("temporada.avisos_previos");

        if (warningMinutes.isEmpty()) {
            return;
        }

        long endTimeMillis = season.endDate;
        long currentTimeMillis = System.currentTimeMillis();

        for (int minutes : warningMinutes) {
            long warningTimeMillis = endTimeMillis - (minutes * 60 * 1000);

            if (warningTimeMillis > currentTimeMillis) {
                long delayMillis = warningTimeMillis - currentTimeMillis;
                long delayTicks = delayMillis / 50;

                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String timeText = minutes >= 60 ?
                            (minutes / 60) + " hora" + (minutes / 60 > 1 ? "s" : "") :
                            minutes + " minuto" + (minutes > 1 ? "s" : "");

                    String message = plugin.getConfigManager().getMessages().getMessage("temporada.aviso_fechamento",
                            "{tempo}", timeText);

                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                }, delayTicks);

                warningTasks.put(minutes, task);
            }
        }
    }

    /**
     * Cancela todas as tarefas de finalização programadas
     */
    private void cancelEndTasks() {
        if (endSeasonTask != null) {
            endSeasonTask.cancel();
            endSeasonTask = null;
        }

        for (BukkitTask task : warningTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }

        warningTasks.clear();
    }

    /**
     * Obtém uma temporada pelo ID
     *
     * @param id ID da temporada
     * @return Temporada encontrada ou Optional vazio
     */
    public Optional<Season> getSeason(int id) {
        return plugin.getDatabaseManager().getAdapter().getSeason(id);
    }

    /**
     * Cria arquivo detalhado da temporada finalizada na pasta temporadas/
     */
    private void createSeasonArchiveFile(Season season, List<hplugins.hliga.models.ClanPoints> finalRanking, boolean hasValidWinners) {
        try {
            LogUtils.info("Criando arquivo detalhado da temporada: " + season.name);

            java.io.File temporadasDir = new java.io.File(plugin.getDataFolder(), "temporadas");
            if (!temporadasDir.exists()) {
                temporadasDir.mkdirs();
            }

            String dateStr = formatTimestamp(season.startDate).replace("/", "-").replace(" ", "_").replace(":", "-");
            String fileName = season.name.replaceAll("[^a-zA-Z0-9]", "_") + "_" + dateStr + ".yml";
            java.io.File seasonFile = new java.io.File(temporadasDir, fileName);

            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

            config.set("temporada.nome", season.name);
            config.set("temporada.id", season.id);
            config.set("temporada.inicio", formatTimestamp(season.startDate));
            config.set("temporada.termino", formatTimestamp(season.endDate));
            config.set("temporada.duracao_dias", (season.endDate - season.startDate) / (24 * 60 * 60 * 1000));
            config.set("temporada.finalizada_em", formatTimestamp(System.currentTimeMillis()));

            if (hasValidWinners && !finalRanking.isEmpty()) {
                hplugins.hliga.models.ClanPoints winner = finalRanking.get(0);
                config.set("vencedor.clan_tag", winner.getClanTag());
                config.set("vencedor.clan_nome", plugin.getClansManager().getClanName(winner.getClanTag()));
                config.set("vencedor.pontos_finais", winner.getPoints());

                for (int i = 0; i < Math.min(finalRanking.size(), 10); i++) {
                    hplugins.hliga.models.ClanPoints clan = finalRanking.get(i);
                    if (clan.getPoints() > 0) {
                        String path = "ranking." + (i + 1);
                        config.set(path + ".posicao", i + 1);
                        config.set(path + ".clan_tag", clan.getClanTag());
                        config.set(path + ".clan_nome", plugin.getClansManager().getClanName(clan.getClanTag()));
                        config.set(path + ".pontos", clan.getPoints());
                    }
                }
            }

            int clansParticipantes = finalRanking.size();
            int clansComPontos = (int) finalRanking.stream().filter(clan -> clan.getPoints() > 0).count();
            int totalPontos = finalRanking.stream().mapToInt(clan -> clan.getPoints()).sum();

            config.set("estatisticas.clans_participantes", clansParticipantes);
            config.set("estatisticas.clans_com_pontos", clansComPontos);
            config.set("estatisticas.clans_sem_pontos", clansParticipantes - clansComPontos);
            config.set("estatisticas.total_pontos_distribuidos", totalPontos);
            config.set("estatisticas.media_pontos_por_clan", clansComPontos > 0 ? totalPontos / clansComPontos : 0);

            config.set("metadados.tags_distribuidas", hasValidWinners);
            config.set("metadados.recompensas_distribuidas", hasValidWinners);
            config.set("metadados.tipo_finalizacao", "sistema");
            config.set("metadados.arquivo_criado_em", formatTimestamp(System.currentTimeMillis()));

            config.save(seasonFile);
            LogUtils.info("Arquivo da temporada criado: " + fileName);

        } catch (Exception e) {
            LogUtils.error("Erro ao criar arquivo da temporada: " + e.getMessage());
        }
    }

    /**
     * Inicializa o sistema de temporadas verificando configuração manual
     */
    public void initialize() {
        try {
            LogUtils.info("Inicializando sistema de temporadas...");

            checkAndCreateSeasonFromConfig();

            Optional<Season> activeSeasonOpt = getActiveSeason();
            if (activeSeasonOpt.isPresent()) {
                Season activeSeason = activeSeasonOpt.get();
                LogUtils.info("Temporada ativa encontrada: " + activeSeason.name);

                scheduleSeasonEnd(activeSeason);
            } else {
                LogUtils.info("Nenhuma temporada ativa encontrada");
            }

        } catch (Exception e) {
            LogUtils.error("Erro ao inicializar sistema de temporadas: " + e.getMessage());
        }
    }

    /**
     * Verifica configuração manual no seasons.yml e cria temporada se necessário
     */
    private void checkAndCreateSeasonFromConfig() {
        try {
            java.io.File seasonsFile = new java.io.File(plugin.getDataFolder(), "seasons.yml");
            if (!seasonsFile.exists()) {
                LogUtils.debug("Arquivo seasons.yml não encontrado, pulando verificação");
                return;
            }

            org.bukkit.configuration.file.YamlConfiguration seasonsConfig =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(seasonsFile);

            if (!seasonsConfig.getBoolean("temporada.ativa", false)) {
                LogUtils.debug("Nenhuma temporada configurada manualmente no seasons.yml");
                return;
            }

            if (seasonsConfig.getBoolean("temporada.processada", false)) {
                LogUtils.debug("Temporada já foi processada anteriormente - pulando");
                return;
            }

            if (getActiveSeason().isPresent()) {
                LogUtils.info("Temporada já ativa no banco de dados - ignorando configuração manual");
                return;
            }

            LogUtils.info("Processando configuração manual de temporada do seasons.yml...");

            String nome = seasonsConfig.getString("temporada.nome", "Temporada Configurada");
            String timezone = "America/Sao_Paulo";

            String inicioData = seasonsConfig.getString("temporada.inicio", "");
            String terminoData = seasonsConfig.getString("temporada.termino", "");

            if (inicioData.isEmpty() || terminoData.isEmpty()) {
                LogUtils.error("Datas de início e término devem ser configuradas no formato YYYY-MM-DD HH:MM");
                LogUtils.error("Exemplo: inicio_data: \"2025-06-02 12:00\"");
                return;
            }

            long startTime = parseDateString(inicioData, timezone);
            long endTime = parseDateString(terminoData, timezone);

            if (startTime <= 0 || endTime <= 0) {
                LogUtils.error("Erro ao converter datas - verifique o formato YYYY-MM-DD HH:MM");
                return;
            }

            if (endTime <= startTime) {
                LogUtils.error("Data de término deve ser posterior à data de início");
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (endTime <= currentTime) {
                LogUtils.info("Temporada configurada já expirou - não será criada");
                LogUtils.info("Data de término: " + formatTimestamp(endTime));
                LogUtils.info("Data atual: " + formatTimestamp(currentTime));
                return;
            }

            LogUtils.info("Criando temporada com as seguintes datas:");
            LogUtils.info("Início: " + formatTimestamp(startTime));
            LogUtils.info("Término: " + formatTimestamp(endTime));
            LogUtils.info("Agora: " + formatTimestamp(currentTime));

            long durationMillis = endTime - startTime;
            int durationDays = (int) Math.ceil(durationMillis / (24.0 * 60.0 * 60.0 * 1000.0));

            java.time.Instant endInstant = java.time.Instant.ofEpochMilli(endTime);
            java.time.ZonedDateTime endZoned = endInstant.atZone(java.time.ZoneId.of(timezone));
            int endHour = endZoned.getHour();
            int endMinute = endZoned.getMinute();

            LogUtils.info("Iniciando temporada usando sistema completo...");
            LogUtils.info("Duração calculada: " + durationDays + " dias");
            LogUtils.info("Horário de término: " + endHour + ":" + String.format("%02d", endMinute));

            boolean success = startSeason(nome, durationDays, endHour, endMinute);

            if (success) {
                LogUtils.info("Temporada '" + nome + "' criada com sucesso através do sistema completo!");
                LogUtils.info("Início: " + formatTimestamp(startTime));
                LogUtils.info("Término: " + formatTimestamp(endTime));

                seasonsConfig.set("temporada.ativa", false);
                seasonsConfig.set("temporada.processada", true);
                seasonsConfig.save(seasonsFile);

                LogUtils.info("Temporada integrada ao sistema completo - NPCs, placeholders e rankings ativos");

            } else {
                LogUtils.error("Falha ao criar temporada através do sistema completo");
            }

        } catch (Exception e) {
            LogUtils.error("Erro ao processar configuração manual de temporada: " + e.getMessage());
        }
    }

    /**
     * Converte string de data no formato "YYYY-MM-DD HH:MM" para timestamp
     */
    private long parseDateString(String dateStr, String timezone) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            java.time.ZoneId zoneId = java.time.ZoneId.of(timezone);

            java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(dateStr, formatter);
            java.time.ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);

            return zonedDateTime.toInstant().toEpochMilli();

        } catch (Exception e) {
            LogUtils.error("Erro ao converter data '" + dateStr + "': " + e.getMessage());
            LogUtils.error("Use o formato: YYYY-MM-DD HH:MM (ex: 2025-06-02 12:00)");
            return 0;
        }
    }

    /**
     * Formata timestamp para string legível
     */
    private String formatTimestamp(long timestamp) {
        try {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
            java.time.ZoneId zoneId = java.time.ZoneId.of("America/Sao_Paulo");
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            return instant.atZone(zoneId).format(formatter);
        } catch (Exception e) {
            return "Data inválida";
        }
    }

    /**
     * Força a atualização dos rankings dos clãs
     * Este método deve ser chamado antes de atualizar NPCs para garantir dados corretos
     */
    public void forceRefreshRankings() {
        try {
            LogUtils.debug("Forçando atualização dos rankings dos clãs...");

            plugin.getClansManager().syncClansWithDatabase();

            LogUtils.debug("Rankings atualizados com sucesso");
        } catch (Exception e) {
            LogUtils.error("Erro ao forçar atualização dos rankings: " + e.getMessage());
        }
    }
}
