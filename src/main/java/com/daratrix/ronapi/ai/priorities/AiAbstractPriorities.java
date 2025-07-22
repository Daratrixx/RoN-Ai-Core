package com.daratrix.ronapi.ai.priorities;

import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

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

    public static abstract class AiAbstractPriority<T extends AiAbstractPriorities.AiAbstractPriority> {
        public final int typeId;
        public final int count;
        protected int limit;
        protected Supplier<Boolean> isRequired;

        AiAbstractPriority(int typeId, int count) {
            this.typeId = typeId;
            this.count = count;
            this.limit = count;
        }

        public boolean isCompleted(IAiPlayer aiPlayer) {
            return aiPlayer.count(typeId) >= count;
        }

        public T limit(int limit) {
            this.limit = limit;
            return (T) this;
        }


        protected final static Supplier<Boolean> TRUE = () -> true;
        protected final static Supplier<Boolean> FALSE = () -> false;

        public T required() {
            this.isRequired = TRUE;
            return (T) this;
        }

        public T required(boolean required) {
            this.isRequired = required ? TRUE : FALSE;
            return (T) this;
        }

        public T required(Supplier<Boolean> required) {
            this.isRequired = required;
            return (T) this;
        }

        public boolean isRequired() {
            if (this.isRequired == null) {
                return false;
            }

            return this.isRequired == TRUE || (this.isRequired != FALSE && this.isRequired.get());
        }

        public int count() {
            return this.isRequired()
                    ? this.count
                    : Math.min(this.count, this.limit);
        }
    }
}
