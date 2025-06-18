package hplugins.hliga.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado APÓS alterar pontos de um clã com sucesso
 * Este evento não pode ser cancelado
 */
@Getter
public class HLigaClanPointsChangedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
    private final String clanTag;
    private final int oldPoints;
    private final int newPoints;
    private final String reason;
    private final HLigaClanPointsChangeEvent.ChangeType changeType;
    
    public HLigaClanPointsChangedEvent(String clanTag, int oldPoints, int newPoints, String reason, HLigaClanPointsChangeEvent.ChangeType changeType) {
        this.clanTag = clanTag;
        this.oldPoints = oldPoints;
        this.newPoints = newPoints;
        this.reason = reason;
        this.changeType = changeType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}