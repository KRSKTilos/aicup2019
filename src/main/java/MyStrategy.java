import model.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyStrategy {
  private static int MIN_HEALTH_PERCENT = 50;
  private LootBox dest = null;

  public UnitAction getAction(Unit unit, Game game, Debug debug) {
    /* Лутбокс подобран */
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


    LootBox nearestWeapon = null;
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.Weapon) {
        if (nearestWeapon == null || distanceSqr(unit.getPosition(),
            lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition())) {
          nearestWeapon = lootBox;
        }
      }
    }

    Vec2Double targetPos = unit.getPosition();
    if (dest != null) {
      targetPos = dest.getPosition();
    }

    if (dest == null) {
      if (unit.getWeapon() == null && nearestWeapon != null) {
        targetPos = nearestWeapon.getPosition();
      } else if (enemy != null) {
        targetPos = enemy.getPosition();
      }
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

    UnitAction action = new UnitAction();
    action.setVelocity(targetPos.getX() - unit.getPosition().getX());
    action.setJump(jump);
    action.setJumpDown(!jump);
    action.setAim(aim);
    action.setShoot(true);
    action.setSwapWeapon(false);
    action.setPlantMine(false);
    return action;
  }

  static double distanceSqr(Vec2Double a, Vec2Double b) {
    return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
  }
}