import model.*;

import java.util.*;

public class MyStrategy {
  private static int MIN_DISTANCE_TO_ENEMY = 1;
  private static int MIN_HEALTH_PERCENT = 50;
  private boolean findGoodWeapon = false;
  private boolean swapWeapon = false;
  private boolean goodWeapon = false;
  private LootBox dest = null;

  public UnitAction getAction(Unit unit, Game game, Debug debug) {
    swapWeapon = false;
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
      debug.draw(new CustomData.Log("LOW HP"));
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
      aim = new Vec2Double(enemy.getPosition().getX() - unit.getPosition().getX(),
              enemy.getPosition().getY() - unit.getPosition().getY());
    }

    boolean jump = targetPos.getY() > unit.getPosition().getY();
    if (targetPos.getX() > unit.getPosition().getX() && game.getLevel()
        .getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
      jump = true;
    }

    if (targetPos.getX() < unit.getPosition().getX() && game.getLevel()
        .getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
      jump = true;
    }

    boolean enemySpotted = true;
    int tilesToEnemyX = (int) Math.abs(unit.getPosition().getX()-enemy.getPosition().getX());
    if (tilesToEnemyX > 0) {
      /* check blind tiles */
      for (int i=0; i<tilesToEnemyX; i++) {
        Vec2Double point = pointAtVectorOnDistance(unit.getPosition(), enemy.getPosition(), i);
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

    if (enemySpotted) {
      if (!enemy.getJumpState().isCanCancel() && enemy.getJumpState().getSpeed()<=0) {
        double delta = game.getProperties().getUnitFallSpeed()/game.getProperties().getUpdatesPerTick();
        aim.setY(aim.getY()-delta);
      }
    }

    if (enemySpotted && dest==null) {
      for (Bullet bullet : game.getBullets()) {
        if (bullet.getPlayerId() != unit.getPlayerId()) {
          /* jump from bullet */
        }
      }
    }

    UnitAction action = new UnitAction();
    if (swapWeapon) {
      action.setVelocity(0);
    } else {
      action.setVelocity((targetPos.getX() - unit.getPosition().getX()) * game.getProperties().getUnitMaxHorizontalSpeed());
    }
    action.setJump(jump);
    action.setJumpDown(!jump);
    action.setAim(aim);
    action.setShoot(enemySpotted);
    action.setSwapWeapon(swapWeapon);
    action.setPlantMine(false);
    return action;
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
}