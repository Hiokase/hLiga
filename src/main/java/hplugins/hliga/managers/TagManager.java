package hplugins.hliga.managers;

import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.TagType;
import hplugins.hliga.utils.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador do sistema de tags
 * Compatível com todas as versões do Minecraft
 */
public class TagManager {

    private final Main plugin;
    private FileConfiguration tagsConfig;
    private BukkitTask updateTask;

    private final Map<UUID, List<PlayerTag>> playerTagsCache = new ConcurrentHashMap<>();

    public TagManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
        if (isSystemEnabled()) {
            startAutoUpdate();
        }
    }

    /**
     * Carrega a configuração de tags
     */
    public void loadConfig() {
        this.tagsConfig = plugin.getConfigManager().getTagsConfig();
        if (tagsConfig == null) {
            LogUtils.error("Configuração de tags não encontrada, sistema desabilitado.");
            return;
        }

        playerTagsCache.clear();
        LogUtils.debug("Configuração de tags carregada.");
    }

    /**
     * Verifica se o sistema está habilitado
     */
    public boolean isSystemEnabled() {
        return tagsConfig != null && tagsConfig.getBoolean("sistema.ativado", true);
    }

    /**
     * Obtém a configuração de tags
     */
    public FileConfiguration getTagsConfig() {
        return tagsConfig;
    }

    /**
     * Inicia o sistema de atualização automática
     */
    private void startAutoUpdate() {
        if (!tagsConfig.getBoolean("sistema.atualizacao_automatica", true)) {
            return;
        }

        int intervalMinutes = tagsConfig.getInt("sistema.intervalo_verificacao", 5);
        long intervalTicks = intervalMinutes * 60L * 20L; // Converter para ticks

        Bukkit.getScheduler().runTaskLater(plugin, this::updateRankingTags, 20L);

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateRankingTags();
        }, intervalTicks, intervalTicks);

        LogUtils.info("Sistema de tags iniciado - Atualização automática a cada " + intervalMinutes + " minutos");
    }

    /**
     * Para o sistema de atualização automática
     */
    public void stopAutoUpdate() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    /**
     * Atualiza as tags de ranking baseadas na posição atual dos clãs
     * APENAS durante temporada ativa - NUNCA após reset
     */
    public void updateRankingTags() {
        if (!isSystemEnabled()) {
            return;
        }

        try {
            String modo = tagsConfig.getString("sistema.modo", "normal").toLowerCase();

            if ("temporada".equals(modo)) {
                Season currentSeason = plugin.getSeasonManager().getCurrentSeason();
                if (currentSeason == null) {
                    LogUtils.debug("Modo temporada: Não há temporada ativa - tags não aplicadas");
                    return;
                }

                if (currentSeason.isFinished() || currentSeason.endDate <= System.currentTimeMillis()) {
                    LogUtils.debug("Modo temporada: Temporada finalizada - tags não aplicadas");
                    return;
                }
            }

            List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(5);
            boolean hasActiveClans = topClans.stream().anyMatch(clan -> clan.getPoints() > 0);

            boolean needsUpdate = checkIfTagsNeedUpdate(topClans);
            if (!needsUpdate) {
                LogUtils.debug("Tags já estão atualizadas - não é necessário modificar");
                return;
            }

            if (!hasActiveClans) {
                List<PlayerTag> existingTags = plugin.getDatabaseManager().getAdapter().getTagsByType(TagType.RANKING);
                if (!existingTags.isEmpty()) {
                    LogUtils.debug("Removendo tags de ranking - nenhum clã com pontos");
                    clearRankingTags();
                }
                return;
            }

            LogUtils.debug("Modo " + modo + ": Atualizando tags de ranking");

            clearRankingTags();

            for (int i = 0; i < topClans.size(); i++) {
                int position = i + 1;
                ClanPoints clanPoints = topClans.get(i);
                String clanTag = clanPoints.getClanTag();

                if (clanPoints.getPoints() <= 0) {
                    continue;
                }

                GenericClan clan = plugin.getClansManager().getClanByTag(clanTag);
                if (clan == null) {
                    continue;
                }

                String tagFormat = tagsConfig.getString("tags_ranking." + position);
                if (tagFormat == null) {
                    continue;
                }

                List<UUID> allMembers = plugin.getClansManager().getClanMembers(clanTag);
                for (UUID memberUuid : allMembers) {
                    setRankingTag(memberUuid, position, tagFormat);
                }

                LogUtils.debug("Tag aplicada ao clã " + clanTag + " (posição " + position + ") - " + allMembers.size() + " membros");
            }

            LogUtils.debug("Tags de ranking atualizadas para " + topClans.size() + " clãs");

        } catch (Exception e) {
            LogUtils.error("Erro ao atualizar tags de ranking: " + e.getMessage());
        }
    }

    /**
     * Verifica se as tags precisam ser atualizadas
     */
    private boolean checkIfTagsNeedUpdate(List<ClanPoints> currentTopClans) {
        try {
            List<PlayerTag> currentTags = plugin.getDatabaseManager().getAdapter().getTagsByType(TagType.RANKING);

            if (currentTags.isEmpty() && !currentTopClans.isEmpty()) {
                return true;
            }

            if (!currentTags.isEmpty() && currentTopClans.isEmpty()) {
                return true;
            }

            Set<String> currentTaggedClans = new HashSet<>();
            for (PlayerTag tag : currentTags) {
                try {
                    UUID playerUuid = tag.getPlayerUuid();
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null && player.isOnline()) {
                        String clanTag = plugin.getClansManager().getPlayerClanTag(player);
                        if (clanTag != null) {
                            currentTaggedClans.add(clanTag);
                        }
                    }
                } catch (Exception e) {
                }
            }

            Set<String> newTopClans = new HashSet<>();
            for (ClanPoints clanPoints : currentTopClans) {
                if (clanPoints.getPoints() > 0) {
                    newTopClans.add(clanPoints.getClanTag());
                }
            }

            return !currentTaggedClans.equals(newTopClans);

        } catch (Exception e) {
            LogUtils.debug("Erro ao verificar necessidade de atualização: " + e.getMessage());
            return true; // Em caso de erro, atualizar para garantir
        }
    }

    /**
     * Remove todas as tags de ranking
     */
    public void clearRankingTags() {
        try {
            LogUtils.info("Removendo todas as tags de ranking...");

            boolean success = plugin.getDatabaseManager().getAdapter().removeAllRankingTags();

            if (success) {
                LogUtils.info("✓ Tags de ranking removidas do banco de dados");

                playerTagsCache.clear();
                LogUtils.info("✓ Cache de tags limpo");

                for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                    try {
                        UUID playerUuid = onlinePlayer.getUniqueId();

                        String currentTag = getPlayerRankingTag(playerUuid);
                        if (currentTag.isEmpty()) {
                            LogUtils.debug("✓ Tag de ranking removida para: " + onlinePlayer.getName());
                        } else {
                            LogUtils.warning("Tag ainda presente para: " + onlinePlayer.getName() + " (" + currentTag + ")");
                        }

                    } catch (Exception e) {
                        LogUtils.debug("Erro ao verificar tag para " + onlinePlayer.getName() + ": " + e.getMessage());
                    }
                }

                LogUtils.info("✅ Todas as tags de ranking foram removidas com sucesso");

            } else {
                LogUtils.error("❌ Falha ao remover tags de ranking no banco de dados");
            }
        } catch (Exception e) {
            LogUtils.error("Erro ao remover tags de ranking: " + e.getMessage());
        }
    }

    /**
     * Reset completo do sistema quando a temporada finaliza
     * Remove todas as tags temporárias e atualiza nametags
     */
    public void resetSeasonComplete() {
        try {
            LogUtils.info("Iniciando reset completo do sistema de tags...");

            playerTagsCache.clear();
            LogUtils.info("✓ Cache de tags limpo ANTES da remoção");

            boolean success = plugin.getDatabaseManager().getAdapter().removeAllRankingTags();

            if (success) {
                LogUtils.info("✓ Tags de ranking removidas do banco de dados");
            } else {
                LogUtils.warning("✗ Falha ao remover tags de ranking do banco de dados");
            }

            playerTagsCache.clear();
            LogUtils.info("✓ Cache de tags limpo APÓS a remoção");

            // Isso garante que plugins usando %hliga_tag% vejam as mudanças imediatamente
            for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
                    UUID playerUuid = onlinePlayer.getUniqueId();

                    List<PlayerTag> playerTags = playerTagsCache.get(playerUuid);
                    if (playerTags != null) {
                        playerTags.removeIf(tag -> tag.getTagType() == TagType.RANKING);
                        if (playerTags.isEmpty()) {
                            playerTagsCache.remove(playerUuid);
                        }
                    }

                    try {
                        java.util.Optional<PlayerTag> rankingTagFromDB = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(playerUuid, TagType.RANKING);

                        if (rankingTagFromDB.isPresent()) {
                            LogUtils.warning("Tag de ranking ainda encontrada no banco para " + onlinePlayer.getName() + " - removendo forçadamente");
                            plugin.getDatabaseManager().getAdapter().removePlayerTag(playerUuid, "RANKING", rankingTagFromDB.get().getPosition());
                        }

                        String currentTag = getPlayerActiveTag(playerUuid);
                        if (currentTag.isEmpty()) {
                            LogUtils.debug("✓ Tag removida com sucesso para jogador: " + onlinePlayer.getName());
                        } else {
                            LogUtils.debug("→ Jogador " + onlinePlayer.getName() + " mantém tag permanente: " + currentTag);
                        }

                    } catch (Exception e) {
                        LogUtils.debug("Erro ao verificar tag para " + onlinePlayer.getName() + ": " + e.getMessage());
                    }

                } catch (Exception e) {
                    LogUtils.debug("Erro ao limpar cache para jogador " + onlinePlayer.getName() + ": " + e.getMessage());
                }
            }

            LogUtils.info("=== EXECUTANDO LIMPEZA TOTAL DE TAGS DE RANKING ===");

            try {
                boolean tagsRemoved = plugin.getDatabaseManager().getAdapter().removeAllRankingTags();
                if (tagsRemoved) {
                    LogUtils.info("✅ Tags de ranking removidas diretamente do banco de dados");
                } else {
                    LogUtils.warning("❌ Falha ao remover tags de ranking do banco");
                }
            } catch (Exception e) {
                LogUtils.warning("Erro ao remover tags do banco: " + e.getMessage());
            }

            if (playerTagsCache != null) {
                playerTagsCache.clear();
                LogUtils.info("✅ Cache interno de tags completamente limpo");
            }

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    LogUtils.info("Forçando limpeza completa do PlaceholderAPI...");

                    for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                        try {
                            String playerName = onlinePlayer.getName();
                            UUID uuid = onlinePlayer.getUniqueId();

                            try {
                                java.util.Optional<PlayerTag> checkRanking = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(uuid, TagType.RANKING);
                                if (checkRanking.isPresent()) {
                                    LogUtils.warning("ALERTA: Tag de ranking ainda encontrada para " + playerName + " - removendo forçadamente");
                                    plugin.getDatabaseManager().getAdapter().removePlayerTag(uuid, "RANKING", checkRanking.get().getPosition());
                                }
                            } catch (Exception e) {
                                LogUtils.debug("Erro ao verificar tag para " + playerName + ": " + e.getMessage());
                            }

                            onlinePlayer.recalculatePermissions();

                            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                String finalTag = getPlayerActiveTag(uuid);
                                if (finalTag.isEmpty()) {
                                    LogUtils.info("✅ " + playerName + " - Tag de ranking removida completamente");
                                } else {
                                    LogUtils.info("→ " + playerName + " - Mantém tag permanente: " + finalTag);
                                }
                            }, 3L);

                        } catch (Exception e) {
                            LogUtils.debug("Erro ao processar jogador " + onlinePlayer.getName() + ": " + e.getMessage());
                        }
                    }

                    LogUtils.info("✅ Limpeza de tags finalizada - PlaceholderAPI atualizado");

                } catch (Exception e) {
                    LogUtils.warning("Erro durante limpeza do PlaceholderAPI: " + e.getMessage());
                }
            });

            if (plugin.getNpcManager() != null) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getNpcManager().resetAllNPCsToDefault();
                    LogUtils.info("✓ NPCs resetados para configuração padrão");
                }, 40L); // 2 segundos de delay para garantir que tudo seja processado
            }

            LogUtils.info("✅ Reset completo do sistema de tags finalizado - tags temporárias removidas");
            LogUtils.info("→ Placeholders %hliga_tag% atualizados para TAB, chat e outros plugins");

        } catch (Exception e) {
            LogUtils.error("Erro durante reset completo do sistema de tags: " + e.getMessage());
        }
    }

    /**
     * Define uma tag de ranking para um jogador
     */
    private void setRankingTag(UUID playerUuid, int position, String tagFormat) {
        try {
            String formattedTag = ChatColor.translateAlternateColorCodes('&', tagFormat);
            PlayerTag tag = new PlayerTag(playerUuid, position, formattedTag, "TOP" + position);

            boolean success = plugin.getDatabaseManager().getAdapter().savePlayerTag(tag);

            if (success) {
                List<PlayerTag> playerTags = playerTagsCache.computeIfAbsent(playerUuid, k -> new ArrayList<>());
                playerTags.removeIf(t -> t.getTagType() == TagType.RANKING);
                playerTags.add(tag);
                LogUtils.debug("Tag de ranking definida para jogador na posição " + position);
            } else {
                LogUtils.warning("Falha ao salvar tag de ranking no banco de dados - usando apenas cache temporário");
                List<PlayerTag> playerTags = playerTagsCache.computeIfAbsent(playerUuid, k -> new ArrayList<>());
                playerTags.removeIf(t -> t.getTagType() == TagType.RANKING);
                playerTags.add(tag);
            }
        } catch (Exception e) {
            LogUtils.warning("Erro ao definir tag de ranking - operação ignorada: " + e.getMessage());
        }
    }

    /**
     * Distribui tags permanentes usando ranking já calculado
     * CRÍTICO: Evita problema do reset de pontos antes da distribuição
     */
    public void distributeSeasonTagsWithRanking(Season season, List<ClanPoints> preCalculatedRanking) {
        LogUtils.debug("=== DISTRIBUIÇÃO COM RANKING PRÉ-CALCULADO ===");
        LogUtils.debug("Sistema de tags habilitado: " + isSystemEnabled());
        LogUtils.debug("Temporada: " + season.name + " (ID: " + season.id + ")");
        LogUtils.debug("Ranking pré-calculado recebido: " + preCalculatedRanking.size() + " clãs");

        if (!isSystemEnabled()) {
            LogUtils.warning("Sistema de tags desabilitado - não distribuindo tags de temporada");
            return;
        }

        try {
            int positionsRewarded = tagsConfig.getInt("tags_temporada.posicoes_premiadas", 3);
            LogUtils.debug("Posições premiadas configuradas: " + positionsRewarded);

            List<ClanPoints> topClans = preCalculatedRanking.subList(0, Math.min(preCalculatedRanking.size(), positionsRewarded));
            LogUtils.debug("Top clãs selecionados: " + topClans.size());

            for (int i = 0; i < topClans.size(); i++) {
                ClanPoints cp = topClans.get(i);
                if (cp != null) {
                    LogUtils.debug("  " + (i+1) + "º lugar: " + cp.getClanTag() + " com " + cp.getPoints() + " pontos");
                } else {
                    LogUtils.debug("  " + (i+1) + "º lugar: ClanPoints é NULL!");
                }
            }

            boolean hasValidClansForTags = false;
            for (int i = 0; i < Math.min(topClans.size(), positionsRewarded); i++) {
                ClanPoints cp = topClans.get(i);
                if (cp != null && cp.getPoints() > 0) {
                    hasValidClansForTags = true;
                    break;
                }
            }

            if (!hasValidClansForTags) {
                LogUtils.warning("❌ NENHUM clã válido encontrado para distribuição de tags de temporada: " + season.name);
                LogUtils.warning("   Motivo: Todos os clãs nas primeiras " + positionsRewarded + " posições têm 0 pontos");
                return;
            }

            LogUtils.info("🏆 Distribuindo tags de temporada para ganhadores da temporada: " + season.name);
            LogUtils.info("📊 Top clãs selecionados: " + topClans.size() + " | Posições premiadas: " + positionsRewarded);
            LogUtils.info("✅ Confirmado: Há clãs válidos para receber tags permanentes");

            int totalMembersTagged = 0;

            for (int i = 0; i < Math.min(topClans.size(), positionsRewarded); i++) {
                ClanPoints clanPoints = topClans.get(i);
                if (clanPoints == null || clanPoints.getPoints() <= 0) {
                    LogUtils.debug("⏭️ Pulando posição " + (i+1) + " - clã sem pontos válidos");
                    continue;
                }

                String clanTag = clanPoints.getClanTag();
                int position = i + 1;

                LogUtils.info("🎯 PROCESSANDO clã " + clanPoints.getClanTag() + " na posição " + position + " com " + clanPoints.getPoints() + " pontos");

                String tagConfig = tagsConfig.getString("tags_temporada.posicoes." + position);
                if (tagConfig == null || tagConfig.isEmpty()) {
                    LogUtils.warning("❌ Tag não configurada para a posição " + position + " - pulando");
                    continue;
                }

                String formattedTag = tagConfig
                        .replace("{temporada}", season.name)
                        .replace("{posicao}", String.valueOf(position));

                GenericClan clan = plugin.getClansManager().getClanByTag(clanTag);
                if (clan == null) {
                    LogUtils.warning("❌ Clã não encontrado no sistema: " + clanTag);
                    continue;
                }

                List<UUID> allMembers = clan.getAllMemberUUIDs();

                LogUtils.info("👥 Clã " + clanTag + " (posição " + position + ") - Total de membros: " + allMembers.size());
                LogUtils.info("🏷️ Tag a ser distribuída: " + formattedTag);

                int membersTagged = 0;
                for (UUID memberUuid : allMembers) {
                    if (memberUuid != null) {
                        LogUtils.info("   📝 Criando tag permanente para membro: " + memberUuid);
                        giveSeasonTag(memberUuid, position, season.id, formattedTag, season.name);
                        membersTagged++;
                    } else {
                        LogUtils.warning("   ❌ UUID nulo encontrado no clã " + clanTag + " - pulando membro");
                    }
                }

                LogUtils.info("✅ Tags de temporada distribuídas para o clã " + clanTag + " - " + membersTagged + " membros");
                totalMembersTagged += membersTagged;
            }

            LogUtils.info("🎉 DISTRIBUIÇÃO CONCLUÍDA - Total de jogadores que receberam tags: " + totalMembersTagged);

        } catch (Exception e) {
            LogUtils.error("Erro ao distribuir tags de temporada: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Distribui tags permanentes no final de uma temporada
     * CORRIGIDO: Só distribui tags se houver ganhadores válidos com pontuação
     */
    public void distributeSeasonTags(Season season) {
        LogUtils.debug("=== INICIANDO DISTRIBUIÇÃO DE TAGS DE TEMPORADA ===");
        LogUtils.debug("Sistema de tags habilitado: " + isSystemEnabled());
        LogUtils.debug("Temporada: " + season.name + " (ID: " + season.id + ")");

        if (!isSystemEnabled()) {
            LogUtils.warning("Sistema de tags desabilitado - não distribuindo tags de temporada");
            return;
        }

        try {
            int positionsRewarded = tagsConfig.getInt("tags_temporada.posicoes_premiadas", 3);
            LogUtils.debug("Posições premiadas configuradas: " + positionsRewarded);

            List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(positionsRewarded);
            LogUtils.debug("Top clãs obtidos: " + topClans.size());

            for (int i = 0; i < topClans.size(); i++) {
                ClanPoints cp = topClans.get(i);
                if (cp != null) {
                    LogUtils.debug("  " + (i+1) + "º lugar: " + cp.getClanTag() + " com " + cp.getPoints() + " pontos");
                } else {
                    LogUtils.debug("  " + (i+1) + "º lugar: ClanPoints é NULL!");
                }
            }

            int validClansCount = 0;
            for (int i = 0; i < topClans.size(); i++) {
                ClanPoints clanPoints = topClans.get(i);
                if (clanPoints != null) {
                    boolean isValid = clanPoints.getPoints() > 0;
                    LogUtils.debug("Verificando clã " + (i+1) + ": " + clanPoints.getClanTag() + " | Pontos: " + clanPoints.getPoints() + " | Válido: " + isValid);
                    if (isValid) {
                        validClansCount++;
                    }
                } else {
                    LogUtils.debug("ClanPoints nulo encontrado na posição " + (i+1) + "!");
                }
            }

            LogUtils.debug("Total de clãs válidos para premiação: " + validClansCount + "/" + topClans.size());

            boolean hasValidClansForTags = false;
            for (int i = 0; i < Math.min(topClans.size(), positionsRewarded); i++) {
                ClanPoints cp = topClans.get(i);
                if (cp != null && cp.getPoints() > 0) {
                    hasValidClansForTags = true;
                    break;
                }
            }

            if (!hasValidClansForTags) {
                LogUtils.warning("❌ NENHUM clã válido encontrado para distribuição de tags de temporada: " + season.name);
                LogUtils.warning("   Motivo: Todos os clãs nas primeiras " + positionsRewarded + " posições têm 0 pontos");
                return;
            }

            LogUtils.info("🏆 Distribuindo tags de temporada para ganhadores da temporada: " + season.name);
            LogUtils.info("📊 Top clãs encontrados: " + topClans.size() + " | Posições premiadas: " + positionsRewarded);
            LogUtils.info("✅ Confirmado: Há clãs válidos para receber tags permanentes");
            int tagsDistributed = 0;

            for (int i = 0; i < Math.min(topClans.size(), positionsRewarded); i++) {
                int position = i + 1;
                ClanPoints clanPoints = topClans.get(i);

                if (clanPoints == null) {
                    LogUtils.warning("❌ ClanPoints nulo na posição " + position + " - pulando distribuição de tag");
                    continue;
                }

                if (clanPoints.getPoints() <= 0) {
                    LogUtils.info("⏭️ Clã " + clanPoints.getClanTag() + " na posição " + position + " tem " + clanPoints.getPoints() + " pontos - pulando (sem pontos)");
                    continue;
                }

                LogUtils.info("🎯 PROCESSANDO clã " + clanPoints.getClanTag() + " na posição " + position + " com " + clanPoints.getPoints() + " pontos");

                String clanTag = clanPoints.getClanTag();
                GenericClan clan = plugin.getClansManager().getClanByTag(clanTag);
                if (clan == null) {
                    LogUtils.debug("Clã " + clanTag + " não encontrado - pulando distribuição de tag");
                    continue;
                }

                String tagFormat = tagsConfig.getString("tags_temporada.formatos." + position);
                LogUtils.debug("Formato de tag para posição " + position + ": " + tagFormat);
                if (tagFormat == null) {
                    LogUtils.warning("Formato de tag não configurado para posição " + position + " - pulando");
                    continue;
                }

                String seasonIdentifier = getSeasonIdentifier(season);
                tagFormat = tagFormat.replace("{temporada}", seasonIdentifier);
                String formattedTag = ChatColor.translateAlternateColorCodes('&', tagFormat);
                LogUtils.debug("Tag formatada final: " + formattedTag + " (identificador: " + seasonIdentifier + ")");

                int membersTagged = 0;

                List<UUID> allMembers = clan.getAllMemberUUIDs();

                LogUtils.info("👥 Clã " + clanTag + " (posição " + position + ") - Total de membros: " + allMembers.size());
                LogUtils.info("🏷️ Tag a ser distribuída: " + formattedTag);

                for (UUID memberUuid : allMembers) {
                    if (memberUuid != null) {
                        LogUtils.info("   📝 Criando tag permanente para membro: " + memberUuid);
                        giveSeasonTag(memberUuid, position, season.id, formattedTag, season.name);
                        membersTagged++;
                    } else {
                        LogUtils.warning("   ❌ UUID nulo encontrado no clã " + clanTag + " - pulando membro");
                    }
                }

                int tagsVerified = 0;
                for (UUID memberUuid : allMembers) {
                    if (memberUuid != null) {
                        Optional<PlayerTag> savedTag = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(memberUuid, TagType.SEASON);
                        if (savedTag.isPresent() && savedTag.get().getSeasonNumber() == season.id) {
                            tagsVerified++;
                        }
                    }
                }

                LogUtils.info("✅ Tags de temporada distribuídas para o clã " + clanTag + " (posição " + position + ", " + membersTagged + " membros)");
                LogUtils.info("📊 Membros processados: " + membersTagged + " | Tags verificadas no banco: " + tagsVerified);
                tagsDistributed++;
            }

            if (tagsDistributed > 0) {
                LogUtils.info("Distribuição de tags de temporada concluída - " + tagsDistributed + " clãs premiados");
            } else {
                LogUtils.debug("Nenhuma tag de temporada foi distribuída - sem ganhadores válidos");
            }

        } catch (Exception e) {
            LogUtils.error("Erro ao distribuir tags de temporada: " + e.getMessage());
        }
    }

    /**
     * Obtém o identificador da temporada (nome ou número)
     */
    private String getSeasonIdentifier(Season season) {
        if (season.name != null && !season.name.trim().isEmpty() && !season.name.startsWith("Temporada ")) {
            return season.name;
        }
        return "T" + season.id;
    }

    /**
     * Obtém APENAS a tag de ranking de um jogador (%hliga_tag_ranking%)
     */
    public String getPlayerRankingTag(UUID playerUuid) {
        if (!isSystemEnabled()) {
            return "";
        }

        try {
            Optional<PlayerTag> rankingTag = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(playerUuid, TagType.RANKING);
            if (rankingTag.isPresent()) {
                return rankingTag.get().getFormattedTag();
            }
            return "";

        } catch (Exception e) {
            LogUtils.debug("Erro ao buscar tag de ranking: " + e.getMessage());
            return "";
        }
    }

    /**
     * Obtém APENAS a tag permanente de um jogador (%hliga_tag_permanentes%)
     */
    public String getPlayerPermanentTag(UUID playerUuid) {
        if (!isSystemEnabled()) {
            LogUtils.debug("Sistema de tags desabilitado para jogador " + playerUuid);
            return "";
        }

        if (!isTagsEnabledForPlayer(playerUuid)) {
            LogUtils.debug("Tags desabilitadas individualmente para jogador " + playerUuid);
            return "";
        }

        try {
            LogUtils.debug("Buscando tag permanente para jogador " + playerUuid);
            Optional<PlayerTag> seasonTag = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(playerUuid, TagType.SEASON);
            if (seasonTag.isPresent()) {
                String tag = seasonTag.get().getFormattedTag();
                LogUtils.debug("✓ Tag permanente encontrada para jogador " + playerUuid + ": " + tag);
                return tag;
            } else {
                LogUtils.debug("Nenhuma tag permanente encontrada para jogador " + playerUuid);
            }
            return "";

        } catch (Exception e) {
            LogUtils.error("Erro ao buscar tag permanente para jogador " + playerUuid + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Obtém a tag ativa de um jogador para usar em placeholders (método legado)
     * @deprecated Use getPlayerRankingTag() ou getPlayerPermanentTag()
     */
    @Deprecated
    public String getPlayerActiveTag(UUID playerUuid) {
        if (!isSystemEnabled()) {
            return "";
        }

        if (!isTagsEnabledForPlayer(playerUuid)) {
            return "";
        }

        try {
            Optional<PlayerTag> rankingTag = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(playerUuid, TagType.RANKING);
            if (rankingTag.isPresent()) {
                return rankingTag.get().getFormattedTag();
            }

            Optional<PlayerTag> seasonTag = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(playerUuid, TagType.SEASON);
            if (seasonTag.isPresent()) {
                return seasonTag.get().getFormattedTag();
            }

            return "";

        } catch (Exception e) {
            LogUtils.debug("Erro ao buscar tag ativa: " + e.getMessage());
            return "";
        }
    }

    /**
     * Dá uma tag de temporada permanente para um jogador
     */
    private void giveSeasonTag(UUID playerUuid, int position, int seasonId, String formattedTag, String seasonName) {
        try {
            LogUtils.debug("📝 Iniciando criação de tag permanente para jogador " + playerUuid);
            LogUtils.debug("   📊 Dados: Posição=" + position + ", Temporada=" + seasonId + ", Tag=" + formattedTag);

            PlayerTag tag = new PlayerTag(playerUuid, position, seasonId, formattedTag, "SEASON_" + seasonId + "_" + position);

            if (!tag.isSeasonTag()) {
                LogUtils.error("❌ ERRO CRÍTICO: Tag não foi criada como SEASON! Tipo: " + tag.getTagType());
                return;
            }

            LogUtils.debug("✅ Tag criada corretamente - Tipo: " + tag.getTagType());

            boolean saved = plugin.getDatabaseManager().getAdapter().savePlayerTag(tag);
            if (!saved) {
                LogUtils.error("❌ FALHA ao salvar tag permanente no banco para jogador " + playerUuid);
                return;
            }

            LogUtils.debug("✅ Tag salva no banco de dados com sucesso");

            Optional<PlayerTag> savedTag = plugin.getDatabaseManager().getAdapter().getActivePlayerTag(playerUuid, TagType.SEASON);
            if (savedTag.isPresent() && savedTag.get().getSeasonNumber() == seasonId) {
                LogUtils.debug("✅ Verificação de integridade: Tag encontrada no banco - Temporada " + savedTag.get().getSeasonNumber());
            } else {
                LogUtils.error("❌ ERRO DE INTEGRIDADE: Tag não encontrada no banco após salvamento!");
                LogUtils.error("   📊 Esperado: Temporada " + seasonId + " | Encontrado: " +
                        (savedTag.isPresent() ? "Temporada " + savedTag.get().getSeasonNumber() : "Nenhuma tag"));
                return;
            }

            List<PlayerTag> playerTags = playerTagsCache.computeIfAbsent(playerUuid, k -> new ArrayList<>());
            playerTags.add(tag);
            LogUtils.debug("✅ Cache atualizado");

            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                String message = tagsConfig.getString("mensagens.tag_permanente_ganha", "&6Parabéns! Você ganhou a tag permanente: {tag}")
                        .replace("{tag}", formattedTag)
                        .replace("{temporada}", seasonName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                LogUtils.info("🎉 Jogador ONLINE notificado: " + player.getName() + " - Tag: " + formattedTag);
            } else {
                LogUtils.debug("💾 Jogador OFFLINE - tag salva no banco para acesso futuro");
            }

            LogUtils.debug("🏆 Tag permanente concedida com SUCESSO: " + formattedTag + " para jogador " + playerUuid);

        } catch (Exception e) {
            LogUtils.error("❌ ERRO CRÍTICO ao conceder tag de temporada para jogador " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtém todas as tags de um jogador
     */
    public List<PlayerTag> getPlayerTags(UUID playerUuid) {
        List<PlayerTag> cachedTags = playerTagsCache.get(playerUuid);
        if (cachedTags != null) {
            return new ArrayList<>(cachedTags);
        }

        List<PlayerTag> tags = plugin.getDatabaseManager().getAdapter().getPlayerTags(playerUuid);
        playerTagsCache.put(playerUuid, new ArrayList<>(tags));
        return tags;
    }

    /**
     * Obtém a tag formatada de um jogador para um placeholder específico
     */
    public String getFormattedTag(UUID playerUuid, String placeholderKey) {
        List<PlayerTag> tags = getPlayerTags(playerUuid);
        if (tags.isEmpty()) {
            return getEmptyValue(placeholderKey);
        }

        String format = getPlaceholderFormat(placeholderKey);
        return formatTags(tags, format);
    }

    /**
     * Obtém o formato de um placeholder
     */
    private String getPlaceholderFormat(String placeholderKey) {
        for (String key : tagsConfig.getConfigurationSection("placeholders").getKeys(false)) {
            String placeholder = tagsConfig.getString("placeholders." + key + ".placeholder");
            if (placeholderKey.equals(placeholder)) {
                return tagsConfig.getString("placeholders." + key + ".formato", "");
            }
        }
        return "{permanentes} {temporaria}"; // Padrão
    }

    /**
     * Obtém o valor vazio de um placeholder
     */
    private String getEmptyValue(String placeholderKey) {
        for (String key : tagsConfig.getConfigurationSection("placeholders").getKeys(false)) {
            String placeholder = tagsConfig.getString("placeholders." + key + ".placeholder");
            if (placeholderKey.equals(placeholder)) {
                return tagsConfig.getString("placeholders." + key + ".vazio", "");
            }
        }
        return "";
    }

    /**
     * Formata as tags baseado no formato especificado
     */
    private String formatTags(List<PlayerTag> tags, String format) {
        StringBuilder rankingTags = new StringBuilder();
        StringBuilder seasonTags = new StringBuilder();
        PlayerTag lastSeasonTag = null;

        for (PlayerTag tag : tags) {
            if (tag.getTagType() == TagType.RANKING) {
                if (rankingTags.length() > 0) rankingTags.append(" ");
                rankingTags.append(tag.getFormattedTag());
            } else if (tag.getTagType() == TagType.SEASON) {
                if (seasonTags.length() > 0) seasonTags.append(" ");
                seasonTags.append(tag.getFormattedTag());
                if (lastSeasonTag == null || tag.getObtainedDate() > lastSeasonTag.getObtainedDate()) {
                    lastSeasonTag = tag;
                }
            }
        }

        String result = format
                .replace("{temporaria}", rankingTags.toString())
                .replace("{permanentes}", seasonTags.toString())
                .replace("{ultima_permanente}", lastSeasonTag != null ? lastSeasonTag.getFormattedTag() : "");

        return ChatColor.translateAlternateColorCodes('&', result.trim());
    }

    /**
     * Ativa exibição de tags para um jogador específico
     */
    public void enableTagsForPlayer(UUID playerUuid) {
        plugin.getDatabaseManager().getAdapter().savePlayerTagPreference(playerUuid, true);
        LogUtils.debug("Tags ativadas para jogador: " + playerUuid);
    }

    /**
     * Desativa exibição de tags para um jogador específico
     */
    public void disableTagsForPlayer(UUID playerUuid) {
        plugin.getDatabaseManager().getAdapter().savePlayerTagPreference(playerUuid, false);
        LogUtils.debug("Tags desativadas para jogador: " + playerUuid);
    }

    /**
     * Verifica se um jogador tem as tags ativadas
     */
    public boolean isTagsEnabledForPlayer(UUID playerUuid) {
        return plugin.getDatabaseManager().getAdapter().getPlayerTagPreference(playerUuid);
    }

    /**
     * Força uma atualização completa do sistema
     */
    public void forceUpdate() {
        updateRankingTags();
    }

    /**
     * Limpa o cache de tags
     */
    public void clearCache() {
        playerTagsCache.clear();
    }

    /**
     * Para o gerenciador
     */
    public void shutdown() {
        stopAutoUpdate();
        clearCache();
    }

    /**
     * Método de debug para verificar tags de temporada de um jogador
     */
    public void debugPlayerSeasonTags(UUID playerUuid) {
        try {
            LogUtils.info("=== DEBUG TAGS DE TEMPORADA PARA JOGADOR " + playerUuid + " ===");

            List<PlayerTag> allPlayerTags = plugin.getDatabaseManager().getAdapter().getPlayerTags(playerUuid);
            LogUtils.info("Tags encontradas no banco de dados: " + allPlayerTags.size());

            for (PlayerTag tag : allPlayerTags) {
                LogUtils.info("  - Tipo: " + tag.getTagType() +
                        " | Posição: " + tag.getPosition() +
                        " | Temporada: " + tag.getSeasonNumber() +
                        " | Tag: " + tag.getFormattedTag() +
                        " | Ativa: " + tag.isActive());
            }

            List<PlayerTag> seasonTags = allPlayerTags.stream()
                    .filter(tag -> tag.getTagType() == TagType.SEASON)
                    .collect(java.util.stream.Collectors.toList());

            LogUtils.info("Tags de temporada encontradas: " + seasonTags.size());

            String rankingTag = getPlayerRankingTag(playerUuid);
            String permanentTag = getPlayerPermanentTag(playerUuid);
            String activeTag = getPlayerActiveTag(playerUuid);

            LogUtils.info("Placeholders:");
            LogUtils.info("  - hliga_tag_ranking: '" + rankingTag + "'");
            LogUtils.info("  - hliga_tag_permanentes: '" + permanentTag + "'");
            LogUtils.info("  - hliga_tag (ativo): '" + activeTag + "'");

            LogUtils.info("=== FIM DEBUG ===");

        } catch (Exception e) {
            LogUtils.error("Erro durante debug de tags: " + e.getMessage());
            e.printStackTrace();
        }
    }
}