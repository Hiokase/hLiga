package hplugins.hliga.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import hplugins.hliga.Main;
import hplugins.hliga.utils.LogUtils;


import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;


import eu.decentsoftware.holograms.api.DHAPI;

/**
 * Gerenciador de hologramas com suporte a HolographicDisplays e DecentHolograms
 * Sistema com fallback automático entre as duas APIs
 */
public class HologramManager {
    
    private final Main plugin;
    private final Map<String, Object> activeHolograms = new HashMap<>();
    /**
     * -- GETTER --
     *  Retorna o provider atual
     */
    @Getter
    private HologramProvider provider;
    
    public enum HologramProvider {
        HOLOGRAPHIC_DISPLAYS,
        DECENT_HOLOGRAMS,
        NONE
    }
    
    public HologramManager(Main plugin) {
        this.plugin = plugin;
        detectProvider();
    }
    
    /**
     * Detecta qual plugin de holograma está disponível
     */
    private void detectProvider() {
        Plugin hdPlugin = Bukkit.getPluginManager().getPlugin("HolographicDisplays");
        Plugin dhPlugin = Bukkit.getPluginManager().getPlugin("DecentHolograms");
        
        if (hdPlugin != null && hdPlugin.isEnabled()) {
            try {
                
                HolographicDisplaysAPI.get(plugin);
                this.provider = HologramProvider.HOLOGRAPHIC_DISPLAYS;
                LogUtils.debug("HolographicDisplays detectado e será usado para hologramas");
                return;
            } catch (Exception e) {
                LogUtils.warn("HolographicDisplays encontrado mas API não disponível: " + e.getMessage());
            }
        }
        
        if (dhPlugin != null && dhPlugin.isEnabled()) {
            try {
                
                DHAPI.class.getName();
                this.provider = HologramProvider.DECENT_HOLOGRAMS;
                LogUtils.debug("DecentHolograms detectado e será usado para hologramas");
                return;
            } catch (Exception e) {
                LogUtils.warn("DecentHolograms encontrado mas API não disponível: " + e.getMessage());
            }
        }
        
        this.provider = HologramProvider.NONE;
        LogUtils.warn("Nenhum plugin de holograma compatível encontrado!");
        LogUtils.warn("Instale HolographicDisplays ou DecentHolograms para usar hologramas");
    }
    
    /**
     * Cria um holograma na localização especificada
     */
    public boolean createHologram(String id, Location location, List<String> lines) {
        if (provider == HologramProvider.NONE) {
            LogUtils.warn("Tentativa de criar holograma sem plugin compatível");
            return false;
        }
        
        
        removeHologram(id);
        
        try {
            switch (provider) {
                case HOLOGRAPHIC_DISPLAYS:
                    return createHolographicDisplaysHologram(id, location, lines);
                case DECENT_HOLOGRAMS:
                    return createDecentHologram(id, location, lines);
                default:
                    return false;
            }
        } catch (Exception e) {
            LogUtils.error("Erro ao criar holograma " + id + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cria holograma usando HolographicDisplays
     */
    private boolean createHolographicDisplaysHologram(String id, Location location, List<String> lines) {
        try {
            HolographicDisplaysAPI api = HolographicDisplaysAPI.get(plugin);
            Hologram hologram = api.createHologram(location);
            
            
            for (String line : lines) {
                hologram.getLines().appendText(line);
            }
            
            activeHolograms.put(id, hologram);
            LogUtils.debug("Holograma HolographicDisplays criado: " + id);
            return true;
            
        } catch (Exception e) {
            LogUtils.error("Erro ao criar holograma HolographicDisplays: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cria holograma usando DecentHolograms
     */
    private boolean createDecentHologram(String id, Location location, List<String> lines) {
        try {
            Object hologram = DHAPI.createHologram(id, location, lines);
            
            if (hologram != null) {
                activeHolograms.put(id, hologram);
                LogUtils.debug("Holograma DecentHolograms criado: " + id);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LogUtils.error("Erro ao criar holograma DecentHolograms: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Atualiza as linhas de um holograma existente
     */
    public boolean updateHologram(String id, List<String> lines) {
        if (!activeHolograms.containsKey(id)) {
            LogUtils.warn("Tentativa de atualizar holograma inexistente: " + id);
            return false;
        }
        
        try {
            switch (provider) {
                case HOLOGRAPHIC_DISPLAYS:
                    return updateHolographicDisplaysHologram(id, lines);
                case DECENT_HOLOGRAMS:
                    return updateDecentHologram(id, lines);
                default:
                    return false;
            }
        } catch (Exception e) {
            LogUtils.error("Erro ao atualizar holograma " + id + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Atualiza holograma HolographicDisplays
     */
    private boolean updateHolographicDisplaysHologram(String id, List<String> lines) {
        try {
            Hologram hologram = (Hologram) activeHolograms.get(id);
            
            
            hologram.getLines().clear();
            
            
            for (String line : lines) {
                hologram.getLines().appendText(line);
            }
            
            LogUtils.debug("Holograma HolographicDisplays atualizado: " + id);
            return true;
            
        } catch (Exception e) {
            LogUtils.error("Erro ao atualizar holograma HolographicDisplays: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Atualiza holograma DecentHolograms
     */
    private boolean updateDecentHologram(String id, List<String> lines) {
        try {
            eu.decentsoftware.holograms.api.holograms.Hologram hologram = DHAPI.getHologram(id);
            if (hologram != null) {
                DHAPI.setHologramLines(hologram, lines);
            }
            
            LogUtils.debug("Holograma DecentHolograms atualizado: " + id);
            return true;
            
        } catch (Exception e) {
            LogUtils.error("Erro ao atualizar holograma DecentHolograms: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove um holograma
     */
    public boolean removeHologram(String id) {
        if (!activeHolograms.containsKey(id)) {
            return true; 
        }
        
        try {
            switch (provider) {
                case HOLOGRAPHIC_DISPLAYS:
                    return removeHolographicDisplaysHologram(id);
                case DECENT_HOLOGRAMS:
                    return removeDecentHologram(id);
                default:
                    return false;
            }
        } catch (Exception e) {
            LogUtils.error("Erro ao remover holograma " + id + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove holograma HolographicDisplays
     */
    private boolean removeHolographicDisplaysHologram(String id) {
        try {
            Hologram hologram = (Hologram) activeHolograms.get(id);
            hologram.delete();
            activeHolograms.remove(id);
            
            LogUtils.debug("Holograma HolographicDisplays removido: " + id);
            return true;
            
        } catch (Exception e) {
            LogUtils.error("Erro ao remover holograma HolographicDisplays: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove holograma DecentHolograms
     */
    private boolean removeDecentHologram(String id) {
        try {
            DHAPI.removeHologram(id);
            
            activeHolograms.remove(id);
            LogUtils.debug("Holograma DecentHolograms removido: " + id);
            return true;
            
        } catch (Exception e) {
            LogUtils.error("Erro ao remover holograma DecentHolograms: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove todos os hologramas
     */
    public void removeAllHolograms() {
        List<String> toRemove = new ArrayList<>(activeHolograms.keySet());
        for (String id : toRemove) {
            removeHologram(id);
        }
        LogUtils.debug("Todos os hologramas foram removidos");
    }
    
    /**
     * Verifica se há suporte a hologramas
     */
    public boolean hasHologramSupport() {
        return provider != HologramProvider.NONE;
    }

    /**
     * Retorna a quantidade de hologramas ativos
     */
    public int getActiveHologramCount() {
        return activeHolograms.size();
    }
}