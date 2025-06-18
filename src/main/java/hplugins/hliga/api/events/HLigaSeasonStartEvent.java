package hplugins.hliga.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

/**
 * Evento disparado ANTES de iniciar uma nova temporada
 * Este evento pode ser cancelado para impedir o início da temporada
 */
@Setter
@Getter
public class HLigaSeasonStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    

    private String seasonName;
    private Integer durationDays;
    private LocalDate endDate;
    
    public HLigaSeasonStartEvent(String seasonName, int durationDays) {
        this.seasonName = seasonName;
        this.durationDays = durationDays;
        this.endDate = null;
    }
    
    public HLigaSeasonStartEvent(String seasonName, LocalDate endDate) {
        this.seasonName = seasonName;
        this.durationDays = null;
        this.endDate = endDate;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
        this.endDate = null;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.durationDays = null;
    }
    
    public boolean hasSpecificEndDate() {
        return endDate != null;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}