package hplugins.hliga.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Evento disparado APÓS remover uma tag de um jogador com sucesso
 * Este evento não pode ser cancelado
 */
@Getter
public class HLigaPlayerTagRemovedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
    private final UUID playerId;
    private final String tagId;
    
    public HLigaPlayerTagRemovedEvent(UUID playerId, String tagId) {
        this.playerId = playerId;
        this.tagId = tagId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}