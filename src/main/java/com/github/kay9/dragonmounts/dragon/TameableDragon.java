package com.github.kay9.dragonmounts.dragon;

import com.github.kay9.dragonmounts.DMLConfig;
import com.github.kay9.dragonmounts.DMLRegistry;
import com.github.kay9.dragonmounts.DragonMountsLegacy;
import com.github.kay9.dragonmounts.client.DragonAnimator;
import com.github.kay9.dragonmounts.client.KeyMappings;
import com.github.kay9.dragonmounts.client.MountCameraManager;
import com.github.kay9.dragonmounts.client.MountControlsMessenger;
import com.github.kay9.dragonmounts.data.CrossBreedingManager;
import com.github.kay9.dragonmounts.dragon.abilities.Ability;
import com.github.kay9.dragonmounts.dragon.ai.DragonBodyController;
import com.github.kay9.dragonmounts.dragon.ai.DragonBreedGoal;
import com.github.kay9.dragonmounts.dragon.ai.DragonFollowOwnerGoal;
import com.github.kay9.dragonmounts.dragon.ai.DragonMoveController;
import com.github.kay9.dragonmounts.dragon.egg.HatchableEggBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SaddleItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fluids.FluidType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;

/**
 * Here be dragons.
 * <p>
 * Let the legacy live on.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 * @author Kay9
 */
@SuppressWarnings({"deprecation", "SameReturnValue"})
public class TameableDragon extends TamableAnimal implements Saddleable, FlyingAnimal, PlayerRideable, VariantHolder<Holder<DragonBreed>>
{
    // base attributes
    public static final double BASE_SPEED_GROUND = 0.3; // actual speed varies from ground friction
    public static final double BASE_SPEED_FLYING = 0.32;
    public static final double BASE_DAMAGE = 8;
    public static final double BASE_HEALTH = 60;
    public static final double BASE_FOLLOW_RANGE = 16;
    public static final int BASE_KB_RESISTANCE = 1;
    public static final float BASE_WIDTH = 2.75f; // adult sizes
    public static final float BASE_HEIGHT = 2.75f;
    public static final int BASE_REPRO_LIMIT = 2;
    public static final int BASE_GROWTH_TIME = 72000;
    public static final float BASE_SIZE_MODIFIER = 1.0f;

    // data value IDs
    public static final EntityDataSerializer<Optional<Holder<DragonBreed>>> DRAGON_BREED_SERIALIZER = EntityDataSerializer.forValueType(DragonBreed.STREAM_CODEC.apply(ByteBufCodecs::optional));
    private static final EntityDataAccessor<Optional<Holder<DragonBreed>>> DATA_BREED = SynchedEntityData.defineId(TameableDragon.class, DRAGON_BREED_SERIALIZER);
    private static final EntityDataAccessor<Boolean> DATA_SADDLED = SynchedEntityData.defineId(TameableDragon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(TameableDragon.class, EntityDataSerializers.INT);

    // data NBT IDs
    public static final String NBT_BREED = "Breed";
    private static final String NBT_SADDLED = "Saddle";
    private static final String NBT_REPRO_COUNT = "ReproCount";

    // other constants
    public static final int AGE_UPDATE_INTERVAL = 100; // every 5 seconds
    public static final ResourceLocation AGE_ATTR_MODIFIER_ID = DragonMountsLegacy.id("age_scale_modifier");
    public static final int GROUND_CLEARENCE_THRESHOLD = 3; // height in blocks (multiplied by scale of dragon)
    private final EntityDimensions SITTING_DIMENSIONS = EntityDimensions.scalable(BASE_WIDTH, 2.15f).withEyeHeight(2.58f);

    // server/client delegates
    private final DragonAnimator animator;
    private final List<Ability> abilities = new ArrayList<>();
    private int reproductionCount;
    private float ageProgress = 1; // default to adult
    private boolean flying;
    private boolean nearGround;

    private final GroundPathNavigation groundNavigation;
    private final FlyingPathNavigation flyingNavigation;

    public TameableDragon(EntityType<? extends TameableDragon> type, Level level)
    {
        super(type, level);

        noCulling = true;

        moveControl = new DragonMoveController(this);
        animator = level.isClientSide? new DragonAnimator(this) : null;

        flyingNavigation = new FlyingPathNavigation(this, level);
        groundNavigation = new GroundPathNavigation(this, level);

        flyingNavigation.setCanFloat(true);
        groundNavigation.setCanFloat(true);

        navigation = groundNavigation;
    }

    @Override
    @NotNull
    public BodyRotationControl createBodyControl()
    {
        return new DragonBodyController(this);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(MOVEMENT_SPEED, BASE_SPEED_GROUND)
                .add(MAX_HEALTH, BASE_HEALTH)
                .add(FOLLOW_RANGE, BASE_FOLLOW_RANGE)
                .add(KNOCKBACK_RESISTANCE, BASE_KB_RESISTANCE)
                .add(ATTACK_DAMAGE, BASE_DAMAGE)
                .add(FLYING_SPEED, BASE_SPEED_FLYING);
    }

    @Override
    protected void registerGoals() // TODO: Much Smarter AI and features
    {
//        goalSelector.addGoal(1, new DragonLandGoal(this));
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1, true));
//        goalSelector.addGoal(4, new DragonBabuFollowParent(this, 10));
        goalSelector.addGoal(5, new DragonFollowOwnerGoal(this, 1f, 10f, 3.5f, 32f));
        goalSelector.addGoal(5, new DragonBreedGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.85f));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, LivingEntity.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(1, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
        targetSelector.addGoal(3, new NonTameRandomTargetGoal<>(this, Animal.class, false, e -> !(e instanceof TameableDragon)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);

        builder.define(DATA_BREED, Optional.empty());
        builder.define(DATA_SADDLED, false);
        builder.define(DATA_AGE, 0); // default to adult stage
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data)
    {
        if (DATA_BREED.equals(data))
        {
            getBreed().initialize(this);
            updateAgeProperties();
        }
        else if (DATA_FLAGS_ID.equals(data)) refreshDimensions();
        else if (DATA_AGE.equals(data)) updateAgeProperties();
        else super.onSyncedDataUpdated(data);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound)
    {
        super.addAdditionalSaveData(compound);

        if (getBreedHolder() != null) // entity creation sometimes looks to merge already existing data, which we don't have at creation...
        {
            compound.putString(NBT_BREED, getBreedHolder().getRegisteredName());
            for (var ability : getAbilities()) ability.write(this, compound);
        }

        compound.putBoolean(NBT_SADDLED, isSaddled());
        compound.putInt(NBT_REPRO_COUNT, reproductionCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound)
    {
        // read and set breed first before reading everything else so things can override correctly,
        // e.g. attributes.
        Holder<DragonBreed> breed = DragonBreed.parse(compound.getString(NBT_BREED), registryAccess());
        if (breed == null)
            breed = DragonBreed.getRandom(registryAccess(), getRandom()); // wasn't assigned one (summon command?)

        setBreed(breed);

        super.readAdditionalSaveData(compound);

        setSaddled(compound.getBoolean(NBT_SADDLED));
        this.reproductionCount = compound.getInt(NBT_REPRO_COUNT);

        for (var ability : getAbilities()) ability.read(this, compound);

        // set sync age data after we read it in AgeableMob
        getEntityData().set(DATA_AGE, getAge());
    }

    public void setBreed(Holder<DragonBreed> dragonBreed)
    {
        if (getBreedHolder() != null)
            throw new IllegalStateException("Trying to set breed of a dragon that already has one!");

        getEntityData().set(DATA_BREED, Optional.of(dragonBreed));
    }

    /**
     * Since a breed type cannot be passed into the constructor (due to the dynamic nature of breeds)
     * and sometimes a breed type cannot be deserialized in time, there's always the possibility of
     * a nullable breed.
     */
    @Nullable
    public DragonBreed getBreed()
    {
        if (getBreedHolder() == null) return null;
        return getBreedHolder().get();
    }

    /**
     * Nullable: The breed type may not be available pre-deserialization or the client hasn't received a data sync
     * yet.
     * @return The holder of this dragon's breed type.
     */
    @Nullable
    public Holder<DragonBreed> getBreedHolder()
    {
        return getEntityData().get(DATA_BREED).orElse(null);
    }

    @Override
    public void setVariant(Holder<DragonBreed> variant)
    {
        setBreed(variant);
    }

    @Override
    public Holder<DragonBreed> getVariant()
    {
        return getBreedHolder();
    }

    public List<Ability> getAbilities()
    {
        return abilities;
    }

    /**
     * Returns true if the dragon is saddled.
     */
    public boolean isSaddled()
    {
        return entityData.get(DATA_SADDLED);
    }

    @Override
    public boolean isSaddleable()
    {
        return isAlive() && !isHatchling() && isTame();
    }


    @Override
    public void equipSaddle(ItemStack itemStack, @org.jetbrains.annotations.Nullable SoundSource soundSource)
    {
        setSaddled(true);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.HORSE_SADDLE, getSoundSource(), 1, 1);
    }

    /**
     * Set or remove the saddle of the dragon.
     */
    public void setSaddled(boolean saddled)
    {
        entityData.set(DATA_SADDLED, saddled);
    }

    public void addReproCount()
    {
        reproductionCount++;
    }

    public boolean canFly()
    {
        // hatchling's can't fly
        return !isHatchling();
    }

    public boolean shouldFly()
    {
        if (isFlying()) return !onGround(); // more natural landings
        return canFly() && !isInWater() && !isNearGround();
    }

    /**
     * Returns true if the entity is flying.
     */
    public boolean isFlying()
    {
        return flying;
    }

    /**
     * Set the flying flag of the entity.
     */
    public void setFlying(boolean flying)
    {
        this.flying = flying;
    }

    public boolean isNearGround()
    {
        return nearGround;
    }

    public void setNavigation(boolean flying)
    {
            navigation = flying ?
                    flyingNavigation :
                    groundNavigation;
    }

    @Override
    public void tick()
    {
        super.tick();

        if (isServer())
        {
            // periodically sync age data back to client
            if (!isAdult() && tickCount % AGE_UPDATE_INTERVAL == 0) entityData.set(DATA_AGE, age);

            // heal randomly
            if (isAlive() && getRandom().nextFloat() < 0.001) heal(1f);
        }
        else
        {
            // update animations on the client
            animator.tick();

            // because vanilla age does not increment on client...
            int age = getAge();
            if (age < 0) setAge(++age);
            else if (age > 0) setAge(--age);
        }

        // update nearGround state when moving for flight and animation logic
        nearGround = onGround() || !level().noCollision(this, new AABB(getX(), getY(), getZ(), getX(), getY() - (GROUND_CLEARENCE_THRESHOLD * getAgeScale()), getZ()));

        // update flying state based on the distance to the ground
        boolean flying = shouldFly();
        if (flying != isFlying())
        {
            setFlying(flying);

            // update pathfinding method
            if (isServer()) setNavigation(flying);
        }

        updateAgeProgress();
        for (var ability : getAbilities()) ability.tick(this);
    }

    @Override
    public void travel(Vec3 vec3)
    {
        if (isFlying())
        {
            if (isControlledByLocalInstance())
            {
                // Move relative to yaw - handled in the move controller or by driver
                moveRelative(getSpeed(), vec3);
                move(MoverType.SELF, getDeltaMovement());
                if (getDeltaMovement().lengthSqr() < 0.1) // we're not actually going anywhere, bob up and down.
                    setDeltaMovement(getDeltaMovement().add(0, Math.sin(tickCount / 4f) * 0.03, 0));
                setDeltaMovement(getDeltaMovement().scale(0.9f)); // smoothly slow down
            }

            calculateEntityAnimation(true);
        }
        else super.travel(vec3);
    }

    @Override
    protected Vec3 getRiddenInput(Player driver, Vec3 move)
    {
        double moveSideways = move.x;
        double moveY = move.y;
        double moveForward = Math.min(Math.abs(driver.zza) + Math.abs(driver.xxa), 1);

        if (isFlying() && hasLocalDriver())
        {
            moveForward = moveForward > 0? moveForward : 0;
            if (driver.jumping) moveY = 1;
            else if (KeyMappings.FLIGHT_DESCENT_KEY.isDown()) moveY = -1;
            else if (moveForward > 0 && DMLConfig.cameraDrivenFlight()) moveY = -driver.getXRot() / 90; // normalize from -1 to 1
        }

        // mimic dogshit implementation of AI movement vectors
        // the way this works is that it will mimic how setSpeed in Mob works:
        // it sets the normal speed variable,
        // and then sets the walk forward variable to the same value.
        // so if speed is 0.3, walk forward will also be 0.3 instead of 1.0.
        // so when moveRelative calculates movespeed, (walkforward * speed) we get 0.15.
        // so I guess we should do it to.
        var speed = getRiddenSpeed(driver);
        return new Vec3(moveSideways * speed, moveY * speed, moveForward * speed);
    }

    @Override
    protected void tickRidden(Player driver, Vec3 move)
    {
        // rotate head to match driver.
        float yaw = driver.yHeadRot;
        if (move.z > 0) // rotate in the direction of the drivers controls
            yaw += (float) Mth.atan2(driver.zza, driver.xxa) * (180f / (float) Math.PI) - 90;
        yHeadRot = yaw;
        setXRot(driver.getXRot() * 0.68f);

        // rotate body towards the head
        setYRot(Mth.rotateIfNecessary(yHeadRot, getYRot(), 4));

        if (isControlledByLocalInstance())
        {
            if (!isFlying() && canFly() && driver.jumping) liftOff();
        }
    }

    @Override
    protected float getRiddenSpeed(Player driver)
    {
        return (float) getAttributeValue(isFlying()? FLYING_SPEED : MOVEMENT_SPEED);
    }

    @Override
    @SuppressWarnings("ConstantConditions") // I bet the breed exists at this point...
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        var stack = player.getItemInHand(hand);

        var stackResult = stack.interactLivingEntity(player, this, hand);
        if (stackResult.consumesAction()) return stackResult;

        // tame
        if (!isTame())
        {
            if (isServer() && getBreed().tamingItems().contains(stack.getItem().builtInRegistryHolder()))
            {
                stack.shrink(1);
                tamedFor(player, getRandom().nextInt(5) == 0);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS; // pass regardless. We don't want to perform breeding, age ups, etc. on untamed.
        }

        // heal
        if (getHealthFraction() < 1 && isFoodItem(stack))
        {
            //noinspection ConstantConditions

            heal(stack.get(DataComponents.FOOD).nutrition());
            playSound(getEatingSound(stack), 0.7f, 1);
            stack.shrink(1);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // saddle up!
        if (isTamedFor(player) && isSaddleable() && !isSaddled() && stack.getItem() instanceof SaddleItem)
        {
            stack.shrink(1);
            equipSaddle(stack, getSoundSource());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // give the saddle back!
        if (isTamedFor(player) && isSaddled() && stack.is(Tags.Items.SHEARS))
        {
            spawnAtLocation(Items.SADDLE);
            player.playSound(SoundEvents.SHEEP_SHEAR, 1f, 1f);
            setSaddled(false);
            gameEvent(GameEvent.SHEAR, player);
            stack.hurtAndBreak(1, player, getSlotForHand(hand));

            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // sit!
        if (isTamedFor(player) && (player.isSecondaryUseActive() || stack.is(Items.BONE))) // "bone sitting" for legacy reasons
        {
            if (isServer())
            {
                navigation.stop();
                setOrderedToSit(!isOrderedToSit());
                if (isOrderedToSit()) setTarget(null);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // ride on
        if (isTamedFor(player) && isSaddled() && !isHatchling() && !isFood(stack))
        {
            if (isServer())
            {
                player.startRiding(this);
                navigation.stop();
                setTarget(null);
            }
            setOrderedToSit(false);
            setInSittingPose(false);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    public void liftOff()
    {
        if (canFly()) jumpFromGround();
    }

    @Override
    protected float getJumpPower()
    {
        // stronger jumps for easier lift-offs
        return super.getJumpPower() * (canFly()? 3 : 1);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource)
    {
        return !canFly() && super.causeFallDamage(pFallDistance, pMultiplier, pSource);
    }

    @Override
    protected void tickDeath()
    {
        // unmount any riding entities
        ejectPassengers();

        // freeze at place
        setDeltaMovement(Vec3.ZERO);
        setYRot(yRotO);
        setYHeadRot(yHeadRotO);

        if (deathTime >= getMaxDeathTime()) remove(RemovalReason.KILLED); // actually delete entity after the time is up

        deathTime++;
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return Optional.ofNullable(getBreedHolder())
                .flatMap(b -> b.get().ambientSound())
                .map(Holder::get)
                .orElse(DMLRegistry.DRAGON_AMBIENT_SOUND.get());
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    public SoundEvent getStepSound()
    {
        return DMLRegistry.DRAGON_STEP_SOUND.get();
    }

    /**
     * Returns the sound this mob makes on death.
     */
    @Override
    protected SoundEvent getDeathSound()
    {
        return DMLRegistry.DRAGON_DEATH_SOUND.get();
    }

    @Override
    public SoundEvent getEatingSound(ItemStack itemStackIn)
    {
        return SoundEvents.GENERIC_EAT;
    }

    public SoundEvent getAttackSound()
    {
        return SoundEvents.GENERIC_EAT;
    }

    public SoundEvent getWingsSound()
    {
        return SoundEvents.ENDER_DRAGON_FLAP;
    }

    /**
     * Plays step sound at given x, y, z for the entity
     */
    @Override
    protected void playStepSound(BlockPos entityPos, BlockState state)
    {
        if (isInWater()) return;

        if (isHatchling())
        {
            super.playStepSound(entityPos, state);
            return;
        }

        // override sound type if the top block is snowy
        var soundType = state.getSoundType();
        if (level().getBlockState(entityPos.above()).getBlock() == Blocks.SNOW)
            soundType = Blocks.SNOW.getSoundType(state, level(), entityPos, this);

        // play stomping for bigger dragons
        playSound(getStepSound(), soundType.getVolume(), soundType.getPitch() * getVoicePitch());
    }

    @Override
    public void playAmbientSound()
    {
        if (getBreed() != null) // EntityType likes to invoke this before deserializing, so let's guard it.
            super.playAmbientSound();
    }

    @Override
    public int getAmbientSoundInterval()
    {
        return 240;
    }

    @Override
    protected float getSoundVolume()
    {
        return getAgeScale();
    }

    @Override
    public float getVoicePitch()
    {
        return 2 - getAgeScale();
    }

    @Override
    public ItemStack getPickedResult(HitResult target)
    {
        if (getBreed() == null) return ItemStack.EMPTY;
        return DragonSpawnEgg.create(getBreedHolder());
    }

    @Override
    protected Component getTypeName()
    {
        if (getBreedHolder() != null) return DragonBreed.getTranslation(getBreedHolder());
        return super.getTypeName();
    }

    // todo: DRAGON_FOOD tag?
    public boolean isFoodItem(ItemStack stack)
    {
        return stack.is(ItemTags.MEAT);
    }

    // the "food" that enables breeding mode
    @Override
    @SuppressWarnings("ConstantConditions") // I bet the breed exists at this point...
    public boolean isFood(ItemStack stack)
    {
        return stack.is(getBreed().breedingItems());
    }

    public void tamedFor(Player player, boolean successful)
    {
        if (successful)
        {
            tame(player);
            navigation.stop();
            setTarget(null);
            setOwnerUUID(player.getUUID());
            level().broadcastEntityEvent(this, (byte) 7);
        }
        else
        {
            level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    public boolean isTamedFor(Player player)
    {
        return isTame() && isOwnedBy(player);
    }

    /**
     * Determines if an entity can be despawned, used on idle far away entities
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }

    /**
     * returns true if this entity is by a ladder, false otherwise
     */
    @Override
    public boolean onClimbable()
    {
        // this better doesn't happen...
        return false;
    }

    @Override
    protected void dropEquipment()
    {
        super.dropEquipment();
        if (isSaddled()) spawnAtLocation(Items.SADDLE);
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable()
    {
        if (isServer() && getBreed() != null)
        {
            // needs to use server's reloadable resources; loot tables aren't present on the normal registry
            ResourceKey<LootTable> table = DragonBreed.getLootTableKey(getBreedHolder(), level().getServer().reloadableRegistries());
            if (table != null) return table;
        }

        return super.getDefaultLootTable();
    }

    //todo what was this for?
//    @Override
//    @SuppressWarnings("ConstantConditions")
//    public boolean doHurtTarget(Entity entityIn)
//    {
//        boolean attacked = entityIn.hurt(damageSources().mobAttack(this), (float) getAttribute(ATTACK_DAMAGE).getValue());
//
//        if (attacked) doEnchantDamageEffects(this, entityIn);
//
//        return attacked;
//    }

    public void onWingsDown(float speed)
    {
        if (!isInWater())
        {
            // play wing sounds
            float pitch = (1 - speed);
            float volume = 0.3f + (1 - speed) * 0.2f;
            pitch *= getVoicePitch();
            volume *= getSoundVolume();
            level().playLocalSound(getX(), getY(), getZ(), getWingsSound(), SoundSource.VOICE, volume, pitch, true);
        }
    }

    @Override
    public void swing(InteractionHand hand)
    {
        // play eating sound
        playSound(getAttackSound(), 1, 0.7f);
        super.swing(hand);
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource src, float par2)
    {
        if (isInvulnerableTo(src)) return false;

        // don't just sit there!
        setOrderedToSit(false);

        return super.hurt(src, par2);
    }

    /**
     * Returns true if the mob is currently able to mate with the specified mob.
     */
    @Override
    public boolean canMate(Animal mate)
    {
        if (mate == this) return false; // No. Just... no.
        if (!(mate instanceof TameableDragon dragonMate)) return false;
        if (!canReproduce()) return false;

        if (!dragonMate.canReproduce()) return false;

        return isInLove() && mate.isInLove();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canReproduce()
    {
        if (!isTame() || getBreed() == null) return false;

        var limit = getBreed().getReproductionLimit();
        return reproductionCount < limit || limit == -1;
    }

    @Override
    @SuppressWarnings("ConstantConditions") // breed nullability is checked in canReproduce
    public void spawnChildFromBreeding(ServerLevel level, Animal animal)
    {
        if (!(animal instanceof TameableDragon mate))
        {
            DragonMountsLegacy.LOG.warn("Tried to mate with non-dragon? Hello? {}", animal);
            return;
        }

        // pick a breed to inherit from, and place hatching.
        var state = DMLRegistry.EGG_BLOCK.get().defaultBlockState().setValue(HatchableEggBlock.HATCHING, true);
        var offSpringBreed = CrossBreedingManager.INSTANCE.getCrossBreed(getBreedHolder(), mate.getBreedHolder(), level.registryAccess());
        if (offSpringBreed == null) offSpringBreed = getRandom().nextBoolean()? getBreedHolder() : mate.getBreedHolder();

        var egg = HatchableEggBlock.place(level, blockPosition(), state, offSpringBreed);

        // mix the custom names in case both parents have one
        if (hasCustomName() && animal.hasCustomName())
        {
            String p1Name = getCustomName().getString();
            String p2Name = animal.getCustomName().getString();
            String babyName;

            if (p1Name.contains(" ") || p2Name.contains(" "))
            {
                // combine two words with space
                // "Tempor Invidunt Dolore" + "Magna"
                // = "Tempor Magna" or "Magna Tempor"
                String[] p1Names = p1Name.split(" ");
                String[] p2Names = p2Name.split(" ");

                p1Name = StringUtils.capitalize(p1Names[getRandom().nextInt(p1Names.length)]);
                p2Name = StringUtils.capitalize(p2Names[getRandom().nextInt(p2Names.length)]);

                babyName = getRandom().nextBoolean()? p1Name + " " + p2Name : p2Name + " " + p1Name;
            }
            else
            {
                // scramble two words
                // "Eirmod" + "Voluptua"
                // = "Eirvolu" or "Volueir" or "Modptua" or "Ptuamod" or ...
                if (getRandom().nextBoolean()) p1Name = p1Name.substring(0, (p1Name.length() - 1) / 2);
                else p1Name = p1Name.substring((p1Name.length() - 1) / 2);

                if (getRandom().nextBoolean()) p2Name = p2Name.substring(0, (p2Name.length() - 1) / 2);
                else p2Name = p2Name.substring((p2Name.length() - 1) / 2);

                p2Name = StringUtils.capitalize(p2Name);

                babyName = getRandom().nextBoolean()? p1Name + p2Name : p2Name + p1Name;
            }

            egg.setCustomName(Component.literal(babyName));
        }

        // increase reproduction counter
        addReproCount();
        mate.addReproCount();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob)
    {
        var offspring = DMLRegistry.DRAGON.get().create(level);
        if (getBreed() != null) offspring.setBreed(getBreedHolder());
        return offspring;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner)
    {
        return !(target instanceof TamableAnimal tameable) || !Objects.equals(tameable.getOwner(), owner);
    }

    @Override
    public boolean canAttack(LivingEntity target)
    {
        return !isHatchling() && !hasControllingPassenger() && super.canAttack(target);
    }

    /**
     * For vehicles, the first passenger is generally considered the controller and "drives" the vehicle. For example,
     * Pigs, Horses, and Boats are generally "steered" by the controlling passenger.
     */
    @Override
    public LivingEntity getControllingPassenger()
    {
        return getFirstPassenger() instanceof LivingEntity driver && isOwnedBy(driver)? driver : null;
    }

    @Override
    protected void addPassenger(Entity passenger)
    {
        super.addPassenger(passenger);

        if (passenger instanceof Player)
        {
            passenger.setYRot(getYRot());
            passenger.setXRot(getXRot());
        }

        if (hasLocalDriver())
        {
            MountControlsMessenger.sendControlsMessage();
            MountCameraManager.onDragonMount();
        }
    }

    @Override
    protected void removePassenger(Entity passenger)
    {
        if (hasLocalDriver()) MountCameraManager.onDragonDismount();
        super.removePassenger(passenger);
    }

    @Override
    protected void positionRider(Entity ridden, MoveFunction pCallback)
    {
        // fix rider rotation
        ridden.xRotO = ridden.getXRot();
        ridden.yRotO = ridden.getYRot();
        ridden.setYBodyRot(yBodyRot);

        super.positionRider(ridden, pCallback);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity ridden, EntityDimensions dimensions, float partialTick)
    {
        return new Vec3(0, getBbHeight(), 1).yRot(-getYRot() * ((float) Math.PI / 180f));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src)
    {
        Entity srcEnt = src.getEntity();
        if (srcEnt != null && (srcEnt == this || hasPassenger(srcEnt))) return true;

        if (getBreed() != null) return getBreed().immunities().contains(src.typeHolder());

        return super.isInvulnerableTo(src);
    }

    /**
     * Returns the entity's health relative to the maximum health.
     *
     * @return health normalized between 0 and 1
     */
    public float getHealthFraction()
    {
        return getHealth() / getMaxHealth();
    }

    public int getMaxDeathTime()
    {
        return 120;
    }

    /**
     * Public wrapper for protected final setScale(), used by DragonLifeStageHelper.
     */
    @Override
    public void refreshDimensions()
    {
        double posXTmp = getX();
        double posYTmp = getY();
        double posZTmp = getZ();
        boolean onGroundTmp = onGround();

        super.refreshDimensions();

        // workaround for a vanilla bug; the position is apparently not set correcty
        // after changing the entity size, causing asynchronous server/client positioning
        setPos(posXTmp, posYTmp, posZTmp);

        // otherwise, setScale stops the dragon from landing while it is growing
        setOnGround(onGroundTmp);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose)
    {
        return isInSittingPose()? SITTING_DIMENSIONS.scale(getAgeScale()) : super.getDefaultDimensions(pose);
    }
    
    @Override
    public float getAgeScale()
    {
        var mod = getBreed() == null? 1f : getBreed().sizeModifier();
        return (0.33f + (0.67f * getAgeProgress())) * mod;
    }

    @Override
    public int getAge()
    {
        return age;
    }

    public void updateAgeProgress()
    {
        // no reason to recalculate this value several times per tick/frame...
        float growth = -BASE_GROWTH_TIME;
        if (getBreed() != null) growth = -getBreed().growthTime();
        float min = Math.min(getAge(), 0);
        ageProgress = 1 - (min / growth);
    }

    public float getAgeProgress()
    {
        return ageProgress;
    }

    /**
     * Updates properties/attributes/traits of dragons based on the current age scale.
     * Also syncs the current age to the client.
     * Called at an interval (of ticks) described by {@link TameableDragon#AGE_UPDATE_INTERVAL}
     */
    @SuppressWarnings("ConstantConditions")
    private void updateAgeProperties()
    {
        setAge(entityData.get(DATA_AGE));
        updateAgeProgress();
        refreshDimensions();

        getAttribute(STEP_HEIGHT).setBaseValue(Math.max(2 * getAgeProgress(), 1));

        // update attributes and health only on the server
        if (isServer())
        {
            // health does not update on modifier application, so have to store the health frac first
            var healthFrac = getHealthFraction();

            // negate modifier value since the operation is as follows: base_value += modifier * base_value
            double modValue = -(1d - Math.max(getAgeProgress(), 0.1));
            var mod = new AttributeModifier(AGE_ATTR_MODIFIER_ID, modValue, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

            getAttribute(MAX_HEALTH).addOrReplacePermanentModifier(mod); // needs to be serialized otherwise health fraction gets weird at deserialization time
            getAttribute(ATTACK_DAMAGE).addOrUpdateTransientModifier(mod);

            // restore health fraction
            setHealth(healthFrac * getMaxHealth());
        }
    }

    public boolean isHatchling()
    {
        return getAgeProgress() < 0.5f;
    }

    public boolean isJuvenile()
    {
        return getAgeProgress() >= 0.5f && getAgeProgress() < 1f;
    }

    public boolean isAdult()
    {
        return getAgeProgress() >= 1f;
    }

    @Override
    public boolean isBaby()
    {
        return !isAdult();
    }

    @Override
    public void setBaby(boolean baby)
    {
        var growth = -BASE_GROWTH_TIME;
        if (getBreed() != null) growth = -getBreed().growthTime();
        setAge(baby? growth : 0);
        entityData.set(DATA_AGE, age);
    }

    @Override
    public void ageUp(int p_146741_, boolean p_146742_)
    {
        super.ageUp(p_146741_, p_146742_);
        entityData.set(DATA_AGE, getAge());
    }

    // simple helper method to determine if we're on the server thread.
    public boolean isServer()
    {
        return !level().isClientSide;
    }

    public DragonAnimator getAnimator()
    {
        return animator;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type)
    {
        if (getBreed() == null) return super.canDrownInFluidType(type);
        return !getBreed().immunities().contains(damageSources().drown().typeHolder());
    }

    @Override
    public boolean fireImmune()
    {
        if (super.fireImmune()) return true;
        if (getBreed() == null) return false;
        return getBreed().immunities().contains(damageSources().onFire().typeHolder());
    }

    @Override
    protected void onChangedBlock(ServerLevel pLevel, BlockPos pPos)
    {
        super.onChangedBlock(pLevel, pPos);
        for (var ability : getAbilities()) ability.onMove(this);
    }

    @Override
    public boolean isInWall()
    {
        if (noPhysics) return false;
        else
        {
            // Reduce suffocation risks. They're fat and clusmy.
            var collider = getBoundingBox().deflate(getBbWidth() * 0.2f);
            return BlockPos.betweenClosedStream(collider).anyMatch((pos) ->
            {
                BlockState state = level().getBlockState(pos);
                return !state.isAir() && state.isSuffocating(level(), pos) && Shapes.joinIsNotEmpty(state.getCollisionShape(level(), pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(collider), BooleanOp.AND);
            });
        }
    }

    @Override
    public Vec3 getLightProbePosition(float p_20309_)
    {
        return new Vec3(getX(), getY() + getBbHeight(), getZ());
    }

    public boolean hasLocalDriver()
    {
        return getControllingPassenger() instanceof Player p && p.isLocalPlayer();
    }
}