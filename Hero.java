import java.awt.Color;

public class Hero extends Entity {
    final String name;
    final Color color;

    Hero(String name, double x, double y, Color color, int maxHp, int damage, double attackSpeed) {
        super(x, y, maxHp, damage, 90, 1.0 / attackSpeed);
        this.name = name;
        this.color = color;
    }
}
