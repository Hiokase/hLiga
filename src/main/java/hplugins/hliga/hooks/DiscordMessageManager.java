package hplugins.hliga.hooks;

import com.google.gson.*;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.Season;
import hplugins.hliga.utils.LogUtils;
import hplugins.hliga.utils.TimeUtils;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gerenciador de mensagens Discord para o sistema hLiga
 * Responsável por criar e formatar embeds personalizados para diferentes tipos de notificações
 */
public class DiscordMessageManager {

    private final Main plugin;
    private final Gson gson;
    private JsonObject messagesConfig;

    public DiscordMessageManager(Main plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadMessagesConfig();
    }

    /**
     * Carrega a configuração de mensagens do arquivo discord.json
     */
    public void loadMessagesConfig() {
        File configFile = new File(plugin.getDataFolder(), "discord.json");

        if (!configFile.exists()) {
            LogUtils.debugMedium("Arquivo discord.json não encontrado, criando a partir dos recursos...");
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }

                plugin.saveResource("discord.json", false);
                LogUtils.debugMedium("Arquivo discord.json criado com sucesso em: " + configFile.getAbsolutePath());
            } catch (Exception e) {
                LogUtils.warning("Erro ao criar arquivo discord.json: " + e.getMessage());
                messagesConfig = null;
                return;
            }
        }

        try {
            String content = readFileContent(configFile);
            LogUtils.debugMedium("=== CARREGANDO DISCORD.JSON ===");
            LogUtils.debugMedium("Arquivo: " + configFile.getAbsolutePath());
            LogUtils.debugMedium("Existe: " + configFile.exists());
            LogUtils.debugMedium("Tamanho: " + content.length() + " caracteres");
            LogUtils.debugMedium("Últimas modificações: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(configFile.lastModified())));

            JsonParser parser = new JsonParser();
            this.messagesConfig = parser.parse(content).getAsJsonObject();

            LogUtils.debugMedium("JSON parseado com sucesso. Seções encontradas:");

            if (messagesConfig.has("embed_nova_temporada")) {
                JsonObject config = messagesConfig.getAsJsonObject("embed_nova_temporada");
                boolean ativo = config.has("ativo") ? config.get("ativo").getAsBoolean() : false;
                String titulo = config.has("titulo") ? config.get("titulo").getAsString() : "NÃO DEFINIDO";
                String descricao = config.has("descricao") ? config.get("descricao").getAsString() : "NÃO DEFINIDO";
                LogUtils.debugMedium("- embed_nova_temporada: " + (ativo ? "ATIVO" : "INATIVO"));
                LogUtils.debugMedium("  └─ Título: " + titulo);
                LogUtils.debugMedium("  └─ Descrição: " + (descricao.length() > 50 ? descricao.substring(0, 50) + "..." : descricao));
            }

            if (messagesConfig.has("embed_fim_temporada")) {
                JsonObject config = messagesConfig.getAsJsonObject("embed_fim_temporada");
                boolean ativo = config.has("ativo") ? config.get("ativo").getAsBoolean() : false;
                LogUtils.debugMedium("- embed_fim_temporada: " + (ativo ? "ATIVO" : "INATIVO"));
            }

            if (messagesConfig.has("embed_pontos_adicionados")) {
                JsonObject config = messagesConfig.getAsJsonObject("embed_pontos_adicionados");
                boolean ativo = config.has("ativo") ? config.get("ativo").getAsBoolean() : false;
                LogUtils.debugMedium("- embed_pontos_adicionados: " + (ativo ? "ATIVO" : "INATIVO"));
            }

            if (messagesConfig.has("embed_pontos_removidos")) {
                JsonObject config = messagesConfig.getAsJsonObject("embed_pontos_removidos");
                boolean ativo = config.has("ativo") ? config.get("ativo").getAsBoolean() : false;
                LogUtils.debugMedium("- embed_pontos_removidos: " + (ativo ? "ATIVO" : "INATIVO"));
            }

            LogUtils.debugMedium("=== FIM CARREGAMENTO DISCORD.JSON ===");

        } catch (Exception e) {
            LogUtils.warning("Erro ao carregar configuração Discord: " + e.getMessage());
            e.printStackTrace();
            this.messagesConfig = null;
        }
    }

    /**
     * Recarrega as configurações do discord.json
     */
    public void reloadConfig() {
        LogUtils.debugMedium("Recarregando configurações do discord.json...");
        loadMessagesConfig();
    }

    /**
     * Lê o conteúdo de um arquivo
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return content.toString();
    }

    /**
     * Cria um embed para início de temporada
     */
    public DiscordWebhook.WebhookEmbed createSeasonStartEmbed(Season season) {
        LogUtils.debugMedium("=== CRIANDO EMBED NOVA TEMPORADA ===");
        LogUtils.debugMedium("messagesConfig disponível: " + (messagesConfig != null));

        if (messagesConfig != null && messagesConfig.has("embed_nova_temporada")) {
            LogUtils.debugMedium("Configuração embed_nova_temporada encontrada, processando...");
            try {
                JsonObject config = messagesConfig.getAsJsonObject("embed_nova_temporada");

                boolean ativo = config.has("ativo") ? config.get("ativo").getAsBoolean() : false;
                LogUtils.debugMedium("Embed ativo: " + ativo);

                if (!ativo) {
                    LogUtils.debugMedium("Embed desativado, usando configuração padrão");
                    return createDefaultSeasonStartEmbed(season);
                }

                DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder();

                String titulo = config.has("titulo") ?
                        config.get("titulo").getAsString() : "🏆 Nova Temporada Iniciada!";
                LogUtils.debugMedium("Título configurado: " + titulo);
                builder.title(titulo);

                String descricao = config.has("descricao") ?
                        config.get("descricao").getAsString() : "A temporada **{nome_temporada}** começou!";
                LogUtils.debugMedium("Descrição original: " + (descricao.length() > 100 ? descricao.substring(0, 100) + "..." : descricao));
                descricao = replacePlaceholders(descricao, season, null, null, null);
                LogUtils.debugMedium("Descrição final: " + (descricao.length() > 100 ? descricao.substring(0, 100) + "..." : descricao));
                builder.description(descricao);

                int cor = config.has("cor") ? config.get("cor").getAsInt() : 65280;
                LogUtils.debugMedium("Cor configurada: " + cor);
                builder.color(cor);

                if (config.has("thumbnail") && !config.get("thumbnail").getAsString().isEmpty()) {
                    String thumbnail = config.get("thumbnail").getAsString();
                    LogUtils.debugMedium("Thumbnail configurado: " + thumbnail);
                    builder.thumbnail(thumbnail);
                }

                if (config.has("campos")) {
                    JsonObject campos = config.getAsJsonObject("campos");

                    for (Map.Entry<String, JsonElement> entry : campos.entrySet()) {
                        try {
                            JsonObject campo = entry.getValue().getAsJsonObject();

                            if (campo.has("ativo") && campo.get("ativo").getAsBoolean()) {
                                String fieldTitle = campo.has("titulo") ? campo.get("titulo").getAsString() : "Campo";
                                String fieldValue = campo.has("valor") ? campo.get("valor").getAsString() : "";
                                boolean fieldInline = campo.has("inline") && campo.get("inline").getAsBoolean();

                                fieldValue = replacePlaceholders(fieldValue, season, null, null, null);

                                builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                        .name(fieldTitle)
                                        .value(fieldValue)
                                        .inline(fieldInline)
                                        .build());
                            }
                        } catch (Exception e) {
                            LogUtils.warning("Erro ao processar campo '" + entry.getKey() + "': " + e.getMessage());
                        }
                    }
                }

                String rodape = config.has("rodape") ?
                        config.get("rodape").getAsString() : "hLiga - Sistema de Ligas de Clãs";
                LogUtils.debugMedium("Rodapé configurado: " + rodape);
                builder.footer(rodape);

                LogUtils.debugMedium("=== EMBED NOVA TEMPORADA CRIADO COM SUCESSO ===");
                return builder.build();

            } catch (Exception e) {
                LogUtils.warning("Erro ao processar configuração do Discord (nova_temporada): " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            LogUtils.warning("USANDO FALLBACK! messagesConfig é null ou não tem embed_nova_temporada");
            LogUtils.warning("messagesConfig null: " + (messagesConfig == null));
            if (messagesConfig != null) {
                LogUtils.warning("embed_nova_temporada existe: " + messagesConfig.has("embed_nova_temporada"));
            }
        }

        LogUtils.warning("=== USANDO CONFIGURAÇÃO PADRÃO (FALLBACK) ===");
        return createDefaultSeasonStartEmbed(season);
    }

    /**
     * Cria um embed para final de temporada
     */
    public DiscordWebhook.WebhookEmbed createSeasonEndEmbed(Season season, List<ClanPoints> topClans) {
        List<ClanPoints> clansWithValidPoints = new ArrayList<>();
        if (topClans != null) {
            for (ClanPoints clan : topClans) {
                if (clan.getPoints() > 0) {
                    clansWithValidPoints.add(clan);
                }
            }
        }

        if (messagesConfig != null && messagesConfig.has("embed_fim_temporada")) {
            try {
                JsonObject config = messagesConfig.getAsJsonObject("embed_fim_temporada");

                if (!config.has("ativo") || !config.get("ativo").getAsBoolean()) {
                    return createDefaultSeasonEndEmbed(season, topClans);
                }

                DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder();

                String titulo = config.has("titulo") ?
                        config.get("titulo").getAsString() : "🏁 Temporada Finalizada!";
                builder.title(titulo);

                String descricao = config.has("descricao") ?
                        config.get("descricao").getAsString() : "A temporada **{nome_temporada}** chegou ao fim!";
                descricao = descricao.replace("{nome_temporada}", season.getName());
                builder.description(descricao);

                int cor = config.has("cor") ? config.get("cor").getAsInt() : 16766720;
                builder.color(cor);

                if (config.has("thumbnail") && !config.get("thumbnail").getAsString().isEmpty()) {
                    builder.thumbnail(config.get("thumbnail").getAsString());
                }

                if (config.has("ranking")) {
                    JsonObject rankingConfig = config.getAsJsonObject("ranking");

                    if (rankingConfig.has("ativo") && rankingConfig.get("ativo").getAsBoolean()) {
                        String tituloRanking = rankingConfig.has("titulo") ?
                                rankingConfig.get("titulo").getAsString() : "🏆 Ranking Final";

                        if (!clansWithValidPoints.isEmpty()) {
                            int mostrarTop = rankingConfig.has("mostrar_top") ?
                                    rankingConfig.get("mostrar_top").getAsInt() : 3;

                            StringBuilder topText = new StringBuilder();
                            for (int i = 0; i < Math.min(mostrarTop, clansWithValidPoints.size()); i++) {
                                ClanPoints clan = clansWithValidPoints.get(i);

                                String medal = "🏆";
                                if (rankingConfig.has("medalhas")) {
                                    JsonObject medalhas = rankingConfig.getAsJsonObject("medalhas");
                                    if (i == 0 && medalhas.has("primeiro")) {
                                        medal = medalhas.get("primeiro").getAsString();
                                    } else if (i == 1 && medalhas.has("segundo")) {
                                        medal = medalhas.get("segundo").getAsString();
                                    } else if (i == 2 && medalhas.has("terceiro")) {
                                        medal = medalhas.get("terceiro").getAsString();
                                    }
                                }

                                topText.append(medal).append(" **").append(clan.getClanTag()).append("**: ")
                                        .append(clan.getPoints()).append(" pontos\n");
                            }

                            builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                    .name(tituloRanking)
                                    .value(topText.toString().trim())
                                    .inline(false)
                                    .build());
                        } else {
                            String semGanhadores = rankingConfig.has("sem_ganhadores") ?
                                    rankingConfig.get("sem_ganhadores").getAsString() : "Não houve ganhadores";

                            builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                    .name(tituloRanking)
                                    .value(semGanhadores)
                                    .inline(false)
                                    .build());
                        }
                    }
                }

                if (config.has("info_temporada")) {
                    JsonObject infoConfig = config.getAsJsonObject("info_temporada");

                    if (infoConfig.has("ativo") && infoConfig.get("ativo").getAsBoolean()) {
                        String tituloInfo = infoConfig.has("titulo") ?
                                infoConfig.get("titulo").getAsString() : "ℹ️ Informações";
                        String valorInfo = infoConfig.has("valor") ?
                                infoConfig.get("valor").getAsString() : "Duração: {duracao_dias} dias";
                        boolean inline = infoConfig.has("inline") && infoConfig.get("inline").getAsBoolean();

                        valorInfo = valorInfo.replace("{duracao_dias}", String.valueOf(season.getDurationDays()));

                        builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                .name(tituloInfo)
                                .value(valorInfo)
                                .inline(inline)
                                .build());
                    }
                }

                String rodape = config.has("rodape") ?
                        config.get("rodape").getAsString() : "hLiga - Sistema de Ligas de Clãs";
                builder.footer(rodape);

                return builder.build();

            } catch (Exception e) {
                LogUtils.warning("Erro ao processar configuração do Discord (fim_temporada): " + e.getMessage());
            }
        }

        return createDefaultSeasonEndEmbed(season, topClans);
    }

    /**
     * Cria um embed para pontos de clãs (adicionados ou removidos)
     */
    public DiscordWebhook.WebhookEmbed createClanPointsEmbed(String clanTag, int points, int totalPoints, int position, String description) {
        boolean isAdding = points >= 0;
        String embedType = isAdding ? "embed_pontos_adicionados" : "embed_pontos_removidos";

        if (messagesConfig != null && messagesConfig.has(embedType)) {
            try {
                JsonObject config = messagesConfig.getAsJsonObject(embedType);

                if (!config.has("ativo") || !config.get("ativo").getAsBoolean()) {
                    return createDefaultClanPointsEmbed(clanTag, points, totalPoints, position, description);
                }

                DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder();

                String titulo = config.has("titulo") ?
                        config.get("titulo").getAsString() : (isAdding ? "➕ Pontos Adicionados" : "➖ Pontos Removidos");
                builder.title(titulo);

                String descricao = config.has("descricao") ?
                        config.get("descricao").getAsString() :
                        "O clã **{clan_tag}** " + (isAdding ? "recebeu" : "perdeu") + " **{pontos}** pontos!";

                ClanPoints tempClanPoints = new ClanPoints(clanTag, totalPoints);
                descricao = replacePlaceholders(descricao, null, tempClanPoints, description, points);
                builder.description(descricao);

                int cor = config.has("cor") ? config.get("cor").getAsInt() : (isAdding ? 65280 : 16711680);
                builder.color(cor);

                if (config.has("thumbnail") && !config.get("thumbnail").getAsString().isEmpty()) {
                    builder.thumbnail(config.get("thumbnail").getAsString());
                }

                if (config.has("campos")) {
                    JsonObject campos = config.getAsJsonObject("campos");

                    for (Map.Entry<String, JsonElement> entry : campos.entrySet()) {
                        try {
                            JsonObject campo = entry.getValue().getAsJsonObject();

                            if (campo.has("ativo") && campo.get("ativo").getAsBoolean()) {
                                String fieldTitle2 = campo.has("titulo") ? campo.get("titulo").getAsString() : "Campo";
                                String fieldValue2 = campo.has("valor") ? campo.get("valor").getAsString() : "";
                                boolean fieldInline2 = campo.has("inline") && campo.get("inline").getAsBoolean();

                                fieldValue2 = replacePlaceholders(fieldValue2, null, tempClanPoints, description, points);

                                builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                        .name(fieldTitle2)
                                        .value(fieldValue2)
                                        .inline(fieldInline2)
                                        .build());
                            }
                        } catch (Exception e) {
                            LogUtils.warning("Erro ao processar campo '" + entry.getKey() + "': " + e.getMessage());
                        }
                    }
                }

                String rodape = config.has("rodape") ?
                        config.get("rodape").getAsString() : "hLiga - Sistema de Pontuação";
                builder.footer(rodape);

                return builder.build();

            } catch (Exception e) {
                LogUtils.warning("Erro ao processar configuração do Discord (" + embedType + "): " + e.getMessage());
            }
        }

        return createDefaultClanPointsEmbed(clanTag, points, totalPoints, position, description);
    }

    /**
     * Método auxiliar para adicionar campos se estiverem ativos
     */
    private void addFieldIfActive(DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder, JsonObject campos,
                                  String fieldName, String defaultTitle, String value, boolean inline) {
        if (campos.has(fieldName)) {
            JsonObject campo = campos.getAsJsonObject(fieldName);
            if (campo.has("ativo") && campo.get("ativo").getAsBoolean()) {
                String titulo = campo.has("titulo") ? campo.get("titulo").getAsString() : defaultTitle;
                String valorFinal = campo.has("valor") ? campo.get("valor").getAsString() : value;
                boolean isInline = campo.has("inline") ? campo.get("inline").getAsBoolean() : inline;

                valorFinal = valorFinal.replace("{valor}", value);

                builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name(titulo)
                        .value(valorFinal)
                        .inline(isInline)
                        .build());
            }
        }
    }



    /**
     * Cria um embed para pontos adicionados a um clã
     */
    public DiscordWebhook.WebhookEmbed createPointsAddedEmbed(String clanTag, int points, int totalPoints, String description) {
        int position = calcularPosicaoClan(clanTag);
        return createClanPointsEmbed(clanTag, points, totalPoints, position, description);
    }

    /**
     * Cria um embed para pontos removidos de um clã
     */
    public DiscordWebhook.WebhookEmbed createPointsRemovedEmbed(String clanTag, int points, int totalPoints, String description) {
        int position = calcularPosicaoClan(clanTag);
        return createClanPointsEmbed(clanTag, -points, totalPoints, position, description);
    }

    /**
     * Cria um embed padrão para início de temporada
     */
    private DiscordWebhook.WebhookEmbed createDefaultSeasonStartEmbed(Season season) {
        DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder()
                .title("🏆 Nova Temporada Iniciada!")
                .description("Uma nova temporada de liga de clãs foi iniciada!")
                .color(0x00FF00)
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Nome da Temporada")
                        .value(season.getName())
                        .inline(true)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Data de Início")
                        .value(season.getStartDate() != 0 ?
                                TimeUtils.formatDate(season.getStartDate()) : "Agora")
                        .inline(true)
                        .build())
                .footer("hLiga - Sistema de Ligas de Clãs");

        if (season.getEndDate() != 0) {
            builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                    .name("Data de Término")
                    .value(TimeUtils.formatDate(season.getEndDate()))
                    .inline(true)
                    .build());
        }

        return builder.build();
    }

    /**
     * Cria um embed padrão para final de temporada
     */
    private DiscordWebhook.WebhookEmbed createDefaultSeasonEndEmbed(Season season, List<ClanPoints> topClans) {
        DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder()
                .title("🏁 Temporada Finalizada!")
                .description("A temporada **" + season.getName() + "** foi finalizada!")
                .color(0xFFD700)
                .footer("hLiga - Sistema de Ligas de Clãs");

        if (!topClans.isEmpty()) {
            List<ClanPoints> clansWithValidPoints = new ArrayList<>();
            for (ClanPoints clan : topClans) {
                if (clan.getPoints() > 0) {
                    clansWithValidPoints.add(clan);
                }
            }

            if (!clansWithValidPoints.isEmpty()) {
                StringBuilder topText = new StringBuilder();
                for (int i = 0; i < Math.min(3, clansWithValidPoints.size()); i++) {
                    ClanPoints clan = clansWithValidPoints.get(i);
                    String medal = (i == 0) ? "🥇" : (i == 1) ? "🥈" : "🥉";
                    topText.append(medal).append(" **").append(clan.getClanTag()).append("**: ")
                            .append(clan.getPoints()).append(" pontos\n");
                }

                builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("🏆 Ranking Final")
                        .value(topText.toString().trim())
                        .inline(false)
                        .build());
            } else {
                builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("🏆 Ranking Final")
                        .value("Não houve ganhadores")
                        .inline(false)
                        .build());
            }
        } else {
            builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                    .name("🏆 Ranking Final")
                    .value("Não houve ganhadores")
                    .inline(false)
                    .build());
        }

        return builder.build();
    }

    /**
     * Cria um embed padrão para pontos de clãs
     */
    private DiscordWebhook.WebhookEmbed createDefaultClanPointsEmbed(String clanTag, int points, int totalPoints, int position, String description) {
        boolean isAdding = points >= 0;

        DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder()
                .title(isAdding ? "➕ Pontos Adicionados" : "➖ Pontos Removidos")
                .description("O clã **" + clanTag + "** " + (isAdding ? "recebeu" : "perdeu") + " **" +
                        Math.abs(points) + "** pontos!")
                .color(isAdding ? 0x00FF00 : 0xFF0000)
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("👥 Clã")
                        .value(clanTag)
                        .inline(true)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name(isAdding ? "➕ Pontos Ganhos" : "➖ Pontos Perdidos")
                        .value(String.valueOf(Math.abs(points)))
                        .inline(true)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("💰 Total de Pontos")
                        .value(String.valueOf(totalPoints))
                        .inline(true)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("🏆 Posição no Ranking")
                        .value(position + "º lugar")
                        .inline(true)
                        .build())
                .footer("hLiga - Sistema de Pontuação");

        if (description != null && !description.isEmpty()) {
            builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                    .name("📝 Motivo")
                    .value(description)
                    .inline(false)
                    .build());
        }

        return builder.build();
    }

    /**
     * Substitui placeholders em strings com dados reais
     */
    private String replacePlaceholders(String text, Season season, ClanPoints clanPoints, String motivo, Integer pontos) {
        if (text == null) return "";

        String result = text;

        if (season != null) {
            result = result.replace("{nome_temporada}", season.getName());
            result = result.replace("{duracao_dias}", String.valueOf(season.getDurationDays()));

            String dataInicio = season.getStartDate() != 0 ?
                    TimeUtils.formatDate(season.getStartDate()) : "Agora";
            String dataFim = season.getEndDate() != 0 ?
                    TimeUtils.formatDate(season.getEndDate()) : "Indefinido";

            result = result.replace("{data_inicio}", dataInicio);
            result = result.replace("{data_fim}", dataFim);

            try {
                List<ClanPoints> allClans = plugin.getPointsManager().getTopClans(1000);
                int totalClans = allClans.size();
                int totalClansValidos = 0;
                for (ClanPoints clan : allClans) {
                    if (clan.getPoints() > 0) totalClansValidos++;
                }

                result = result.replace("{total_clans}", String.valueOf(totalClans));
                result = result.replace("{total_clans_validos}", String.valueOf(totalClansValidos));
            } catch (Exception e) {
                result = result.replace("{total_clans}", "0");
                result = result.replace("{total_clans_validos}", "0");
            }
        }

        if (clanPoints != null) {
            result = result.replace("{clan_tag}", clanPoints.getClanTag());
            result = result.replace("{total_pontos}", String.valueOf(clanPoints.getPoints()));
            result = result.replace("{posicao}", String.valueOf(calcularPosicaoClan(clanPoints.getClanTag())));
        }

        if (pontos != null) {
            result = result.replace("{pontos}", String.valueOf(Math.abs(pontos)));
        }

        if (motivo != null && !motivo.trim().isEmpty()) {
            result = result.replace("{motivo}", motivo);
        } else {
            result = result.replace("{motivo}", "Operação administrativa");
        }

        result = result.replace("{timestamp}", String.valueOf(System.currentTimeMillis() / 1000));
        result = result.replace("{servidor}", Bukkit.getServer().getName());
        result = result.replace("{versao_plugin}", plugin.getDescription().getVersion());

        return result;
    }

    /**
     * Calcula a posição de um clã no ranking
     */
    private int calcularPosicaoClan(String clanTag) {
        try {
            List<ClanPoints> ranking = plugin.getPointsManager().getTopClans(100);
            for (int i = 0; i < ranking.size(); i++) {
                if (ranking.get(i).getClanTag().equals(clanTag)) {
                    return i + 1;
                }
            }
        } catch (Exception e) {
            LogUtils.warning("Erro ao calcular posição do clã: " + e.getMessage());
        }
        return 1; // Fallback
    }
}