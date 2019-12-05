import model.*;

import java.util.*;

public class MyStrategy {
  private static int MIN_DISTANCE_TO_ENEMY = 1;
  private static int MIN_HEALTH_PERCENT = 45;
  private boolean findGoodWeapon = false;
  private boolean goodWeapon = false;
  private LootBox dest = null;
  private Vec2Double lastPosition = new Vec2Double(0,0);
  private Tile lastTile = Tile.EMPTY;
  private boolean needStand = false;

  public UnitAction getAction(Unit unit, Game game, Debug debug) {

    boolean swapWeapon = false;
    /* loot box pickup */
    if (dest != null) {
      if (findGoodWeapon) {
        double deltaX = dest.getPosition().getX() - unit.getPosition().getX();
        double deltaY = dest.getPosition().getY() - unit.getPosition().getY();
        /* todo: boxsize */
        if (Math.abs(deltaX) <= 0.5 && Math.abs(deltaY) <= 0.5) {
          debug.draw(new CustomData.Log("BINGO! Normal Weapon!"));
          findGoodWeapon = false;
          swapWeapon = true;
          dest = null;
          goodWeapon = true;
        }
      } else {
        boolean contains = false;
        for (LootBox lootBox : game.getLootBoxes()) {
          if (lootBox.equals(dest)) {
            contains = true;
            break;
          }
        }
        if (!contains) {
          dest = null;
        }
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
      findHealthPack(unit, game);
    } else if (enemy!=null && enemy.getWeapon()!=null) {
      if (enemy.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)
              && unit.getHealth() <= game.getProperties().getWeaponParams()
              .get(WeaponType.ROCKET_LAUNCHER).getExplosion().getDamage()) {
        debug.draw(new CustomData.Log("LOW HP! ROCKET LAUNCHER PANIC!"));
        findHealthPack(unit, game);
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
        jump = true;
        int lastX = (int) lastPosition.getX();
        int lastY = (int) lastPosition.getY();
        if (lastTile.equals(Tile.PLATFORM) && currentTile.equals(Tile.EMPTY) && (lastX!=x || lastY!=y)) {
          needStand = true;
        }
      }
    }

    if (targetPos.getX() <= unit.getPosition().getX()) {
      int x = (int) unit.getPosition().getX();
      int y = (int) unit.getPosition().getY();
      Tile leftTile = game.getLevel().getTiles()[x-1][y];
      Tile currentTile = game.getLevel().getTiles()[x][y];
      if (leftTile.equals(Tile.WALL)) {
        jump = true;
        int lastX = (int) lastPosition.getX();
        int lastY = (int) lastPosition.getY();
        if (lastTile.equals(Tile.PLATFORM) && currentTile.equals(Tile.EMPTY) && (lastX!=x || lastY!=y)) {
          needStand = true;
        }
      }
    }

    boolean enemySpotted = true;
    int tilesToEnemyX = (int) Math.abs(unit.getPosition().getX()-enemy.getPosition().getX());
    if (tilesToEnemyX > 0) {
      /* check blind tiles */
      for (int tileIndex=0; tileIndex<tilesToEnemyX; tileIndex++) {
        Vec2Double point = pointAtVectorOnDistance(unit.getPosition(), enemy.getPosition(), tileIndex);
        Tile tile = game.getLevel().getTiles()[(int) point.getX()][(int) point.getY()];
        if (tile.equals(Tile.WALL)) {
          enemySpotted = false;
          break;
        }
      }
    }

    if (dest==null && unit.getWeapon()!=null) {
      /* safe distance */
      double minDistance = MIN_DISTANCE_TO_ENEMY;
      if (unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)) {
        minDistance = unit.getWeapon().getParams().getExplosion().getRadius()*2;
      }
      if (unit.getPosition().getX() > targetPos.getX()) {
        targetPos.setX(targetPos.getX()+minDistance);
      } else {
        targetPos.setX(targetPos.getX()-minDistance);
      }
    }

    UnitAction action = new UnitAction();
    if (swapWeapon) {
      action.setVelocity(0);
    } else {
      action.setVelocity((targetPos.getX() - unit.getPosition().getX()) * game.getProperties().getUnitMaxHorizontalSpeed());
    }

    if (dest==null) {
      /* flee from bullets */
      for (Bullet bullet : game.getBullets()) {
        /*
        if (bullet.getPlayerId() != unit.getPlayerId()) {
          System.out.println("TICK " + game.getCurrentTick());
          System.out.println("TPS " + game.getProperties().getTicksPerSecond());
          System.out.println("BULLET   X:" + bullet.getPosition().getX() + " Y:" + bullet.getPosition().getY());
          System.out.println("VELOCITY X:" + bullet.getVelocity().getX() + " Y:" + bullet.getVelocity().getY());
          double deltaX = bullet.getVelocity().getX()/game.getProperties().getTicksPerSecond();
          double deltaY = bullet.getVelocity().getY()/game.getProperties().getTicksPerSecond();
          double ticksMultiply = 6;
          Vec2Double bulletPosition = new Vec2Double(
                  bullet.getPosition().getX()+(deltaX*ticksMultiply),
                  bullet.getPosition().getY()+(deltaY*ticksMultiply)
          );

          System.out.println("BULLET_POSITION X:" + bulletPosition.getX() + " Y:" + bullet.getPosition().getY());
          int hitStrategy = unitHit(unit, bullet.getPosition(), bulletPosition);
          switch (hitStrategy) {
            case 1:
              fleeJump = (int) (game.getProperties().getTicksPerSecond()/2);
              break;
            case 2:
              break;
            case 3:
              fleeJump = 0;
              break;
            case 4:
            default:
          }
          System.out.println("-----");
        }
      */
      }
    }

    boolean reload = unit.getWeapon()!=null && unit.getWeapon().getMagazine()==0;

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

  private void findHealthPack(Unit unit, Game game) {
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
      }
    }
  }

  private void findGoodWeapon(Unit unit, Game game) {
    if (goodWeapon)
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
    if (weaponTypes.contains(WeaponType.ASSAULT_RIFLE)) {
      findType = WeaponType.ASSAULT_RIFLE;
    } else if (weaponTypes.contains(WeaponType.PISTOL)) {
      findType = WeaponType.PISTOL;
    }

    if (findType != null) {
      Iterator<Map.Entry<LootBox, Double>> lootBoxIterator = sortedMap.entrySet().iterator();
      while (lootBoxIterator.hasNext()) {
        LootBox lootBox = lootBoxIterator.next().getKey();
        Item.Weapon weapon = (Item.Weapon) lootBox.getItem();
        if (weapon.getWeaponType().equals(findType)) {
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

  /**
   * Направление уворота.
   * 0 - ignore
   * 1 - up
   * 2 - right
   * 3 - down
   * 4 - left
   * @param unit
   * @param start
   * @param stop
   * @return
   */
  static int unitHit(Unit unit, Vec2Double start, Vec2Double stop) {
    final int HOLD = 0;
    final int UP = 1;
    final int RIGHT = 2;
    final int DOWN = 3;
    final int LEFT = 4;
    int result = HOLD;
    int distance = (int) distanceSqr(start, stop);
    System.out.println("distance " + distance);
    for (int i=0; i<distance; i++) {
      Vec2Double point = pointAtVectorOnDistance(start, stop, i);
      double x = unit.getPosition().getX()-unit.getSize().getX()/2;
      double y = unit.getPosition().getY();
      if (x<point.getX() && point.getX()<unit.getPosition().getX()+unit.getSize().getX()
              && y<point.getY() && point.getY()<unit.getPosition().getY()+unit.getSize().getY()) {
        System.out.println("<< DANGER >>");
        boolean head = point.getY() >= unit.getPosition().getY()+unit.getSize().getY()/2;
        if (head) {
          System.out.println("<HEAD>");
        } else {
          System.out.println("<LEG>");
          return UP;
        }
      }
    }
    return result;
  }
}