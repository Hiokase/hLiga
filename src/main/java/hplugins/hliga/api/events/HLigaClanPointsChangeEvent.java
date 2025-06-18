package hplugins.hliga.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado ANTES de alterar pontos de um clã
 * Este evento pode ser cancelado para impedir a alteração
 */
@Setter
@Getter
public class HLigaClanPointsChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final String clanTag;
    private final int oldPoints;
    private int newPoints;
    private final String reason;
    private final ChangeType changeType;
    
    public enum ChangeType {
        ADD, REMOVE, SET
    }
    
    public HLigaClanPointsChangeEvent(String clanTag, int oldPoints, int newPoints, String reason, ChangeType changeType) {
        this.clanTag = clanTag;
        this.oldPoints = oldPoints;
        this.newPoints = newPoints;
        this.reason = reason;
        this.changeType = changeType;
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