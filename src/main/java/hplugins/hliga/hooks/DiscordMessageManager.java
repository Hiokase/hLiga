package hplugins.hliga.hooks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.models.Season;
import hplugins.hliga.utils.LogUtils;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gerenciador para as mensagens enviadas para o Discord.
 * Utiliza configurações armazenadas em discord.json
 */
@Getter
public class DiscordMessageManager {
    private final Main plugin;
    /**
     * -- GETTER --
     *  Obtém as configurações de mensagens do Discord
     *
     * @return JsonObject com as configurações
     */
    private JsonObject messagesConfig;
    private final Gson gson = new Gson();

    public DiscordMessageManager(Main plugin) {
        this.plugin = plugin;
        loadDiscordConfig();
    }

    /**
     * Carrega as configurações do arquivo discord.json
     */
    private void loadDiscordConfig() {
        File dataFolder = plugin.getDataFolder();
        File configFile = new File(dataFolder, "discord.json");

        if (!configFile.exists()) {
            try {
                dataFolder.mkdirs();

                java.io.InputStream inputStream = plugin.getResource("discord.json");

                if (inputStream == null) {
                    plugin.getLogger().warning("Recurso discord.json não encontrado no plugin, criando modelo padrão...");
                    createDefaultDiscordJson(configFile);
                } else {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                        StringBuilder jsonContent = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            jsonContent.append(line).append("\n");
                        }

                        Files.write(configFile.toPath(), jsonContent.toString().getBytes(StandardCharsets.UTF_8));
                        LogUtils.debug("Arquivo discord.json criado com sucesso!");
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao criar arquivo discord.json: " + e.getMessage(), e);
                createDefaultDiscordJson(configFile);
                return;
            }
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(configFile), StandardCharsets.UTF_8))) {
            messagesConfig = gson.fromJson(reader, JsonObject.class);

            if (messagesConfig == null) {
                plugin.getLogger().warning("Arquivo discord.json inválido, criando modelo padrão...");
                createDefaultDiscordJson(configFile);
                try (BufferedReader newReader = new BufferedReader(
                        new InputStreamReader(new java.io.FileInputStream(configFile), StandardCharsets.UTF_8))) {
                    messagesConfig = gson.fromJson(newReader, JsonObject.class);
                }
            } else {
                LogUtils.debug("Configurações de mensagens do Discord carregadas com sucesso!");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao carregar discord.json: " + e.getMessage(), e);
            createDefaultDiscordJson(configFile);
            messagesConfig = new JsonObject();
        }
    }

    /**
     * Cria um embed para início de temporada
     *
     * @param season Temporada iniciada
     * @return WebhookEmbed configurado
     */
    public DiscordWebhook.WebhookEmbed createSeasonStartEmbed(Season season) {
        if (messagesConfig == null || !messagesConfig.has("mensagens") ||
                !messagesConfig.getAsJsonObject("mensagens").has("temporada_iniciada")) {
            return createDefaultSeasonStartEmbed(season);
        }

        try {
            JsonObject config = messagesConfig.getAsJsonObject("mensagens")
                    .getAsJsonObject("temporada_iniciada");

            DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder();

            String title = replaceTokens(
                    config.has("titulo") ? config.get("titulo").getAsString() : "Nova Temporada Iniciada",
                    createSeasonTokenMap(season)
            );

            String description = replaceTokens(
                    config.has("descricao") ? config.get("descricao").getAsString() : "Uma nova temporada foi iniciada!",
                    createSeasonTokenMap(season)
            );

            builder.title(title)
                    .description(description);

            if (config.has("cor")) {
                builder.color(config.get("cor").getAsInt());
            }

            if (config.has("thumbnail") && !config.get("thumbnail").getAsString().isEmpty()) {
                builder.thumbnail(config.get("thumbnail").getAsString());
            }

            if (config.has("campos") && config.get("campos").isJsonArray()) {
                JsonArray fieldsArray = config.getAsJsonArray("campos");

                for (JsonElement fieldElement : fieldsArray) {
                    if (fieldElement.isJsonObject()) {
                        JsonObject fieldObj = fieldElement.getAsJsonObject();

                        String name = replaceTokens(
                                fieldObj.has("nome") ? fieldObj.get("nome").getAsString() : "",
                                createSeasonTokenMap(season)
                        );

                        String value = replaceTokens(
                                fieldObj.has("valor") ? fieldObj.get("valor").getAsString() : "",
                                createSeasonTokenMap(season)
                        );

                        boolean inline = fieldObj.has("inline") && fieldObj.get("inline").getAsBoolean();

                        builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                .name(name)
                                .value(value)
                                .inline(inline)
                                .build());
                    }
                }
            }

            if (config.has("rodape")) {
                builder.footer(config.get("rodape").getAsString());
            }

            return builder.build();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao processar configuração do Discord para início de temporada", e);
            return createDefaultSeasonStartEmbed(season);
        }
    }

    /**
     * Cria um embed padrão para início de temporada
     */
    private DiscordWebhook.WebhookEmbed createDefaultSeasonStartEmbed(Season season) {
        return DiscordWebhook.WebhookEmbed.builder()
                .title("\uD83C\uDFC6 Nova Temporada Iniciada")
                .description("Uma nova temporada de ligas de clãs foi iniciada!")
                .color(3447003) // Azul
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Nome da Temporada")
                        .value("**" + season.name + "**")
                        .inline(false)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Data de Início")
                        .value(hplugins.hliga.utils.TimeUtils.formatDate(season.startDate))
                        .inline(true)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Data de Término")
                        .value(hplugins.hliga.utils.TimeUtils.formatDate(season.endDate))
                        .inline(true)
                        .build())
                .footer("hLiga - Sistema de Ligas de Clãs")
                .build();
    }

    /**
     * Cria um embed para fim de temporada
     *
     * @param season Temporada encerrada
     * @param topClans Top clãs da temporada
     * @return WebhookEmbed configurado
     */
    public DiscordWebhook.WebhookEmbed createSeasonEndEmbed(Season season, List<ClanPoints> topClans) {
        if (messagesConfig == null || !messagesConfig.has("mensagens") ||
                !messagesConfig.getAsJsonObject("mensagens").has("temporada_encerrada")) {
            return createDefaultSeasonEndEmbed(season, topClans);
        }

        try {
            JsonObject config = messagesConfig.getAsJsonObject("mensagens")
                    .getAsJsonObject("temporada_encerrada");

            DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder();

            Map<String, String> tokenMap = createSeasonTokenMap(season);
            addTopClansTokens(tokenMap, topClans);

            String title = replaceTokens(
                    config.has("titulo") ? config.get("titulo").getAsString() : "Temporada Encerrada",
                    tokenMap
            );

            String description = replaceTokens(
                    config.has("descricao") ? config.get("descricao").getAsString() : "A temporada foi encerrada!",
                    tokenMap
            );

            builder.title(title)
                    .description(description);

            if (config.has("cor")) {
                builder.color(config.get("cor").getAsInt());
            }

            if (config.has("thumbnail") && !config.get("thumbnail").getAsString().isEmpty()) {
                builder.thumbnail(config.get("thumbnail").getAsString());
            }

            if (config.has("campos") && config.get("campos").isJsonArray()) {
                JsonArray fieldsArray = config.getAsJsonArray("campos");

                for (JsonElement fieldElement : fieldsArray) {
                    if (fieldElement.isJsonObject()) {
                        JsonObject fieldObj = fieldElement.getAsJsonObject();

                        String name = replaceTokens(
                                fieldObj.has("nome") ? fieldObj.get("nome").getAsString() : "",
                                tokenMap
                        );

                        String value = replaceTokens(
                                fieldObj.has("valor") ? fieldObj.get("valor").getAsString() : "",
                                tokenMap
                        );

                        boolean inline = fieldObj.has("inline") && fieldObj.get("inline").getAsBoolean();

                        builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                .name(name)
                                .value(value)
                                .inline(inline)
                                .build());
                    }
                }
            }

            if (config.has("rodape")) {
                builder.footer(config.get("rodape").getAsString());
            }

            return builder.build();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao processar configuração do Discord para fim de temporada", e);
            return createDefaultSeasonEndEmbed(season, topClans);
        }
    }

    /**
     * Cria um embed padrão para fim de temporada
     */
    private DiscordWebhook.WebhookEmbed createDefaultSeasonEndEmbed(Season season, List<ClanPoints> topClans) {
        FileConfiguration config = plugin.getConfig();

        DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder embedBuilder = DiscordWebhook.WebhookEmbed.builder()
                .title("\uD83C\uDFC1 Temporada Encerrada")
                .description("A temporada **" + season.name + "** foi encerrada!")
                .color(15158332) // Vermelho-laranja
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Período")
                        .value(hplugins.hliga.utils.TimeUtils.formatDateRange(season.startDate, season.endDate))
                        .inline(false)
                        .build());

        int topLimit = Math.min(config.getInt("discord.top_resultados", 5), topClans.size());
        embedBuilder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                .name("Top Clãs")
                .value(getTopClansText(topClans, topLimit, config))
                .inline(false)
                .build());

        embedBuilder.footer("hLiga - Sistema de Ligas de Clãs");

        return embedBuilder.build();
    }

    /**
     * Cria um embed para pontos adicionados a um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos adicionados
     * @param totalPoints Pontos totais
     * @param position Posição no ranking
     * @return WebhookEmbed configurado
     */
    public DiscordWebhook.WebhookEmbed createClanPointsEmbed(String clanTag, int points, int totalPoints, int position) {
        return createClanPointsEmbed(clanTag, points, totalPoints, position, null);
    }

    /**
     * Cria um embed para notificação de pontos adicionados a um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos adicionados ou removidos (valor negativo)
     * @param totalPoints Pontos totais
     * @param position Posição no ranking
     * @param description Descrição opcional da operação
     * @return WebhookEmbed configurado
     */
    public DiscordWebhook.WebhookEmbed createClanPointsEmbed(String clanTag, int points, int totalPoints, int position, String description) {
        if (messagesConfig == null || !messagesConfig.has("mensagens") ||
                !messagesConfig.getAsJsonObject("mensagens").has("pontos_adicionados")) {
            return createDefaultClanPointsEmbed(clanTag, points, totalPoints, position, description);
        }

        try {
            JsonObject config = messagesConfig.getAsJsonObject("mensagens")
                    .getAsJsonObject(points >= 0 ? "pontos_adicionados" : "pontos_removidos");

            if (points < 0 && !messagesConfig.getAsJsonObject("mensagens").has("pontos_removidos")) {
                config = messagesConfig.getAsJsonObject("mensagens")
                        .getAsJsonObject("pontos_adicionados");
            }

            DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder();

            Map<String, String> tokenMap = createPointsTokenMap(clanTag, points, totalPoints, position);

            if (description != null && !description.isEmpty()) {
                tokenMap.put("descricao", description);
            }

            String defaultTitle = points >= 0 ? "Pontos Adicionados" : "Pontos Removidos";
            String defaultDesc = points >= 0 ?
                    "O clã **" + clanTag + "** recebeu pontos!" :
                    "O clã **" + clanTag + "** perdeu pontos!";

            String title = replaceTokens(
                    config.has("titulo") ? config.get("titulo").getAsString() : defaultTitle,
                    tokenMap
            );

            String embedDescription = replaceTokens(
                    config.has("descricao") ? config.get("descricao").getAsString() : defaultDesc,
                    tokenMap
            );

            builder.title(title)
                    .description(embedDescription);

            if (config.has("cor")) {
                builder.color(config.get("cor").getAsInt());
            }

            if (config.has("thumbnail") && !config.get("thumbnail").getAsString().isEmpty()) {
                builder.thumbnail(config.get("thumbnail").getAsString());
            }

            if (config.has("campos") && config.get("campos").isJsonArray()) {
                JsonArray fieldsArray = config.getAsJsonArray("campos");

                for (JsonElement fieldElement : fieldsArray) {
                    if (fieldElement.isJsonObject()) {
                        JsonObject fieldObj = fieldElement.getAsJsonObject();

                        String name = replaceTokens(
                                fieldObj.has("nome") ? fieldObj.get("nome").getAsString() : "",
                                tokenMap
                        );

                        String value = replaceTokens(
                                fieldObj.has("valor") ? fieldObj.get("valor").getAsString() : "",
                                tokenMap
                        );

                        boolean inline = fieldObj.has("inline") && fieldObj.get("inline").getAsBoolean();

                        builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                                .name(name)
                                .value(value)
                                .inline(inline)
                                .build());
                    }
                }
            }

            if (config.has("rodape")) {
                builder.footer(config.get("rodape").getAsString());
            }

            return builder.build();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao processar configuração do Discord para pontos", e);
            return createDefaultClanPointsEmbed(clanTag, points, totalPoints, position, description);
        }
    }

    /**
     * Cria um embed padrão para pontos adicionados a um clã
     */
    private DiscordWebhook.WebhookEmbed createDefaultClanPointsEmbed(
            String clanTag, int points, int totalPoints, int position) {
        return createDefaultClanPointsEmbed(clanTag, points, totalPoints, position, null);
    }

    /**
     * Cria um embed padrão para pontos adicionados a um clã, com descrição opcional
     */
    private DiscordWebhook.WebhookEmbed createDefaultClanPointsEmbed(
            String clanTag, int points, int totalPoints, int position, String description) {

        FileConfiguration config = plugin.getConfig();
        boolean isPositive = points >= 0;
        int absPoints = Math.abs(points);

        String pointsName = absPoints == 1 ?
                config.getString("pontos.nome", "ponto") :
                config.getString("pontos.nome_plural", "pontos");

        String title = isPositive ? "💰 Pontos Adicionados" : "⚠️ Pontos Removidos";
        String desc = isPositive ?
                "O clã **" + clanTag + "** recebeu pontos!" :
                "O clã **" + clanTag + "** perdeu pontos!";
        int color = isPositive ? 3066993 : 15105570; // Verde ou laranja
        String fieldName = isPositive ? "Pontos Adicionados" : "Pontos Removidos";

        DiscordWebhook.WebhookEmbed.WebhookEmbedBuilder builder = DiscordWebhook.WebhookEmbed.builder()
                .title(title)
                .description(desc)
                .color(color)
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name(fieldName)
                        .value("**" + absPoints + "** " + pointsName)
                        .inline(true)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Total de Pontos")
                        .value("**" + totalPoints + "** " + (totalPoints == 1 ?
                                config.getString("pontos.nome", "ponto") :
                                config.getString("pontos.nome_plural", "pontos")))
                        .inline(true)
                        .build())
                .addField(DiscordWebhook.WebhookEmbed.Field.builder()
                        .name("Posição Atual")
                        .value(position + "º lugar")
                        .inline(true)
                        .build());

        if (description != null && !description.isEmpty()) {
            builder.addField(DiscordWebhook.WebhookEmbed.Field.builder()
                    .name("Motivo")
                    .value(description)
                    .inline(false)
                    .build());
        }

        return builder.footer("hLiga - Sistema de Ligas de Clãs").build();
    }

    /**
     * Substitui tokens em uma string pelos seus valores
     *
     * @param input String de entrada com tokens no formato {token}
     * @param tokens Mapa de tokens e seus valores
     * @return String com tokens substituídos
     */
    private String replaceTokens(String input, Map<String, String> tokens) {
        if (input == null || input.isEmpty() || tokens == null || tokens.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }

    /**
     * Cria um mapa de tokens para temporada
     *
     * @param season Temporada
     * @return Mapa de tokens
     */
    private Map<String, String> createSeasonTokenMap(Season season) {
        Map<String, String> tokens = new HashMap<>();

        tokens.put("temporada_nome", season.name);
        tokens.put("temporada_inicio", hplugins.hliga.utils.TimeUtils.formatDate(season.startDate));
        tokens.put("temporada_fim", hplugins.hliga.utils.TimeUtils.formatDate(season.endDate));
        long duracao = season.endDate - season.startDate;
        tokens.put("temporada_duracao", hplugins.hliga.utils.TimeUtils.formatDuration(duracao));
        tokens.put("temporada_periodo", hplugins.hliga.utils.TimeUtils.formatDateRange(
                season.startDate, season.endDate
        ));

        return tokens;
    }

    /**
     * Adiciona tokens de top clãs ao mapa de tokens
     *
     * @param tokens Mapa de tokens
     * @param topClans Lista de top clãs
     */
    private void addTopClansTokens(Map<String, String> tokens, List<ClanPoints> topClans) {
        FileConfiguration config = plugin.getConfig();

        String topClansText = getTopClansText(topClans, config.getInt("discord.top_resultados", 5), config);
        tokens.put("top_clas", topClansText);

        int topLimit = Math.min(config.getInt("discord.top_resultados", 5), topClans.size());
        int validClans = 0;

        String medalFirst = "🥇";
        String medalSecond = "🥈";
        String medalThird = "🥉";

        if (messagesConfig != null && messagesConfig.has("mensagens") &&
                messagesConfig.getAsJsonObject("mensagens").has("medalhas")) {
            JsonObject medalhas = messagesConfig.getAsJsonObject("mensagens").getAsJsonObject("medalhas");

            if (medalhas.has("primeiro")) {
                medalFirst = medalhas.get("primeiro").getAsString();
            }

            if (medalhas.has("segundo")) {
                medalSecond = medalhas.get("segundo").getAsString();
            }

            if (medalhas.has("terceiro")) {
                medalThird = medalhas.get("terceiro").getAsString();
            }
        }

        for (int i = 0; i < topLimit; i++) {
            if (i >= topClans.size()) break;

            ClanPoints clanPoints = topClans.get(i);

            if (clanPoints.points <= 0) {
                continue;
            }

            validClans++;

            String medal;
            if (i == 0) medal = medalFirst;
            else if (i == 1) medal = medalSecond;
            else if (i == 2) medal = medalThird;
            else medal = (i + 1) + "º";

            tokens.put("top_cla_" + validClans + "_tag", clanPoints.clanTag);
            tokens.put("top_cla_" + validClans + "_pontos", String.valueOf(clanPoints.points));
            tokens.put("top_cla_" + validClans + "_medalha", medal);
        }

        tokens.put("top_clas_quantidade", String.valueOf(validClans));
    }

    /**
     * Cria um mapa de tokens para pontos de clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos adicionados/removidos
     * @param totalPoints Pontos totais
     * @param position Posição no ranking
     * @return Mapa de tokens
     */

    /**
     * Cria um arquivo discord.json padrão
     * @param configFile Arquivo de destino
     */
    private void createDefaultDiscordJson(File configFile) {
        JsonObject root = new JsonObject();
        JsonObject mensagens = new JsonObject();

        JsonObject tempIniciada = new JsonObject();
        tempIniciada.addProperty("titulo", "🏆 Nova Temporada Iniciada");
        tempIniciada.addProperty("descricao", "Uma nova temporada de ligas de clãs foi iniciada!");
        tempIniciada.addProperty("cor", 3447003);

        JsonArray camposIniciada = new JsonArray();

        JsonObject campoNome = new JsonObject();
        campoNome.addProperty("nome", "Nome da Temporada");
        campoNome.addProperty("valor", "{temporada_nome}");
        campoNome.addProperty("inline", false);
        camposIniciada.add(campoNome);

        JsonObject campoInicio = new JsonObject();
        campoInicio.addProperty("nome", "Data de Início");
        campoInicio.addProperty("valor", "{temporada_inicio}");
        campoInicio.addProperty("inline", true);
        camposIniciada.add(campoInicio);

        JsonObject campoFim = new JsonObject();
        campoFim.addProperty("nome", "Data de Término");
        campoFim.addProperty("valor", "{temporada_fim}");
        campoFim.addProperty("inline", true);
        camposIniciada.add(campoFim);

        JsonObject campoDuracao = new JsonObject();
        campoDuracao.addProperty("nome", "Duração");
        campoDuracao.addProperty("valor", "{temporada_duracao}");
        campoDuracao.addProperty("inline", true);
        camposIniciada.add(campoDuracao);

        tempIniciada.add("campos", camposIniciada);
        tempIniciada.addProperty("rodape", "hLiga - Sistema de Ligas de Clãs");
        tempIniciada.addProperty("thumbnail", "");

        JsonObject pontosAdd = new JsonObject();
        pontosAdd.addProperty("titulo", "💰 Pontos Adicionados");
        pontosAdd.addProperty("descricao", "O clã **{cla_tag}** recebeu pontos!");
        pontosAdd.addProperty("cor", 3066993);

        JsonArray camposPontosAdd = new JsonArray();

        JsonObject campoPontosAdd = new JsonObject();
        campoPontosAdd.addProperty("nome", "Pontos Adicionados");
        campoPontosAdd.addProperty("valor", "**{pontos_adicionados}** {pontos_nome}");
        campoPontosAdd.addProperty("inline", true);
        camposPontosAdd.add(campoPontosAdd);

        JsonObject campoTotal = new JsonObject();
        campoTotal.addProperty("nome", "Total de Pontos");
        campoTotal.addProperty("valor", "**{pontos_total}** {pontos_nome_plural}");
        campoTotal.addProperty("inline", true);
        camposPontosAdd.add(campoTotal);

        JsonObject campoPosicao = new JsonObject();
        campoPosicao.addProperty("nome", "Posição Atual");
        campoPosicao.addProperty("valor", "{posicao}º lugar");
        campoPosicao.addProperty("inline", true);
        camposPontosAdd.add(campoPosicao);

        JsonObject campoMotivo = new JsonObject();
        campoMotivo.addProperty("nome", "Motivo");
        campoMotivo.addProperty("valor", "{descricao}");
        campoMotivo.addProperty("inline", false);
        camposPontosAdd.add(campoMotivo);

        pontosAdd.add("campos", camposPontosAdd);
        pontosAdd.addProperty("rodape", "hLiga - Sistema de Ligas de Clãs");
        pontosAdd.addProperty("thumbnail", "");

        mensagens.add("temporada_iniciada", tempIniciada);
        mensagens.add("pontos_adicionados", pontosAdd);

        JsonObject cores = new JsonObject();
        cores.addProperty("azul", 3447003);
        cores.addProperty("verde", 3066993);
        cores.addProperty("vermelho", 15158332);
        cores.addProperty("amarelo", 16776960);

        mensagens.add("cores", cores);

        root.add("mensagens", mensagens);

        try {
            Files.write(configFile.toPath(), gson.toJson(root).getBytes(StandardCharsets.UTF_8));
            LogUtils.debug("Arquivo discord.json padrão criado com sucesso!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar arquivo discord.json padrão", e);
        }
    }

    private Map<String, String> createPointsTokenMap(String clanTag, int points, int totalPoints, int position) {
        Map<String, String> tokens = new HashMap<>();
        FileConfiguration config = plugin.getConfig();

        boolean isPositive = points >= 0;
        int absPoints = Math.abs(points);

        String pointsName = absPoints == 1 ?
                config.getString("pontos.nome", "ponto") :
                config.getString("pontos.nome_plural", "pontos");

        String totalPointsName = totalPoints == 1 ?
                config.getString("pontos.nome", "ponto") :
                config.getString("pontos.nome_plural", "pontos");

        tokens.put("cla_tag", clanTag);
        tokens.put("pontos_adicionados", String.valueOf(absPoints));
        tokens.put("pontos_removidos", String.valueOf(absPoints));
        tokens.put("pontos_total", String.valueOf(totalPoints));
        tokens.put("pontos_nome", pointsName);
        tokens.put("pontos_nome_plural", config.getString("pontos.nome_plural", "pontos"));
        tokens.put("posicao", String.valueOf(position));
        tokens.put("posicao_texto", position + "º lugar");
        tokens.put("acao", isPositive ? "recebeu" : "perdeu");

        tokens.put("descricao", "Sem motivo especificado");

        return tokens;
    }

    /**
     * Formata a lista de clãs para exibição no Discord
     *
     * @param topClans Lista de clãs ordenados por pontuação
     * @param topLimit Limite de clãs a exibir
     * @param config Configuração do plugin
     * @return Texto formatado com a lista de clãs
     */
    private String getTopClansText(List<ClanPoints> topClans, int topLimit, FileConfiguration config) {
        LogUtils.debug("[hLiga Debug] getTopClansText: Recebido " + topClans.size() + " clãs para formatar");

        if (topClans.isEmpty()) {
            plugin.getLogger().warning("[hLiga Debug] getTopClansText: Lista de clãs vazia!");

            List<GenericClan> clansAvailable = plugin.getClansManager().getAllClans();
            if (!clansAvailable.isEmpty()) {
                LogUtils.debug("[hLiga Debug] Existem " + clansAvailable.size() + " clãs disponíveis");

                for (int i = 0; i < Math.min(5, clansAvailable.size()); i++) {
                    GenericClan clan = clansAvailable.get(i);
                    LogUtils.debug("[hLiga Debug] Clã #" + i + ": " + clan.getTag() + " - " + clan.getName());
                }
            } else {
                plugin.getLogger().warning("[hLiga Debug] Não existem clãs disponíveis no sistema!");
            }

            return "Nenhum clã com pontuação registrada nesta temporada.";
        }

        List<ClanPoints> clansWithPoints = new java.util.ArrayList<>(topClans);
        clansWithPoints.removeIf(clan -> clan.getPoints() <= 0);

        LogUtils.debug("[hLiga Debug] getTopClansText: Após filtro, restaram " + clansWithPoints.size() + " clãs com pontos");

        if (clansWithPoints.isEmpty()) {
            return "Nenhum clã com pontuação registrada nesta temporada.";
        }

        StringBuilder topClansText = new StringBuilder();

        for (int i = 0; i < Math.min(topLimit, clansWithPoints.size()); i++) {
            ClanPoints clanPoints = clansWithPoints.get(i);

            String medal = "";
            if (i == 0) medal = "🥇 ";
            else if (i == 1) medal = "🥈 ";
            else if (i == 2) medal = "🥉 ";
            else medal = (i + 1) + "º ";

            String pointsName = clanPoints.points == 1 ?
                    config.getString("pontos.nome", "ponto") :
                    config.getString("pontos.nome_plural", "pontos");

            LogUtils.debug("[hLiga Debug] Adicionando clã " + clanPoints.getClanTag() + " com " + clanPoints.getPoints() + " pontos ao texto");

            topClansText.append(medal)
                    .append("**")
                    .append(clanPoints.clanTag)
                    .append("**: ")
                    .append(clanPoints.points)
                    .append(" ")
                    .append(pointsName)
                    .append("\n");
        }

        if (topClansText.isEmpty()) {
            plugin.getLogger().warning("[hLiga Debug] Texto final vazio após processamento dos clãs!");
            return "Nenhum clã com pontuação registrada nesta temporada.";
        }

        return topClansText.toString();
    }
}