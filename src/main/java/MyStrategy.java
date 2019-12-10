import model.*;

import java.util.*;

public class MyStrategy {
  private static final int FLEE_TICKS = 12;
  private static int MAX_SECONDS_AT_DEST = 2;
  private static WeaponType PERFECT_WEAPON_TYPE = WeaponType.PISTOL;
  private static WeaponType GOOD_WEAPON_TYPE = WeaponType.ASSAULT_RIFLE;
  private static double MIN_DISTANCE_TO_ENEMY = 0.5f;
  private static int MIN_HEALTH_PERCENT = 70;
  private static double MAX_HORIZONTAL_SPEED = 20;
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

  public UnitAction getAction(Unit unit, Game game, Debug debug) {
    if (fleeTicks > 0) {
      fleeTicks--;
    } else {
      fleeVector = null;
    }

    MAX_HORIZONTAL_SPEED = game.getProperties().getUnitMaxHorizontalSpeed();
    if (findGoodWeapon) {
      debug.draw(new CustomData.Log("FIND_GOOD_WEAPON"));
    }
    checkDestinationExists(game);
    boolean swapWeapon = false;
    if (dest != null) {
      double deltaX = dest.getPosition().getX() - unit.getPosition().getX();
      double deltaY = dest.getPosition().getY() - unit.getPosition().getY();
      if (Math.abs(deltaX)<=0.2 && Math.abs(deltaY)<=0.2) {
        System.out.println("AT_DEST");
        tickAtDest++;
        if (findGoodWeapon) {
          System.out.println("BINGO");
          findGoodWeapon = false;
          swapWeapon = true;
          goodWeapon = true;
          dest = null;
          tickAtDest = 0;
        }
        if (tickAtDest > game.getProperties().getTicksPerSecond()*MAX_SECONDS_AT_DEST) {
          System.out.println("<< LONG_WAIT >>");
          tickAtDest = 0;
          dest = null;
        }
      }
    }

    if (findNearWeapon) {
      /* maybe already pickup */
      if (unit.getWeapon() != null) {
        findNearWeapon = false;
        dest = null;
      }
    }

    Unit enemy = null;
    for (Unit other : game.getUnits()) {
      if (other.getPlayerId() != unit.getPlayerId()) {
        enemy = other;
      }
    }

    int userHealthPercent = game.getProperties().getUnitMaxHealth() / 100 * unit.getHealth();
    if (userHealthPercent < MIN_HEALTH_PERCENT) {
      debug.draw(new CustomData.Log("LOW HP! LESS THAN " + MIN_HEALTH_PERCENT));
      findHealthPack(unit, enemy, game);
    } else if (enemy!=null && enemy.getWeapon()!=null) {
      if (enemy.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)) {
        int bulletDamage = game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getBullet().getDamage();
        int explosionDamage = game.getProperties().getWeaponParams().get(WeaponType.ROCKET_LAUNCHER).getExplosion().getDamage();
        int totalMaxDamage = bulletDamage + explosionDamage;
        if (totalMaxDamage >= unit.getHealth()) {
          debug.draw(new CustomData.Log("LOW HP! ROCKET LAUNCHER PANIC!"));
          findHealthPack(unit, enemy, game);
        }
      }
    }

    if (dest==null && unit.getWeapon()==null) {
      findNearWeapon(unit, game);
    }

    if (dest==null && unit.getWeapon()!=null) {
      findGoodWeapon(unit, game);
    }

    Vec2Double targetPos = unit.getPosition();
    if (dest != null) {
      targetPos = dest.getPosition();
    }

    if (dest==null && enemy!=null) {
      targetPos = enemy.getPosition();
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
          int lastX = (int) lastPosition.getX();
          int lastY = (int) lastPosition.getY();
          if (lastTile.equals(Tile.PLATFORM) && currentTile.equals(Tile.EMPTY) && (lastX!=x || lastY!=y)) {
            needStand = isNeedStand(game, x, y);
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
          int lastX = (int) lastPosition.getX();
          int lastY = (int) lastPosition.getY();
          if (lastTile.equals(Tile.PLATFORM) && currentTile.equals(Tile.EMPTY) && (lastX!=x || lastY!=y)) {
            needStand = isNeedStand(game, x, y);
          }
        }
      }
    }

    /* перепрыгнуть противника при движении к цели */
    if (dest!=null && enemy!=null) {
      if (dest.getPosition().getX()<unit.getPosition().getX()
              && isJumpOnEnemy(unit, enemy, true)) {
        jump = true;
      }
      if (dest.getPosition().getX()>unit.getPosition().getX()
              && isJumpOnEnemy(unit, enemy, false)) {
        jump = true;
      }
    }

    boolean enemySpotted = false;
    if (enemy != null) {
      enemySpotted = isEnemySpotted(game, unit, enemy);
    }

    double minDistance = MIN_DISTANCE_TO_ENEMY;
    if (dest==null && unit.getWeapon()!=null) {
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

    if (closeCombat != 0) {
      System.out.println("closeCombat = " + closeCombat);
    }

    if (enemySpotted) {
      closeCombat = 0;
    }

    double velocity;
    velocity = (targetPos.getX()-unit.getPosition().getX()) * game.getProperties().getUnitMaxHorizontalSpeed();
    UnitAction action = new UnitAction();
    if (swapWeapon) {
      velocity = 0;
    } else if (dest==null && enemy!=null) {
      final int RIGHT = 1;
      final int LEFT = -1;
      if (Math.abs(enemy.getPosition().getX() - unit.getPosition().getX()) <= minDistance) {
        if (enemy.getPosition().getY() < unit.getPosition().getY()) {
          if (enemySpotted) {
            closeCombat = 0;
            velocity = 0;
          } else {
            if (closeCombat == 0) {
              if (enemy.getPosition().getX() <= unit.getPosition().getX()) {
                closeCombat = LEFT;
              }
              if (enemy.getPosition().getX() >= unit.getPosition().getX()) {
                closeCombat = RIGHT;
              }
            }
          }
        }
      }
      if (closeCombat == LEFT) {
        velocity = -game.getProperties().getUnitMaxHorizontalSpeed();
      }
      if (closeCombat == RIGHT) {
        velocity = game.getProperties().getUnitMaxHorizontalSpeed();
      }
    }

    /* flee from bullets */
    for (Bullet bullet : game.getBullets()) {
      if (bullet.getPlayerId() != unit.getPlayerId()) {
        double deltaX = bullet.getVelocity().getX()/game.getProperties().getTicksPerSecond();
        double deltaY = bullet.getVelocity().getY()/game.getProperties().getTicksPerSecond();
        double ticksMultiply = 6;
        Vec2Double bulletPosition = new Vec2Double(
                bullet.getPosition().getX()+(deltaX*ticksMultiply),
                bullet.getPosition().getY()+(deltaY*ticksMultiply)
        );
        FleeVector vector = fleeFromBulletsVector(game, unit, velocity, bullet.getPosition(), bulletPosition);
        if (vector != null) {
          fleeVector = vector;
          /* set velocity */
          fleeTicks = FLEE_TICKS;
          break;
        }
      }
    }

    if (fleeTicks>0 && fleeVector!=null) {
      velocity = fleeVector.velocity;
      jump = fleeVector.jump;
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

    if (needStand) {
      if (unit.getJumpState().isCanCancel()) {
        if (tile.equals(Tile.EMPTY) && lastTile.equals(Tile.EMPTY)) {
          jump = false;
        }
      } else {
        needStand = false;
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

    lastTile = tile;
    lastPosition = unit.getPosition();
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

  private boolean isJumpOnEnemy(Unit unit, Unit enemy, boolean leftWalk) {
    int enemyX = (int) enemy.getPosition().getX();
    int x = (int) unit.getPosition().getX();
    if (leftWalk && ((x-1)==enemyX))
      return true;

    if (!leftWalk && (x+1)==enemyX)
      return true;

    return false;
  }

  private void findHealthPack(Unit unit, Unit enemy, Game game) {
    if (dest==null || dest.getItem() instanceof Item.HealthPack) {
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
        dest = nearPack;
        if (unit.getPosition().getX() < enemy.getPosition().getX()) {
          /*find left from enemy*/
          for (LootBox lootBox : sortedMap.keySet()) {
            if (lootBox.getPosition().getX() < enemy.getPosition().getX()) {
              dest = lootBox;
              break;
            }
          }
        }
        if (unit.getPosition().getX() > enemy.getPosition().getX()) {
          /*find right from enemy*/
          for (LootBox lootBox : sortedMap.keySet()) {
            if (lootBox.getPosition().getX() > enemy.getPosition().getX()) {
              dest = lootBox;
              break;
            }
          }
        }
      }
    }
  }

  private void checkDestinationExists(Game game) {
    if (dest!=null) {
      System.out.println("DEST x:" + dest.getPosition().getX() + " y:" + dest.getPosition().getY());
    }
    if (dest != null) {
      boolean exists = false;
      for (LootBox lootBox : game.getLootBoxes()) {
        if (lootBox.getPosition().getX()==dest.getPosition().getX()
                && lootBox.getPosition().getY()==dest.getPosition().getY()) {
          exists = true;
          break;
        }
      }
      if (!exists) {
        dest = null;
      }
    }
  }

  /**
   * Определение видимости противника.
   */
  private boolean isEnemySpotted(Game game, Unit unit, Unit enemy) {
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
      Tile tile = game.getLevel().getTiles()[tileX][tileY];
      if (tile == Tile.WALL) {
        enemySpotted = false;
        break;
      }
    }
    return enemySpotted;
  }

  private void findGoodWeapon(Unit unit, Game game) {
    if (unit.getWeapon().getTyp().equals(PERFECT_WEAPON_TYPE) || goodWeapon)
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
          findGoodWeapon = true;
          dest = lootBox;
          break;
        }
      }
    }
  }

  private void findNearWeapon(Unit unit, Game game) {
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.Weapon) {
        if (dest == null || distanceSqr(unit.getPosition(),
                lootBox.getPosition()) < distanceSqr(unit.getPosition(), dest.getPosition())) {
          System.out.println("NEAR WEAPON " + ((Item.Weapon) lootBox.getItem()).getWeaponType());
          findNearWeapon = true;
          dest = lootBox;
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
                                                  Vec2Double bPosStart, Vec2Double bPosStop) {
    int distance = (int) distanceSqr(bPosStart, bPosStop);
    Vec2Double uPos = unit.getPosition();
    int tilesSize = game.getLevel().getTiles().length;
    int uX = (int) uPos.getX();
    Vec2Double uPosCenter = new Vec2Double(uPos.getX(), uPos.getY()+(unit.getSize().getY()/2d));
    for (int i=0; i<distance; i++) {
      Vec2Double point = pointAtVectorOnDistance(bPosStart, bPosStop, i);
      double x = unit.getPosition().getX()-unit.getSize().getX()/2;
      double y = unit.getPosition().getY();
      if (x<point.getX() && point.getX()<unit.getPosition().getX()+unit.getSize().getX()
              && y<point.getY() && point.getY()<unit.getPosition().getY()+unit.getSize().getY()) {
        System.out.println("<< DANGER >> unitVelocity " + unitVelocity);
        if (bPosStart.getX()<uPos.getX() && bPosStop.getX()>uPos.getX()) {
          System.out.println(">> LEFT_TO_RIGHT >>");
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in legs line");
            /* jump up */
            if (unitVelocity == 0) {
              /* jump up right */
              return new FleeVector(true, (tilesSize-uX) * MAX_HORIZONTAL_SPEED);
            } else return new FleeVector(true, unitVelocity);
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in head line");
            /* lay down */
            if (unit.getJumpState().isCanJump()) {
              if (unitVelocity == 0) {
                /* jump up right */
                return new FleeVector(true, (tilesSize-uX) * MAX_HORIZONTAL_SPEED);
              } else return new FleeVector(true, unitVelocity);
            }
            if (unit.getJumpState().isCanCancel()) {
              /* lay down to left */
              return new FleeVector(false, (-uX) * MAX_HORIZONTAL_SPEED);
            }
            return new FleeVector(false, unitVelocity);
          }
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in legs out head");
            /* jump left */
            return new FleeVector(true, (-uX) * MAX_HORIZONTAL_SPEED);
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in head out legs");
            if (unit.getJumpState().isCanJump()) {
              /* jump right */
              return new FleeVector(true, (tilesSize-uX) * MAX_HORIZONTAL_SPEED);
            } else {
              /* lay down left */
              return new FleeVector(false, (-uX) * MAX_HORIZONTAL_SPEED);
            }
          }
        }
        if (bPosStart.getX()>uPos.getX() && bPosStop.getX()<uPos.getX()) {
          System.out.println("<< RIGHT_TO_LEFT <<");
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in legs line");
            /* jump */
            return new FleeVector(true, unitVelocity);
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in head line");
            /* lay down */
            if (unit.getJumpState().isCanJump()) {
              return new FleeVector(true, unitVelocity);
            }
            if (unit.getJumpState().isCanCancel()) {
              /* lay down to right */
              return new FleeVector(false, (tilesSize-uX) * MAX_HORIZONTAL_SPEED);
            }
            return new FleeVector(false, unitVelocity);
          }
          if (bPosStart.getY()<=uPosCenter.getY() && bPosStop.getY()>=uPosCenter.getY()) {
            System.out.println("hit in legs out head");
            /* jump right */
            return new FleeVector(true, (tilesSize-uX) * MAX_HORIZONTAL_SPEED);
          }
          if (bPosStart.getY()>=uPosCenter.getY() && bPosStop.getY()<=uPosCenter.getY()) {
            System.out.println("hit in head out legs");
            if (unit.getJumpState().isCanJump()) {
              /* jump left */
              return new FleeVector(true, (-uX) * MAX_HORIZONTAL_SPEED);
            } else {
              /* lay down right */
              return new FleeVector(false, (tilesSize-uX) * MAX_HORIZONTAL_SPEED);
            }
          }
        }
        if (bPosStart.getX()<=uPosCenter.getX() && bPosStop.getX()<=uPosCenter.getX()) {
          System.out.println("VERTICAL_LEFT");
          /* jump right */
          return new FleeVector(true, (tilesSize-uX) * MAX_HORIZONTAL_SPEED);
        }
        if (bPosStart.getX()>=uPosCenter.getX() && bPosStop.getX()>=uPosCenter.getX()) {
          System.out.println("VERTICAL_RIGHT");
          /* jump left */
          return new FleeVector(true, (-uX) * MAX_HORIZONTAL_SPEED);
        }
      }
    }
    return null;
  }

  private static class FleeVector {
    public double velocity;
    public boolean jump;
    FleeVector(boolean jump, double velocity) {
      this.jump = jump;
      this.velocity = velocity;
    }
  }
}