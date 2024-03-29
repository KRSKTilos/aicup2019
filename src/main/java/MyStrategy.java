import model.*;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class MyStrategy {
  private static boolean DRAW_DEBUG = true;
  private static final int FLEE_TICKS = 6;
  private static int MAX_SECONDS_AT_DEST = 2;
  private static WeaponType PERFECT_WEAPON_TYPE = WeaponType.PISTOL;
  private static WeaponType GOOD_WEAPON_TYPE = WeaponType.ASSAULT_RIFLE;
  private static double MIN_DISTANCE_TO_ENEMY = 0.5f;
  private static int MIN_HEALTH_PERCENT = 70;
  private static double MAX_HORIZONTAL_SPEED = 20;
  private int lastTick = -1;
  private boolean firstUnit = false;
  private Person person1;
  private Person person2;

  public UnitAction getAction(Unit unit, Game game, Debug debug) {
    System.out.println("--------------------------------------------");
    System.out.println("tick " + game.getCurrentTick() + " last " + lastTick);
    boolean nextTick = game.getCurrentTick()!=lastTick;
    firstUnit = nextTick;
    if (nextTick) {
      lastTick = game.getCurrentTick();
    }
    Person p = currentPerson();

    if (p.fleeTicks > 0) {
      p.fleeTicks--;
    } else {
      p.fleeVector = null;
    }

    int unitPlayers = 0;
    Unit unit2 = null;
    for (Unit u : game.getUnits()) {
      if (unit.getPlayerId() == u.getPlayerId()) {
        unitPlayers++;
        if (unit.getPosition().getX()!=u.getPosition().getX()
                || unit.getPosition().getY()!=u.getPosition().getY()) {
          unit2 = u;
        }
      }
    }

    MAX_HORIZONTAL_SPEED = game.getProperties().getUnitMaxHorizontalSpeed();
    if (p.findGoodWeapon) {
      debug.draw(new CustomData.Log("FIND_GOOD_WEAPON"));
    }
    checkDestinationExists(p, game);
    boolean swapWeapon = false;
    if (p.dest != null) {
      double deltaX = p.dest.getPosition().getX() - unit.getPosition().getX();
      double deltaY = p.dest.getPosition().getY() - unit.getPosition().getY();
      if (Math.abs(deltaX)<=0.2 && Math.abs(deltaY)<=0.2) {
        System.out.println("AT_DEST");
        p.tickAtDest++;
        if (p.findGoodWeapon) {
          System.out.println("BINGO");
          p.findGoodWeapon = false;
          swapWeapon = true;
          p.goodWeapon = true;
          p.dest = null;
          p.tickAtDest = 0;
        }
        if (p.tickAtDest > game.getProperties().getTicksPerSecond()*MAX_SECONDS_AT_DEST) {
          System.out.println("<< LONG_WAIT >>");
          p.tickAtDest = 0;
          p.dest = null;
        }
      }
    }

    if (p.findNearWeapon) {
      /* maybe already pickup */
      if (unit.getWeapon() != null) {
        p.findNearWeapon = false;
        p.dest = null;
      }
    }

    Unit enemy = null;
    List<Unit> enemies = new LinkedList<>();
    for (Unit other : game.getUnits()) {
      if (other.getPlayerId() != unit.getPlayerId()) {
        enemies.add(other);
        enemy = other;
      }
    }

    /* find near enemy */
    double distanceToEnemy = -1;
    for (Unit e : enemies) {
      double dist = distanceSqr(unit.getPosition(), e.getPosition());
      if (distanceToEnemy < 0) {
        distanceToEnemy = dist;
        enemy = e;
      } else {
        if (dist < distanceToEnemy) {
          distanceToEnemy = dist;
          enemy = e;
        }
      }
    }

    /* find near spotted enemy */
    if (enemy != null) {
      for (Unit e : enemies) {
        if (isEnemySpotted(game, unit, e, unit2, debug)) {
          enemy = e;
          break;
        }
      }
    }

    int userHealthPercent = game.getProperties().getUnitMaxHealth() / 100 * unit.getHealth();
    if (userHealthPercent < MIN_HEALTH_PERCENT) {
      debug.draw(new CustomData.Log("LOW HP! LESS THAN " + MIN_HEALTH_PERCENT));
      findHealthPack(p, unit, enemy, game);
    } else if (enemy!=null && enemy.getWeapon()!=null) {
      if (enemy.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)) {
        int bulletDamage = game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getBullet().getDamage();
        int explosionDamage = game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getExplosion().getDamage();
        int totalMaxDamage = bulletDamage + explosionDamage;
        if (totalMaxDamage >= unit.getHealth()) {
          debug.draw(new CustomData.Log("LOW HP! ROCKET LAUNCHER PANIC!"));
          findHealthPack(p, unit, enemy, game);
        }
      }
    }

    if (p.dest==null && unit.getWeapon()==null) {
      findNearWeapon(p, unit, game);
    }

    if (p.dest==null && unit.getWeapon()!=null) {
      findGoodWeapon(p, unit, game);
    }

    Vec2Double targetPos = unit.getPosition();
    if (p.dest != null) {
      targetPos = p.dest.getPosition();
    }

    if (p.dest==null && enemy!=null) {
      targetPos = enemy.getPosition();
    }

    if (enemy!=null && DRAW_DEBUG) {
      debug.draw(new CustomData.Rect(
              new Vec2Float((float) (enemy.getPosition().getX()-(enemy.getSize().getX()/2)), (float) enemy.getPosition().getY()),
              new Vec2Float((float) enemy.getSize().getX(), (float) enemy.getSize().getY()),
              new ColorFloat(255, 0, 0, 0.5f))
      );
    }

    if (p.dest!=null && DRAW_DEBUG) {
      debug.draw(new CustomData.Rect(
              new Vec2Float((float) (p.dest.getPosition().getX()-(p.dest.getSize().getX()/2)), (float) p.dest.getPosition().getY()),
              new Vec2Float((float) p.dest.getSize().getX(), (float) p.dest.getSize().getY()),
              new ColorFloat(0, 255, 0, 0.5f))
      );
    }

    if (targetPos!=null && DRAW_DEBUG) {
      debug.draw(new CustomData.Rect(
              new Vec2Float((float) targetPos.getX(), (float) targetPos.getY()),
              new Vec2Float((float) 1, (float) 1),
              new ColorFloat(0, 0, 255, 0.5f))
      );
    }

    Vec2Double aim = new Vec2Double(0, 0);
    if (enemy != null) {
      aim = new Vec2Double(
              enemy.getPosition().getX() - unit.getPosition().getX(),
              enemy.getPosition().getY() - unit.getPosition().getY()
      );
    }

    boolean jump = targetPos.getY() > unit.getPosition().getY();
    if (targetPos.getX() >= unit.getPosition().getX()) {
      int x = (int) unit.getPosition().getX();
      int y = (int) unit.getPosition().getY();
      Tile rightTile = game.getLevel().getTiles()[x+1][y];
      Tile currentTile = game.getLevel().getTiles()[x][y];
      if (rightTile.equals(Tile.WALL)) {
        if (slideDown(game, unit, targetPos)) {
          jump = false;
        } else {
          jump = true;
          int lastX = (int) p.lastPosition.getX();
          int lastY = (int) p.lastPosition.getY();
          if (p.lastTile.equals(Tile.PLATFORM) && currentTile.equals(Tile.EMPTY) && (lastX!=x || lastY!=y)) {
            p.needStand = isNeedStand(game, x, y);
          }
        }
      }
    }

    if (targetPos.getX() < unit.getPosition().getX()) {
      int x = (int) unit.getPosition().getX();
      int y = (int) unit.getPosition().getY();
      Tile leftTile = game.getLevel().getTiles()[x-1][y];
      Tile currentTile = game.getLevel().getTiles()[x][y];
      if (leftTile.equals(Tile.WALL)) {
        if (slideDown(game, unit, targetPos)) {
          jump = false;
        } else {
          jump = true;
          int lastX = (int) p.lastPosition.getX();
          int lastY = (int) p.lastPosition.getY();
          if (p.lastTile.equals(Tile.PLATFORM) && currentTile.equals(Tile.EMPTY) && (lastX!=x || lastY!=y)) {
            p.needStand = isNeedStand(game, x, y);
          }
        }
      }
    }

    /* перепрыгнуть противника при движении к цели */
    if (p.dest!=null && enemy!=null) {
      /* to left */
      if (p.dest.getPosition().getX()<unit.getPosition().getX()
              && (isJumpOnUnit(unit, enemy, true) || isJumpOnUnit(unit, unit2, true))) {
        jump = true;
      }
      /* to right */
      if (p.dest.getPosition().getX()>unit.getPosition().getX()
              && (isJumpOnUnit(unit, enemy, false) || isJumpOnUnit(unit, unit2, false))) {
        jump = true;
      }
    }

    if (p.dest==null && enemy!=null && unit2!=null) {
      /* to left */
      if (targetPos.getX()<unit.getPosition().getX()
              && (isJumpOnUnit(unit, enemy, true) || isJumpOnUnit(unit, unit2, true))) {
        jump = true;
      }
      /* to right */
      if (targetPos.getX()>unit.getPosition().getX()
              && (isJumpOnUnit(unit, enemy, false) || isJumpOnUnit(unit, unit2, false))) {
        jump = true;
      }
    }

    boolean enemySpotted = false;
    if (enemy != null) {
      enemySpotted = isEnemySpotted(game, unit, enemy, unit2, debug);
    }

    double minDistance = MIN_DISTANCE_TO_ENEMY;
    if (p.dest==null && unit.getWeapon()!=null) {
      /* safe distance */
      if (unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)) {
        minDistance = unit.getWeapon().getParams().getExplosion().getRadius()*2;
      }
      if (unit.getPosition().getX() > targetPos.getX()) {
        targetPos.setX(targetPos.getX()+minDistance);
      } else {
        targetPos.setX(targetPos.getX()-minDistance);
      }
    }

    if (p.closeCombat != 0) {
      System.out.println("closeCombat = " + p.closeCombat);
    }

    if (enemySpotted) {
      p.closeCombat = 0;
    }

    double velocity;
    velocity = (targetPos.getX()-unit.getPosition().getX()) * game.getProperties().getUnitMaxHorizontalSpeed();
    UnitAction action = new UnitAction();
    if (swapWeapon) {
      velocity = 0;
    } else if (p.dest==null && enemy!=null) {
      final int RIGHT = 1;
      final int LEFT = -1;
      if (Math.abs(enemy.getPosition().getX() - unit.getPosition().getX()) <= minDistance) {
        if (enemy.getPosition().getY() < unit.getPosition().getY()) {
          if (enemySpotted) {
            p.closeCombat = 0;
            velocity = 0;
          } else {
            if (p.closeCombat == 0) {
              if (enemy.getPosition().getX() <= unit.getPosition().getX()) {
                p.closeCombat = LEFT;
              }
              if (enemy.getPosition().getX() >= unit.getPosition().getX()) {
                p.closeCombat = RIGHT;
              }
            }
          }
        }
      }
      if (p.closeCombat == LEFT) {
        velocity = -game.getProperties().getUnitMaxHorizontalSpeed();
      }
      if (p.closeCombat == RIGHT) {
        velocity = game.getProperties().getUnitMaxHorizontalSpeed();
      }
    }

    /* flee from bullets */
    if (p.dest==null && !p.findGoodWeapon && !p.findNearWeapon) {
      for (Bullet bullet : game.getBullets()) {
        if (bullet.getPlayerId() != unit.getPlayerId()) {
          double deltaX = bullet.getVelocity().getX() / game.getProperties().getTicksPerSecond();
          double deltaY = bullet.getVelocity().getY() / game.getProperties().getTicksPerSecond();
          int distanceToUnit = (int) distanceSqr(bullet.getPosition(), unit.getPosition());
          distanceToUnit++;
          Vec2Double bulletPosition = new Vec2Double(
                  bullet.getPosition().getX() + (deltaX * distanceToUnit),
                  bullet.getPosition().getY() + (deltaY * distanceToUnit)
          );
          FleeVector vector = fleeFromBulletsVector(game, unit, velocity, bullet.getPosition(), bulletPosition, bullet);
          if (vector != null) {
            p.fleeVector = vector;
            /* set velocity */
            p.fleeTicks = FLEE_TICKS;
            break;
          }
        }
      }
    }

    if (p.fleeTicks>0 && p.fleeVector!=null) {
      velocity = p.fleeVector.velocity;
      jump = p.fleeVector.jump;
    }

    for (Mine mine : game.getMines()) {
      double safeRadius = 1;
      double radius = mine.getExplosionParams().getRadius() + safeRadius;
      double deltaX = Math.abs(unit.getPosition().getX() - mine.getPosition().getX());
      double deltaY = Math.abs(unit.getPosition().getY() - mine.getPosition().getY());
      if (mine.getState()==MineState.TRIGGERED) {
        if (deltaX<=radius && deltaY<=radius) {
          int levelSize = game.getLevel().getTiles().length;
          debug.draw(new CustomData.Log("FLEE_FROM_MINE"));
          if (mine.getPosition().getX() <= unit.getPosition().getX()) {
            /*flee to right*/
            velocity = ((levelSize-1)-unit.getPosition().getX()) * game.getProperties().getUnitMaxHorizontalSpeed();
            jump = true;
          }
          if (mine.getPosition().getX() > unit.getPosition().getX()) {
            /*flee to left*/
            velocity = (0-unit.getPosition().getX()) * game.getProperties().getUnitMaxHorizontalSpeed();
            jump = true;
          }
        }
      }
    }

    boolean reload = false;
    if (unit.getWeapon() != null) {
      Weapon weapon = unit.getWeapon();
      if (weapon.getMagazine() == 0) {
        reload = true;
      } else if (!enemySpotted) {
        if (weapon.getMagazine() <= weapon.getParams().getMagazineSize()/2) {
          reload = true;
        }
      }
    }

    int x = (int) unit.getPosition().getX();
    int y = (int) unit.getPosition().getY();
    Tile tile = game.getLevel().getTiles()[x][y];

    if (p.needStand) {
      if (unit.getJumpState().isCanCancel()) {
        if (tile.equals(Tile.EMPTY) && p.lastTile.equals(Tile.EMPTY)) {
          jump = false;
          System.out.println("need stand");
        }
      } else {
        p.needStand = false;
      }
    }

    action.setVelocity(velocity);
    action.setJump(jump);
    action.setJumpDown(!jump);
    action.setAim(aim);
    action.setShoot(enemySpotted);
    action.setReload(reload);
    action.setSwapWeapon(swapWeapon);
    action.setPlantMine(false);

    p.lastTile = tile;
    p.lastPosition = unit.getPosition();
    return action;
  }

  private boolean isNeedStand(Game game, int currentTileX, int currentTileY) {
    boolean emptyTilesUp = true;
    int tileUpCheckSize = 2;
    for (int tileUpIndex=1; tileUpIndex<=tileUpCheckSize; tileUpIndex++) {
      int tileUpY = currentTileY + tileUpIndex;
      if (tileUpIndex <= game.getLevel().getTiles().length) {
        Tile upTile = game.getLevel().getTiles()[currentTileX][tileUpY];
        if (upTile != Tile.EMPTY) {
          emptyTilesUp = false;
        }
      }
    }
    return !emptyTilesUp;
  }

  private boolean slideDown(Game game, Unit unit, Vec2Double target) {
    boolean slideDown = false;
    int posX = (int) unit.getPosition().getX();
    int posY = (int) unit.getPosition().getY();
    int destX = (int) target.getX();
    int destY = (int) target.getY();
    if (destX==posX && destY<=posY) {
      boolean free = true;
      int deltaY = destY-posY;
      for (int i=1; i<deltaY; i++) {
        Tile tile = game.getLevel().getTiles()[posX][posY+i];
        if (tile == Tile.WALL) {
          free = false;
          break;
        }
      }
      if (free) {
        slideDown = true;
      }
    }
    if (slideDown) {
      System.out.println("SLIDE_DOWN");
    }
    return slideDown;
  }

  private boolean isJumpOnUnit(Unit unit, Unit enemy, boolean leftWalk) {
    if (enemy == null)
      return false;

    int enemyX = (int) enemy.getPosition().getX();
    int x = (int) unit.getPosition().getX();
    if (leftWalk && ((x-1)==enemyX))
      return true;

    if (!leftWalk && (x+1)==enemyX)
      return true;

    return false;
  }

  private void findHealthPack(Person p, Unit unit, Unit enemy, Game game) {
    if (p.dest==null || p.dest.getItem() instanceof Item.HealthPack) {
      Map<LootBox, Double> packs = new HashMap<>();
      for (LootBox lootBox : game.getLootBoxes()) {
        if (lootBox.getItem() instanceof Item.HealthPack) {
          packs.put(lootBox, distanceSqr(unit.getPosition(), lootBox.getPosition()));
        }
      }
      if (!packs.isEmpty()) {
        LinkedHashMap<LootBox, Double> sortedMap = new LinkedHashMap<>();
        packs.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        LootBox nearPack = sortedMap.entrySet().iterator().next().getKey();
        p.dest = nearPack;
        if (unit.getPosition().getX() < enemy.getPosition().getX()) {
          /*find left from enemy*/
          for (LootBox lootBox : sortedMap.keySet()) {
            if (lootBox.getPosition().getX() < enemy.getPosition().getX()) {
              p.dest = lootBox;
              break;
            }
          }
        }
        if (unit.getPosition().getX() > enemy.getPosition().getX()) {
          /*find right from enemy*/
          for (LootBox lootBox : sortedMap.keySet()) {
            if (lootBox.getPosition().getX() > enemy.getPosition().getX()) {
              p.dest = lootBox;
              break;
            }
          }
        }
      }
    }
  }

  private void checkDestinationExists(Person p, Game game) {
    if (p.dest != null) {
      boolean exists = false;
      for (LootBox lootBox : game.getLootBoxes()) {
        if (lootBox.getPosition().getX()==p.dest.getPosition().getX()
                && lootBox.getPosition().getY()==p.dest.getPosition().getY()) {
          exists = true;
          break;
        }
      }
      if (!exists) {
        p.dest = null;
      }
    }
  }

  /**
   * Определение видимости противника.
   */
  private boolean isEnemySpotted(Game game, Unit unit, Unit enemy, Unit friendUnit, Debug debug) {
    boolean enemySpotted = true;
    Vec2Double unitPosition = new Vec2Double(
            unit.getPosition().getX(),
            unit.getPosition().getY()+unit.getSize().getY()/2
    );
    Vec2Double enemyPosition = new Vec2Double(
            enemy.getPosition().getX(),
            enemy.getPosition().getY()+enemy.getSize().getY()/2
    );
    int k = (int) game.getProperties().getTicksPerSecond();
    double deltaX = (unitPosition.getX()-enemyPosition.getX())/k;
    double deltaY = (unitPosition.getY()-enemyPosition.getY())/k;
    double positionX = unitPosition.getX();
    double positionY = unitPosition.getY();
    for (int i=0; i<k; i++) {
      positionX-=deltaX;
      positionY-=deltaY;
      int tileX = (int) positionX;
      int tileY = (int) positionY;
      if (friendUnit!=null && unit.getWeapon()!=null) {
        BulletParams bullet = unit.getWeapon().getParams().getBullet();
        Rectangle2D friendRect = new Rectangle2D.Double(
                friendUnit.getPosition().getX()-(friendUnit.getSize().getX()/2),
                friendUnit.getPosition().getY(),
                friendUnit.getSize().getX(),
                friendUnit.getSize().getY()
        );
        Point2D bulPoint = new Point2D.Double(positionX, positionY);
        if (friendRect.contains(bulPoint)) {
          System.out.println("FRIEND IN LINE!");
          debug.draw(new CustomData.Rect(
                  new Vec2Float((float) (positionX-bullet.getSize()/2), (float) (positionY-bullet.getSize()/2)),
                  new Vec2Float((float) bullet.getSize(), (float) bullet.getSize()),
                  new ColorFloat(250, 0, 0, 1f)
          ));
          return false;
        } else {
          debug.draw(new CustomData.Rect(
                  new Vec2Float((float) (positionX-bullet.getSize()/2), (float) (positionY-bullet.getSize()/2)),
                  new Vec2Float((float) bullet.getSize(), (float) bullet.getSize()),
                  new ColorFloat(250, 250, 250, 0.1f)
          ));
        }
      }
      Tile tile = game.getLevel().getTiles()[tileX][tileY];
      if (tile == Tile.WALL) {
        enemySpotted = false;
        break;
      }
    }
    return enemySpotted;
  }

  private void findGoodWeapon(Person p, Unit unit, Game game) {
    if (unit.getWeapon().getTyp().equals(PERFECT_WEAPON_TYPE) || p.goodWeapon)
      return;

    Set<WeaponType> weaponTypes = new HashSet<>();
    Map<LootBox, Double> weaponBoxes = new HashMap<>();
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.Weapon) {
        double distance = distanceSqr(unit.getPosition(), lootBox.getPosition());
        if (weaponBoxes.containsKey(lootBox)) {
          if (weaponBoxes.get(lootBox) > distance) {
            weaponBoxes.put(lootBox, distance);
          }
        } else weaponBoxes.put(lootBox, distance);
        weaponTypes.add(((Item.Weapon) lootBox.getItem()).getWeaponType());
      }
    }

    LinkedHashMap<LootBox, Double> sortedMap = new LinkedHashMap<>();
    weaponBoxes.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

    WeaponType findType = null;
    if (weaponTypes.contains(PERFECT_WEAPON_TYPE)) {
      findType = PERFECT_WEAPON_TYPE;
    } else if (weaponTypes.contains(GOOD_WEAPON_TYPE)) {
      findType = GOOD_WEAPON_TYPE;
    }

    if (findType != null) {
      Iterator<Map.Entry<LootBox, Double>> lootBoxIterator = sortedMap.entrySet().iterator();
      while (lootBoxIterator.hasNext()) {
        LootBox lootBox = lootBoxIterator.next().getKey();
        Item.Weapon weapon = (Item.Weapon) lootBox.getItem();
        if (weapon.getWeaponType().equals(findType)) {
          System.out.println("FIND GOOD WEAPON: " + weapon.getWeaponType());
          p.findGoodWeapon = true;
          p.dest = lootBox;
          break;
        }
      }
    }
  }

  private void findNearWeapon(Person p, Unit unit, Game game) {
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.Weapon) {
        if (p.dest == null || distanceSqr(unit.getPosition(),
                lootBox.getPosition()) < distanceSqr(unit.getPosition(), p.dest.getPosition())) {
          System.out.println("NEAR WEAPON " + ((Item.Weapon) lootBox.getItem()).getWeaponType());
          p.findNearWeapon = true;
          p.dest = lootBox;
        }
      }
    }
  }

  static double distanceSqr(Vec2Double a, Vec2Double b) {
    return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
  }

  static Vec2Double pointAtVectorOnDistance(Vec2Double a, Vec2Double b, double rac) {
    double rab = Math.sqrt(Math.pow(b.getX() - a.getX(), 2) + Math.pow(b.getY() - a.getY(), 2));
    double k = rac / rab;
    double x = a.getX() + (b.getX()-a.getX())*k;
    double y = a.getY() + (b.getY()-a.getY())*k;
    return new Vec2Double(x, y);
  }

  private static FleeVector fleeFromBulletsVector(Game game, Unit unit, double unitVelocity,
                                                  Vec2Double bPosStart, Vec2Double bPosStop, Bullet bullet) {
    int distance = (int) distanceSqr(bPosStart, bPosStop);
    int distanceToUnit = (int) distanceSqr(bPosStart, unit.getPosition());
    Vec2Double uPos = unit.getPosition();
    int tilesSize = game.getLevel().getTiles().length;
    int uX = (int) uPos.getX();
    Vec2Double uPosCenter = new Vec2Double(uPos.getX(), uPos.getY()+(unit.getSize().getY()/2d));
    final double VELOCITY_RIGHT = (tilesSize-uX) * MAX_HORIZONTAL_SPEED;
    final double VELOCITY_LEFT = (-uX) * MAX_HORIZONTAL_SPEED;
    for (int i=0; i<distance; i++) {
      Vec2Double point = pointAtVectorOnDistance(bPosStart, bPosStop, i);
      double bulletSize = bullet.getSize();
      if (bullet.getWeaponType() == WeaponType.ROCKET_LAUNCHER) {
        bulletSize = bullet.getExplosionParams().getRadius()*2;
      }
      boolean isCollision = isCollision(point, unit, bulletSize);
      if (isCollision) {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("#COLLISION# tick " + game.getCurrentTick() + " pX " + point.getX() + " pY " + point.getY() + " distance " + distance );
        System.out.println("isCanJump " + unit.getJumpState().isCanJump() + "; isCanCancel " + unit.getJumpState().isCanCancel() + "; jumpSpeed " + unit.getJumpState().getSpeed());
      }
      if (isCollision) {
        System.out.println("<< DANGER >> distanceToUnit " + distanceToUnit);
        if (bPosStart.getX()<uPos.getX() && bPosStop.getX()>uPos.getX()) {
          System.out.println(">> LEFT_TO_RIGHT >>");
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in legs line");
            if (unitVelocity == 0) {
              System.out.println("jump_right");
              return new FleeVector(true, VELOCITY_RIGHT);
            } else {
              System.out.println("jump");
              return new FleeVector(true, unitVelocity);
            }
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in head line");
            if (unit.getJumpState().isCanJump()) {
              System.out.println("move_right");
              return new FleeVector(false, VELOCITY_RIGHT);
            } else {
              System.out.println("lay_down_to_left");
              return new FleeVector(false, VELOCITY_LEFT);
            }
          }
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in legs out head");
            System.out.println("jump_left");
            return new FleeVector(true, VELOCITY_LEFT);
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in head out legs");
            if (unit.getJumpState().isCanJump()) {
              System.out.println("jump_right");
              return new FleeVector(true, VELOCITY_RIGHT);
            } else {
              System.out.println("lay_down_left");
              return new FleeVector(false, VELOCITY_LEFT);
            }
          }
        }
        if (bPosStart.getX()>uPos.getX() && bPosStop.getX()<uPos.getX()) {
          System.out.println("<< RIGHT_TO_LEFT <<");
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in legs line");
            System.out.println("jump");
            return new FleeVector(true, unitVelocity);
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in head line");
            if (unit.getJumpState().isCanJump()) {
              System.out.println("move_left");
              return new FleeVector(false, VELOCITY_LEFT);
            } else {
              System.out.println("lay_down_to_right");
              return new FleeVector(false, VELOCITY_RIGHT);
            }
          }
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in legs out head");
            System.out.println("jump_right");
            return new FleeVector(true, VELOCITY_RIGHT);
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in head out legs");
            if (unit.getJumpState().isCanJump()) {
              System.out.println("jump_left");
              return new FleeVector(true, VELOCITY_LEFT);
            } else {
              System.out.println("lay_down_to_right");
              return new FleeVector(false, VELOCITY_RIGHT);
            }
          }
        }
        if (bPosStart.getX()<=uPosCenter.getX() && bPosStop.getX()<=uPosCenter.getX()) {
          System.out.println("VERTICAL_LEFT");
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in head quarter");
            System.out.println("move_right");
            return new FleeVector(false, VELOCITY_RIGHT);
          }
          System.out.println("jump_right");
          return new FleeVector(true, VELOCITY_RIGHT);
        }
        if (bPosStart.getX()>=uPosCenter.getX() && bPosStop.getX()>=uPosCenter.getX()) {
          System.out.println("VERTICAL_RIGHT");
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in head quarter");
            System.out.println("move_left");
            return new FleeVector(false, VELOCITY_LEFT);
          }
          System.out.println("jump_left");
          return new FleeVector(true, VELOCITY_LEFT);
        }
      }
    }
    return null;
  }

  private static boolean isCollision(Vec2Double bulletPos, Unit unit, double bulletSize) {
    double x1 = bulletPos.getX() - bulletSize/2;
    double y1 = bulletPos.getY() - bulletSize/2;
    double x2 = bulletPos.getX() + bulletSize/2;
    double y2 = bulletPos.getY() + bulletSize/2;
    double x3 = unit.getPosition().getX() - unit.getSize().getX()/2;
    double y3 = unit.getPosition().getY();
    double x4 = unit.getPosition().getX() + unit.getSize().getX()/2;
    double y4 = unit.getPosition().getY() + unit.getSize().getY();
    double left = Math.max(x1, x3);
    double top = Math.max(y2, y4);
    double right = Math.max(x2, x4);
    double bottom = Math.max(y1, y3);

    double width = right - left;
    double height = top - bottom;

    if (width<0 || height<0)
      return false;

    return true;
  }

  private Person currentPerson() {
    if (firstUnit) {
      System.out.println("person1");
      if (person1 == null) {
        person1 = new Person();
      }
      return person1;
    } else {
      System.out.println("person2");
      if (person2 == null) {
        person2 = new Person();
      }
      return person2;
    }
  }

  private static class FleeVector {
    public double velocity;
    public boolean jump;
    FleeVector(boolean jump, double velocity) {
      this.jump = jump;
      this.velocity = velocity;
    }
  }

  private static class Person {
    private boolean findGoodWeapon = false;
    private boolean findNearWeapon = false;
    private boolean goodWeapon = false;
    private LootBox dest = null;
    private Vec2Double lastPosition = new Vec2Double(0,0);
    private Tile lastTile = Tile.EMPTY;
    private boolean needStand = false;
    private int closeCombat = 0;
    private int tickAtDest = 0;
    private int fleeTicks = 0;
    private FleeVector fleeVector = null;
  }
}