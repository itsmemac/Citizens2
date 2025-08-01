package net.citizensnpcs.npc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.SetMultimap;

import net.citizensnpcs.NPCNeedsRespawnEvent;
import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.astar.pathfinder.MinecraftBlockExaminer;
import net.citizensnpcs.api.astar.pathfinder.SwimmingExaminer;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.event.NPCTeleportEvent;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.AbstractNPC;
import net.citizensnpcs.api.npc.BlockBreaker;
import net.citizensnpcs.api.npc.BlockBreaker.BlockBreakerConfiguration;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Spawned;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.Messaging;
import net.citizensnpcs.npc.ai.CitizensNavigator;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.AttributeTrait;
import net.citizensnpcs.trait.CurrentLocation;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.HologramTrait;
import net.citizensnpcs.trait.HologramTrait.HologramRenderer;
import net.citizensnpcs.trait.PacketNPC;
import net.citizensnpcs.trait.ScoreboardTrait;
import net.citizensnpcs.trait.SitTrait;
import net.citizensnpcs.trait.SkinLayers;
import net.citizensnpcs.trait.SneakTrait;
import net.citizensnpcs.util.ChunkCoord;
import net.citizensnpcs.util.Messages;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;
import net.citizensnpcs.util.PlayerUpdateTask;
import net.citizensnpcs.util.Util;

public class CitizensNPC extends AbstractNPC {
    private ChunkCoord cachedCoord;
    private EntityController entityController;
    private final CitizensNavigator navigator = new CitizensNavigator(this);
    private int updateCounter = 0;

    public CitizensNPC(UUID uuid, int id, String name, EntityController controller, NPCRegistry registry) {
        super(uuid, id, name, registry);
        setEntityController(controller);
    }

    @Override
    public boolean despawn(DespawnReason reason) {
        if (reason == DespawnReason.RELOAD) {
            for (Trait trait : traits.values()) {
                HandlerList.unregisterAll(trait);
            }
        }
        if (getEntity() == null && reason != DespawnReason.DEATH) {
            Messaging.debug("Tried to despawn", this, "while already despawned, DespawnReason." + reason);
            return true;
        }
        NPCDespawnEvent event = new NPCDespawnEvent(this, reason);
        if (reason == DespawnReason.CHUNK_UNLOAD) {
            event.setCancelled(data().get(NPC.Metadata.KEEP_CHUNK_LOADED, Setting.KEEP_CHUNKS_LOADED.asBoolean()));
        }
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() && reason != DespawnReason.DEATH) {
            Messaging.debug("Couldn't despawn", this, "due to despawn event cancellation. Will load chunk.",
                    getEntity().isValid(), ", DespawnReason." + reason);
            return false;
        }
        boolean keepSelected = getOrAddTrait(Spawned.class).shouldSpawn();
        if (!keepSelected) {
            data().remove("selectors");
        }
        if (getEntity() instanceof Player) {
            PlayerUpdateTask.deregister(getEntity());
        }
        navigator.onDespawn();
        for (Trait trait : new ArrayList<>(traits.values())) {
            trait.onDespawn(reason);
        }
        Messaging.debug("Despawned", this, "DespawnReason." + reason);

        if (reason == DespawnReason.DEATH) {
            entityController.die();
        } else {
            entityController.remove();
        }
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
        resetCachedCoord();
    }

    @Override
    public void faceLocation(Location location) {
        if (!isSpawned())
            return;

        Util.faceLocation(getEntity(), location);
    }

    @Override
    public BlockBreaker getBlockBreaker(Block targetBlock, BlockBreakerConfiguration config) {
        return NMS.getBlockBreaker(getEntity(), targetBlock, config);
    }

    @Override
    public Entity getEntity() {
        return entityController == null ? null : entityController.getBukkitEntity();
    }

    public EntityController getEntityController() {
        return entityController;
    }

    @Override
    public Navigator getNavigator() {
        return navigator;
    }

    @Override
    public Location getStoredLocation() {
        return isSpawned() ? getEntity().getLocation() : getOrAddTrait(CurrentLocation.class).getLocation();
    }

    @Override
    public boolean isFlyable() {
        updateFlyableState();
        return super.isFlyable();
    }

    @Override
    public boolean isSpawned() {
        return getEntity() != null && (hasTrait(PacketNPC.class) || NMS.isValid(getEntity()));
    }

    @Override
    public boolean isUpdating(NPCUpdate update) {
        return update == NPCUpdate.PACKET
                ? updateCounter > data().get(NPC.Metadata.PACKET_UPDATE_DELAY, Setting.PACKET_UPDATE_DELAY.asTicks())
                : false;
    }

    @Override
    public void load(DataKey root) {
        super.load(root);

        if (getOrAddTrait(Spawned.class).shouldSpawn()) {
            CurrentLocation current = getOrAddTrait(CurrentLocation.class);
            if (current.getLocation() != null) {
                spawn(current.getLocation(), SpawnReason.RESPAWN);
            } else if (current.getChunkCoord() != null) {
                Bukkit.getPluginManager().callEvent(new NPCNeedsRespawnEvent(this, current.getChunkCoord()));
            }
        }
        navigator.load(root.getRelative("navigator"));
    }

    @Override
    public boolean requiresNameHologram() {
        return !data().has(NPC.Metadata.HOLOGRAM_RENDERER)
                && (super.requiresNameHologram() || Setting.ALWAYS_USE_NAME_HOLOGRAM.asBoolean());
    }

    private void resetCachedCoord() {
        if (cachedCoord == null)
            return;
        Set<NPC> npcs = CHUNK_LOADERS.get(cachedCoord);
        npcs.remove(this);
        if (npcs.size() == 0) {
            cachedCoord.setForceLoaded(false);
        }
        cachedCoord = null;
    }

    @Override
    public void save(DataKey root) {
        super.save(root);

        if (!data().get(NPC.Metadata.SHOULD_SAVE, true))
            return;

        navigator.save(root.getRelative("navigator"));
    }

    @Override
    public void scheduleUpdate(NPCUpdate update) {
        if (update == NPCUpdate.PACKET) {
            updateCounter = data().get(NPC.Metadata.PACKET_UPDATE_DELAY, Setting.PACKET_UPDATE_DELAY.asTicks()) + 1;
        }
    }

    @Override
    public void setBukkitEntityType(EntityType type) {
        EntityController controller = EntityControllers.createForType(type);
        if (controller == null)
            throw new IllegalArgumentException("Unsupported entity type " + type);
        setEntityController(controller);
    }

    public void setEntityController(EntityController newController) {
        Objects.requireNonNull(newController);
        boolean wasSpawned = entityController == null ? false : isSpawned();
        Location prev = null;
        if (wasSpawned) {
            prev = getEntity().getLocation();
            despawn(DespawnReason.PENDING_RESPAWN);
        }
        PacketNPC packet = getTraitNullable(PacketNPC.class);
        if (packet != null) {
            newController = packet.wrap(newController);
        }
        entityController = newController;
        if (wasSpawned) {
            spawn(prev, SpawnReason.RESPAWN);
        }
    }

    @Override
    public void setFlyable(boolean flyable) {
        super.setFlyable(flyable);
        updateFlyableState();
    }

    @Override
    public void setMoveDestination(Location destination) {
        if (!isSpawned())
            return;
        if (destination == null) {
            NMS.cancelMoveDestination(getEntity());
        } else {
            NMS.setDestination(getEntity(), destination.getX(), destination.getY(), destination.getZ(),
                    getNavigator().getDefaultParameters().speedModifier());
        }
    }

    @Override
    protected void setNameInternal(String name) {
        super.setNameInternal(name);
        if (requiresNameHologram()) {
            HologramRenderer hr = getOrAddTrait(HologramTrait.class).getNameRenderer();
            if (hr != null) {
                hr.updateText(this, getRawName());
            }
        }
        updateCustomName();
    }

    @Override
    public void setSneaking(boolean sneaking) {
        getOrAddTrait(SneakTrait.class).setSneaking(sneaking);
    }

    @Override
    public boolean shouldRemoveFromPlayerList() {
        return data().get(NPC.Metadata.REMOVE_FROM_PLAYERLIST, Setting.REMOVE_PLAYERS_FROM_PLAYER_LIST.asBoolean());
    }

    @Override
    public boolean shouldRemoveFromTabList() {
        return data().get(NPC.Metadata.REMOVE_FROM_TABLIST, Setting.DISABLE_TABLIST.asBoolean());
    }

    @Override
    public boolean spawn(Location at, SpawnReason reason, Consumer<Entity> callback) {
        Objects.requireNonNull(at, "location cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        if (getEntity() != null) {
            Messaging.debug("Tried to spawn", this, "while already spawned. SpawnReason." + reason);
            return false;
        }
        if (at.getWorld() == null) {
            Messaging.debug("Tried to spawn", this, "but the world was null. SpawnReason." + reason);
            return false;
        }
        at = at.clone();

        if (reason == SpawnReason.CHUNK_LOAD || reason == SpawnReason.COMMAND) {
            at.getChunk().load();
        }
        getOrAddTrait(CurrentLocation.class).setLocation(at);
        entityController.create(at.clone(), this);

        if (getEntity() instanceof SkinnableEntity && !hasTrait(SkinLayers.class)) {
            ((SkinnableEntity) getEntity()).setSkinFlags(EnumSet.allOf(SkinLayers.Layer.class));
        }
        for (Trait trait : traits.values().toArray(new Trait[traits.values().size()])) {
            try {
                trait.onPreSpawn();
            } catch (Throwable ex) {
                Messaging.severeTr(Messages.TRAIT_ONSPAWN_FAILED, trait.getName(), getId());
                ex.printStackTrace();
            }
        }
        data().set(NPC.Metadata.NPC_SPAWNING_IN_PROGRESS, true);
        boolean wasLoaded = Messaging.isDebugging() ? Util.isLoaded(at) : false;
        boolean couldSpawn = entityController.spawn(at);

        if (!couldSpawn) {
            if (Messaging.isDebugging()) {
                Messaging.debug("Retrying spawn of", this, "later, SpawnReason." + reason + ". Was loaded", wasLoaded,
                        "is loaded", Util.isLoaded(at));
            }
            // we need to wait before trying to spawn
            entityController.remove();
            Bukkit.getPluginManager().callEvent(new NPCNeedsRespawnEvent(this, at));
            data().remove(NPC.Metadata.NPC_SPAWNING_IN_PROGRESS);
            return false;
        }
        NMS.setLocationDirectly(getEntity(), at);
        NMS.setHeadAndBodyYaw(getEntity(), at.getYaw());

        // Paper now doesn't actually set entities as valid for a few ticks while adding entities to chunks
        // Need to check the entity is really valid for a few ticks before finalising spawning
        Location to = at;
        Consumer<Runnable> postSpawn = new Consumer<Runnable>() {
            private int timer;

            @Override
            public void accept(Runnable cancel) {
                if (getEntity() == null || !hasTrait(PacketNPC.class) && !getEntity().isValid()) {
                    if (timer++ > Setting.ENTITY_SPAWN_WAIT_DURATION.asTicks()) {
                        Messaging.debug("Couldn't spawn ", CitizensNPC.this, "waited", timer,
                                "ticks but entity not added to world");
                        entityController.remove();
                        cancel.run();
                        Bukkit.getPluginManager().callEvent(new NPCNeedsRespawnEvent(CitizensNPC.this, to));
                    }
                    return;
                }
                // Set the spawned state
                getOrAddTrait(CurrentLocation.class).setLocation(to);
                getOrAddTrait(Spawned.class).setSpawned(true);

                NPCSpawnEvent spawnEvent = new NPCSpawnEvent(CitizensNPC.this, to, reason);
                Bukkit.getPluginManager().callEvent(spawnEvent);

                if (spawnEvent.isCancelled()) {
                    Messaging.debug("Couldn't spawn", CitizensNPC.this, "SpawnReason." + reason,
                            "due to event cancellation.");
                    entityController.remove();
                    cancel.run();
                    return;
                }
                navigator.onSpawn();

                for (Trait trait : traits.values().toArray(ObjectArrays.newArray(Trait.class, traits.size()))) {
                    try {
                        trait.onSpawn();
                    } catch (Throwable ex) {
                        Messaging.severeTr(Messages.TRAIT_ONSPAWN_FAILED, trait.getName(), getId());
                        ex.printStackTrace();
                    }
                }
                NMS.replaceTracker(getEntity());
                data().remove(NPC.Metadata.NPC_SPAWNING_IN_PROGRESS);
                EntityType type = getEntity().getType();
                if (type.isAlive()) {
                    LivingEntity entity = (LivingEntity) getEntity();
                    entity.setRemoveWhenFarAway(false);

                    if (type == EntityType.PLAYER || Util.isHorse(type)) {
                        if (SUPPORT_ATTRIBUTES && !hasTrait(AttributeTrait.class)
                                || !getTrait(AttributeTrait.class).hasAttribute(Util
                                        .getRegistryValue(Registry.ATTRIBUTE, "generic.step_height", "step_height"))) {
                            NMS.setStepHeight(entity, 1);
                        }
                    }
                    if (type == EntityType.PLAYER) {
                        PlayerUpdateTask.register(getEntity());
                        if (SUPPORT_ATTRIBUTES
                                && Util.getRegistryValue(Registry.ATTRIBUTE, "waypoint_transmit_range") != null) {
                            AttributeTrait attr = getOrAddTrait(AttributeTrait.class);
                            if (!attr.hasAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE)) {
                                attr.setAttributeValue(Attribute.WAYPOINT_TRANSMIT_RANGE, 0);
                            }
                        }
                    }
                    entity.setNoDamageTicks(data().get(NPC.Metadata.SPAWN_NODAMAGE_TICKS,
                            Setting.DEFAULT_SPAWN_NODAMAGE_DURATION.asTicks()));
                }
                if (requiresNameHologram() && !hasTrait(HologramTrait.class)) {
                    addTrait(HologramTrait.class);
                }
                updateFlyableState();
                updateCustomNameVisibility();
                updateScoreboard();

                Messaging.debug("Spawned", CitizensNPC.this, "SpawnReason." + reason);
                cancel.run();
                if (callback != null) {
                    callback.accept(getEntity());
                }
            }
        };
        if (getEntity() != null && getEntity().isValid()) {
            postSpawn.accept(() -> {
            });
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    postSpawn.accept(this::cancel);
                }
            }.runTaskTimer(CitizensAPI.getPlugin(), 0, 1);
        }
        return true;
    }

    @Override
    public void teleport(Location location, TeleportCause reason) {
        if (!isSpawned())
            return;

        if (hasTrait(SitTrait.class) && getOrAddTrait(SitTrait.class).isSitting()) {
            getOrAddTrait(SitTrait.class).setSitting(location);
        }
        Location npcLoc = getEntity().getLocation();
        if (isSpawned() && npcLoc.getWorld() == location.getWorld()) {
            if (npcLoc.distance(location) < 1) {
                NMS.setHeadAndBodyYaw(getEntity(), location.getYaw());
            }
            if (getEntity().getType() == EntityType.PLAYER && !getEntity().isInsideVehicle()
                    && NMS.getPassengers(getEntity()).size() == 0) {
                NPCTeleportEvent event = new NPCTeleportEvent(this, location);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled())
                    return;
                NMS.setLocationDirectly(getEntity(), location);
                return;
            }
        }
        super.teleport(location, reason);
    }

    @Override
    public String toString() {
        EntityType mobType = hasTrait(MobType.class) ? getTraitNullable(MobType.class).getType() : null;
        return getId() + "{" + getRawName() + ", " + mobType + "}";
    }

    @Override
    public void update() {
        try {
            super.update();
            if (!isSpawned()) {
                resetCachedCoord();
                return;
            }
            Location loc = getEntity().getLocation();
            if (data().has(NPC.Metadata.ACTIVATION_RANGE)) {
                int range = data().get(NPC.Metadata.ACTIVATION_RANGE);
                if (range == -1 || CitizensAPI.getLocationLookup().getNearbyPlayers(loc, range).iterator().hasNext()) {
                    NMS.activate(getEntity());
                }
            }
            boolean shouldSwim = data().get(NPC.Metadata.SWIM,
                    !useMinecraftAI() && SwimmingExaminer.isWaterMob(getEntity()))
                    && MinecraftBlockExaminer.isLiquid(loc.getBlock().getType());
            if (navigator.isNavigating()) {
                if (shouldSwim) {
                    getEntity().setVelocity(getEntity().getVelocity().multiply(
                            data().get(NPC.Metadata.WATER_SPEED_MODIFIER, Setting.NPC_WATER_SPEED_MODIFIER.asFloat())));
                    Location currentDest = navigator.getPathStrategy().getCurrentDestination();
                    if (currentDest == null || currentDest.getY() > loc.getY()) {
                        NMS.trySwim(getEntity());
                    }
                }
            } else if (shouldSwim) {
                Gravity trait = getTraitNullable(Gravity.class);
                if (trait == null || trait.hasGravity()) {
                    NMS.trySwim(getEntity());
                }
            }
            if (SUPPORT_GLOWING && data().has(NPC.Metadata.GLOWING)) {
                getEntity().setGlowing(data().get(NPC.Metadata.GLOWING, false));
            }
            if (SUPPORT_SILENT && data().has(NPC.Metadata.SILENT)) {
                getEntity().setSilent(Boolean.parseBoolean(data().get(NPC.Metadata.SILENT).toString()));
            }
            if (data().has(NPC.Metadata.AGGRESSIVE)) {
                NMS.setAggressive(getEntity(), data().<Boolean> get(NPC.Metadata.AGGRESSIVE));
            }
            boolean isLiving = getEntity() instanceof LivingEntity;
            if (isUpdating(NPCUpdate.PACKET)) {
                if (data().get(NPC.Metadata.KEEP_CHUNK_LOADED, Setting.KEEP_CHUNKS_LOADED.asBoolean())) {
                    ChunkCoord currentCoord = new ChunkCoord(loc);
                    if (!currentCoord.equals(cachedCoord)) {
                        resetCachedCoord();
                        currentCoord.setForceLoaded(true);
                        CHUNK_LOADERS.put(currentCoord, this);
                        cachedCoord = currentCoord;
                    }
                }
                if (isLiving) {
                    updateScoreboard();
                }
                updateCounter = 0;
            }
            updateCustomNameVisibility();

            if (isLiving) {
                NMS.setKnockbackResistance((LivingEntity) getEntity(), isProtected() ? 1D : 0D);
                if (SUPPORT_PICKUP_ITEMS) {
                    ((LivingEntity) getEntity()).setCanPickupItems(data().get(NPC.Metadata.PICKUP_ITEMS, false));
                }
                if (getEntity() instanceof Player) {
                    updateUsingItemState((Player) getEntity());
                }
            }
            navigator.run();

            updateCounter++;
        } catch (Exception ex) {
            Throwable error = Throwables.getRootCause(ex);
            Messaging.logTr(Messages.EXCEPTION_UPDATING_NPC, getId(), error.getMessage());
            error.printStackTrace();
        }
    }

    private void updateCustomName() {
        if (getEntity() == null)
            return;
        if (coloredNameComponentCache != null) {
            NMS.setCustomName(getEntity(), coloredNameComponentCache, coloredNameStringCache);
        } else {
            getEntity().setCustomName(getFullName());
        }
    }

    private void updateCustomNameVisibility() {
        String nameplateVisible = data().<Object> get(NPC.Metadata.NAMEPLATE_VISIBLE, true).toString();
        if (requiresNameHologram()) {
            nameplateVisible = "false";
        }
        if (nameplateVisible.equals("true") || nameplateVisible.equals("hover")) {
            updateCustomName();
        }
        getEntity().setCustomNameVisible(Boolean.parseBoolean(nameplateVisible));
    }

    private void updateFlyableState() {
        if (!CitizensAPI.hasImplementation())
            return;

        EntityType type = isSpawned() ? getEntity().getType() : getOrAddTrait(MobType.class).getType();
        if (type == null || !Util.isAlwaysFlyable(type))
            return;

        if (!data().has(NPC.Metadata.FLYABLE)) {
            data().setPersistent(NPC.Metadata.FLYABLE, true);
        }
        if (!hasTrait(Gravity.class)) {
            getOrAddTrait(Gravity.class).setHasGravity(false);
        }
    }

    private void updateScoreboard() {
        if (data().has(NPC.Metadata.SCOREBOARD_FAKE_TEAM_NAME)) {
            getOrAddTrait(ScoreboardTrait.class).update();
        }
    }

    private void updateUsingItemState(Player player) {
        boolean useItem = data().get(NPC.Metadata.USING_HELD_ITEM, false),
                offhand = data().get(NPC.Metadata.USING_OFFHAND_ITEM, false);

        if (!SUPPORT_USE_ITEM)
            return;

        if (data().has("citizens-was-using-item")) {
            PlayerAnimation.STOP_USE_ITEM.play(player, 64);
            data().remove("citizens-was-using-item");
        }
        try {
            if (useItem) {
                PlayerAnimation.START_USE_MAINHAND_ITEM.play(player, 64);
                data().set("citizens-was-using-item", true);
            } else if (offhand) {
                PlayerAnimation.START_USE_OFFHAND_ITEM.play(player, 64);
                data().set("citizens-was-using-item", true);
            }
        } catch (UnsupportedOperationException ex) {
            SUPPORT_USE_ITEM = false;
        }
    }

    private static final SetMultimap<ChunkCoord, NPC> CHUNK_LOADERS = HashMultimap.create();
    private static boolean SUPPORT_ATTRIBUTES = false;
    private static boolean SUPPORT_GLOWING = false;
    private static boolean SUPPORT_PICKUP_ITEMS = false;
    private static boolean SUPPORT_SILENT = false;
    private static boolean SUPPORT_USE_ITEM = true;
    static {
        try {
            Entity.class.getMethod("setGlowing", boolean.class);
            SUPPORT_GLOWING = true;
        } catch (NoSuchMethodException | SecurityException e) {
        }
        try {
            Entity.class.getMethod("setSilent", boolean.class);
            SUPPORT_SILENT = true;
        } catch (NoSuchMethodException | SecurityException e) {
        }
        try {
            LivingEntity.class.getMethod("setCanPickupItems", boolean.class);
            SUPPORT_PICKUP_ITEMS = true;
        } catch (NoSuchMethodException | SecurityException e) {
        }
        try {
            Class.forName("org.bukkit.attribute.Attribute");
            SUPPORT_ATTRIBUTES = true;
        } catch (ClassNotFoundException e) {
        }
    }
}
