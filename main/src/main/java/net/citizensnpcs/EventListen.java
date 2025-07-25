package net.citizensnpcs;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.ChunkEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.NavigationBeginEvent;
import net.citizensnpcs.api.ai.event.NavigationCompleteEvent;
import net.citizensnpcs.api.event.CitizensPreReloadEvent;
import net.citizensnpcs.api.event.CommandSenderCreateNPCEvent;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.EntityTargetNPCEvent;
import net.citizensnpcs.api.event.NPCCombustByBlockEvent;
import net.citizensnpcs.api.event.NPCCombustByEntityEvent;
import net.citizensnpcs.api.event.NPCCombustEvent;
import net.citizensnpcs.api.event.NPCDamageByBlockEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCKnockbackEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCLinkToPlayerEvent;
import net.citizensnpcs.api.event.NPCMoveEvent;
import net.citizensnpcs.api.event.NPCPushEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.event.NPCSeenByPlayerEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.event.NPCVehicleDamageEvent;
import net.citizensnpcs.api.event.PlayerCreateNPCEvent;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.trait.trait.PlayerFilter;
import net.citizensnpcs.api.util.Messaging;
import net.citizensnpcs.api.util.SpigotUtil;
import net.citizensnpcs.editor.Editor;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.npc.skin.SkinUpdateTracker;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.ClickRedirectTrait;
import net.citizensnpcs.trait.CommandTrait;
import net.citizensnpcs.trait.Controllable;
import net.citizensnpcs.trait.CurrentLocation;
import net.citizensnpcs.trait.HologramTrait.HologramRenderer;
import net.citizensnpcs.trait.TargetableTrait;
import net.citizensnpcs.trait.versioned.SnowmanTrait;
import net.citizensnpcs.util.ChunkCoord;
import net.citizensnpcs.util.Messages;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;
import net.citizensnpcs.util.Util;

public class EventListen implements Listener {
    private Listener chunkEventListener;
    private Citizens plugin;
    private final SkinUpdateTracker skinUpdateTracker;
    private final ListMultimap<ChunkCoord, NPC> toRespawn = ArrayListMultimap.create(64, 4);

    EventListen(Citizens plugin) {
        this.plugin = plugin;
        skinUpdateTracker = new SkinUpdateTracker();
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.MONITOR)
                public void onEntityPickupItem(EntityPickupItemEvent event) {
                    if (event.getItem() instanceof NPCHolder) {
                        event.setCancelled(true);
                    }
                }
            }, plugin);
        } catch (Throwable ex) {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onPlayerPickupItemEvent(PlayerPickupItemEvent event) {
                    if (event.getItem() instanceof NPCHolder) {
                        event.setCancelled(true);
                    }
                }
            }, plugin);
        }
        try {
            Class.forName("org.bukkit.event.world.EntitiesLoadEvent");
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                public void onEntitiesLoad(EntitiesLoadEvent event) {
                    loadNPCs(event);
                }

                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                public void onEntitiesUnload(EntitiesUnloadEvent event) {
                    List<NPC> toDespawn = Lists.newArrayList(plugin.getLocationLookup().getNearbyNPCs(event.getWorld(),
                            new double[] { (event.getChunk().getX() << 4) - 0.5, 0,
                                    (event.getChunk().getZ() << 4) - 0.5 },
                            new double[] { (event.getChunk().getX() + 1 << 4) + 0.5, 256,
                                    (event.getChunk().getZ() + 1 << 4) + 0.5 }));
                    for (Entity entity : event.getEntities()) {
                        NPC npc = plugin.getNPCRegistry().getNPC(entity);
                        // XXX npc#isSpawned() checks valid status which is now inconsistent on chunk unload
                        // between different server software so check for npc.getEntity() == null instead.
                        if (npc == null || npc.getEntity() == null || toDespawn.contains(npc))
                            continue;

                        toDespawn.add(npc);
                    }
                    if (toDespawn.isEmpty())
                        return;
                    unloadNPCs(event, toDespawn);
                }
            }, plugin);
        } catch (Throwable ex) {
        }
        try {
            Class.forName("org.bukkit.event.entity.EntityTransformEvent");
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onEntityTransform(EntityTransformEvent event) {
                    NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
                    if (npc == null)
                        return;
                    if (npc.isProtected()) {
                        event.setCancelled(true);
                    }
                }
            }, plugin);
        } catch (Throwable ex) {
        }
        Class<?> kbc = null;
        try {
            kbc = Class.forName("com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent");
        } catch (ClassNotFoundException e) {
        }
        if (kbc != null) {
            registerKnockbackEvent(kbc);
        }
        Class<?> pbeac = null;
        try {
            pbeac = Class.forName("io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent");
        } catch (ClassNotFoundException e) {
        }
        if (pbeac != null) {
            registerPushEvent(pbeac);
        }
        Class<?> paperEntityMoveEventClazz = null;
        try {
            paperEntityMoveEventClazz = Class.forName("io.papermc.paper.event.entity.EntityMoveEvent");
        } catch (ClassNotFoundException e) {
        }
        if (paperEntityMoveEventClazz != null) {
            registerMoveEvent(paperEntityMoveEventClazz);
        }
    }

    private void checkCreationEvent(CommandSenderCreateNPCEvent event) {
        if (event.getCreator().hasPermission("citizens.admin.avoid-limits"))
            return;
        int limit = Setting.DEFAULT_NPC_LIMIT.asInt();
        int maxChecks = Setting.MAX_NPC_LIMIT_CHECKS.asInt();
        for (int i = maxChecks; i >= 0; i--) {
            if (!event.getCreator().hasPermission("citizens.npc.limit." + i)) {
                continue;
            }
            limit = i;
            break;
        }
        if (limit < 0)
            return;
        int owned = 0;
        for (NPC npc : plugin.getNPCRegistry()) {
            if (!event.getNPC().equals(npc) && npc.hasTrait(Owner.class)
                    && npc.getTraitNullable(Owner.class).isOwnedBy(event.getCreator())) {
                owned++;
            }
        }
        int wouldOwn = owned + 1;
        if (wouldOwn > limit) {
            event.setCancelled(true);
            event.setCancelReason(Messaging.tr(Messages.OVER_NPC_LIMIT, limit));
        }
    }

    private Iterable<NPC> getAllNPCs() {
        return Iterables.filter(Iterables.concat(plugin.getNPCRegistries()), Objects::nonNull);
    }

    void loadNPCs(ChunkEvent event) {
        ChunkCoord coord = new ChunkCoord(event.getChunk());
        Runnable runnable = () -> respawnAllFromCoord(coord, event);
        if (Messaging.isDebugging() && Setting.DEBUG_CHUNK_LOADS.asBoolean() && toRespawn.containsKey(coord)) {
            new Exception("CITIZENS CHUNK LOAD DEBUG " + coord).printStackTrace();
        }
        if (event instanceof Cancellable) {
            runnable.run();
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (chunkEventListener != null)
            return;
        loadNPCs(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (chunkEventListener != null)
            return;
        List<NPC> toDespawn = Lists.newArrayList(plugin.getLocationLookup().getNearbyNPCs(event.getWorld(),
                new double[] { (event.getChunk().getX() << 4) - 0.5, 0, (event.getChunk().getZ() << 4) - 0.5 },
                new double[] { (event.getChunk().getX() + 1 << 4) + 0.5, 256,
                        (event.getChunk().getZ() + 1 << 4) + 0.5 }));
        if (SpigotUtil.getVersion()[1] < 21) {
            for (Entity entity : event.getChunk().getEntities()) {
                NPC npc = plugin.getNPCRegistry().getNPC(entity);
                // XXX npc#isSpawned() checks valid status which is now inconsistent on chunk unload
                // between different server software so check for npc.getEntity() == null instead.
                if (npc == null || npc.getEntity() == null || toDespawn.contains(npc))
                    continue;

                toDespawn.add(npc);
            }
        }
        if (toDespawn.isEmpty())
            return;
        unloadNPCs(event, toDespawn);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCitizensReload(CitizensPreReloadEvent event) {
        skinUpdateTracker.reset();
        toRespawn.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommandSenderCreateNPC(CommandSenderCreateNPCEvent event) {
        checkCreationEvent(event);
    }

    @EventHandler
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null)
            return;
        if (npc.getEntity() instanceof Snowman) {
            boolean formSnow = npc.hasTrait(SnowmanTrait.class)
                    ? npc.getTraitNullable(SnowmanTrait.class).shouldFormSnow()
                    : npc.useMinecraftAI();
            event.setCancelled(!formSnow);
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null)
            return;

        final NPCCombustEvent npcCombustEvent;
        if (event instanceof EntityCombustByEntityEvent) {
            npcCombustEvent = new NPCCombustByEntityEvent((EntityCombustByEntityEvent) event, npc);
        } else if (event instanceof EntityCombustByBlockEvent) {
            npcCombustEvent = new NPCCombustByBlockEvent((EntityCombustByBlockEvent) event, npc);
        } else {
            npcCombustEvent = new NPCCombustEvent(event, npc);
        }
        if (npc.isProtected()) {
            npcCombustEvent.setCancelled(true);
        }
        Bukkit.getPluginManager().callEvent(npcCombustEvent);
        if (npcCombustEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null) {
            if (event instanceof EntityDamageByEntityEvent) {
                npc = plugin.getNPCRegistry().getNPC(((EntityDamageByEntityEvent) event).getDamager());
                if (npc == null)
                    return;
                event.setCancelled(!npc.data().get(NPC.Metadata.DAMAGE_OTHERS, true));
                NPCDamageEntityEvent damageEvent = new NPCDamageEntityEvent(npc, (EntityDamageByEntityEvent) event);
                Bukkit.getPluginManager().callEvent(damageEvent);
                if (damageEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (npc.isProtected()) {
            event.setCancelled(true);
        }
        if (event instanceof EntityDamageByEntityEvent) {
            NPCDamageByEntityEvent damageEvent = new NPCDamageByEntityEvent(npc, (EntityDamageByEntityEvent) event);
            Bukkit.getPluginManager().callEvent(damageEvent);
            if (!damageEvent.isCancelled() || !(damageEvent.getDamager() instanceof Player))
                return;

            Player damager = (Player) damageEvent.getDamager();

            if (npc.hasTrait(ClickRedirectTrait.class)) {
                npc = npc.getTraitNullable(ClickRedirectTrait.class).getRedirectToNPC();
                if (npc == null)
                    return;
            }
            NPCLeftClickEvent leftClickEvent = new NPCLeftClickEvent(npc, damager);
            Bukkit.getPluginManager().callEvent(leftClickEvent);
            if (leftClickEvent.isCancelled())
                return;

            if (npc.hasTrait(CommandTrait.class)) {
                npc.getTraitNullable(CommandTrait.class).dispatch(damager, CommandTrait.Hand.LEFT);
            }
        } else {
            final NPCDamageEvent npcDamageEvent;
            if (event instanceof EntityDamageByBlockEvent) {
                npcDamageEvent = new NPCDamageByBlockEvent(npc, (EntityDamageByBlockEvent) event);
            } else {
                npcDamageEvent = new NPCDamageEvent(npc, event);
            }
            Bukkit.getPluginManager().callEvent(npcDamageEvent);
            if (npcDamageEvent.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityDeath(EntityDeathEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null)
            return;

        if (!npc.data().get(NPC.Metadata.DROPS_ITEMS, false)) {
            event.getDrops().clear();
        }
        Location location = npc.getStoredLocation();
        Bukkit.getPluginManager().callEvent(new NPCDeathEvent(npc, event));
        npc.despawn(DespawnReason.DEATH);

        int delay = npc.data().get(NPC.Metadata.RESPAWN_DELAY, -1);
        if (delay < 0)
            return;

        int deathAnimationTicks = event.getEntity() instanceof LivingEntity ? 20 : 2;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (!npc.isSpawned() && npc.getOwningRegistry().getByUniqueId(npc.getUniqueId()) == npc) {
                npc.spawn(location, SpawnReason.TIMED_RESPAWN);
            }
        }, delay + deathAnimationTicks);
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null || npc.getEntity().getType() != EntityType.PLAYER)
            return;

        event.setCancelled(true);
        npc.despawn(DespawnReason.PENDING_RESPAWN);
        event.getTo().getChunk();
        npc.spawn(event.getTo(), SpawnReason.RESPAWN);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.isCancelled() && plugin.getNPCRegistry().isNPC(event.getEntity())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null || !npc.isProtected())
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        final Entity targeted = event.getTarget();
        NPC npc = plugin.getNPCRegistry().getNPC(targeted);
        final Entity targeter = event.getEntity();
        if (npc != null) {
            final EntityTargetNPCEvent targetNPCEvent = new EntityTargetNPCEvent(event, npc);
            targetNPCEvent.setCancelled(!npc.getOrAddTrait(TargetableTrait.class).isTargetable());
            Bukkit.getPluginManager().callEvent(targetNPCEvent);
            if (targetNPCEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }
            if (event.isCancelled() || !(targeter instanceof Mob))
                return;
            npc.getOrAddTrait(TargetableTrait.class).addTargeter(targeter.getUniqueId());
        } else if (targeter instanceof Mob) {
            final NPC prev = plugin.getNPCRegistry().getNPC(((Mob) targeter).getTarget());
            if (prev == null)
                return;
            prev.getOrAddTrait(TargetableTrait.class).removeTargeter(targeter.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null)
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onNavigationBegin(NavigationBeginEvent event) {
        skinUpdateTracker.onNPCNavigationBegin(event.getNPC());
    }

    @EventHandler
    public void onNavigationComplete(NavigationCompleteEvent event) {
        skinUpdateTracker.onNPCNavigationComplete(event.getNPC());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNPCDespawn(NPCDespawnEvent event) {
        if (event.getReason() == DespawnReason.PLUGIN || event.getReason() == DespawnReason.REMOVAL
                || event.getReason() == DespawnReason.RELOAD) {
            Messaging.idebug(() -> Joiner.on(' ').join("Preventing further respawns of", event.getNPC(),
                    "due to DespawnReason." + event.getReason()));
            toRespawn.values().remove(event.getNPC());
        } else {
            Messaging.idebug(() -> Joiner.on(' ').join("Removing", event.getNPC(),
                    "from skin tracker due to DespawnReason." + event.getReason().name()));
        }
        skinUpdateTracker.onNPCDespawn(event.getNPC());
    }

    @EventHandler
    public void onNPCKnockback(NPCKnockbackEvent event) {
        event.setCancelled(event.getNPC().isProtected());
        if (event.getNPC().data().has(NPC.Metadata.KNOCKBACK)) {
            event.setCancelled(!event.getNPC().data().get(NPC.Metadata.KNOCKBACK, true));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNPCLinkToPlayer(NPCLinkToPlayerEvent event) {
        NPC npc = event.getNPC();
        if (npc.isSpawned()) {
            NMS.markPoseDirty(npc.getEntity());
        }
        if (npc.getEntity() instanceof SkinnableEntity) {
            SkinnableEntity skinnable = (SkinnableEntity) npc.getEntity();
            if (skinnable.getSkinTracker().getSkin() != null) {
                skinnable.getSkinTracker().getSkin().apply(skinnable);
            }
            onNPCPlayerLinkToPlayer(event);
        }
        if (npc.data().has(NPC.Metadata.HOLOGRAM_RENDERER)) {
            HologramRenderer hr = npc.data().get(NPC.Metadata.HOLOGRAM_RENDERER);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> hr.onSeenByPlayer(npc, event.getPlayer()), 2);
        }
    }

    @EventHandler
    public void onNPCNeedsRespawn(NPCNeedsRespawnEvent event) {
        ChunkCoord coord = event.getSpawnLocation();
        if (toRespawn.containsEntry(coord, event.getNPC()))
            return;

        Messaging.debug("Stored", event.getNPC(), "for respawn from NPCNeedsRespawnEvent");
        toRespawn.put(coord, event.getNPC());
    }

    private void onNPCPlayerLinkToPlayer(NPCLinkToPlayerEvent event) {
        Entity tracker = event.getNPC().getEntity();
        boolean resetYaw = event.getNPC().data().get(NPC.Metadata.RESET_YAW_ON_SPAWN,
                Setting.RESET_YAW_ON_SPAWN.asBoolean());
        boolean sendTabRemove = NMS.sendTabListAdd(event.getPlayer(), (Player) tracker);
        if (!sendTabRemove || !event.getNPC().shouldRemoveFromTabList()) {
            NMS.sendPositionUpdate(tracker, ImmutableList.of(event.getPlayer()), false, null, null,
                    NMS.getHeadYaw(tracker));
            if (resetYaw) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                        () -> PlayerAnimation.ARM_SWING.play((Player) tracker, event.getPlayer()));
            }
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (!tracker.isValid() || !event.getPlayer().isValid())
                return;

            NMS.sendTabListRemove(event.getPlayer(), (Player) tracker);
            NMS.sendPositionUpdate(tracker, ImmutableList.of(event.getPlayer()), false, null, null,
                    NMS.getHeadYaw(tracker));
            if (resetYaw) {
                PlayerAnimation.ARM_SWING.play((Player) tracker, event.getPlayer());
            }
        }, Setting.TABLIST_REMOVE_PACKET_DELAY.asTicks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNPCRemove(NPCRemoveEvent event) {
        toRespawn.values().remove(event.getNPC());
    }

    @EventHandler(ignoreCancelled = true)
    public void onNPCSeenByPlayer(NPCSeenByPlayerEvent event) {
        NPC npc = event.getNPC();
        PlayerFilter pf = npc.getTraitNullable(PlayerFilter.class);
        if (pf != null) {
            event.setCancelled(pf.onSeenByPlayer(event.getPlayer()));
        }
        ClickRedirectTrait crt = npc.getTraitNullable(ClickRedirectTrait.class);
        if (crt != null) {
            npc = crt.getRedirectToNPC();
        }
        pf = npc.getTraitNullable(PlayerFilter.class);
        if (pf != null) {
            event.setCancelled(pf.onSeenByPlayer(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNPCSpawn(NPCSpawnEvent event) {
        skinUpdateTracker.onNPCSpawn(event.getNPC());
        Messaging.idebug(() -> Joiner.on(' ').join("Removing respawns of", event.getNPC(),
                "due to SpawnReason." + event.getReason()));
        toRespawn.values().remove(event.getNPC());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        skinUpdateTracker.removePlayer(event.getPlayer().getUniqueId());
        skinUpdateTracker.updatePlayer(event.getPlayer(), 20, true);
        if (plugin.getNPCRegistry().getNPC(event.getPlayer()) == null)
            return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            NMS.replaceTracker(event.getPlayer());
            NMS.removeFromServerPlayerList(event.getPlayer());
        }, 1);
        // on teleport, player NPCs are added to the server player list. this is
        // undesirable as player NPCs are not real players and confuse plugins.
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCreateNPC(PlayerCreateNPCEvent event) {
        checkCreationEvent(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null)
            return;

        if (npc.requiresNameHologram() && event.getDeathMessage() != null) {
            event.setDeathMessage(event.getDeathMessage().replace(event.getEntity().getName(), npc.getFullName()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (plugin.getNPCRegistry().isNPC(event.getCaught())
                && plugin.getNPCRegistry().getNPC(event.getCaught()).isProtected()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getRightClicked());
        if (npc == null || Util.isOffHand(event))
            return;

        ClickRedirectTrait crt = npc.getTraitNullable(ClickRedirectTrait.class);
        if (crt != null && (npc = crt.getRedirectToNPC()) == null)
            return;

        Player player = event.getPlayer();
        NPCRightClickEvent rightClickEvent = new NPCRightClickEvent(npc, player);
        if (event.getPlayer().getItemInHand().getType() == Material.NAME_TAG) {
            rightClickEvent.setCancelled(npc.isProtected());
        }
        Bukkit.getPluginManager().callEvent(rightClickEvent);
        if (rightClickEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        if (npc.hasTrait(CommandTrait.class)) {
            npc.getTraitNullable(CommandTrait.class).dispatch(player, CommandTrait.Hand.RIGHT);
            rightClickEvent.setDelayedCancellation(true);
        }
        if (rightClickEvent.isDelayedCancellation()) {
            event.setCancelled(true);
        }
        if (event.isCancelled()) {
            if (SUPPORT_STOP_USE_ITEM) {
                try {
                    PlayerAnimation.STOP_USE_ITEM.play(player);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                            () -> PlayerAnimation.STOP_USE_ITEM.play(player));
                } catch (UnsupportedOperationException e) {
                    SUPPORT_STOP_USE_ITEM = false;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        skinUpdateTracker.updatePlayer(event.getPlayer(), Setting.INITIAL_PLAYER_JOIN_SKIN_PACKET_DELAY.asTicks(),
                true);
        plugin.getLocationLookup().onJoin(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLeashEntity(PlayerLeashEntityEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null)
            return;

        boolean leashProtected = npc.isProtected();
        if (npc.data().get(NPC.Metadata.LEASH_PROTECTED, leashProtected)) {
            event.setCancelled(true);
        }
    }

    // recalculate player NPCs the first time a player moves and every time
    // a player moves a certain distance from their last position.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        skinUpdateTracker.onPlayerMove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Editor.leave(event.getPlayer());
        if (event.getPlayer().isInsideVehicle()) {
            NPC npc = plugin.getNPCRegistry().getNPC(event.getPlayer().getVehicle());
            if (npc != null) {
                event.getPlayer().leaveVehicle();
            }
        }
        skinUpdateTracker.removePlayer(event.getPlayer().getUniqueId());
        plugin.getLocationLookup().onQuit(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        skinUpdateTracker.updatePlayer(event.getPlayer(), 15, true);
    }

    @EventHandler
    public void onPlayerShearEntityEvent(PlayerShearEntityEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null)
            return;
        if (npc.isProtected()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getPlayer());
        if (event.getCause() == TeleportCause.PLUGIN && npc != null && !npc.data().has("citizens-force-teleporting")
                && Setting.PLAYER_TELEPORT_DELAY.asTicks() > 0) {
            event.setCancelled(true);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                npc.data().set("citizens-force-teleporting", true);
                event.getPlayer().teleport(event.getTo());
                npc.data().remove("citizens-force-teleporting");
            }, Setting.PLAYER_TELEPORT_DELAY.asTicks());
        }
        skinUpdateTracker.updatePlayer(event.getPlayer(), 15, true);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        // hack: Spigot now unloads plugin classes on disable in reverse order so prefer unloading at the start of
        // plugin disable cycle
        PluginDescriptionFile file = event.getPlugin().getDescription();
        for (String depend : Iterables.concat(file.getDepend(), file.getSoftDepend())) {
            if (depend.equalsIgnoreCase("citizens") && plugin.isEnabled()) {
                plugin.onDependentPluginDisable();
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplashEvent(PotionSplashEvent event) {
        for (LivingEntity entity : event.getAffectedEntities()) {
            NPC npc = plugin.getNPCRegistry().getNPC(entity);
            if (npc == null)
                continue;

            if (npc.isProtected()) {
                event.setIntensity(entity, 0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getHitEntity());
        if (npc != null && npc.isProtected() && event.getEntityType().name().contains("WIND_CHARGE")) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof FishHook))
            return;
        NMS.removeHookIfNecessary((FishHook) event.getEntity());
        new BukkitRunnable() {
            int n = 0;

            @Override
            public void run() {
                if (n++ > 5 || !plugin.isEnabled()) {
                    cancel();
                    return;
                }
                NMS.removeHookIfNecessary((FishHook) event.getEntity());
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onVehicleDamage(VehicleDamageEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getVehicle());
        if (npc == null)
            return;

        event.setCancelled(npc.isProtected());

        NPCVehicleDamageEvent damageEvent = new NPCVehicleDamageEvent(npc, event);
        Bukkit.getPluginManager().callEvent(damageEvent);

        if (!damageEvent.isCancelled() || !(damageEvent.getDamager() instanceof Player))
            return;

        Player damager = (Player) damageEvent.getDamager();

        NPCLeftClickEvent leftClickEvent = new NPCLeftClickEvent(npc, damager);
        Bukkit.getPluginManager().callEvent(leftClickEvent);
        if (npc.hasTrait(CommandTrait.class)) {
            npc.getTraitNullable(CommandTrait.class).dispatch(damager, CommandTrait.Hand.LEFT);
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getVehicle());
        if (npc == null)
            return;

        event.setCancelled(npc.isProtected());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        NPC npc = plugin.getNPCRegistry().getNPC(event.getVehicle());
        NPC rider = plugin.getNPCRegistry().getNPC(event.getEntered());
        if (npc == null) {
            if (rider != null && rider.isProtected() && (event.getVehicle().getType().name().contains("BOAT")
                    || event.getVehicle() instanceof Minecart)) {
                event.setCancelled(true);
            }
            return;
        }
        if (rider != null || !(npc instanceof Vehicle))
            return;

        if (!npc.hasTrait(Controllable.class) || !npc.getTraitNullable(Controllable.class).isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        for (NPC npc : getAllNPCs()) {
            if (npc == null || !npc.isSpawned() || !npc.getEntity().getWorld().equals(event.getWorld()))
                continue;

            boolean despawned = npc.despawn(DespawnReason.WORLD_UNLOAD);
            if (event.isCancelled() || !despawned) {
                for (ChunkCoord coord : Lists.newArrayList(toRespawn.keySet())) {
                    if (event.getWorld().getUID().equals(coord.worldUUID)) {
                        respawnAllFromCoord(coord, event);
                    }
                }
                event.setCancelled(true);
                return;
            }
            if (npc.isSpawned()) {
                toRespawn.put(new ChunkCoord(npc.getEntity().getLocation()), npc);
                Messaging.debug("Despawned", npc, "due to world unload at", event.getWorld().getName());
            }
        }
        plugin.getLocationLookup().onWorldUnload(event);
    }

    private void registerKnockbackEvent(Class<?> kbc) {
        try {
            HandlerList handlers = (HandlerList) kbc.getMethod("getHandlerList").invoke(null);
            Method getEntity = kbc.getMethod("getEntity");
            Method getHitBy = kbc.getMethod("getHitBy");
            Method getKnockbackStrength = kbc.getMethod("getKnockbackStrength");
            Method getAcceleration = kbc.getMethod("getAcceleration");
            handlers.register(new RegisteredListener(new Listener() {
            }, (listener, event) -> {
                try {
                    if (event.getClass() != kbc)
                        return;
                    Entity entity = (Entity) getEntity.invoke(event);
                    if (!(entity instanceof NPCHolder))
                        return;
                    NPC npc = ((NPCHolder) entity).getNPC();
                    Entity hitBy = (Entity) getHitBy.invoke(event);
                    float strength = (float) getKnockbackStrength.invoke(event);
                    Vector vector = (Vector) getAcceleration.invoke(event);
                    NPCKnockbackEvent kb = new NPCKnockbackEvent(npc, strength, vector, hitBy);
                    Bukkit.getPluginManager().callEvent(kb);
                    ((Cancellable) event).setCancelled(kb.isCancelled());
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }, EventPriority.NORMAL, plugin, true));
        } catch (Throwable ex) {
            Messaging.severe("Error registering knockback event forwarder");
            ex.printStackTrace();
        }
    }

    private void registerMoveEvent(Class<?> clazz) {
        try {
            final HandlerList handlers = (HandlerList) clazz.getMethod("getHandlerList").invoke(null);
            final Method getEntity = clazz.getMethod("getEntity");
            final Method getFrom = clazz.getMethod("getFrom");
            final Method getTo = clazz.getMethod("getTo");
            handlers.register(new RegisteredListener(new Listener() {
            }, (listener, event) -> {
                if (NPCMoveEvent.getHandlerList().getRegisteredListeners().length == 0 || event.getClass() != clazz)
                    return;
                try {
                    final Entity entity = (Entity) getEntity.invoke(event);
                    if (!(entity instanceof NPCHolder))
                        return;
                    final NPC npc = ((NPCHolder) entity).getNPC();
                    final Location from = (Location) getFrom.invoke(event);
                    final Location to = (Location) getTo.invoke(event);
                    final NPCMoveEvent npcMoveEvent = new NPCMoveEvent(npc, from, to.clone());
                    Bukkit.getPluginManager().callEvent(npcMoveEvent);
                    if (!npcMoveEvent.isCancelled()) {
                        final Location eventTo = npcMoveEvent.getTo();
                        if (!to.equals(eventTo)) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> entity.teleport(eventTo), 1L);
                        }
                    } else {
                        final Location eventFrom = npcMoveEvent.getFrom();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> entity.teleport(eventFrom), 1L);
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }, EventPriority.NORMAL, plugin, true));
        } catch (Throwable ex) {
            Messaging.severe("Error registering move event forwarder");
            ex.printStackTrace();
        }
    }

    private void registerPushEvent(Class<?> clazz) {
        try {
            HandlerList handlers = (HandlerList) clazz.getMethod("getHandlerList").invoke(null);
            Method getEntity = clazz.getMethod("getEntity");
            Method getPushedBy = clazz.getMethod("getPushedBy");
            Method getAcceleration = clazz.getMethod("getAcceleration");
            handlers.register(new RegisteredListener(new Listener() {
            }, (listener, event) -> {
                if (NPCPushEvent.getHandlerList().getRegisteredListeners().length == 0 || event.getClass() != clazz)
                    return;
                try {
                    Entity entity = (Entity) getEntity.invoke(event);
                    if (!(entity instanceof NPCHolder))
                        return;
                    NPC npc = ((NPCHolder) entity).getNPC();
                    Entity pushedBy = (Entity) getPushedBy.invoke(event);
                    Vector vector = (Vector) getAcceleration.invoke(event);
                    NPCPushEvent push = new NPCPushEvent(npc, vector, pushedBy);
                    if (pushedBy == null && !npc.data().get(NPC.Metadata.COLLIDABLE, !npc.isProtected())) {
                        push.setCancelled(true);
                    }
                    Bukkit.getPluginManager().callEvent(push);
                    ((Cancellable) event).setCancelled(push.isCancelled());
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }, EventPriority.NORMAL, plugin, true));
        } catch (Throwable ex) {
            Messaging.severe("Error registering push event forwarder");
            ex.printStackTrace();
        }
    }

    private void respawnAllFromCoord(ChunkCoord coord, Event event) {
        List<NPC> ids = Lists.newArrayList(toRespawn.get(coord));
        if (ids.size() > 0) {
            Messaging.idebug(() -> Joiner.on(' ').join("Respawning all NPCs at", coord, "due to", event, "at", coord));
        }
        for (int i = 0; i < ids.size(); i++) {
            NPC npc = ids.get(i);
            if (npc.getOwningRegistry().getById(npc.getId()) != npc) {
                Messaging.idebug(() -> "Prevented deregistered NPC from respawning " + npc);
                continue;
            }
            if (npc.isSpawned()) {
                Messaging.idebug(() -> "Can't respawn NPC " + npc + ": already spawned");
                continue;
            }
            boolean success = spawn(npc);
            if (!success) {
                ids.remove(i--);
                Messaging.idebug(() -> Joiner.on(' ').join("Couldn't respawn", npc, "during", event, "at", coord));
                continue;
            }
            Messaging.idebug(() -> Joiner.on(' ').join("Spawned", npc, "during", event, "at", coord));
        }
        for (NPC npc : ids) {
            toRespawn.remove(coord, npc);
        }
    }

    private boolean spawn(NPC npc) {
        Location spawn = npc.getOrAddTrait(CurrentLocation.class).getLocation();
        if (spawn == null) {
            Messaging.idebug(() -> "Couldn't find a spawn location for despawned NPC " + npc);
            return false;
        }
        return npc.spawn(spawn, SpawnReason.CHUNK_LOAD);
    }

    private void unloadNPCs(ChunkEvent event, List<NPC> toDespawn) {
        ChunkCoord coord = new ChunkCoord(event.getChunk());
        boolean loadChunk = false;
        for (NPC npc : toDespawn) {
            if (toRespawn.containsValue(npc))
                continue;
            if (!npc.despawn(DespawnReason.CHUNK_UNLOAD)) {
                if (!(event instanceof Cancellable)) {
                    Messaging.idebug(() -> Joiner.on(' ').join("Reloading chunk because", npc, "couldn't despawn"));
                    loadChunk = true;
                    toRespawn.put(coord, npc);
                    continue;
                }
                ((Cancellable) event).setCancelled(true);
                Messaging.idebug(() -> "Cancelled chunk unload at " + coord);
                respawnAllFromCoord(coord, event);
                return;
            }
            toRespawn.put(coord, npc);
            Messaging.idebug(() -> Joiner.on(' ').join("Despawned", npc, "due to chunk unload at", coord));
        }
        if (Messaging.isDebugging() && Setting.DEBUG_CHUNK_LOADS.asBoolean()) {
            new Exception("CITIZENS CHUNK UNLOAD DEBUG " + coord).printStackTrace();
        }
        if (loadChunk) {
            Messaging.idebug(() -> Joiner.on(' ').join("Loading chunk in 10 ticks due to forced chunk load at", coord));
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (!event.getChunk().isLoaded()) {
                    event.getChunk().load();
                }
            }, 10);
        }
    }

    private static boolean SUPPORT_STOP_USE_ITEM = true;
}