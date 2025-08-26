package noarmor;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Minecraft plugin that penalizes players for wearing armor by draining their health.
 * Players take damage over time when wearing any armor pieces.
 */
public class NoArmorPlugin extends JavaPlugin implements Listener {

  private static final double DAMAGE_PER_SECOND = 4.0;
  private static final String ARMOR_DRAIN_MESSAGE = "§cArmor is draining your health!";
  private static final String ARMOR_SLOT_PATTERN = ".*(?:CHEST|LEGS|FEET|HEAD).*";
  private static final String ARMOR_TYPE_PATTERN = ".*(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS|TURTLE_SHELL)$";

  private final Map<UUID, BukkitTask> damageTasks = new ConcurrentHashMap<>();

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    getLogger().info("NoArmor plugin has been enabled!");
  }

  @Override
  public void onDisable() {
    damageTasks.values().forEach(BukkitTask::cancel);
    damageTasks.clear();
    getLogger().info("NoArmor plugin has been disabled!");
  }

  @EventHandler
  public void onEquipmentChange(EntityEquipmentChangedEvent event) {
    if (!(event.getEntity() instanceof Player player))
      return;

    UUID playerId = player.getUniqueId();
    boolean hasArmor = isWearingArmor(player);

    if (hasArmorSlotChanges(event)) {
      if (hasArmor && !damageTasks.containsKey(playerId)) {
        damageTasks.put(playerId, Bukkit.getScheduler().runTaskTimer(this, () -> {
          player.damage(DAMAGE_PER_SECOND);
          player.sendActionBar(Component.text(ARMOR_DRAIN_MESSAGE));
        }, 20L, 20L));
      } else if (!hasArmor) {
        var task = damageTasks.remove(playerId);
        if (task != null)
          task.cancel();
      }
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    var task = damageTasks.remove(event.getPlayer().getUniqueId());
    if (task != null)
      task.cancel();
  }

  /**
   * Checks if the equipment change event involves armor slots.
   */
  private boolean hasArmorSlotChanges(EntityEquipmentChangedEvent event) {
    return event.getEquipmentChanges().keySet().stream()
        .anyMatch(slot -> slot.name().matches(ARMOR_SLOT_PATTERN));
  }

  /**
   * Checks if a player is currently wearing any armor pieces.
   */
  private boolean isWearingArmor(Player player) {
    return Arrays.stream(player.getInventory().getArmorContents())
        .anyMatch(armor -> armor != null && !armor.getType().isAir() &&
            armor.getType().name().matches(ARMOR_TYPE_PATTERN));
  }
}