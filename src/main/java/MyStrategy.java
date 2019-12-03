import model.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyStrategy {
  private static int MIN_DISTANCE_TO_ENEMY = 2;
  private static int USER_VELOCITY_MULTIPLY = 3;
  private static int MIN_HEALTH_PERCENT = 50;
  private LootBox dest = null;

  public UnitAction getAction(Unit unit, Game game, Debug debug) {
    /* loot box pickup */
    if (dest != null) {
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

    Unit enemy = null;
    for (Unit other : game.getUnits()) {
      if (other.getPlayerId() != unit.getPlayerId()) {
        enemy = other;
      }
    }

    int userHealthPercent = game.getProperties().getUnitMaxHealth() / 100 * unit.getHealth();
    if (userHealthPercent < MIN_HEALTH_PERCENT) {
      debug.draw(new CustomData.Log("LOH HP"));
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
      /* find weapon */
      for (LootBox lootBox : game.getLootBoxes()) {
        if (lootBox.getItem() instanceof Item.Weapon) {
          if (dest == null || distanceSqr(unit.getPosition(),
                  lootBox.getPosition()) < distanceSqr(unit.getPosition(), dest.getPosition())) {
            dest = lootBox;
          }
        }
      }
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
        if (tile.equals(Tile.WALL) || tile.equals(Tile.PLATFORM)) {
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
    action.setVelocity((targetPos.getX() - unit.getPosition().getX())*USER_VELOCITY_MULTIPLY);
    action.setJump(jump);
    action.setJumpDown(!jump);
    action.setAim(aim);
    action.setShoot(enemySpotted);
    action.setSwapWeapon(false);
    action.setPlantMine(false);
    return action;
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