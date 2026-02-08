public class Monster extends Entity {
    private double speed = 1.2;
    private double stunTimer = 0;

    Monster(double x, double y, int maxHp, int damage, double range) {
        super(x, y, maxHp, damage, range, 0.8);
        this.speed += Math.min(1.2, maxHp / 200.0);
    }

    @Override
    void update() {
        super.update();
        if (stunTimer > 0) {
            stunTimer -= 1.0 / 60.0;
        }
    }

    void stun(double seconds) {
        stunTimer = Math.max(stunTimer, seconds);
    }

    boolean isStunned() {
        return stunTimer > 0;
    }

    void moveToward(double tx, double ty) {
        double dx = tx - x;
        double dy = ty - y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        dx /= len;
        dy /= len;
        x += dx * speed;
        y += dy * speed;
    } 
    
}
