package ru.spawnshield;

import io.papermc.paper.event.block.PlayerShearBlockEvent;
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import io.papermc.paper.event.player.PlayerLecternPageChangeEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;

public final class SpawnListener implements Listener {

    private static final List<String> DEFAULT_PHRASES = List.of(
            "<gradient:#FF6B6B:#FFB86B>Снова ты?</gradient>",
            "<gradient:#7DE3F4:#B388FF>Ты воскрес...</gradient>",
            "<gradient:#A8E6CF:#7DE3F4>С возвращением.</gradient>",
            "<gradient:#FFD166:#FF6B6B>Смерть — не конец.</gradient>",
            "<gradient:#C5C6CA:#7DE3F4>Опять?</gradient>",
            "<gradient:#B388FF:#7DE3F4>Новая попытка.</gradient>"
    );

    private final SpawnShieldPlugin plugin;

    public SpawnListener(SpawnShieldPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean canModify(Player player) {
        return plugin.canModify(player);
    }

    private boolean protectedAt(Location location) {
        return !plugin.editMode() && plugin.inSpawn(location);
    }

    // ---------------------------------------------------------------- blocks

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        boolean inSpawn = plugin.inSpawn(player.getLocation())
                || (block != null && plugin.inSpawn(block.getLocation()));
        if (!inSpawn || canModify(player)) {
            return;
        }
        if (block != null
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && plugin.allowContainers()
                && plugin.inSpawn(block.getLocation())
                && isAllowedContainer(block)) {
            return;
        }
        if (isAllowedItemUse(event)) {
            return;
        }
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }

    private static boolean isAllowedContainer(Block block) {
        BlockState state = block.getState();
        return state instanceof Container || state instanceof EnderChest;
    }

    private static boolean isAllowedItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        Block block = event.getClickedBlock();
        if (block != null && block.getType().isInteractable()) {
            return false;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.FISHING_ROD
                || type.isEdible()
                || type == Material.POTION
                || type == Material.HONEY_BOTTLE
                || type == Material.MILK_BUCKET;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFlowerPot(PlayerFlowerPotManipulateEvent event) {
        if (plugin.inSpawn(event.getFlowerpot().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (plugin.inSpawn(event.getHarvestedBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onShearBlock(PlayerShearBlockEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInsertLectern(PlayerInsertLecternBookEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTakeLectern(PlayerTakeLecternBookEvent event) {
        if (plugin.inSpawn(event.getLectern().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLecternPage(PlayerLecternPageChangeEvent event) {
        if (plugin.inSpawn(event.getLectern().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignOpen(PlayerOpenSignEvent event) {
        if (plugin.inSpawn(event.getSign().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // --------------------------------------------------------------- entities

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (plugin.inSpawn(event.getRightClicked().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (plugin.inSpawn(event.getRightClicked().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (plugin.inSpawn(event.getRightClicked().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (plugin.inSpawn(event.getBed().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        if (plugin.inSpawn(event.getPlayer().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY
                && plugin.inSpawn(event.getPlayer().getLocation())
                && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onShearEntity(PlayerShearEntityEvent event) {
        if (plugin.inSpawn(event.getEntity().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (plugin.inSpawn(event.getEntity().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onUnleash(PlayerUnleashEntityEvent event) {
        if (plugin.inSpawn(event.getEntity().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEditBook(PlayerEditBookEvent event) {
        if (plugin.inSpawn(event.getPlayer().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        if (plugin.inSpawn(event.getPlayer().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof FishHook) {
            return;
        }
        if (event.getEntity().getShooter() instanceof Player player
                && plugin.inSpawn(player.getLocation())
                && !canModify(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFishingHookHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishHook) || event.getHitBlock() == null) {
            return;
        }
        Block block = event.getHitBlock();
        if (!plugin.inSpawn(block.getLocation())) {
            return;
        }
        Material type = block.getType();
        if (Tag.PRESSURE_PLATES.isTagged(type)
                || Tag.BUTTONS.isTagged(type)
                || type == Material.LEVER
                || type == Material.TRIPWIRE
                || type == Material.TRIPWIRE_HOOK
                || type == Material.TARGET
                || type == Material.SCULK_SENSOR
                || type == Material.CALIBRATED_SCULK_SENSOR
                || type == Material.SCULK_SHRIEKER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (plugin.inSpawn(event.getEntity().getLocation())
                && (event.getPlayer() == null || !canModify(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!plugin.inSpawn(event.getEntity().getLocation())) {
            return;
        }
        if (event instanceof HangingBreakByEntityEvent byEntity
                && byEntity.getRemover() instanceof Player player
                && canModify(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        Location location = event.getBlock() != null
                ? event.getBlock().getLocation()
                : event.getEntity().getLocation();
        if (plugin.inSpawn(location) && (event.getPlayer() == null || !canModify(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!plugin.blockMobSpawns() || plugin.editMode() || !plugin.inSpawn(event.getLocation())) {
            return;
        }
        switch (event.getSpawnReason()) {
            case CUSTOM, COMMAND -> {
                return;
            }
            default -> event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------ environment

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (plugin.editMode()) {
            return;
        }
        if (plugin.inSpawn(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (protectedAt(event.getBlock().getLocation()) && !canModify(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onForm(BlockFormEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (protectedAt(event.getBlock().getLocation()) || protectedAt(event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        for (BlockState state : event.getBlocks()) {
            if (protectedAt(state.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (pistonTouchesSpawn(event.getBlock(), event.getBlocks(),
                event.getDirection().getModX(), event.getDirection().getModY(), event.getDirection().getModZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (pistonTouchesSpawn(event.getBlock(), event.getBlocks(),
                event.getDirection().getModX(), event.getDirection().getModY(), event.getDirection().getModZ())) {
            event.setCancelled(true);
        }
    }

    private boolean pistonTouchesSpawn(Block piston, List<Block> moved, int dx, int dy, int dz) {
        if (protectedAt(piston.getLocation())) {
            return true;
        }
        for (Block block : moved) {
            if (protectedAt(block.getLocation()) || protectedAt(block.getRelative(dx, dy, dz).getLocation())) {
                return true;
            }
        }
        return protectedAt(piston.getLocation().clone().add(dx, dy, dz));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (protectedAt(event.getBlock().getLocation()) || protectedAt(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (protectedAt(event.getBlock().getLocation()) || protectedAt(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        for (BlockState state : event.getBlocks()) {
            if (protectedAt(state.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        for (BlockState state : event.getBlocks()) {
            if (protectedAt(state.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (protectedAt(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (protectedAt(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> protectedAt(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (protectedAt(event.getEntity().getLocation())) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> protectedAt(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (protectedAt(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent event) {
        if (event.toWeatherState() && !plugin.editMode() && plugin.isProtectedWorld(event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.handleWorldLoad(event.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!plugin.blockHunger() || plugin.editMode() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.inSpawn(player.getLocation())) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- damage

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            if (!plugin.voidEnabled() || !plugin.hasSpawn()) {
                return;
            }
            event.setCancelled(true);
            plugin.handleVoid(player);
            return;
        }

        if (entity instanceof Player player && plugin.isTeleporting(player.getUniqueId())) {
            plugin.cancelTeleport(player, plugin.text("teleport.damaged",
                    "<#FF6B6B>× Телепорт отменён</#FF6B6B> <dark_gray>•</dark_gray> <gray>вы получили урон</gray>"));
        }

        if (!plugin.blockEntityDamage() && !(entity instanceof Player)) {
            return;
        }
        if (!protectedAt(entity.getLocation())) {
            return;
        }
        if (entity instanceof Player player && canModify(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = findAttacker(event.getDamager());
        if (attacker != null && !canModify(attacker) && protectedAt(attacker.getLocation())) {
            event.setCancelled(true);
        }
    }

    private static Player findAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    // ---------------------------------------------------------------- players

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.voidEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) {
            return;
        }
        if (to.getY() < to.getWorld().getMinHeight() - plugin.voidOffset()) {
            plugin.handleVoid(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.cancelTeleport(event.getPlayer(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.getRespawnReason() == PlayerRespawnEvent.RespawnReason.DEATH
                && !event.isBedSpawn()
                && !event.isAnchorSpawn()
                && plugin.hasSpawn()) {
            event.setRespawnLocation(plugin.spawn().clone());
        }
        if (!plugin.deathTitles()) {
            return;
        }
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            List<String> phrases = plugin.getConfig().getStringList("death.phrases");
            List<String> source = phrases.isEmpty() ? DEFAULT_PHRASES : phrases;
            String phrase = source.get(ThreadLocalRandom.current().nextInt(source.size()));
            plugin.title(player,
                    phrase,
                    plugin.text("death.subtitle", "<gray>Ты снова в игре</gray>"),
                    Title.Times.times(
                            Duration.ofMillis(350L),
                            Duration.ofMillis(1400L),
                            Duration.ofMillis(600L)
                    ));
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.7F, 1.15F);
        }, () -> {}, 20L);
    }
}
