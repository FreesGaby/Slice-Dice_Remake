import java.awt.Color;

public class Particle {
    double x;
    double y;
    double vx;
    double vy;
    double size;
    double life;
    Color color;

    Particle(double x, double y, double vx, double vy, double size, double life, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.life = life;
        this.color = color;
    }

    void update() {
        x += vx;
        y += vy;
        vx *= 0.98;
        vy *= 0.98;
        size *= 0.96;
        life -= 1.0 / 60.0;
    }
    
}
