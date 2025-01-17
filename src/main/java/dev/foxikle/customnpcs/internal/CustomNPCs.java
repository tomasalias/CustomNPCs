package dev.foxikle.customnpcs.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.foxikle.customnpcs.api.Action;
import dev.foxikle.customnpcs.api.conditions.Conditional;
import dev.foxikle.customnpcs.api.conditions.ConditionalTypeAdapter;
import dev.foxikle.customnpcs.internal.commands.CommandCore;
import dev.foxikle.customnpcs.internal.commands.NPCActionCommand;
import dev.foxikle.customnpcs.internal.listeners.Listeners;
import dev.foxikle.customnpcs.internal.listeners.NPCMenuListeners;
import dev.foxikle.customnpcs.internal.menu.MenuCore;
import dev.foxikle.customnpcs.internal.menu.MenuUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * <p> The class that represents the plugin
 * </p>
 */
public final class CustomNPCs extends JavaPlugin implements PluginMessageListener {
    /**
     * The List of inventories that make up the skin selection menus
     */
    public List<Inventory> catalogueInventories;

    /**
     * The List of players the plugin is waiting for title text input
     */
    public List<Player> titleWaiting = new ArrayList<>();

    /**
     * The List of players the plugin is waiting for title text input
     */
    public List<Player> targetWaiting = new ArrayList<>();

    /**
     * The List of players the plugin is waiting for server name text input
     */
    public List<Player> serverWaiting = new ArrayList<>();

    /**
     * The List of players the plugin is waiting for action bar text input
     */
    public List<Player> actionbarWaiting = new ArrayList<>();

    /**
     * The List of players the plugin is waiting for message text input
     */
    public List<Player> messageWaiting = new ArrayList<>();

    /**
     * The List of players the plugin is waiting for command command input
     */
    public List<Player> commandWaiting = new ArrayList<>();

    /**
     * The List of players the plugin is waiting for name text input
     */
    public List<Player> nameWaiting = new ArrayList<>();

    /**
     * The List of players the plugin is waiting for sound text input
     */
    public List<Player> soundWaiting = new ArrayList<>();

    /**
     * The List of NPC holograms
     */
    public List<TextDisplay> holograms = new ArrayList<>();

    /**
     * The Singleton of the FileManager class
     */
    public FileManager fileManager;

    /**
     * The Map of the pages players are on. Keyed by player.
     */
    public Map<Player, Integer> pages = new HashMap<>();

    /**
     * The Map of NPCs keyed by their UUIDs
     */
    public Map<UUID, InternalNpc> npcs = new HashMap<>();

    /**
     * The Map of player's MenuCores
     */
    public Map<Player, MenuCore> menuCores = new HashMap<>();

    /**
     * The Map of the action a player is editing
     */
    public Map<Player, Action> editingActions = new HashMap<>();

    /**
     * The Map of the original actions a player is editing
     */
    public Map<Player, String> originalEditingActions = new HashMap<>();

    /**
     * The Map of the action a player is editing
     */
    public Map<Player, Conditional> editingConditionals = new HashMap<>();

    /**
     * The Map of the original actions a player is editing
     */
    public Map<Player, String> originalEditingConditionals = new HashMap<>();

    /**
     * Singleton for the NPCBuilder
     */
    private static CustomNPCs instance;

    /**
     * Singleton for menu utilites
     */
    private MenuUtils mu;

    /**
     * Singleton for automatic updates
     */
    private AutoUpdater updater;

    /**
     * If the plugin should try to format messages with PlaceholderAPI
     */
    public boolean papi = false;

    /**
     * The plugin's json handler
     */
    private static Gson gson;

    /**
     * If there is a new update available
     */
    public boolean update;

    /**
     * The plugin's MiniMessage instance
     */
    public MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * <p> Logic for when the plugin is enabled
     * </p>
     */
    @Override
    public void onEnable() {
        instance = this;
        if (!setup()) {
            Bukkit.getLogger().severe("Incompatible server version! Please use 1.20.2! Shutting down plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        try {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("npc");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setPrefix(ChatColor.DARK_GRAY + "[NPC] ");
        } catch (IllegalArgumentException ignored) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("npc");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setPrefix(ChatColor.DARK_GRAY + "[NPC] ");
        }

        this.getServer().getPluginManager().registerEvents(new NPCMenuListeners(this), this);
        this.getServer().getPluginManager().registerEvents(new Listeners(this), this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        gson = new GsonBuilder()
                .registerTypeAdapter(Conditional.class, new ConditionalTypeAdapter())
                .create();
        this.fileManager = new FileManager(this);
        this.mu = new MenuUtils(this);
        this.updater = new AutoUpdater(this);
        update = updater.checkForUpdates();
        if (fileManager.createFiles()) {
            getCommand("npc").setExecutor(new CommandCore(this));
            getCommand("npcaction").setExecutor(new NPCActionCommand(this));
            this.getLogger().info("Loading NPCs!");
            for (UUID uuid : fileManager.getNPCIds()) {
                fileManager.loadNPC(uuid);
            }
            Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getOnlinePlayers().forEach(player -> npcs.values().forEach(npc -> Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> npc.injectPlayer(player), 5))), 20);
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> catalogueInventories = this.getMenuUtils().getCatalogueInventories(), 20);
            // setup bstats
            Metrics metrics = new Metrics(this, 18898);

            // setup service manager for the API
            Bukkit.getServer().getServicesManager().register(CustomNPCs.class, this, this, ServicePriority.Normal);

            // setup papi
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                this.getLogger().info("Successfully hooked into PlaceholderAPI.");
                papi = true;
            } else {
                papi = false;
                this.getLogger().warning("Could not find PlaceholderAPI! PlaceholderAPI isn't required, but CustomNPCs does support it.");
            }
        }

    }

    /**
     * <p> Checks if the plugin is compatable with the server version
     * </p>
     *
     * @return If the plugin is compatable with the server
     */

    public boolean setup() {
        return (Bukkit.getServer().getMinecraftVersion().equals("1.20.2"));
    }

    /**
     * <p> Logic for when the plugin is disabled
     * </p>
     */
    @Override
    public void onDisable() {
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getServicesManager().unregister(this);
        try {
            Bukkit.getScoreboardManager().getMainScoreboard().getTeam("npc").unregister();
        } catch (IllegalArgumentException | NullPointerException ignored) {
        }
        for (InternalNpc npc : npcs.values()) {
            npc.remove();
        }
    }

    /**
     * <p> Gets list of current NPCs
     * </p>
     *
     * @return the list of current NPCs
     */
    public List<InternalNpc> getNPCs() {
        return npcs.values().stream().toList();
    }

    /**
     * <p> Adds an NPC to the list of current NPCs
     * </p>
     *
     * @param npc      The NPC to add
     * @param hologram the TextDisplay representing the NPC's name
     */
    public void addNPC(InternalNpc npc, TextDisplay hologram) {
        holograms.add(hologram);
        npcs.put(npc.getUUID(), npc);
    }

    /**
     * <p> Gets the FileManager
     * </p>
     *
     * @return the file manager object
     */
    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * <p> Gets the page the player is in.
     * </p>
     *
     * @param p The player to get the page of
     * @return the current page in the Skin browser the player is in
     */
    public int getPage(Player p) {
        return pages.get(p);
    }

    /**
     * <p> Sets the page the player is in. Does not actually set the player's open inventory.
     * </p>
     *
     * @param p    The player to ser the page of
     * @param page The page number to set.
     */
    public void setPage(Player p, int page) {
        pages.put(p, page);
    }

    /**
     * <p> Gets the delay of an action
     * </p>
     *
     * @param uuid The UUID of the npc
     * @return the NPC of the specified UUID
     * @throws NullPointerException     if the specified UUID is null
     * @throws IllegalArgumentException if an NPC with the specified UUID does not exist
     */
    public InternalNpc getNPCByID(UUID uuid) {
        if (uuid == null) throw new NullPointerException("uuid cannot be null");
        if (!npcs.containsKey(uuid))
            throw new IllegalArgumentException("An NPC with the uuid '" + uuid + "' does not exist");
        return npcs.get(uuid);
    }

    /**
     * <p> Gets the MenuUtils object
     * </p>
     *
     * @return the MenuUtils object
     */
    public MenuUtils getMenuUtils() {
        return mu;
    }

    /**
     * <p> Gets the Gson object
     * </p>
     *
     * @return the Gson object
     */
    public static Gson getGson() {
        return gson;
    }

    /**
     * <p> Doesn't do anything since this plugin is not expecting to receive any plugin messages. It exists soley to be able to send a player to a bungeecord server.
     * </p>
     */
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
    }

    /**
     * <p> Gets the plugin's minimessage parser.
     * <p>
     *
     * @return the plugin's minimessage instance
     */
    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}
