import java.awt.Color;

public class Ring {
    double x;
    double y;
    double radius;
    double expandRate;
    double life;
    double thickness;
    Color color;

    Ring(double x, double y, double radius, double expandRate, double life, double thickness, Color color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.expandRate = expandRate;
        this.life = life;
        this.thickness = thickness;
        this.color = color;
    }

    void update() {
        radius += expandRate * (1.0 / 60.0);
        thickness *= 0.97;
        life -= 1.0 / 60.0;
        int alpha = Math.max(0, (int) (color.getAlpha() * (life / 0.35)));
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
    
}
