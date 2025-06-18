package hplugins.hliga.config;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe para gerenciar mensagens localizadas
 */
@Getter
public class Messages {

    /**
     * -- GETTER --
     *  Obtém a configuração do arquivo de mensagens
     *
     * @return FileConfiguration do messages.yml
     */
    private final FileConfiguration config;
    private final Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
    public Messages(FileConfiguration config) {
        this.config = config;
    }
    
    /**
     * Verifica se um caminho de mensagem existe no arquivo de configuração
     * 
     * @param path Caminho da mensagem
     * @return true se existe, false caso contrário
     */
    public boolean has(String path) {
        return config.contains(path);
    }
    
    /**
     * Obtém uma mensagem do arquivo de configuração
     * 
     * @param path Caminho da mensagem
     * @return Mensagem formatada ou o caminho se não encontrada
     */
    public String getMessage(String path) {
        String message = config.getString(path);
        
        if (message == null) {
            
            System.out.println("[hLiga Debug] Mensagem não encontrada: " + path);
            
            
            if (path.contains(".titulo")) {
                return "&c&lTEMPORADA ENCERRADA";
            } else if (path.contains(".subtitulo")) {
                return "&f&lParabéns aos vencedores!";
            } else if (path.contains(".vencedor")) {
                return "&cNenhum vencedor";
            } else if (path.contains("sem_temporada")) {
                return "&c[X] &fNenhuma temporada ativa.";
            } else if (path.contains("tempo_finalizada")) {
                return "&c[X] &fTemporada já finalizada.";
            } else {
                return "&c[Mensagem não encontrada: " + path + "]";
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Obtém uma mensagem do arquivo de configuração e substitui placeholders
     * 
     * @param path Caminho da mensagem
     * @param placeholders Placeholders para substituir (formato: chave1, valor1, chave2, valor2...)
     * @return Mensagem formatada com placeholders substituídos
     */
    public String getMessage(String path, String... placeholders) {
        String message = config.getString(path);
        
        if (message == null) {
            
            if (placeholders != null && placeholders.length == 1) {
                message = placeholders[0]; 
            } else {
                
                System.out.println("[hLiga Debug] Mensagem não encontrada: " + path);
                
                
                if (path.contains(".titulo")) {
                    message = "&c&lTEMPORADA ENCERRADA";
                } else if (path.contains(".subtitulo")) {
                    message = "&f&lParabéns aos vencedores!";
                } else if (path.contains(".vencedor")) {
                    message = "&cNenhum vencedor";
                } else if (path.contains(".sem_clan")) {
                    message = "&7Nenhum clã encontrado";
                } else if (path.contains(".pontos")) {
                    message = "0";
                } else if (path.contains("sem_temporada")) {
                    message = "&c[X] &fNenhuma temporada ativa.";
                } else if (path.contains("tempo_finalizada")) {
                    message = "&c[X] &fTemporada já finalizada.";
                } else {
                    message = "&c[Mensagem não encontrada: " + path + "]";
                }
            }
        }
        
        if (placeholders != null && placeholders.length > 0 && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                
                message = message.replace(placeholder, value);
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Obtém uma mensagem do arquivo de configuração e substitui placeholders usando um mapa
     * 
     * @param message Mensagem base com placeholders
     * @param placeholders Placeholders para substituir (formato: chave1, valor1, chave2, valor2...)
     * @return Mensagem formatada com placeholders substituídos
     */
    public String formatMessage(String message, String... placeholders) {
        if (message == null) {
            return "";
        }
        
        if (placeholders != null && placeholders.length > 0) {
            if (placeholders.length % 2 != 0) {
                throw new IllegalArgumentException("Número de argumentos de placeholder deve ser par");
            }
            
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                
                message = message.replace(placeholder, value);
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Verifica se uma mensagem contém um placeholder específico
     * 
     * @param message Mensagem para verificar
     * @param placeholder Placeholder para procurar
     * @return true se o placeholder estiver presente, false caso contrário
     */
    public boolean containsPlaceholder(String message, String placeholder) {
        Matcher matcher = placeholderPattern.matcher(message);
        
        while (matcher.find()) {
            String found = matcher.group(1);
            if (found.equals(placeholder)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extrai todos os placeholders de uma mensagem
     * 
     * @param message Mensagem para analisar
     * @return Array de placeholders encontrados
     */
    public String[] extractPlaceholders(String message) {
        Matcher matcher = placeholderPattern.matcher(message);
        List<String> placeholders = new ArrayList<>();
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders.toArray(new String[0]);
    }
    
    /**
     * Obtém uma lista de strings do arquivo de configuração
     * 
     * @param path Caminho da lista de mensagens
     * @return Lista de strings ou lista vazia se não encontrada
     */
    public List<String> getStringList(String path) {
        List<String> messages = config.getStringList(path);
        
        if (messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        return messages;
    }
    
    /**
     * Obtém uma lista de strings do arquivo de configuração com placeholders substituídos
     * 
     * @param path Caminho da lista de mensagens
     * @param placeholders Placeholders para substituir (formato: chave1, valor1, chave2, valor2...)
     * @return Lista de strings formatada com placeholders substituídos
     */
    public List<String> getStringList(String path, String... placeholders) {
        List<String> messages = getStringList(path);
        List<String> formattedMessages = new ArrayList<>();
        
        for (String message : messages) {
            String formattedMessage = formatMessage(message, placeholders);
            formattedMessages.add(formattedMessage);
        }
        
        return formattedMessages;
    }

}
