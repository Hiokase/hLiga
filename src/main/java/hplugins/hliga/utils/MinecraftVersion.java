package hplugins.hliga.utils;

import org.bukkit.Bukkit;

/**
 * Utilidade para verificar a versão do servidor
 */
public class MinecraftVersion {
    
    private static final String VERSION;
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    
    
    public static final MinecraftVersion V1_8 = new MinecraftVersion(1, 8);
    public static final MinecraftVersion V1_9 = new MinecraftVersion(1, 9);
    public static final MinecraftVersion V1_10 = new MinecraftVersion(1, 10);
    public static final MinecraftVersion V1_11 = new MinecraftVersion(1, 11);
    public static final MinecraftVersion V1_12 = new MinecraftVersion(1, 12);
    public static final MinecraftVersion V1_13 = new MinecraftVersion(1, 13);
    public static final MinecraftVersion V1_14 = new MinecraftVersion(1, 14);
    public static final MinecraftVersion V1_15 = new MinecraftVersion(1, 15);
    public static final MinecraftVersion V1_16 = new MinecraftVersion(1, 16);
    public static final MinecraftVersion V1_17 = new MinecraftVersion(1, 17);
    public static final MinecraftVersion V1_18 = new MinecraftVersion(1, 18);
    public static final MinecraftVersion V1_19 = new MinecraftVersion(1, 19);
    public static final MinecraftVersion V1_20 = new MinecraftVersion(1, 20);
    
    private final int major;
    private final int minor;
    
    static {
        
        String bukkitVersion = Bukkit.getServer().getBukkitVersion();
        VERSION = bukkitVersion.split("-")[0]; 
        
        
        String[] versionParts = VERSION.split("\\.");
        MAJOR_VERSION = Integer.parseInt(versionParts[0]);
        MINOR_VERSION = Integer.parseInt(versionParts[1]);
    }
    
    /**
     * Cria uma nova referência de versão
     * 
     * @param major Versão principal
     * @param minor Versão secundária
     */
    public MinecraftVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }
    
    /**
     * Verifica se o servidor está rodando pelo menos esta versão
     * 
     * @param version Versão para comparar
     * @return true se o servidor estiver rodando esta versão ou superior
     */
    public static boolean isAtLeast(MinecraftVersion version) {
        return MAJOR_VERSION > version.major || 
               (MAJOR_VERSION == version.major && MINOR_VERSION >= version.minor);
    }
    
    /**
     * Verifica se o servidor está rodando exatamente esta versão
     * 
     * @param version Versão para comparar
     * @return true se o servidor estiver rodando exatamente esta versão
     */
    public static boolean isExactly(MinecraftVersion version) {
        return MAJOR_VERSION == version.major && MINOR_VERSION == version.minor;
    }
    
    /**
     * Retorna a string da versão do servidor
     * 
     * @return String da versão (formato: "major.minor")
     */
    public static String getVersion() {
        return VERSION;
    }
    
    /**
     * Retorna a versão principal do servidor
     * 
     * @return Versão principal (major)
     */
    public static int getMajor() {
        return MAJOR_VERSION;
    }
    
    /**
     * Retorna a versão secundária do servidor
     * 
     * @return Versão secundária (minor)
     */
    public static int getMinor() {
        return MINOR_VERSION;
    }
}