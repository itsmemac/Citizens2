package net.citizensnpcs.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;

import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.command.CommandManager;
import net.citizensnpcs.api.command.exception.CommandException;
import net.citizensnpcs.api.npc.BlockBreaker;
import net.citizensnpcs.api.npc.BlockBreaker.BlockBreakerConfiguration;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.BoundingBox;
import net.citizensnpcs.api.util.EntityDim;
import net.citizensnpcs.api.util.SpigotUtil.InventoryViewAPI;
import net.citizensnpcs.npc.ai.MCNavigationStrategy.MCNavigator;
import net.citizensnpcs.npc.ai.MCTargetStrategy.TargetNavigator;
import net.citizensnpcs.trait.EntityPoseTrait.EntityPose;
import net.citizensnpcs.trait.MirrorTrait;
import net.citizensnpcs.trait.versioned.ArmadilloTrait.ArmadilloState;
import net.citizensnpcs.trait.versioned.CamelTrait.CamelPose;
import net.citizensnpcs.trait.versioned.SnifferTrait.SnifferState;
import net.citizensnpcs.util.EntityPacketTracker.PacketAggregator;
import net.citizensnpcs.util.NMS.MinecraftNavigationType;

public interface NMSBridge {
    default void activate(Entity entity) {
    }

    public boolean addEntityToWorld(Entity entity, SpawnReason custom);

    public void addOrRemoveFromPlayerList(Entity entity, boolean remove);

    public void attack(LivingEntity attacker, LivingEntity target);

    public void cancelMoveDestination(Entity entity);

    public boolean canNavigateTo(Entity entity, Location dest, NavigatorParameters params);

    public default Iterable<Object> createBundlePacket(List<Object> packets) {
        return packets;
    }

    public EntityPacketTracker createPacketTracker(Entity entity, PacketAggregator agg);

    public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) throws Throwable;

    public BlockBreaker getBlockBreaker(Entity entity, Block targetBlock, BlockBreakerConfiguration config);

    public default Object getBossBar(Entity entity) {
        throw new UnsupportedOperationException();
    }

    public BoundingBox getBoundingBox(Entity handle);

    public default double getBoundingBoxHeight(Entity entity) {
        return entity.getHeight();
    }

    public BoundingBox getCollisionBox(Block block);

    public default BoundingBox getCollisionBox(Object blockdata) {
        return BoundingBox.ONE;
    }

    public default Map<String, Object> getComponentMap(ItemStack item) {
        return item.getItemMeta().serialize();
    }

    public Location getDestination(Entity entity);

    public float getForwardBackwardMovement(Entity entity);

    public GameProfileRepository getGameProfileRepository();

    public float getHeadYaw(Entity entity);

    public float getMovementSpeed(Entity entity);

    public EntityPacketTracker getPacketTracker(Entity entity);

    public List<Entity> getPassengers(Entity entity);

    public GameProfile getProfile(Player player);

    public GameProfile getProfile(SkullMeta meta);

    public default float getRidingHeightOffset(Entity entity, Entity mount) {
        return 0;
    }

    public String getSoundPath(Sound flag) throws CommandException;

    public Entity getSource(BlockCommandSender sender);

    public float getStepHeight(Entity entity);

    public TargetNavigator getTargetNavigator(Entity handle, Entity target, NavigatorParameters parameters);

    public MCNavigator getTargetNavigator(Entity entity, Iterable<Vector> dest, NavigatorParameters params);

    public MCNavigator getTargetNavigator(Entity entity, Location dest, NavigatorParameters params);

    public Entity getVehicle(Entity entity);

    public default Collection<Player> getViewingPlayers(Entity entity) {
        return entity.getTrackedBy();
    }

    public double getWidth(Entity entity);

    public float getXZMovement(Entity entity);

    public float getYaw(Entity entity);

    public boolean isOnGround(Entity entity);

    public default boolean isSneaking(Entity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).isSneaking();
        }
        return false;
    }

    public boolean isSolid(Block in);

    public default boolean isSprinting(Entity entity) {
        return entity instanceof Player ? ((Player) entity).isSprinting() : false;
    }

    public boolean isValid(Entity entity);

    public void load(CommandManager commands);

    public void look(Entity from, Entity to);

    public void look(Entity entity, float yaw, float pitch);

    public void look(Entity entity, Location to, boolean headOnly, boolean immediate);

    public default void markPoseDirty(Entity tracker) {
    }

    public void mount(Entity entity, Entity passenger);

    public default void onPlayerInfoAdd(Player player, Object source, Function<UUID, MirrorTrait> mirrorTraits) {
    }

    public InventoryView openAnvilInventory(Player player, Inventory anvil, String title);

    public void openHorseInventory(Tameable horse, Player equipper);

    public void playAnimation(PlayerAnimation animation, Player player, Iterable<Player> to);

    public Runnable playerTicker(NPC npc, Player entity);

    public default void positionInteractionText(Player player, Entity interaction, Entity mount, double height) {
    }

    public void registerEntityClass(Class<?> clazz, Object type);

    public void remove(Entity entity);

    public void removeFromServerPlayerList(Player player);

    public void removeFromWorld(org.bukkit.entity.Entity entity);

    public void removeHookIfNecessary(FishHook entity);

    public void replaceTrackerEntry(Entity entity);

    public void sendComponent(Player player, Object component);

    public void sendPositionUpdate(Entity from, Collection<Player> to, boolean position, Float bodyYaw, Float pitch,
            Float headYaw);

    public boolean sendTabListAdd(Player recipient, Player listPlayer);

    public void sendTabListRemove(Player recipient, Collection<Player> players);

    public void sendTeamPacket(Player recipient, Team team, int mode);

    default public void setAggressive(Entity entity, boolean aggro) {
    }

    public default void setAllayDancing(Entity entity, boolean dancing) {
        throw new UnsupportedOperationException();
    }

    public default void setArmadilloState(Entity entity, ArmadilloState state) {
    }

    public void setBodyYaw(Entity entity, float yaw);

    public void setBoundingBox(Entity entity, BoundingBox box);

    public default void setCamelPose(Entity entity, CamelPose pose) {
        throw new UnsupportedOperationException();
    }

    public void setCustomName(Entity entity, Object component, String string);

    public void setDestination(Entity entity, double x, double y, double z, float speed);

    public void setDimensions(Entity entity, EntityDim desired);

    public void setEndermanAngry(Enderman enderman, boolean angry);

    public void setHeadAndBodyYaw(Entity entity, float yaw);

    public void setHeadYaw(Entity entity, float yaw);

    public void setKnockbackResistance(LivingEntity entity, double d);

    public void setLocationDirectly(Entity entity, Location location);

    public default void setLyingDown(Entity cat, boolean lying) {
        throw new UnsupportedOperationException();
    }

    public void setNavigationTarget(Entity handle, Entity target, float speed);

    public void setNavigationType(Entity entity, MinecraftNavigationType type);

    public void setNoGravity(Entity entity, boolean nogravity);

    public void setOpWithoutSaving(Player player, boolean op);

    public default void setPandaSitting(Entity entity, boolean sitting) {
        throw new UnsupportedOperationException();
    }

    public default void setPeekShulker(Entity entity, int peek) {
        throw new UnsupportedOperationException();
    }

    public default void setPiglinDancing(Entity entity, boolean dancing) {
        throw new UnsupportedOperationException();
    }

    public void setPitch(Entity entity, float pitch);

    public default void setPolarBearRearing(Entity entity, boolean rearing) {
        throw new UnsupportedOperationException();
    }

    public default void setPose(Entity entity, EntityPose pose) {
    }

    public void setProfile(SkullMeta meta, GameProfile profile);

    public void setShouldJump(Entity entity);

    public void setSitting(Ocelot ocelot, boolean sitting);

    public void setSitting(Tameable tameable, boolean sitting);

    public default void setSneaking(Entity entity, boolean sneaking) {
        if (entity instanceof Player) {
            ((Player) entity).setSneaking(sneaking);
        }
    }

    public default void setSnifferState(Entity entity, SnifferState state) {
    }

    public void setStepHeight(Entity entity, float height);

    public default void setTeamNameTagVisible(Team team, boolean visible) {
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, visible ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
    }

    public default void setTextDisplayComponent(Entity entity, Object component) {
    }

    public void setVerticalMovement(Entity bukkitEntity, double d);

    public default void setWardenPose(Entity entity, Object pose) {
    }

    public default void setWitherInvulnerableTicks(Wither wither, int ticks) {
        wither.setInvulnerabilityTicks(ticks);
    }

    public boolean shouldJump(Entity entity);

    public void shutdown();

    public void sleep(Player entity, boolean sleep);

    public void trySwim(Entity entity);

    public void trySwim(Entity entity, float power);

    public void updateInventoryTitle(Player player, InventoryViewAPI view, String newTitle);

    public void updateNavigationWorld(Entity entity, World world);

    public void updatePathfindingRange(NPC npc, float pathfindingRange);
}
