package hplugins.hliga.listeners;

import hplugins.hliga.Main;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Listener para proteger os ArmorStands criados pelo plugin
 */
public class ArmorStandListener implements Listener {

    private final Main plugin;

    public ArmorStandListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Impede que jogadores manipulem os ArmorStands de ranking
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractArmorStand(PlayerArmorStandManipulateEvent event) {
        ArmorStand stand = event.getRightClicked();

        
        if (isRankingArmorStand(stand)) {
            event.setCancelled(true);
        }
    }

    /**
     * Impede danos aos ArmorStands de ranking
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorStandDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof ArmorStand) {
            
            if (isRankingArmorStand((ArmorStand) entity)) {
                event.setCancelled(true);

                
                if (entity instanceof ArmorStand) {
                    hplugins.hliga.utils.VersionUtils.setArmorStandInvulnerable((ArmorStand) entity, true);
                }
            }
        }
    }

    /**
     * Impede danos por entidades aos ArmorStands de ranking
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorStandDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof ArmorStand) {
            
            if (isRankingArmorStand((ArmorStand) entity)) {
                event.setCancelled(true);
                if (event.getDamager() != null) {
                    event.setDamage(0);
                    if (entity instanceof ArmorStand) {
                        hplugins.hliga.utils.VersionUtils.setArmorStandInvulnerable((ArmorStand) entity, true);
                    }
                }
            }
        }
    }

    /**
     * Impede interações com os ArmorStands de ranking
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (entity instanceof ArmorStand && isRankingArmorStand((ArmorStand) entity)) {
            event.setCancelled(true);
        }
    }

    /**
     * Verifica se um ArmorStand pertence ao sistema de ranking
     *
     * @param stand ArmorStand a verificar
     * @return true se for um ArmorStand do sistema de ranking
     */
    private boolean isRankingArmorStand(ArmorStand stand) {
        
        if (!stand.hasGravity() && stand.isSmall() && !stand.isVisible()) {
            
            String customName = stand.getCustomName();
            if (customName != null && (customName.contains("Top #") || customName.contains("pontos") || customName.contains("Líder:"))) {
                return true;
            }
        }
        return false;
    }
}