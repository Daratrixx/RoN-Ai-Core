package com.daratrix.ronapi.ai.priorities;

import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AiAbstractPriorities<T extends AiAbstractPriorities.AiAbstractPriority> {

    protected final List<T> priorities = new ArrayList<>();

    public void reset() {
        priorities.clear();
    }

    public abstract T addPriority(int typeId, int count);

    public boolean isEmpty() {
        return priorities.isEmpty();
    }

    public Iterator<T> iterator() {
        return priorities.iterator();
    }

    public static class AiAbstractPriority {
        public final int typeId;
        public final int count;

        AiAbstractPriority(int typeId, int count) {
            this.typeId = typeId;
            this.count = count;
        }

        public boolean isCompleted(IAiPlayer aiPlayer) {
            return aiPlayer.count(typeId) >= count;
        }
    }
}
