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

    private int wave = 1;
    private int selectedHero = 0;
    private long lastSpawnTime = 0;
    private boolean paused = false;
    
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
        spawnWave(wave);
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

        updateProjectiles();
        resolveCombat();
        cleanupDead();

        if (monsters.isEmpty() && System.currentTimeMillis() - lastSpawnTime > 800) {
            wave++;
            spawnWave(wave);
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
                        remove = true;
                        break;
                    }
                }
            }
            if (remove) projectiles.remove(i);
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

        private void spawnWave(int wave) {
            lastSpawnTime = System.currentTimeMillis();
            int count = 3 + wave;
            for (int i = 0; i < count; i++) {
                double y = 100 + rng.nextInt(HEIGHT - 180);
                double x = WIDTH + 60 + rng.nextInt(300);
                int hp = 40 + wave * 8;
                int dmg = 4 + wave / 2;
                monsters.add(new Monster(x, y, hp, dmg, 42 + wave * 2));
            }
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

            drawArena(g2);
            drawEntities(g2);
            drawUI(g2);
        }

        private void drawArena(Graphics2D g2) {
            g2.setColor(new Color(35, 38, 45));
            g2.fillRoundRect(30, 50, WIDTH - 60, HEIGHT - 90, 18, 18);
            g2.setColor(new Color(60, 64, 74));
            g2.drawRoundRect(30, 50, WIDTH - 60, HEIGHT - 90, 18, 18);
        }

        private void drawEntities(Graphics2D g2) {
            for (Hero h : heroes) {
                g2.setColor(h.color);
                g2.fillRoundRect((int) (h.x - 18), (int) (h.y - 18), 36, 36, 8, 8);

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
                h.x += dx * 120;
                h.y += dy * 120;
                h.x = clamp(h.x, 40, WIDTH - 40);
                h.y = clamp(h.y, 60, HEIGHT - 40);
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
    }
