package hplugins.hliga.hooks.providers;

import hplugins.hliga.Main;
import hplugins.hliga.hooks.BaseClanProvider;
import hplugins.hliga.models.GenericClan;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provedor fallback quando nenhum plugin de clãs está disponível
 * Implementação vazia com valores padrão
 */
public class NullClanProvider extends BaseClanProvider {

    private final Main plugin;
    private static final String PROVIDER_NAME = "NullProvider";
    
    public NullClanProvider(Main plugin) {
        this.plugin = plugin;
        plugin.getLogger().warning("NullClanProvider inicializado. Nenhum plugin de clãs encontrado!");
    }
    
    @Override
    public boolean isAvailable() {
        
        return true;
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public List<GenericClan> getAllClans() {
        
        return new ArrayList<>();
    }
    
    @Override
    public List<String> getAllClanTags() {
        
        return new ArrayList<>();
    }
    
    @Override
    public GenericClan getPlayerClan(Player player) {
        
        return null;
    }
    
    @Override
    public String getPlayerClanTag(Player player) {
        
        return null;
    }
    
    @Override
    public GenericClan getClan(String tag) {
        
        return null;
    }
    
    @Override
    public boolean isPlayerLeader(Player player) {
        
        return false;
    }
    
    @Override
    public List<UUID> getClanMembers(String tag) {
        
        return new ArrayList<>();
    }
    
    @Override
    public String getClanLeaderName(String tag) {
        
        return null;
    }
    
    @Override
    public boolean clanExists(String tag) {
        
        return false;
    }
    
    @Override
    public String getColoredClanTag(String tag) {
        
        return tag;
    }
    
    @Override
    public String getClanName(String tag) {
        
        return tag;
    }
    
    @Override
    public boolean supportsClanCreation() {
        
        return false;
    }
    
    @Override
    public boolean supportsClanDissolution() {
        
        return false;
    }
    
    @Override
    public boolean supportsMemberEditing() {
        
        return false;
    }
    
    @Override
    public GenericClan convertToGenericClan(Object nativeObject) {
        
        return null;
    }
}