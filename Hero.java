import java.awt.Color;

public class Hero extends Entity {
    final String name;
    final Color color;
    final AbilityType ability;
    private final double abilityCooldown;
    private long lastAbilityTime;
    double lastDirX = 1;
    double lastDirY = 0;

    Hero(String name, AbilityType ability, double abilityCooldown, double x, double y, Color color, int maxHp, int damage, double attackSpeed) {
        super(x, y, maxHp, damage, 90, 1.0 / attackSpeed);
        this.name = name;
        this.color = color;
        this.ability = ability;
        this.abilityCooldown = abilityCooldown;
        this.lastAbilityTime = 0;
    }

    void setLastMove(double dx, double dy) {
        lastDirX = dx;
        lastDirY = dy;
    }

    boolean canUseAbility() {
        return getAbilityCooldownRemaining() <= 0;
    }

    void useAbility() {
        lastAbilityTime = System.currentTimeMillis();
    }

    double getAbilityCooldownRemaining() {
        if (lastAbilityTime == 0) return 0;
        double elapsed = (System.currentTimeMillis() - lastAbilityTime) / 1000.0;
        return Math.max(0, abilityCooldown - elapsed);
    }
}
