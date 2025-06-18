package hplugins.hliga.api.events;

import hplugins.hliga.models.Season;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado ANTES de finalizar uma temporada
 * Este evento pode ser cancelado para impedir a finalização
 */
@Getter
public class HLigaSeasonEndEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Season season;
    
    public HLigaSeasonEndEvent(Season season) {
        this.season = season;
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