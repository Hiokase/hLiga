package hplugins.hliga.api.events;

import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Evento disparado ANTES de remover uma tag de um jogador
 * Este evento pode ser cancelado para impedir a remoção
 */
public class HLigaPlayerTagRemoveEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    @Getter
    private final UUID playerId;
    @Getter
    private final String tagId;
    
    public HLigaPlayerTagRemoveEvent(UUID playerId, String tagId) {
        this.playerId = playerId;
        this.tagId = tagId;
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