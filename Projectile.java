public class Projectile {
    double x;
    double y;
    double vx;
    double vy;
    double radius;
    int damage;

    Projectile(double x, double y, double vx, double vy, double radius, int damage) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.damage = damage;
    }

    void update() {
        x += vx;
        y += vy;
    }
}
