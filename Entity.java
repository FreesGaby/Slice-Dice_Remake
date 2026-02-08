abstract class Entity {
    double x;
    double y;
    int hp;
    int maxHp;
    int damage;
    double range;
    private double attackCooldown;
    private double attackTimer;

    Entity(double x, double y, int maxHp, int damage, double range, double attackCooldown) {
        this.x = x;
        this.y = y;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.damage = damage;
        this.range = range;
        this.attackCooldown = attackCooldown;
        this.attackTimer = 0;
    }

    void update() {
        if (attackTimer > 0) attackCooldown -= 1.0 / 60.0;
    }

    boolean canAttack() {
        return attackTimer <= 0;
    }

    void resetAttack() {
        attackTimer = attackCooldown;
    }
}
