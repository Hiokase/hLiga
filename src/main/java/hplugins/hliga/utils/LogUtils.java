package hplugins.hliga.utils;

import hplugins.hliga.Main;
import lombok.Getter;
import org.bukkit.ChatColor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitário para gerenciar logs e mensagens de debug
 */
@Getter
public class LogUtils {

    private static Main plugin;
    private static Logger logger;
    private static boolean debugEnabled = false;
    private static int logLevel = 2;

    /**
     * Inicializa o sistema de logs
     *
     * @param plugin Instância do plugin
     */
    public static void init(Main plugin) {
        LogUtils.plugin = plugin;
        LogUtils.logger = plugin.getLogger();
        
        
        reloadConfig();
    }

    /**
     * Recarrega as configurações de debug do config.yml
     */
    public static void reloadConfig() {
        debugEnabled = plugin.getConfig().getBoolean("sistema.debug", false);
        logLevel = plugin.getConfig().getInt("sistema.log_level", 2);
    }

    /**
     * Registra uma mensagem de informação importante (sempre exibida)
     *
     * @param message Mensagem a ser registrada
     */
    public static void info(String message) {
        logger.info(ChatColor.stripColor(message));
    }

    /**
     * Registra uma mensagem de aviso importante (sempre exibida)
     *
     * @param message Mensagem a ser registrada
     */
    public static void warning(String message) {
        logger.warning(ChatColor.stripColor(message));
    }
    
    public static void warn(String message) {
        logger.warning(ChatColor.stripColor(message));
    }
    
    /**
     * Registra uma mensagem de aviso importante com exceção (sempre exibida)
     *
     * @param message Mensagem a ser registrada
     * @param throwable Exceção associada
     */
    public static void warning(String message, Throwable throwable) {
        logger.log(Level.WARNING, ChatColor.stripColor(message), throwable);
    }

    /**
     * Registra uma mensagem de erro importante (sempre exibida)
     * Alias para severe() para compatibilidade de código
     *
     * @param message Mensagem a ser registrada
     */
    public static void error(String message) {
        logger.severe(ChatColor.stripColor(message));
    }
    
    /**
     * Registra uma mensagem de erro importante (sempre exibida)
     *
     * @param message Mensagem a ser registrada
     */
    public static void severe(String message) {
        logger.severe(ChatColor.stripColor(message));
    }

    /**
     * Registra uma mensagem de erro importante com exceção (sempre exibida)
     *
     * @param message Mensagem a ser registrada
     * @param throwable Exceção associada
     */
    public static void severe(String message, Throwable throwable) {
        logger.log(Level.SEVERE, ChatColor.stripColor(message), throwable);
    }

    /**
     * Registra uma mensagem de depuração de prioridade baixa
     * Exibida apenas quando debug está ativado e log_level >= 3
     *
     * @param message Mensagem de debug
     */
    public static void debug(String message) {
        if (debugEnabled && logLevel >= 3) {
            logger.info("[DEBUG] " + ChatColor.stripColor(message));
        }
    }

    /**
     * Registra uma mensagem de depuração de prioridade média
     * Exibida apenas quando debug está ativado e log_level >= 2
     *
     * @param message Mensagem de debug
     */
    public static void debugMedium(String message) {
        if (debugEnabled && logLevel >= 2) {
            logger.info("[DEBUG] " + ChatColor.stripColor(message));
        }
    }

    /**
     * Registra uma mensagem de depuração de prioridade alta
     * Exibida quando debug está ativado e log_level >= 1
     *
     * @param message Mensagem de debug
     */
    public static void debugHigh(String message) {
        if (debugEnabled && logLevel >= 1) {
            logger.info("[DEBUG] " + ChatColor.stripColor(message));
        }
    }

}