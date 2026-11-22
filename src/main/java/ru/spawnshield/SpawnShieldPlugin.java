package ru.spawnshield;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class SpawnShieldPlugin extends JavaPlugin {

    static final String BYPASS = "spawnshield.bypass";
    static final MiniMessage MM = MiniMessage.miniMessage();

    private volatile Set<World> protectedWorlds = Set.of();
    private final Map<UUID, TeleportSession> sessions = new ConcurrentHashMap<>();

    private File spawnFile;
    private volatile Location spawn;
    private volatile double radius;
    private volatile int warmup;
    private volatile double cancelDistance;
    private volatile boolean blindness;
    private volatile boolean effects;
    private volatile boolean countdownActionBar;
    private volatile boolean deathTitles;
    private volatile boolean voidEnabled;
    private volatile int voidOffset;
    private volatile int voidWarmup;
    private volatile boolean allowContainers;
    private volatile boolean disableWeather;
    private volatile boolean blockHunger;
    private volatile boolean blockMobSpawns;
    private volatile boolean blockEntityDamage;
    private volatile boolean editMode;
    private volatile String prefix;

    @Override
    public void onEnable() {
        this.spawnFile = new File(getDataFolder(), "spawn.yml");
        saveDefaultConfig();
        disableLegacyAuraSpawn();
        reloadAll();

        getServer().getPluginManager().registerEvents(new SpawnListener(this), this);
        PluginCommand command = getCommand("spawn");
        if (command != null) {
            SpawnCommand executor = new SpawnCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        applyWorldSettings();
        getServer().getScheduler().runTaskLater(this, this::refreshWorldState, 40L);
        getLogger().info("SpawnShield enabled, protecting " + protectedWorlds.size()
                + " world(s), spawn at " + describe(spawn));
    }

    /**
     * MultiWorld loads its worlds after SpawnShield on a cold start, so resolve the
     * protected worlds and spawn again as soon as a world becomes available.
     */
    public void handleWorldLoad(World world) {
        if (world == null) {
            return;
        }
        refreshWorldState();
    }

    private void refreshWorldState() {
        Location previousSpawn = this.spawn;
        resolveProtectedWorlds();
        loadSpawn();
        applyWorldSettings();
        if (previousSpawn == null && spawn != null) {
            getLogger().info("Spawn world ready: protecting " + protectedWorlds.size()
                    + " world(s), spawn at " + describe(spawn));
        }
    }

    @Override
    public void onDisable() {
        List<Player> players = new ArrayList<>();
        for (TeleportSession session : sessions.values()) {
            if (session.task != null) {
                session.task.cancel();
            }
            Player player = Bukkit.getPlayer(session.playerId);
            if (player != null) {
                players.add(player);
            }
        }
        sessions.clear();
        if (blindness) {
            for (Player player : players) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }
    }

    /**
     * The old jar may still be present on a live server where it cannot be deleted.
     * Disabling it makes sure only SpawnShield owns /spawn and the protection events.
     */
    private void disableLegacyAuraSpawn() {
        Plugin legacy = getServer().getPluginManager().getPlugin("AuraSpawn");
        if (legacy == null || legacy == this) {
            return;
        }
        getServer().getPluginManager().disablePlugin(legacy);
        getLogger().warning("Legacy AuraSpawn was found and disabled. "
                + "Delete plugins/AuraSpawn-*.jar and the old AuraSpawn folder.");
    }

    public void reloadAll() {
        reloadConfig();

        this.radius = Math.max(0.0D, getConfig().getDouble("spawn-radius", 96.0D));
        this.warmup = clamp(getConfig().getInt("teleport.warmup-seconds", 3), 0, 60);
        this.cancelDistance = Math.max(0.05D, getConfig().getDouble("teleport.cancel-distance", 0.35D));
        this.blindness = getConfig().getBoolean("teleport.blindness", true);
        this.effects = getConfig().getBoolean("teleport.effects", true);
        this.countdownActionBar = getConfig().getBoolean("teleport.countdown-actionbar", true);
        this.deathTitles = getConfig().getBoolean("death.enabled", true);
        this.voidEnabled = getConfig().getBoolean("void.enabled", true);
        this.voidOffset = clamp(getConfig().getInt("void.offset", 8), 1, 256);
        this.voidWarmup = clamp(getConfig().getInt("void.warmup-seconds", 0), 0, 10);
        this.allowContainers = getConfig().getBoolean("protection.allow-containers",
                getConfig().getBoolean("protection.allow-chests", true));
        this.disableWeather = getConfig().getBoolean("protection.disable-weather", true);
        this.blockHunger = getConfig().getBoolean("protection.block-hunger", true);
        this.blockMobSpawns = getConfig().getBoolean("protection.block-mob-spawns", true);
        this.blockEntityDamage = getConfig().getBoolean("protection.block-entity-damage", true);
        this.prefix = getConfig().getString("prefix", "");
        this.editMode = false;

        resolveProtectedWorlds();
        loadSpawn();
        applyWorldSettings();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void resolveProtectedWorlds() {
        Set<World> resolved = ConcurrentHashMap.newKeySet();
        List<String> configured = getConfig().getStringList("protected-worlds");
        if (!configured.isEmpty()) {
            for (String name : configured) {
                World world = findWorld(name);
                if (world != null) {
                    resolved.add(world);
                } else {
                    getLogger().warning("Unknown protected world: " + name);
                }
            }
            this.protectedWorlds = Set.copyOf(resolved);
            return;
        }
        World fallback = findSpawnWorld();
        if (fallback != null) {
            resolved.add(fallback);
        }
        this.protectedWorlds = Set.copyOf(resolved);
    }

    static World findWorld(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        World exact = Bukkit.getWorld(name);
        if (exact != null) {
            return exact;
        }
        String bare = name;
        int colon = name.indexOf(':');
        if (colon >= 0) {
            bare = name.substring(colon + 1);
            NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
            if (key != null) {
                World byKey = Bukkit.getWorld(key);
                if (byKey != null) {
                    return byKey;
                }
            }
        }
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(name)
                    || world.getName().equalsIgnoreCase(bare)
                    || world.getKey().toString().equalsIgnoreCase(name)
                    || world.getKey().getKey().equalsIgnoreCase(bare)) {
                return world;
            }
        }
        return null;
    }

    static World findSpawnWorld() {
        for (World world : Bukkit.getWorlds()) {
            String name = world.getName().toLowerCase(Locale.ROOT);
            if (name.equals("spawn") || name.endsWith(":spawn") || name.endsWith("_spawn")) {
                return world;
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
    }

    private void loadSpawn() {
        Location loaded = null;
        if (spawnFile != null && spawnFile.isFile()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(spawnFile);
            String worldName = data.getString("world", "");
            World world = findWorld(worldName);
            if (world == null && !worldName.isBlank()) {
                this.spawn = null;
                getLogger().info("Waiting for spawn world '" + worldName + "' to load...");
                return;
            }
            if (world != null) {
                loaded = new Location(
                        world,
                        data.getDouble("x"),
                        data.getDouble("y"),
                        data.getDouble("z"),
                        (float) data.getDouble("yaw"),
                        (float) data.getDouble("pitch")
                );
            }
        }
        if (loaded == null) {
            World world = protectedWorlds.isEmpty() ? findSpawnWorld() : protectedWorlds.iterator().next();
            if (world == null) {
                this.spawn = null;
                getLogger().warning("Spawn world not found. Use /spawn set after loading a world.");
                return;
            }
            loaded = world.getSpawnLocation().clone();
            loaded.setPitch(0.0F);
        }
        loaded.setX(loaded.getBlockX() + 0.5D);
        loaded.setZ(loaded.getBlockZ() + 0.5D);
        this.spawn = loaded;
    }

    public void saveSpawn(Location location) {
        Location exact = location.clone();
        exact.setX(location.getBlockX() + 0.5D);
        exact.setZ(location.getBlockZ() + 0.5D);
        exact.setPitch(Math.max(-90.0F, Math.min(90.0F, exact.getPitch())));
        this.spawn = exact;

        World world = exact.getWorld();
        if (world != null) {
            world.setSpawnLocation(exact);
        }

        YamlConfiguration data = new YamlConfiguration();
        data.set("world", world == null ? "" : world.getName());
        data.set("x", exact.getX());
        data.set("y", exact.getY());
        data.set("z", exact.getZ());
        data.set("yaw", exact.getYaw());
        data.set("pitch", exact.getPitch());
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().warning("Could not create plugin data folder");
            }
            data.save(spawnFile);
        } catch (IOException exception) {
            getLogger().warning("Could not save spawn.yml: " + exception.getMessage());
        }
    }

    private void applyWorldSettings() {
        if (!disableWeather) {
            return;
        }
        for (World world : protectedWorlds) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
        }
    }

    private static String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown world";
        }
        return location.getWorld().getName() + " " + location.getBlockX() + " "
                + location.getBlockY() + " " + location.getBlockZ();
    }

    public Location spawn() {
        return spawn;
    }

    public boolean hasSpawn() {
        return spawn != null && spawn.getWorld() != null;
    }

    public boolean inSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (protectedWorlds.contains(location.getWorld())) {
            return true;
        }
        if (!hasSpawn() || radius <= 0.0D || !location.getWorld().equals(spawn.getWorld())) {
            return false;
        }
        double dx = location.getX() - spawn.getX();
        double dz = location.getZ() - spawn.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    public boolean isProtectedWorld(World world) {
        return world != null && protectedWorlds.contains(world);
    }

    public boolean bypass(Player player) {
        return player != null && (player.isOp() || player.hasPermission(BYPASS));
    }

    public boolean editMode() {
        return editMode;
    }

    public void setEditMode(boolean value) {
        this.editMode = value;
    }

    public boolean canModify(Player player) {
        return editMode && bypass(player);
    }

    public double cancelDistance() {
        return cancelDistance;
    }

    public int warmup() {
        return warmup;
    }

    public int voidWarmup() {
        return voidWarmup;
    }

    public boolean blindness() {
        return blindness;
    }

    public boolean effects() {
        return effects;
    }

    public boolean deathTitles() {
        return deathTitles;
    }

    public boolean voidEnabled() {
        return voidEnabled;
    }

    public int voidOffset() {
        return voidOffset;
    }

    public boolean allowContainers() {
        return allowContainers;
    }

    public boolean blockHunger() {
        return blockHunger;
    }

    public boolean blockMobSpawns() {
        return blockMobSpawns;
    }

    public boolean blockEntityDamage() {
        return blockEntityDamage;
    }

    public String text(String path, String fallback) {
        return getConfig().getString(path, fallback);
    }

    public void send(Player player, String text) {
        player.sendMessage(MM.deserialize(prefix + text));
    }

    public void actionBar(Player player, String text) {
        player.sendActionBar(MM.deserialize(text));
    }

    public void title(Player player, String title, String subtitle, Title.Times times) {
        player.showTitle(Title.title(
                MM.deserialize(title),
                MM.deserialize(subtitle),
                times
        ));
    }

    public static Component component(String text) {
        return MM.deserialize(text);
    }

    public boolean isTeleporting(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public boolean startTeleport(Player player, boolean fromVoid) {
        if (!hasSpawn()) {
            send(player, text("messages.spawn-not-found", "<#FF6B6B>Спавн не найден.</#FF6B6B>"));
            return false;
        }
        cancelTeleport(player, null);

        int seconds = fromVoid ? voidWarmup : warmup;
        TeleportSession session = new TeleportSession(
                player.getUniqueId(),
                player.getLocation().clone(),
                System.currentTimeMillis() + seconds * 1000L,
                fromVoid
        );
        sessions.put(session.playerId, session);

        if (blindness) {
            int duration = Math.max(40, seconds * 20 + 60);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS, duration, 0, false, false, false
            ));
        }

        if (fromVoid) {
            title(player,
                    text("void.title", "<gradient:#FF6B6B:#FFB86B>БЕЗДНА</gradient>"),
                    text("void.subtitle", "<gray>Возвращаем на спавн…</gray>"),
                    Title.Times.times(
                            Duration.ofMillis(100L),
                            Duration.ofMillis(Math.max(500, seconds * 1000L)),
                            Duration.ofMillis(200L)
                    ));
            actionBar(player, text("void.actionbar",
                    "<#FF6B6B>Бездна</#FF6B6B> <dark_gray>→</dark_gray> <#7DE3F4>спавн</#7DE3F4>"));
        } else {
            title(player,
                    text("teleport.title", "<gradient:#7DE3F4:#B388FF>НЕ ДВИГАЙТЕСЬ</gradient>"),
                    text("teleport.subtitle",
                            "<gray>Идёт телепортация</gray> <dark_gray>•</dark_gray> <#B388FF>приготовьтесь</#B388FF>"),
                    Title.Times.times(
                            Duration.ofMillis(150L),
                            Duration.ofMillis(Math.max(700, seconds * 1000L)),
                            Duration.ofMillis(250L)
                    ));
            actionBar(player, text("teleport.countdown",
                    "<#7DE3F4>Телепорт</#7DE3F4> <dark_gray>•</dark_gray> <gray>через</gray> <white>%time%</white><gray>с</gray>")
                    .replace("%time%", String.valueOf(seconds)));
        }

        if (effects) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.4F);
        }

        if (seconds <= 0) {
            begin(session);
        } else {
            session.lastSecond = seconds;
            session.task = player.getScheduler().runAtFixedRate(
                    this,
                    task -> tick(session),
                    () -> {},
                    2L,
                    5L
            );
        }
        return true;
    }

    private void tick(TeleportSession session) {
        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null || !player.isOnline()) {
            sessions.remove(session.playerId);
            return;
        }

        if (!session.fromVoid) {
            Location current = player.getLocation();
            if (current.getWorld() != session.origin.getWorld()
                    || current.distanceSquared(session.origin) > cancelDistance * cancelDistance) {
                cancelTeleport(player, text("teleport.moved",
                        "<#FF6B6B>× Телепорт отменён</#FF6B6B> <dark_gray>•</dark_gray> <gray>вы сдвинулись с места</gray>"));
                return;
            }
        }

        long remaining = session.finishAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            begin(session);
            return;
        }

        int seconds = (int) Math.ceil(remaining / 1000.0D);
        if (countdownActionBar) {
            actionBar(player, text("teleport.countdown",
                    "<#7DE3F4>Телепорт</#7DE3F4> <dark_gray>•</dark_gray> <gray>через</gray> <white>%time%</white><gray>с</gray>")
                    .replace("%time%", String.valueOf(seconds)));
        }
        if (effects && seconds != session.lastSecond) {
            session.lastSecond = seconds;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                    0.7F, 1.2F + (float) Math.min(0.6D, (warmup - seconds) * 0.15D));
        }
    }

    private void begin(TeleportSession session) {
        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null || !player.isOnline() || !hasSpawn()) {
            sessions.remove(session.playerId);
            return;
        }
        if (session.task != null) {
            session.task.cancel();
            session.task = null;
        }
        if (session.arriving) {
            return;
        }
        session.arriving = true;

        Location origin = player.getLocation().clone();
        if (blindness) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS, 40, 0, false, false, false
            ));
        }
        if (effects) {
            player.playSound(origin, Sound.BLOCK_PORTAL_TRAVEL, 0.7F, 1.5F);
            player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.2F);
            spawnParticles(origin, true);
        }
        title(player,
                text("teleport.moving-title", "<dark_gray>✦</dark_gray>"),
                text("teleport.moving-subtitle", "<gray>Перемещение…</gray>"),
                Title.Times.times(
                        Duration.ofMillis(60L),
                        Duration.ofMillis(260L),
                        Duration.ofMillis(80L)
                ));

        Location destination = spawn.clone();
        player.getScheduler().runDelayed(this, task -> {
            if (!player.isOnline()) {
                sessions.remove(session.playerId);
                return;
            }
            player.teleportAsync(destination).thenAccept(success -> {
                if (!player.isOnline()) {
                    sessions.remove(session.playerId);
                    return;
                }
                player.getScheduler().runDelayed(this, next -> finish(session, success), () -> {}, 1L);
            });
        }, () -> {}, 4L);
    }

    private void finish(TeleportSession session, boolean success) {
        if (sessions.get(session.playerId) != session) {
            return;
        }
        sessions.remove(session.playerId);

        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (blindness) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        if (!success) {
            send(player, text("messages.teleport-failed",
                    "<#FF6B6B>× Не удалось телепортировать вас на спавн.</#FF6B6B>"));
            return;
        }

        Location destination = spawn.clone();
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.setVelocity(new Vector(0, 0, 0));
        player.setRotation(destination.getYaw(), destination.getPitch());

        if (effects) {
            spawnParticles(destination, false);
            player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9F, 1.4F);
            player.playSound(destination, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, 1.6F);
            player.playSound(destination, Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.7F);
        }

        if (session.fromVoid) {
            actionBar(player, text("void.returned",
                    "<#7DE3F4>Бездна вернула вас на спавн</#7DE3F4>"));
        } else {
            title(player,
                    text("teleport.success-title",
                            "<gradient:#7DE3F4:#B388FF>✦ СПАВН ✦</gradient>"),
                    text("teleport.success-subtitle", "<gray>Добро пожаловать домой</gray>"),
                    Title.Times.times(
                            Duration.ofMillis(200L),
                            Duration.ofMillis(1400L),
                            Duration.ofMillis(400L)
                    ));
            actionBar(player, text("teleport.success-actionbar",
                    "<#7DE3F4>Вы на спавне</#7DE3F4>"));
        }
    }

    private void spawnParticles(Location location, boolean reverse) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(reverse ? Particle.REVERSE_PORTAL : Particle.PORTAL,
                location, reverse ? 60 : 100, 0.55D, 0.9D, 0.55D, reverse ? 0.05D : 0.55D);
        world.spawnParticle(Particle.END_ROD, location, 30, 0.4D, 0.7D, 0.4D, 0.02D);
    }

    public void cancelTeleport(Player player, String message) {
        if (player == null) {
            return;
        }
        TeleportSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.task != null) {
            session.task.cancel();
            session.task = null;
        }
        if (blindness) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        if (message != null) {
            actionBar(player, message);
            send(player, message);
            if (effects) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.8F);
            }
        }
    }

    public void handleVoid(Player player) {
        if (!voidEnabled || !hasSpawn() || sessions.containsKey(player.getUniqueId())) {
            return;
        }
        startTeleport(player, true);
    }

    static final class TeleportSession {
        final UUID playerId;
        final Location origin;
        final long finishAt;
        final boolean fromVoid;
        ScheduledTask task;
        boolean arriving;
        int lastSecond;

        TeleportSession(UUID playerId, Location origin, long finishAt, boolean fromVoid) {
            this.playerId = playerId;
            this.origin = origin;
            this.finishAt = finishAt;
            this.fromVoid = fromVoid;
        }
    }
}
