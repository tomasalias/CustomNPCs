package dev.foxikle.customnpcs.conditions;

import dev.foxikle.customnpcs.CustomNPCs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.Buffer;

public interface Conditional {

    boolean compute(Player player);

    String toJson();

    static Conditional of(String data) {
        return CustomNPCs.getGson().fromJson(data, Conditional.class);
    }

    Value getValue();
    Comparator getComparator();

    Type getType();

    void setComparator(Comparator comparator);
    void setValue(Value value);
    void setTargetValue(String targetValue);
    String getTarget();
    enum Comparator {
        EQUAL_TO(true),
        NOT_EQUAL_TO(true),
        LESS_THAN(false),
        GREATER_THAN(false),
        LESS_THAN_OR_EQUAL_TO(false),
        GREATER_THAN_OR_EQUAL_TO(false);

        private final boolean strictlyLogical;
        Comparator(boolean strictlyLogical) {
            this.strictlyLogical = strictlyLogical;
        }

        public boolean isStrictlyLogical() {
            return strictlyLogical;
        }
    }

    enum Type {
        NUMERIC,
        LOGICAL
    }

    enum Value {
        EXP_LEVELS(false),
        EXP_POINTS(false),
        HEALTH(false),
        ABSORBTION(false),
        HAS_EFFECT(true),
        HAS_PERMISSION(true),
        GAMEMODE(true),
        Y_COORD(false),
        X_COORD(false),
        Z_COORD(false);

        private final boolean isLogical;
        Value(boolean isLogical) {
            this.isLogical = isLogical;
        }

        public boolean isLogical() {
            return isLogical;
        }
    }
}
