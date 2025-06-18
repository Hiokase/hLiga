package hplugins.hliga.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;

import java.lang.reflect.Method;

/**
 * Utilitário para detecção e compatibilidade de versões
 * Suporta Minecraft 1.8 até 1.21.5 e Java 8+
 */
public class VersionUtils {

    /**
     * -- GETTER --
     *  Retorna a versão completa do servidor
     */
    @Getter
    private static String serverVersion;
    private static int majorVersion;
    private static int minorVersion;
    private static int patchVersion;
    private static boolean isLegacy;
    /**
     * -- GETTER --
     *  Retorna a versão do Java
     */
    @Getter
    private static String javaVersion;
    /**
     * -- GETTER --
     *  Retorna a versão principal do Java
     */
    @Getter
    private static int javaMajorVersion;
    
    static {
        detectVersions();
    }
    
    /**
     * Detecta automaticamente as versões do servidor e Java
     */
    private static void detectVersions() {
        try {
            
            serverVersion = Bukkit.getVersion();
            String bukkitVersion = Bukkit.getBukkitVersion();
            
            
            if (bukkitVersion.contains("-")) {
                String versionPart = bukkitVersion.split("-")[0];
                String[] parts = versionPart.split("\\.");
                
                majorVersion = Integer.parseInt(parts[0]);
                minorVersion = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                patchVersion = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            }
            
            
            isLegacy = (majorVersion == 1 && minorVersion < 13);
            
            
            javaVersion = System.getProperty("java.version");
            String[] javaVersionParts = javaVersion.split("\\.");
            
            if (javaVersionParts[0].equals("1")) {
                
                javaMajorVersion = Integer.parseInt(javaVersionParts[1]);
            } else {
                
                javaMajorVersion = Integer.parseInt(javaVersionParts[0]);
            }
            
        } catch (Exception e) {
            
            majorVersion = 1;
            minorVersion = 8;
            patchVersion = 0;
            isLegacy = true;
            javaMajorVersion = 8;
        }
    }

    /**
     * Retorna a versão do Minecraft (ex: 1.20.4)
     */
    public static String getMinecraftVersion() {
        return majorVersion + "." + minorVersion + "." + patchVersion;
    }
    
    /**
     * Verifica se é uma versão legacy (< 1.13)
     */
    public static boolean isLegacy() {
        return isLegacy;
    }
    
    /**
     * Verifica se a versão é maior ou igual à especificada
     */
    public static boolean isVersionAtLeast(int major, int minor) {
        return isVersionAtLeast(major, minor, 0);
    }
    
    /**
     * Verifica se a versão é maior ou igual à especificada
     */
    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        if (majorVersion > major) return true;
        if (majorVersion < major) return false;
        
        if (minorVersion > minor) return true;
        if (minorVersion < minor) return false;
        
        return patchVersion >= patch;
    }
    
    /**
     * Verifica se a versão está entre duas versões (inclusive)
     */
    public static boolean isVersionBetween(int minMajor, int minMinor, int maxMajor, int maxMinor) {
        return isVersionAtLeast(minMajor, minMinor) && 
               (majorVersion < maxMajor || (majorVersion == maxMajor && minorVersion <= maxMinor));
    }

    /**
     * Verifica se está rodando em Java 8
     */
    public static boolean isJava8() {
        return javaMajorVersion == 8;
    }
    
    /**
     * Verifica se está rodando em Java 11+
     */
    public static boolean isJava11Plus() {
        return javaMajorVersion >= 11;
    }
    
    /**
     * Verifica se está rodando em Java 17+
     */
    public static boolean isJava17Plus() {
        return javaMajorVersion >= 17;
    }
    
    /**
     * Envia mensagem ao jogador de forma compatível
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(message);
    }
    
    /**
     * Envia título ao jogador priorizando XSeries com fallbacks robustos
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        
        try {
            Titles.sendTitle(player, fadeIn, stay, fadeOut, title, subtitle);
            return;
        } catch (Exception e) {
            LogUtils.debug("XSeries títulos falhou: " + e.getMessage());
        }
        
        
        try {
            if (isVersionAtLeast(1, 11)) {
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                return;
            }
        } catch (Exception | NoSuchMethodError e) {
            LogUtils.debug("API nativa títulos falhou: " + e.getMessage());
        }
        
        
        LogUtils.debug("Usando fallback para chat devido a incompatibilidades");
        if (title != null && !title.isEmpty()) {
            player.sendMessage("§6§l" + title);
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            player.sendMessage("§e" + subtitle);
        }
    }
    
    /**
     * Envia action bar ao jogador priorizando XSeries com fallbacks robustos
     */
    public static void sendActionBar(Player player, String message) {
        
        try {
            ActionBar.sendActionBar(player, message);
            return;
        } catch (Exception e) {
            LogUtils.debug("XSeries action bar falhou: " + e.getMessage());
        }
        
        
        try {
            if (isVersionAtLeast(1, 9)) {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
                return;
            }
        } catch (Exception | NoSuchMethodError e) {
            LogUtils.debug("API nativa action bar falhou: " + e.getMessage());
        }
        
        
        LogUtils.debug("Usando fallback para chat devido a incompatibilidades de action bar");
        player.sendMessage("§6[Info] §e" + message);
    }
    
    /**
     * Toca som para jogador usando XSeries
     */
    public static void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            XSound xSound = XSound.matchXSound(soundName).orElse(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
            xSound.play(player, volume, pitch);
        } catch (Exception e) {
            LogUtils.debug("Erro ao tocar som " + soundName + ": " + e.getMessage());
        }
    }
    
    /**
     * Envia mensagem usando API moderna (Adventure)
     */
    private static void sendModernMessage(Player player, String message) {
        try {
            
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            
            Object miniMessage = miniMessageClass.getMethod("miniMessage").invoke(null);
            Object component = miniMessageClass.getMethod("deserialize", String.class).invoke(miniMessage, message);
            
            Method sendMessageMethod = player.getClass().getMethod("sendMessage", componentClass);
            sendMessageMethod.invoke(player, component);
            
        } catch (Exception e) {
            
            player.sendMessage(message);
        }
    }
    
    /**
     * Obtém material de forma compatível usando XSeries
     */
    public static Material getMaterial(String materialName) {
        try {
            
            XMaterial xMaterial = XMaterial.matchXMaterial(materialName).orElse(XMaterial.STONE);
            return xMaterial.parseMaterial();
        } catch (Exception e) {
            LogUtils.warning("Material não encontrado: " + materialName + " (versão " + getMinecraftVersion() + ")");
            return XMaterial.STONE.parseMaterial();
        }
    }
    
    /**
     * Cria ItemStack de forma compatível
     */
    public static ItemStack createItem(String materialName, int amount) {
        Material material = getMaterial(materialName);
        return new ItemStack(material, amount);
    }
    
    /**
     * Cria ItemStack com nome customizado
     */
    public static ItemStack createItem(String materialName, int amount, String displayName) {
        ItemStack item = createItem(materialName, amount);
        if (displayName != null && !displayName.isEmpty()) {
            item.getItemMeta().setDisplayName(displayName);
        }
        return item;
    }
    
    /**
     * Converte nomes de materiais modernos para legacy
     */
    private static String convertToLegacyMaterial(String modernName) {
        switch (modernName.toUpperCase()) {
            case "PLAYER_HEAD":
            case "PLAYER_SKULL":
                return "SKULL_ITEM";
            case "OAK_SIGN":
                return "SIGN";
            case "OAK_PLANKS":
                return "WOOD";
            case "GRASS_BLOCK":
                return "GRASS";
            case "WHITE_WOOL":
                return "WOOL";
            case "IRON_INGOT":
                return "IRON_INGOT";
            case "DIAMOND":
                return "DIAMOND";
            case "EMERALD":
                return "EMERALD";
            default:
                return modernName;
        }
    }
    
    /**
     * Executa código específico para versão do Java
     */
    public static void runJavaVersionSpecific(Runnable java8Code, Runnable java11Code, Runnable java17Code) {
        if (isJava17Plus() && java17Code != null) {
            java17Code.run();
        } else if (isJava11Plus() && java11Code != null) {
            java11Code.run();
        } else if (java8Code != null) {
            java8Code.run();
        }
    }
    
    /**
     * Retorna informações completas de compatibilidade
     */
    public static String getCompatibilityInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== INFORMAÇÕES DE COMPATIBILIDADE ===\n");
        info.append("Minecraft: ").append(getMinecraftVersion()).append("\n");
        info.append("Java: ").append(getJavaVersion()).append(" (versão ").append(getJavaMajorVersion()).append(")\n");
        info.append("Legacy: ").append(isLegacy() ? "Sim" : "Não").append("\n");
        info.append("Servidor: ").append(getServerVersion()).append("\n");
        
        
        info.append("\n=== STATUS DE COMPATIBILIDADE ===\n");
        
        if (isVersionAtLeast(1, 8)) {
            info.append("✓ Suporte básico (1.8+)\n");
        }
        
        if (isVersionAtLeast(1, 13)) {
            info.append("✓ Suporte moderno (1.13+)\n");
        }
        
        if (isVersionAtLeast(1, 19)) {
            info.append("✓ Suporte Adventure API (1.19+)\n");
        }
        
        if (isVersionAtLeast(1, 20)) {
            info.append("✓ Suporte recursos avançados (1.20+)\n");
        }
        
        if (javaMajorVersion >= 8) {
            info.append("✓ Java compatível (").append(javaMajorVersion).append("+)\n");
        }
        
        return info.toString();
    }
    
    /**
     * Verifica se todas as dependências são compatíveis
     */
    public static boolean isFullyCompatible() {
        
        if (!isVersionAtLeast(1, 8)) {
            return false;
        }
        
        
        if (majorVersion > 1 || (majorVersion == 1 && minorVersion > 21) || 
           (majorVersion == 1 && minorVersion == 21 && patchVersion > 5)) {
            LogUtils.warning("Versão " + getMinecraftVersion() + " pode não ser totalmente suportada");
        }
        
        
        if (javaMajorVersion < 8) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Inicializa compatibilidade no startup do plugin
     */
    public static void initializeCompatibility() {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(
            org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            "&8[&2hLiga&8] &7MC: &f" + getMinecraftVersion() + " &8| &7Java: &f" + getJavaMajorVersion() + 
            " &8| &7Legacy: &f" + (isLegacy() ? "Sim" : "Não"))
        );
        
        if (!isFullyCompatible()) {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&8[&2hLiga&8] &eATENÇÃO: Versão pode ter compatibilidade limitada!")
            );
        } else {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&8[&2hLiga&8] &a✓ Compatibilidade confirmada")
            );
        }
    }
    
    /**
     * Define invulnerabilidade de ArmorStand de forma compatível
     */
    public static void setArmorStandInvulnerable(org.bukkit.entity.ArmorStand armorStand, boolean invulnerable) {
        try {
            if (isVersionAtLeast(1, 9)) {
                
                armorStand.setInvulnerable(invulnerable);
            } else {
                
                armorStand.setHealth(invulnerable ? Double.MAX_VALUE : 20.0);
            }
        } catch (Exception e) {
            LogUtils.debug("Erro ao definir invulnerabilidade do ArmorStand: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se ArmorStand é invulnerável
     */
    public static boolean isArmorStandInvulnerable(org.bukkit.entity.ArmorStand armorStand) {
        try {
            if (isVersionAtLeast(1, 9)) {
                return armorStand.isInvulnerable();
            } else {
                return armorStand.getHealth() >= Double.MAX_VALUE;
            }
        } catch (Exception e) {
            return false;
        }
    }
}