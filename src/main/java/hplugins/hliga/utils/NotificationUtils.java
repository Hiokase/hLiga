package hplugins.hliga.utils;

import com.cryptomorin.xseries.XSound;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.models.Season;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário para enviar notificações (som, título, actionbar, mensagens)
 */
public class NotificationUtils {

    /**
     * Envia uma notificação completa para todos os jogadores
     *
     * @param plugin Instância do plugin
     * @param title Título principal
     * @param subtitle Subtítulo 
     * @param message Mensagem no chat
     * @param actionBar Mensagem na action bar
     * @param sound Som a ser tocado
     * @param volume Volume do som (1.0f = normal)
     * @param pitch Pitch do som (1.0f = normal)
     */
    public static void broadcastFullNotification(Main plugin, String title, String subtitle,
                                                 String message, String actionBar, String sound, float volume, float pitch) {

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (title != null && !title.isEmpty()) {
                VersionUtils.sendTitle(player, title, subtitle != null ? subtitle : "", 10, 70, 20);
            }

            if (message != null && !message.isEmpty()) {
                player.sendMessage(message);
            }

            if (actionBar != null && !actionBar.isEmpty()) {
                VersionUtils.sendActionBar(player, actionBar);
            }

            if (sound != null && !sound.isEmpty()) {
                VersionUtils.playSound(player, sound, volume, pitch);
            }
        }
    }

    /**
     * Anuncia o início de uma temporada
     *
     * @param plugin Instância do plugin
     * @param season Temporada iniciada
     */
    public static void announceSeasonStart(Main plugin, Season season) {
        String title = plugin.getConfigManager().getMessages().getMessage("titulos_temporada.inicio_titulo");
        String subtitle = plugin.getConfigManager().getMessages().getMessage("titulos_temporada.inicio_subtitulo")
                .replace("{nome}", season.name)
                .replace("{dias}", String.valueOf(season.getDurationDays()));

        List<String> broadcastMessages = plugin.getConfigManager().getMessages().getStringList("temporada.inicio.chat");

        String sound = plugin.getConfigManager().getSonsConfig().getString("temporada.inicio.nome", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfigManager().getSonsConfig().getDouble("temporada.inicio.volume", 1.0);
        float pitch = (float) plugin.getConfigManager().getSonsConfig().getDouble("temporada.inicio.pitch", 0.5);

        LogUtils.debugHigh("Enviando notificação de início de temporada: " + season.name);

        for (Player player : Bukkit.getOnlinePlayers()) {
            VersionUtils.sendTitle(player,
                    ChatColor.translateAlternateColorCodes('&', title),
                    ChatColor.translateAlternateColorCodes('&', subtitle),
                    10, 70, 20);

            if (sound != null && !sound.isEmpty()) {
                XSound.matchXSound(sound).ifPresent(xSound ->
                        xSound.play(player, volume, pitch));
            }
        }

        for (String linha : broadcastMessages) {
            String linhaFormatada = linha
                    .replace("{nome}", season.name)
                    .replace("{data_inicio}", TimeUtils.formatDate(season.startDate))
                    .replace("{data_fim}", TimeUtils.formatDate(season.endDate))
                    .replace("{termino}", TimeUtils.formatDate(season.endDate))
                    .replace("{duracao}", String.valueOf(season.getDurationDays()));
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
        }

        plugin.getLigaManager().sendDiscordSeasonStart(season);
    }

    /**
     * Anuncia o fim de uma temporada
     *
     * @param plugin Instância do plugin
     * @param season Temporada finalizada
     * @param topClans Número de clãs a serem anunciados
     */
    public static void announceSeasonEnd(Main plugin, Season season, int topClans) {
        List<ClanPoints> currentTopClans = plugin.getDatabaseManager().getAdapter().getTopClans(10);
        String winnerName = plugin.getConfigManager().getMessages().getMessage("temporada.sem_vencedor");

        if (!currentTopClans.isEmpty() && currentTopClans.get(0).points > 0) {
            ClanPoints winner = currentTopClans.get(0);
            GenericClan clan = plugin.getClansManager().getClan(winner.clanTag);
            winnerName = clan != null ? clan.getTag() : winner.clanTag;

            season.winnerClan = winner.clanTag;
            season.winnerPoints = winner.points;
            season.topClans = new ArrayList<>(currentTopClans);
            plugin.getDatabaseManager().getAdapter().saveSeason(season);
        }

        String title = plugin.getConfigManager().getMessages().getMessage("titulos_temporada.finalizada_titulo");
        String subtitle = plugin.getConfigManager().getMessages().getMessage("titulos_temporada.finalizada_subtitulo")
                .replace("{vencedor}", winnerName);

        List<String> broadcastMessages = plugin.getConfigManager().getMessages().getStringList("temporada.fim.chat");

        String sound = plugin.getConfigManager().getSonsConfig().getString("temporada.fim.nome", "ENTITY_FIREWORK_ROCKET_BLAST");
        float volume = (float) plugin.getConfigManager().getSonsConfig().getDouble("temporada.fim.volume", 1.0);
        float pitch = (float) plugin.getConfigManager().getSonsConfig().getDouble("temporada.fim.pitch", 1.0);

        LogUtils.debugHigh("Enviando notificação de fim de temporada: " + season.name + " - Vencedor: " + winnerName);

        for (Player player : Bukkit.getOnlinePlayers()) {
            VersionUtils.sendTitle(player,
                    ChatColor.translateAlternateColorCodes('&', title),
                    ChatColor.translateAlternateColorCodes('&', subtitle),
                    10, 100, 20);

            if (season.winnerClan != null && !season.winnerClan.equals("Nenhum")) {
                GenericClan playerClan = plugin.getClansManager().getPlayerClan(player);
                if (playerClan != null && playerClan.getTag().equals(season.winnerClan)) {
                    String vitoriaTitle = plugin.getConfigManager().getMessages().getMessage("titulos_temporada.vitoria_titulo");
                    String vitoriaSubtitle = plugin.getConfigManager().getMessages().getMessage("titulos_temporada.vitoria_subtitulo");

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        VersionUtils.sendTitle(player,
                                ChatColor.translateAlternateColorCodes('&', vitoriaTitle),
                                ChatColor.translateAlternateColorCodes('&', vitoriaSubtitle),
                                10, 70, 20);
                    }, 60L); // 3 segundos
                }
            }

            if (sound != null && !sound.isEmpty()) {
                XSound.matchXSound(sound).ifPresent(xSound ->
                        xSound.play(player, volume, pitch));
            }
        }

        StringBuilder rankingFinal = new StringBuilder();
        for (int i = 0; i < Math.min(5, currentTopClans.size()); i++) {
            ClanPoints clanPoints = currentTopClans.get(i);
            GenericClan clan = plugin.getClansManager().getClan(clanPoints.clanTag);
            String clanName = clan != null ? clan.getTag() : clanPoints.clanTag;
            String posicao = (i + 1) + "º";
            rankingFinal.append(posicao).append(" ").append(clanName)
                    .append(" - ").append(NumberFormatter.format(clanPoints.points)).append(" pontos");
            if (i < Math.min(4, currentTopClans.size() - 1)) {
                rankingFinal.append("\n");
            }
        }

        for (String linha : broadcastMessages) {
            String linhaFormatada = linha
                    .replace("{nome}", season.name)
                    .replace("{data_inicio}", TimeUtils.formatDate(season.startDate))
                    .replace("{data_fim}", TimeUtils.formatDate(season.endDate))
                    .replace("{duracao}", String.valueOf(season.getDurationDays()))
                    .replace("{vencedor}", winnerName)
                    .replace("{pontos_vencedor}", String.valueOf(season.winnerPoints))
                    .replace("{ranking_final}", rankingFinal.toString());
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
        }

        plugin.getLigaManager().sendDiscordSeasonEnd(season);
    }

    /**
     * Retorna um prefixo colorido para a posição (1º, 2º, 3º, etc)
     *
     * @param position Posição (índice começando em 0)
     * @return String formatada e colorida
     */
    private static String getPositionPrefix(int position) {
        switch (position) {
            case 0:
                return "&6&l1º.";
            case 1:
                return "&f&l2º.";
            case 2:
                return "&c&l3º.";
            default:
                return "&7&l" + (position + 1) + "º.";
        }
    }

    /**
     * Anuncia o tempo restante de uma temporada
     *
     * @param plugin Instância do plugin
     * @param season Temporada
     * @param remainingTime Tempo restante formatado
     */
    public static void announceRemainingTime(Main plugin, Season season, String remainingTime) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "&6&l[hLiga] &eA temporada &f" + season.name + " &eencerrará em &f" + remainingTime);

        LogUtils.debugHigh("Enviando notificação de tempo restante: " + season.name + " - " + remainingTime);

        String sound = plugin.getConfigManager().getSonsConfig().getString("temporada.aviso.nome", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) plugin.getConfigManager().getSonsConfig().getDouble("temporada.aviso.volume", 1.0);
        float pitch = (float) plugin.getConfigManager().getSonsConfig().getDouble("temporada.aviso.pitch", 1.2);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            VersionUtils.playSound(player, sound, volume, pitch);
        }
    }

    /**
     * Envia uma notificação de erro para um jogador
     *
     * @param player Jogador
     * @param message Mensagem de erro
     */
    public static void sendErrorNotification(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + message));
        VersionUtils.playSound(player, "ENTITY_VILLAGER_NO", 1.0f, 1.0f);
    }

    /**
     * Envia uma notificação de erro para um jogador com som configurável
     *
     * @param plugin Instância do plugin
     * @param player Jogador
     * @param message Mensagem de erro
     */
    public static void sendErrorNotification(Main plugin, Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + message));

        String sound = plugin.getConfigManager().getSonsConfig().getString("menu.clique.nome", "ENTITY_VILLAGER_NO");
        float volume = (float) plugin.getConfigManager().getSonsConfig().getDouble("menu.clique.volume", 1.0);
        float pitch = (float) plugin.getConfigManager().getSonsConfig().getDouble("menu.clique.pitch", 1.0);

        VersionUtils.playSound(player, sound, volume, pitch);
    }

    /**
     * Envia uma notificação de sucesso para um jogador
     *
     * @param player Jogador
     * @param message Mensagem de sucesso
     */
    public static void sendSuccessNotification(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + message));
        VersionUtils.playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);
    }

    /**
     * Envia uma notificação de sucesso para um jogador com som configurável
     *
     * @param plugin Instância do plugin
     * @param player Jogador
     * @param message Mensagem de sucesso
     */
    public static void sendSuccessNotification(Main plugin, Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + message));

        String sound = plugin.getConfigManager().getSonsConfig().getString("pontos.adicionar.nome", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = (float) plugin.getConfigManager().getSonsConfig().getDouble("pontos.adicionar.volume", 1.0);
        float pitch = (float) plugin.getConfigManager().getSonsConfig().getDouble("pontos.adicionar.pitch", 1.0);

        VersionUtils.playSound(player, sound, volume, pitch);
    }
}