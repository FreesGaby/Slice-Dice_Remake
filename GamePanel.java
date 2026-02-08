import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 540;
    private static final int FPS = 60;

    private final javax.swing.Timer timer;
    private final Random rng = new Random();

    private final java.util.List<Hero> heroes = new ArrayList<>();
    private final java.util.List<Monster> monsters = new ArrayList<>();
    private final java.util.List<Projectile> projectiles = new ArrayList<>();
    private final java.util.List<Particle> particles = new ArrayList<>();
    private final java.util.List<Ring> rings = new ArrayList<>();
    private double flashTimer = 0;
    private double shakeTimer = 0;
    private double shakeMagnitude = 0;
    private double shakeX = 0;
    private double shakeY = 0;

    private int wave = 1;
    private int selectedHero = 0;
    private boolean paused = false;
    private boolean waveActive = false;
    private int waveRemainingToSpawn = 0;
    private double waveSpawnTimer = 0;
    private double waveSpawnInterval = 0.4;
    private double intermissionTimer = 0;
    private static final double STEP = 1.0 / 60.0;
    
    private final Set<Integer> keysDown = new HashSet<>();

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(24, 26, 32));
        setFocusable(true);
        addKeyListener(this);

        heroes.add(new Hero("Tank", AbilityType.STUN, 3.5, 160, 260, new Color(64, 150, 255), 240, 6, 1.2));
        heroes.add(new Hero("Rogue", AbilityType.DASH, 2.0, 210, 320, new Color(255, 120, 90), 140, 12, 2.2));
        heroes.add(new Hero("Mage", AbilityType.FIREBALL, 2.8, 220, 200, new Color(170, 110, 255), 110, 18, 1.4));

        timer = new Timer(1000 / FPS, this);
    }

    void start() {
        timer.start();
        startWave(wave);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused) updateGame();
        repaint();
    }

    private void updateGame() {
        handleInput();

        for (Hero h : heroes) {
            h.update();
        }

        for (Monster m : monsters) {
            m.update();
        }

        updateParticles();
        updateProjectiles();
        resolveCombat();
        cleanupDead();
        updateWaveSystem();
    }

    private void updateWaveSystem() {
        if (waveActive) {
            if (waveRemainingToSpawn > 0) {
                waveSpawnTimer -= STEP;
                if (waveSpawnTimer <= 0) {
                    spawnWaveMonster();
                    waveRemainingToSpawn--;
                    waveSpawnTimer = waveSpawnInterval;
                }
            } else if (monsters.isEmpty()) {
                waveActive = false;
                intermissionTimer = 1.2;
            }
        } else {
            if (intermissionTimer > 0) {
                intermissionTimer -= STEP;
            } else if (monsters.isEmpty()) {
                wave++;
                startWave(wave);
            }
        }
    }

    private void handleInput() {
        if (heroes.isEmpty()) return;
        Hero h = heroes.get(Math.max(0, Math.min(selectedHero, heroes.size() - 1)));

        double speed = 2.2;
        double dx = 0;
        double dy = 0;

        if (keysDown.contains(KeyEvent.VK_W)) dy -= speed;
        if (keysDown.contains(KeyEvent.VK_S)) dy += speed;
        if (keysDown.contains(KeyEvent.VK_A)) dx -= speed;
        if (keysDown.contains(KeyEvent.VK_D)) dx += speed;

        if (dx != 0 || dy != 0) {
            double len = Math.sqrt(dx * dx + dy * dy);
            dx /= len;
            dy /= len;
            h.setLastMove(dx, dy);
            h.x += dx * speed;
            h.y += dy * speed;
            h.x = clamp(h.x, 40, WIDTH - 40);
            h.y = clamp(h.y, 60, HEIGHT - 40);
        }
    }

    private void updateProjectiles() {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update();
            boolean remove = false;
            if (p.x < 0 || p.x > WIDTH || p.y < 0 || p.y > HEIGHT) {
                remove = true;
            } else {
                for (Monster m : monsters) {
                    double d = distance(p.x, p.y, m.x, m.y);
                    if (d <= p.radius + 14) {
                        m.hp -= p.damage;
                        spawnExplosion(p.x, p.y, new Color(255, 160, 80));
                        remove = true;
                        break;
                    }
                }
            }
            if (remove) projectiles.remove(i);
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
        for (int i = rings.size() - 1; i >= 0; i--) {
            Ring r = rings.get(i);
            r.update();
            if (r.life <= 0) {
                rings.remove(i);
            }
        }
        if (flashTimer > 0) {
            flashTimer = Math.max(0, flashTimer - 1.0 / 60.0);
        }
        if (shakeTimer > 0) {
            shakeTimer = Math.max(0, shakeTimer - 1.0 / 60.0);
            double t = Math.max(0.0, shakeTimer / 0.18);
            double mag = shakeMagnitude * t;
            shakeX = (rng.nextDouble() - 0.5) * mag;
            shakeY = (rng.nextDouble() - 0.5) * mag;
        } else {
            shakeX = 0;
            shakeY = 0;
        }
    }

    private void resolveCombat() {
        for (Hero h : heroes) {
            Monster target = findNearestMonster(h);
            if (target == null) continue;

            double dist = distance(h.x, h.y, target.x, target.y);
            if (dist <= h.range) {
                if (h.canAttack()) {
                    target.hp -= h.damage;
                    h.resetAttack();
                }
            }

            for (Monster m : monsters) {
                if (m.isStunned()) continue;
                Hero target2 = findNearestHero(m);
                if (target2 == null) continue;
                double dist2 = distance(m.x, m.y, target2.x, target2.y);
                if (dist2 <= m.range) {
                    if (m.canAttack()) {
                        target2.hp -= m.damage;
                        m.resetAttack();
                    }
                } else {
                    m.moveToward(target2.x, target2.y);
                }
            }
        }
    }

        private void cleanupDead() {
            monsters.removeIf(m -> m.hp <= 0);
            heroes.removeIf(h -> h.hp <= 0);
            if (selectedHero >= heroes.size()) selectedHero = Math.max(0, heroes.size() - 1);
        }

        private void startWave(int wave) {
            waveActive = true;
            int baseCount = 4 + wave * 2;
            if (wave % 5 == 0) baseCount += 4;
            waveRemainingToSpawn = baseCount;
            waveSpawnInterval = Math.max(0.15, 0.55 - wave * 0.02);
            waveSpawnTimer = 0;
        }

        private void spawnWaveMonster() {
            double y = 100 + rng.nextInt(HEIGHT - 180);
            double x = WIDTH + 60 + rng.nextInt(300);
            int hp = 40 + wave * 10;
            int dmg = 4 + wave / 2;
            double range = 42 + wave * 2;
            if (wave % 5 == 0 && waveRemainingToSpawn == 1) {
                hp += 120;
                dmg += 6;
                range += 12;
            }
            monsters.add(new Monster(x, y, hp, dmg, range));
        }

        private Monster findNearestMonster(Entity e) {
            Monster best = null;
            double bestDist = Double.MAX_VALUE;
            for (Monster m : monsters) {
                double d = distance(e.x, e.y, m.x, m.y);
                if (d < bestDist) {
                    bestDist = d;
                    best = m;
                }
            }
            return best;
        }

        private Hero findNearestHero(Entity e) {
            Hero best = null;
            double bestDist = Double.MAX_VALUE;
            for (Hero h : heroes) {
                double d = distance(e.x, e.y, h.x, h.y);
                if (d < bestDist) {
                    bestDist = d;
                    best = h;
                }
            }
            return best;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.translate(shakeX, shakeY);
            drawArena(g2);
            drawEntities(g2);
            drawUI(g2);
            g2.translate(-shakeX, -shakeY);
            drawFlash(g2);
        }

        private void drawArena(Graphics2D g2) {
            g2.setColor(new Color(35, 38, 45));
            g2.fillRoundRect(30, 50, WIDTH - 60, HEIGHT - 90, 18, 18);
            g2.setColor(new Color(60, 64, 74));
            g2.drawRoundRect(30, 50, WIDTH - 60, HEIGHT - 90, 18, 18);
        }

        private void drawEntities(Graphics2D g2) {
            for (Hero h : heroes) {
                drawHero(g2, h);
                g2.setColor(Color.WHITE);
                g2.drawString(h.name, (int) (h.x - 22), (int) (h.y - 26));
                drawHealthBar(g2, h.x - 22, h.y + 22, 44, 6, h.hp, h.maxHp, new Color(80, 220, 140));
            }

            for (Monster m : monsters) {
                if (m.isStunned()) {
                    g2.setColor(new Color(120, 180, 255));
                } else {
                    g2.setColor(new Color(220, 80, 80));
                }
                g2.fillOval((int) (m.x - 16), (int) (m.y - 16), 32, 32);
                drawHealthBar(g2, m.x - 20, m.y + 20, 40, 5, m.hp, m.maxHp, new Color(255, 160, 120));
            }

            for (Projectile p : projectiles) {
                g2.setColor(new Color(255, 190, 120));
                g2.fillOval((int) (p.x - p.radius), (int) (p.y - p.radius), (int) (p.radius * 2), (int) (p.radius * 2));
            }

            for (Particle p : particles) {
                g2.setColor(p.color);
                int size = (int) Math.max(2, p.size);
                g2.fillOval((int) (p.x - size / 2.0), (int) (p.y - size / 2.0), size, size);
            }

            Stroke oldStroke = g2.getStroke();
            for (Ring r : rings) {
                g2.setStroke(new BasicStroke((float) r.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(r.color);
                int size = (int) Math.max(2, r.radius * 2);
                g2.drawOval((int) (r.x - r.radius), (int) (r.y - r.radius), size, size);
            }
            g2.setStroke(oldStroke);
        }

        private void drawHero(Graphics2D g2, Hero h) {
            int cx = (int) h.x;
            int cy = (int) h.y;
            double t = System.currentTimeMillis() / 1000.0;
            double seed = h.name.hashCode() * 0.13;
            int bob = (int) Math.round(Math.sin(t * 32 + seed) * 2.5);
            boolean blink = Math.sin(t * 6.5 + seed) > 0.98;
            cy += bob;

            g2.setColor(h.color.darker());
            g2.fillRoundRect(cx - 12, cy - 10, 24, 26, 6, 6);
            g2.setColor(new Color(240, 240, 245));
            g2.fillOval(cx - 8, cy - 18, 16, 16);
            drawFace(g2, cx, cy - 10, blink);

            if ("Tank".equals(h.name)) {
                g2.setColor(h.color);
                g2.fillRoundRect(cx - 14, cy - 12, 28, 28, 8, 8);
                g2.setColor(new Color(220, 230, 255));
                g2.fillOval(cx - 5, cy - 10, 10, 10);
                drawFace(g2, cx, cy - 10, blink);
                g2.setColor(new Color(90, 120, 180));
                g2.fillRoundRect(cx + 12, cy - 8, 10, 16, 4, 4);
            } else if ("Rogue".equals(h.name)) {
                g2.setColor(h.color);
                g2.fillRoundRect(cx - 10, cy - 12, 20, 22, 6, 6);
                g2.setColor(new Color(60, 60, 60));
                g2.fillOval(cx - 9, cy - 16, 18, 12);
                drawFace(g2, cx, cy - 10, blink);
                g2.setColor(new Color(200, 200, 200));
                g2.fillRect(cx - 18, cy, 8, 2);
                g2.fillRect(cx + 10, cy, 8, 2);
            } else {
                g2.setColor(h.color);
                g2.fillRoundRect(cx - 10, cy - 12, 20, 24, 6, 6);
                drawFace(g2, cx, cy - 10, blink);
                g2.setColor(new Color(140, 80, 30));
                g2.fillRect(cx + 12, cy - 18, 3, 30);
                g2.setColor(new Color(255, 220, 140));
                g2.fillOval(cx + 10, cy - 22, 8, 8);
                g2.setColor(new Color(120, 60, 200));
                g2.fillOval(cx - 8, cy - 16, 16, 10);
            }
        }

        private void drawFace(Graphics2D g2, int cx, int cy, boolean blink) {
            g2.setColor(new Color(30, 30, 30));
            if (blink) {
                g2.drawLine(cx - 5, cy - 1, cx - 2, cy - 1);
                g2.drawLine(cx + 2, cy - 1, cx + 5, cy - 1);
            } else {
                g2.fillOval(cx - 5, cy - 4, 3, 3);
                g2.fillOval(cx + 2, cy - 4, 3, 3);
            }
        }

        private void drawUI(Graphics2D g2) {
            g2.setColor(new Color(235, 235, 245));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString("Wave: " + wave, 40, 30);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString("WASD: move | 1-3: select hero | SPACE: skill | P: pause", 160, 30);

            if (!heroes.isEmpty()) {
                Hero h = heroes.get(Math.max(0, Math.min(selectedHero, heroes.size() - 1)));
                double cd = h.getAbilityCooldownRemaining();
                String cdText = cd <= 0 ? "READY" : String.format( "%.1fs", cd);
                g2.drawString("Skill: " + h.ability + " (" + cdText + ")", 40, HEIGHT - 16);
            }

            if (!waveActive && intermissionTimer > 0 && !heroes.isEmpty()) {
                g2.setColor(new Color(220, 220, 230));
                g2.drawString("Next wave in " + String.format("%.1fs", intermissionTimer), 40, 48);
            } else if (waveActive && waveRemainingToSpawn > 0) {
                g2.setColor(new Color(220, 220, 230));
                g2.drawString("Enemies incoming: " + waveRemainingToSpawn, 40, 48);
            }

            if (paused) {
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
                g2.drawString("PAUSE", WIDTH / 2 - 50, HEIGHT / 2);
            }

            if (heroes.isEmpty()) {
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                g2.setColor(new Color(255, 230, 230));
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
                g2.drawString("DEFEAT", WIDTH / 2 - 60, HEIGHT / 2);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
                g2.drawString("Relance pour rejouer", WIDTH / 2 - 70, HEIGHT / 2 + 26);
            }
        }

        private void drawFlash(Graphics2D g2) {
            if (flashTimer <= 0) return;
            float alpha = (float) Math.min(0.35, flashTimer * 0.6);
            g2.setColor(new Color(255, 245, 210, (int) (alpha * 255)));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
        }

        private void drawHealthBar(Graphics2D g2, double x, double y, int w, int h, int hp, int maxHp, Color color) {
            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect((int) x, (int) y, w, h, 4, 4);
            int fill = (int) Math.max(0, Math.min(w, (hp / (double) maxHp) * w));
            g2.setColor(color);
            g2.fillRoundRect((int) x, (int) y, fill, h, 4, 4);
        }

        private static double clamp(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }
    
        private static double distance(double x1, double y1, double x2, double y2) {
            double dx = x1 - x2;
            double dy = y1 - y2;
            return Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            keysDown.add(e.getKeyCode());
            if (e.getKeyCode() == KeyEvent.VK_P) {
                paused = !paused;
            }
            if (e.getKeyCode() == KeyEvent.VK_1) selectedHero = 0;
            if (e.getKeyCode() == KeyEvent.VK_2) selectedHero = 1;
            if (e.getKeyCode() == KeyEvent.VK_3) selectedHero = 2;
            if (e.getKeyCode() == KeyEvent.VK_SPACE) triggerAbility();
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keysDown.remove(e.getKeyCode());
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // unused
        }

        private void triggerAbility() {
            if (heroes.isEmpty()) return;
            Hero h = heroes.get(Math.max(0, Math.min(selectedHero, heroes.size() - 1)));
            if (!h.canUseAbility()) return;

            if (h.ability == AbilityType.DASH) {
                double dx = h.lastDirX;
                double dy = h.lastDirY;
                if (dx == 0 && dy == 0) {
                    dx = 1;
                    dy = 0;
                }
                double startX = h.x;
                double startY = h.y;
                h.x += dx * 120;
                h.y += dy * 120;
                h.x = clamp(h.x, 40, WIDTH - 40);
                h.y = clamp(h.y, 60, HEIGHT - 40);
                spawnDashTrail(startX, startY, h.x, h.y, h.color);
                h.useAbility();
            } else if (h.ability == AbilityType.FIREBALL) {
                Monster target = findNearestMonster(h);
                if (target == null) return;
                double dx = target.x - h.x;
                double dy = target.y - h.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len == 0) return;
                dx /= len;
                dy /= len;
                projectiles.add(new Projectile(h.x, h.y, dx * 6.5, dy * 6.5, 12, 28));
                h.useAbility();
            } else if (h.ability == AbilityType.STUN) {
                double radius = 90;
                for (Monster m : monsters) {
                    double d = distance (h.x, h.y, m.x, m.y);
                    if (d <= radius) {
                        m.stun(1.5);
                    }
                }
                h.useAbility();
            }
        }

        private void spawnDashTrail(double x1, double y1, double x2, double y2, Color base) {
            int steps = 12;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double x = x1 + (x2 - x1) * t;
                double y = y1 + (y2 - y1) * t;
                double jitterX = (rng.nextDouble() - 0.5) * 6;
                double jitterY = (rng.nextDouble() - 0.5) * 6;
                particles.add(new Particle(x + jitterX, y + jitterY, 0, 0, 12, 0.35, new Color(base.getRed(), base.getGreen(), base.getBlue(), 180)));
            }
        } 

        private void spawnExplosion(double x, double y, Color color) {
            flashTimer = Math.max(flashTimer, 0.18);
            shakeTimer = Math.max(shakeTimer, 0.18);
            shakeMagnitude = Math.max(shakeMagnitude, 10);
            rings.add(new Ring(x, y, 10, 44, 0.35, 3.5, new Color(255, 230, 180, 200)));
            rings.add(new Ring(x, y, 6, 30, 0.28, 2.2, new Color(color.getRed(), color.getGreen(), color.getBlue(), 190)));
            for (int i = 0; i < 18; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double speed = 1.2 + rng.nextDouble() * 2.0;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;
                double size = 6 + rng.nextDouble() * 6;
                particles.add(new Particle(x, y, vx, vy, size, 0.5 + rng.nextDouble() * 0.3, new Color(color.getRed(), color.getGreen(), color.getBlue(), 200)));
            }
        }
    }
