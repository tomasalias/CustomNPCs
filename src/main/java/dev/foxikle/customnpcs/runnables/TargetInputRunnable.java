package dev.foxikle.customnpcs.runnables;

import dev.foxikle.customnpcs.CustomNPCs;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * The runnable for title text collection
 */
public class TargetInputRunnable extends BukkitRunnable {

    /**
     * The player to send the title to
     */
    private final Player player;

    /**
     * The main class instance
     */
    private final CustomNPCs plugin;

    /**
     * <p> Creates a runnable for collecting text input for the target of a conditional
     * </p>
     * @param plugin The instance to get who's waiting for the title
     * @param player The player to display the title to
     */
    public TargetInputRunnable(Player player, CustomNPCs plugin){
        this.player = player;
        this.plugin = plugin;
    }

    /**
     * <p> Repeatedly sends a title to the player with instructions for entering text
     * </p>
     */
    @Override
    public void run() {
        if(!plugin.targetWaiting.contains(player))
            this.cancel();
        player.sendTitle(ChatColor.GOLD + "Type target value in chat", "", 0, 20, 0);
    }
}
