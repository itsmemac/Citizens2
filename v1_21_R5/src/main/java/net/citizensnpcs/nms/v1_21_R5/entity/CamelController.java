package net.citizensnpcs.nms.v1_21_R5.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R5.CraftServer;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftCamel;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftEntity;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_21_R5.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_21_R5.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_21_R5.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.trait.Controllable;
import net.citizensnpcs.trait.HorseModifiers;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CamelController extends MobEntityController {
    public CamelController() {
        super(EntityCamelNPC.class, EntityType.CAMEL);
    }

    @Override
    public void create(Location at, NPC npc) {
        npc.getOrAddTrait(HorseModifiers.class);
        super.create(at, npc);
    }

    @Override
    public org.bukkit.entity.Camel getBukkitEntity() {
        return (org.bukkit.entity.Camel) super.getBukkitEntity();
    }

    public static class CamelNPC extends CraftCamel implements ForwardingNPCHolder {
        public CamelNPC(EntityCamelNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }

    public static class EntityCamelNPC extends Camel implements NPCHolder {
        private double baseMovementSpeed;

        private final CitizensNPC npc;
        private boolean riding;

        public EntityCamelNPC(EntityType<? extends Camel> types, Level level) {
            this(types, level, null);
        }

        public EntityCamelNPC(EntityType<? extends Camel> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                ((org.bukkit.entity.Camel) getBukkitEntity())
                        .setDomestication(((org.bukkit.entity.Camel) getBukkitEntity()).getMaxDomestication());
                baseMovementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
            }
        }

        @Override
        public boolean broadcastToPlayer(ServerPlayer player) {
            return NMS.shouldBroadcastToPlayer(npc, () -> super.broadcastToPlayer(player));
        }

        @Override
        protected boolean canRide(Entity entity) {
            if (npc != null && (entity instanceof Boat || entity instanceof AbstractMinecart))
                return !npc.isProtected();
            return super.canRide(entity);
        }

        @Override
        public boolean causeFallDamage(double f, float f1, DamageSource damagesource) {
            if (npc == null || !npc.isFlyable())
                return super.causeFallDamage(f, f1, damagesource);
            return false;
        }

        @Override
        public void checkDespawn() {
            if (npc == null) {
                super.checkDespawn();
            }
        }

        @Override
        protected void checkFallDamage(double d0, boolean flag, BlockState iblockdata, BlockPos blockposition) {
            if (npc == null || !npc.isFlyable()) {
                super.checkFallDamage(d0, flag, iblockdata, blockposition);
            }
        }

        @Override
        public void customServerAiStep(ServerLevel level) {
            super.customServerAiStep(level);
            if (npc != null) {
                NMSImpl.updateMinecraftAIState(npc, this);
                if (npc.hasTrait(Controllable.class) && npc.getOrAddTrait(Controllable.class).isEnabled()) {
                    riding = getBukkitEntity().getPassengers().size() > 0;
                    getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                            baseMovementSpeed * npc.getNavigator().getDefaultParameters().speedModifier());
                } else {
                    riding = false;
                }
                if (riding) {
                    if (npc.getNavigator().isNavigating()) {
                        org.bukkit.entity.Entity basePassenger = passengers.get(0).getBukkitEntity();
                        NMS.look(basePassenger, getYRot(), getXRot());
                    }
                    setFlag(4, true); // datawatcher method
                }
                NMS.setStepHeight(getBukkitEntity(), 1);
                npc.update();
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return NMSImpl.getSoundEffect(npc, super.getAmbientSound(), NPC.Metadata.AMBIENT_SOUND);
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new CamelNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        protected SoundEvent getDeathSound() {
            return NMSImpl.getSoundEffect(npc, super.getDeathSound(), NPC.Metadata.DEATH_SOUND);
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource damagesource) {
            return NMSImpl.getSoundEffect(npc, super.getHurtSound(damagesource), NPC.Metadata.HURT_SOUND);
        }

        @Override
        public float getJumpPower() {
            return NMS.getJumpPower(npc, super.getJumpPower());
        }

        @Override
        public int getMaxFallDistance() {
            return NMS.getFallDistance(npc, super.getMaxFallDistance());
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public PushReaction getPistonPushReaction() {
            return Util.callPistonPushEvent(npc) ? PushReaction.IGNORE : super.getPistonPushReaction();
        }

        @Override
        public boolean canSimulateMovement() {
            if (npc != null && riding)
                return true;
            return super.canSimulateMovement();
        }

        @Override
        public boolean isLeashed() {
            return NMSImpl.isLeashed(npc, super::isLeashed, this);
        }

        @Override
        public boolean isPushable() {
            return npc == null ? super.isPushable()
                    : npc.data().<Boolean> get(NPC.Metadata.COLLIDABLE, !npc.isProtected());
        }

        @Override
        public boolean isVehicle() {
            return npc != null && npc.getNavigator().isNavigating() ? false : super.isVehicle();
        }

        @Override
        public void knockback(double strength, double dx, double dz) {
            NMS.callKnockbackEvent(npc, (float) strength, dx, dz, evt -> super.knockback((float) evt.getStrength(),
                    evt.getKnockbackVector().getX(), evt.getKnockbackVector().getZ()));
        }

        @Override
        protected AABB makeBoundingBox(Vec3 vec3) {
            return NMSBoundingBox.makeBB(npc, super.makeBoundingBox(vec3));
        }

        @Override
        public boolean onClimbable() {
            if (npc == null || !npc.isFlyable())
                return super.onClimbable();
            else
                return false;
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> datawatcherobject) {
            if (npc == null) {
                super.onSyncedDataUpdated(datawatcherobject);
                return;
            }
            NMSImpl.checkAndUpdateHeight(this, datawatcherobject, super::onSyncedDataUpdated);
        }

        @Override
        public void push(Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.push(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean save(ValueOutput save) {
            return npc == null ? super.save(save) : false;
        }

        @Override
        public Entity teleport(TeleportTransition transition) {
            if (npc == null)
                return super.teleport(transition);
            return NMSImpl.teleportAcrossWorld(this, transition);
        }

        @Override
        public void travel(Vec3 vec3d) {
            if (npc == null || !npc.isFlyable()) {
                super.travel(vec3d);
            } else {
                NMSImpl.moveLogic(this, vec3d);
            }
        }

        @Override
        public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> tagkey, double d0) {
            if (npc == null)
                return super.updateFluidHeightAndDoFluidPushing(tagkey, d0);
            Vec3 old = getDeltaMovement().add(0, 0, 0);
            boolean res = super.updateFluidHeightAndDoFluidPushing(tagkey, d0);
            if (!npc.isPushableByFluids()) {
                setDeltaMovement(old);
            }
            return res;
        }
    }
}
