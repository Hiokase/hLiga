package hplugins.hliga.hooks;

import com.google.gson.JsonObject;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.Season;
import hplugins.hliga.utils.LogUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DiscordWebhook {

    private final Main plugin;
    private final String webhookUrl; // Webhook principal para temporadas
    private final String staffWebhookUrl; // Webhook para logs da staff (pontos)
    private final String botName;
    private final String botAvatar;
    private final int embedColor;
    private final DiscordMessageManager messageManager;

    public DiscordWebhook(Main plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();

        this.webhookUrl = config.getString("discord.webhook_url", "");
        this.staffWebhookUrl = config.getString("discord.staff_webhook_url", "");
        this.botName = config.getString("discord.bot_name", "hLiga");
        this.botAvatar = config.getString("discord.bot_avatar", "");
        this.embedColor = config.getInt("discord.cor", 3066993);

        this.messageManager = new DiscordMessageManager(plugin);

        if (!this.webhookUrl.isEmpty()) {
            LogUtils.debug("Webhook principal do Discord configurado.");
        }

        if (!this.staffWebhookUrl.isEmpty()) {
            LogUtils.debug("Webhook de staff do Discord configurado.");
        }
    }

    /**
     * Envia uma mensagem para o webhook do Discord
     *
     * @param embed Embed a ser enviado
     * @param useStaffWebhook Se true, usa o webhook de staff, caso contrário usa o webhook principal
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    public boolean sendMessage(WebhookEmbed embed, boolean useStaffWebhook) {
        String targetWebhook = useStaffWebhook ? staffWebhookUrl : webhookUrl;

        if (!isValidWebhookUrl(targetWebhook)) {
            String webhookType = useStaffWebhook ? "staff" : "principal";
            plugin.getLogger().warning("URL do webhook " + webhookType + " do Discord não configurada ou inválida. Mensagem não enviada.");
            return false;
        }

        try {
            URL url = new URL(targetWebhook);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "hLiga/1.0");
            connection.setDoOutput(true);

            JsonObject json = new JsonObject();

            if (botName != null && !botName.isEmpty()) {
                json.addProperty("username", botName);
            }

            if (botAvatar != null && !botAvatar.isEmpty()) {
                json.addProperty("avatar_url", botAvatar);
            }

            if (embed.getContent() != null && !embed.getContent().isEmpty()) {
                json.addProperty("content", embed.getContent());
            }

            JsonObject embedJson = new JsonObject();

            if (embed.getTitle() != null && !embed.getTitle().isEmpty()) {
                embedJson.addProperty("title", embed.getTitle());
            }

            if (embed.getDescription() != null && !embed.getDescription().isEmpty()) {
                embedJson.addProperty("description", embed.getDescription());
            }

            if (embed.getColor() != 0) {
                embedJson.addProperty("color", embed.getColor());
            } else {
                embedJson.addProperty("color", embedColor);
            }

            if (embed.getFooter() != null && !embed.getFooter().isEmpty()) {
                JsonObject footerJson = new JsonObject();
                footerJson.addProperty("text", embed.getFooter());
                embedJson.add("footer", footerJson);
            }

            if (embed.getThumbnail() != null && !embed.getThumbnail().isEmpty()) {
                JsonObject thumbnailJson = new JsonObject();
                thumbnailJson.addProperty("url", embed.getThumbnail());
                embedJson.add("thumbnail", thumbnailJson);
            }

            if (!embed.getFields().isEmpty()) {
                JsonObject[] fieldsJson = new JsonObject[embed.getFields().size()];

                for (int i = 0; i < embed.getFields().size(); i++) {
                    WebhookEmbed.Field field = embed.getFields().get(i);

                    JsonObject fieldJson = new JsonObject();
                    fieldJson.addProperty("name", field.name);
                    fieldJson.addProperty("value", field.value);
                    fieldJson.addProperty("inline", field.inline);

                    fieldsJson[i] = fieldJson;
                }

                embedJson.add("fields", gson.toJsonTree(fieldsJson));
            }

            JsonObject[] embedsJson = new JsonObject[1];
            embedsJson[0] = embedJson;
            json.add("embeds", gson.toJsonTree(embedsJson));

            OutputStream os = null;
            try {
                os = connection.getOutputStream();
                byte[] input = json.toString().getBytes("UTF-8");
                os.write(input, 0, input.length);
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                return true;
            } else {
                String errorMessage = "Erro ao enviar mensagem para o Discord. Código: " + responseCode;
                if (responseCode == 400) {
                    errorMessage += " - Payload inválido. Verifique o formato da mensagem.";
                } else if (responseCode == 401 || responseCode == 403) {
                    errorMessage += " - Webhook inválido. Verifique se a URL está correta em config.yml.";
                } else if (responseCode == 404) {
                    errorMessage += " - Webhook não encontrado. O webhook pode ter sido excluído no Discord.";
                } else if (responseCode == 429) {
                    errorMessage += " - Muitas solicitações. Tente novamente mais tarde.";
                } else if (responseCode >= 500) {
                    errorMessage += " - Erro nos servidores do Discord. Tente novamente mais tarde.";
                }
                plugin.getLogger().warning(errorMessage);
                return false;
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao enviar mensagem para o Discord", e);
            return false;
        }
    }

    /**
     * Método de conveniência para manter compatibilidade com código existente
     * Envia uma mensagem para o webhook principal
     *
     * @param embed Embed a ser enviado
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    public boolean sendMessage(WebhookEmbed embed) {
        return sendMessage(embed, false); // Usa o webhook principal por padrão
    }

    /**
     * Envia notificação quando um clã recebe ou perde pontos
     *
     * @param clanTag Tag do clã
     * @param points Pontos adicionados (positivo) ou removidos (negativo)
     * @param totalPoints Total de pontos do clã após a operação
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    public boolean sendClanPointsNotification(String clanTag, int points, int totalPoints) {
        return sendClanPointsNotification(clanTag, points, totalPoints, null);
    }

    /**
     * Envia notificação para o Discord quando pontos são modificados em um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos adicionados (positivo) ou removidos (negativo)
     * @param totalPoints Total de pontos do clã após a operação
     * @param description Descrição opcional da operação
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    public boolean sendClanPointsNotification(String clanTag, int points, int totalPoints, String description) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("discord.anunciar_pontos", true)) {
            LogUtils.debug("Notificações de pontos Discord desabilitadas");
            return false;
        }

        int minPoints = config.getInt("discord.minimo_pontos_anuncio", 0);
        if (points > 0 && points < minPoints) {
            LogUtils.debug("Pontos abaixo do mínimo para anúncio (" + points + " < " + minPoints + ")");
            return false;
        }

        if (clanTag == null || clanTag.trim().isEmpty()) {
            LogUtils.warning("Tag do clã inválida para notificação Discord");
            return false;
        }

        try {
            int position = calcularPosicaoClan(clanTag);

            WebhookEmbed embed = messageManager.createClanPointsEmbed(clanTag, points, totalPoints, position, description);

            if (embed == null) {
                LogUtils.warning("Erro ao criar embed de pontos para Discord");
                return false;
            }

            LogUtils.debug("Enviando notificação de pontos para o webhook de staff (clã: " + clanTag + ", pontos: " + points + ")");
            return sendMessage(embed, true); // true = usar webhook de staff

        } catch (Exception e) {
            LogUtils.warning("Erro ao enviar notificação de pontos para Discord: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envia notificação quando uma temporada começa
     *
     * @param season Temporada iniciada
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    public boolean sendSeasonStartNotification(Season season) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("discord.anunciar_temporadas", true)) {
            LogUtils.debug("Notificações de temporadas Discord desabilitadas");
            return false;
        }

        if (season == null) {
            LogUtils.warning("Temporada nula para notificação Discord de início");
            return false;
        }

        try {
            WebhookEmbed embed = messageManager.createSeasonStartEmbed(season);

            if (embed == null) {
                LogUtils.warning("Erro ao criar embed de início de temporada para Discord");
                return false;
            }

            LogUtils.debug("Enviando notificação de início de temporada para Discord (temporada: " + season.getName() + ")");
            return sendMessage(embed); // Usar webhook principal para temporadas

        } catch (Exception e) {
            LogUtils.warning("Erro ao enviar notificação de início de temporada para Discord: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envia notificação quando uma temporada termina
     *
     * @param season Temporada encerrada
     * @param topClans Top clãs da temporada (já filtrados)
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    public boolean sendSeasonEndNotification(Season season, List<ClanPoints> topClans) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("discord.anunciar_temporadas", true) ||
                !config.getBoolean("discord.anunciar_resultados", true)) {
            LogUtils.debug("Notificações de fim de temporada Discord desabilitadas");
            return false;
        }

        if (season == null) {
            LogUtils.warning("Temporada nula para notificação Discord de fim");
            return false;
        }

        try {
            if (topClans == null) {
                topClans = new ArrayList<>();
            }

            WebhookEmbed embed = messageManager.createSeasonEndEmbed(season, topClans);

            if (embed == null) {
                LogUtils.warning("Erro ao criar embed de fim de temporada para Discord");
                return false;
            }

            LogUtils.debug("Enviando notificação de fim de temporada para Discord (temporada: " + season.getName() + ", clãs: " + topClans.size() + ")");
            return sendMessage(embed); // Usar webhook principal para temporadas

        } catch (Exception e) {
            LogUtils.warning("Erro ao enviar notificação de fim de temporada para Discord: " + e.getMessage());
            return false;
        }
    }

    @Setter
    public static class WebhookEmbed {
        private String title;
        private String description;
        private int color;
        private String footer;
        private String thumbnail;
        private String content;
        private List<Field> fields = new ArrayList<>();

        private WebhookEmbed() {}

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public int getColor() {
            return color;
        }

        public String getFooter() {
            return footer;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public String getContent() {
            return content;
        }

        public List<Field> getFields() {
            return fields;
        }

        public void addField(String name, String value, boolean inline) {
            fields.add(new Field(name, value, inline));
        }

        public static WebhookEmbedBuilder builder() {
            return new WebhookEmbedBuilder();
        }

        public static class WebhookEmbedBuilder {
            private final WebhookEmbed embed = new WebhookEmbed();

            public WebhookEmbedBuilder title(String title) {
                embed.title = title;
                return this;
            }

            public WebhookEmbedBuilder description(String description) {
                embed.description = description;
                return this;
            }

            public WebhookEmbedBuilder color(int color) {
                embed.color = color;
                return this;
            }

            public WebhookEmbedBuilder footer(String footer) {
                embed.footer = footer;
                return this;
            }

            public WebhookEmbedBuilder thumbnail(String thumbnail) {
                embed.thumbnail = thumbnail;
                return this;
            }

            public WebhookEmbedBuilder content(String content) {
                embed.content = content;
                return this;
            }

            public WebhookEmbedBuilder addField(Field field) {
                embed.fields.add(field);
                return this;
            }

            public WebhookEmbed build() {
                return embed;
            }
        }

        @Getter
        @Setter
        public static class Field {
            private String name;
            private String value;
            private boolean inline;

            public Field(String name, String value, boolean inline) {
                this.name = name;
                this.value = value;
                this.inline = inline;
            }

            public static FieldBuilder builder() {
                return new FieldBuilder();
            }

            public static class FieldBuilder {
                private String name;
                private String value;
                private boolean inline;

                public FieldBuilder name(String name) {
                    this.name = name;
                    return this;
                }

                public FieldBuilder value(String value) {
                    this.value = value;
                    return this;
                }

                public FieldBuilder inline(boolean inline) {
                    this.inline = inline;
                    return this;
                }

                public Field build() {
                    return new Field(name, value, inline);
                }
            }
        }
    }

    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    /**
     * Verifica se a URL do webhook é válida e bem formatada
     * Uma URL válida deve seguir este formato:
     * https://discord.com/api/webhooks/ID/TOKEN
     *
     * @param url URL a ser validada
     * @return true se a URL for válida
     */
    private boolean isValidWebhookUrl(String url) {
        if (url == null || url.isEmpty()) {
            plugin.getLogger().warning("URL do webhook do Discord não configurada em config.yml");
            return false;
        }

        if (!url.startsWith("https://discord.com/api/webhooks/") &&
                !url.startsWith("https://discordapp.com/api/webhooks/")) {
            plugin.getLogger().warning("URL do webhook do Discord inválida. Deve começar com https://discord.com/api/webhooks/ ou https://discordapp.com/api/webhooks/");
            return false;
        }

        String[] parts = url.split("/");
        if (parts.length < 7) {
            plugin.getLogger().warning("URL do webhook do Discord mal formatada. Deve conter ID e TOKEN no formato: https://discord.com/api/webhooks/ID/TOKEN");
            return false;
        }

        return true;
    }



    /**
     * Calcula a posição atual de um clã no ranking
     *
     * @param clanTag Tag do clã
     * @return Posição do clã (1 a N) ou 999 se não encontrado
     */
    private int calcularPosicaoClan(String clanTag) {
        try {
            List<ClanPoints> topClans = plugin.getDatabaseManager().getAdapter().getTopClans(Integer.MAX_VALUE);

            for (int i = 0; i < topClans.size(); i++) {
                if (topClans.get(i).getClanTag().equals(clanTag)) {
                    return i + 1; // Posições começam em 1, não 0
                }
            }

            return 999;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao calcular posição do clã", e);
            return 999;
        }
    }

    /**
     * Recarrega as configurações do discord.json
     * Útil para testar mudanças sem reiniciar o servidor
     */
    public void reloadDiscordConfig() {
        LogUtils.info("Recarregando configurações do Discord...");
        messageManager.reloadConfig();
        LogUtils.info("Configurações do Discord recarregadas com sucesso!");
    }
}
