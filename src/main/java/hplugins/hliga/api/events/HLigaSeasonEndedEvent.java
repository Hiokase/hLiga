package hplugins.hliga.api.events;

import hplugins.hliga.models.Season;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado APÓS finalizar uma temporada com sucesso
 * Este evento não pode ser cancelado
 */
@Getter
public class HLigaSeasonEndedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
    private final Season season;
    
    public HLigaSeasonEndedEvent(Season season) {
        this.season = season;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}