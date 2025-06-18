package hplugins.hliga.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Evento disparado APÓS adicionar uma tag a um jogador com sucesso
 * Este evento não pode ser cancelado
 */
@Getter
public class HLigaPlayerTagAddedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
    private final UUID playerId;
    private final String tagId;
    private final String seasonName;
    
    public HLigaPlayerTagAddedEvent(UUID playerId, String tagId, String seasonName) {
        this.playerId = playerId;
        this.tagId = tagId;
        this.seasonName = seasonName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}