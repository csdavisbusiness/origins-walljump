package com.coreythesquish.dragonwalljump;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.Origin;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class DragonWallJumpLogic {
    private static int ticksWallClinged;
    private static int ticksWallSlid;
    private static boolean stopSlid;
    private static int wallJumpCount;
    private static int ticksKeyDown;
    private static double clingX;
    private static double clingZ;
    private static double lastJumpY;
    private static Set<Direction> walls;
    private static Set<Direction> staleWalls;

    private static boolean collidesWithBlock(Level level, AABB box) {
        return !level.noCollision(box);
    }

    public static void doWallJump(LocalPlayer pl) {
        if (!hasDragonOrigin(pl)) return;

        if (pl.isOnGround() || pl.isInWater() || !pl.level().getFluidState(pl.blockPosition()).isEmpty() || pl.isPassenger()) {
            ticksWallClinged = 0;
            ticksWallSlid = 0;
            stopSlid = false;
            clingX = Double.NaN;
            clingZ = Double.NaN;
            lastJumpY = Double.MAX_VALUE;
            staleWalls.clear();
            wallJumpCount = 0;
            return;
        }
        if (stopSlid) return;

        updateWalls(pl);
        ticksKeyDown = pl.isShiftKeyDown() ? ticksKeyDown + 1 : 0;

        if (ticksWallClinged < 1) {
            if (ticksKeyDown > 0 && ticksKeyDown < 4 && !walls.isEmpty() && canWallCling(pl)) {
                if (ModConfig.autoRotation) {
                    pl.setYRot(getClingDirection().toYRot());
                    pl.yRotO = pl.getYRot();
                }
                ticksWallClinged = 1;
                clingX = pl.getX();
                clingZ = pl.getZ();
                playHitSound(pl, getWallPos(pl));
                spawnWallParticle(pl, getWallPos(pl));
            }
            return;
        }

        if (!pl.isShiftKeyDown() || pl.isOnGround() || !pl.level().getFluidState(pl.blockPosition()).isEmpty() || walls.isEmpty() || pl.getFoodData().getFoodLevel() < 1) {
            ticksWallClinged = 0;
            if (!(pl.xxa == 0.0f && pl.yya == 0.0f || pl.isOnGround() || walls.isEmpty())) {
                if (wallJumpCount >= ModConfig.maxWallJumps) return;
                pl.jumpFromGround();
                wallJump(pl, (float) ModConfig.wallJumpHeight);
                staleWalls = new HashSet<>(walls);
            }
            return;
        }

        pl.setPos(clingX, pl.getY(), clingZ);
        double motionY = pl.getDeltaMovement().y;
        if (motionY > 0.0) {
            motionY = 0.0;
        } else if (motionY < -0.6) {
            motionY += 0.2;
            spawnWallParticle(pl, getWallPos(pl));
        } else if (ticksWallClinged++ > ModConfig.wallSlideDelay) {
            if (ticksWallSlid++ > ModConfig.stopWallSlideDelay) {
                stopSlid = true;
            }
            motionY = -0.1;
            spawnWallParticle(pl, getWallPos(pl));
        } else {
            motionY = 0.0;
        }
        pl.setDeltaMovement(0.0, motionY, 0.0);
    }

    private static boolean hasDragonOrigin(LocalPlayer pl) {
        return Origins.getLayers(pl).stream()
                .flatMap(layer -> layer.getOrigins(pl.getUUID()).stream())
                .findFirst()
                .map(origin -> origin.getIdentifier().equals(new net.minecraft.resources.ResourceLocation("l5r:dragon")))
                .orElse(false);
    }

    private static boolean canWallCling(LocalPlayer pl) {
        if (pl.isCreative() || pl.getDeltaMovement().y > 0.1 || pl.getFoodData().getFoodLevel() < 1) {
            return false;
        }
        if (collidesWithBlock(pl.level(), pl.getBoundingBox().move(0.0, -0.8, 0.0))) {
            return false;
        }
        if (pl.getDeltaMovement().y < -0.8) {
            return false;
        }
        if (pl.getY() < lastJumpY - 1.0) {
            return true;
        }
        return !staleWalls.containsAll(walls);
    }

    private static void updateWalls(LocalPlayer pl) {
        Vec3 pos = pl.position();
        AABB box = new AABB(pos.x - 0.001, pos.y, pos.z - 0.001, pos.x + 0.001, pos.y + pl.getBbHeight(), pos.z + 0.001);
        double dist = (pl.getBbWidth() / 2.0f) + (ticksWallClinged > 0 ? 0.1 : 0.06);
        AABB[] axes = new AABB[]{box.move(0.0, 0.0, dist), box.move(-dist, 0.0, 0.0), box.move(0.0, 0.0, -dist), box.move(dist, 0.0, 0.0)};
        int i = 0;
        walls = new HashSet<>();
        for (AABB axis : axes) {
            Direction direction = Direction.fromDelta(i++, 0, 0);
            if (!collidesWithBlock(pl.level(), axis)) continue;
            walls.add(direction);
            pl.setOnGround(true);
        }
    }

    private static Direction getClingDirection() {
        return walls.isEmpty() ? Direction.UP : walls.iterator().next();
    }

    private static BlockPos getWallPos(LocalPlayer player) {
        BlockPos blockPos = player.blockPosition().relative(getClingDirection());
        return player.level().getBlockState(blockPos).isAir() ? blockPos.above() : blockPos;
    }

    private static void wallJump(LocalPlayer pl, float up) {
        float strafe = Math.signum(pl.xxa) * up * up;
        float forward = Math.signum(pl.yya) * up * up;
        float f = (float) (1.0 / Math.sqrt(strafe * strafe + up * up + forward * forward));
        strafe *= f;
        forward *= f;
        float f1 = (float) (Math.sin(pl.getYRot() * ((float) Math.PI / 180)) * 0.45f);
        float f2 = (float) (Math.cos(pl.getYRot() * ((float) Math.PI / 180)) * 0.45f);
        Vec3 motion = pl.getDeltaMovement();
        pl.setDeltaMovement(motion.x + (strafe * f2 - forward * f1), up, motion.z + (forward * f2 + strafe * f1));
        lastJumpY = pl.getY();
        playBreakSound(pl, getWallPos(pl));
        spawnWallParticle(pl, getWallPos(pl));
        wallJumpCount++;
    }

    private static void playHitSound(LocalPlayer entity, BlockPos blockPos) {
        BlockState state = entity.level().getBlockState(blockPos);
        SoundType soundtype = state.getBlock().getSoundType(state, entity.level(), blockPos, entity);
        entity.playSound(soundtype.getHitSound(), soundtype.getVolume() * 0.25f, soundtype.getPitch());
    }

    private static void playBreakSound(LocalPlayer entity, BlockPos blockPos) {
        BlockState state = entity.level().getBlockState(blockPos);
        SoundType soundtype = state.getBlock().getSoundType(state, entity.level(), blockPos, entity);
        entity.playSound(soundtype.getBreakSound(), soundtype.getVolume() * 0.5f, soundtype.getPitch());
    }

    private static void spawnWallParticle(LocalPlayer entity, BlockPos blockPos) {
        BlockState state = entity.level().getBlockState(blockPos);
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 pos = entity.position();
            Vec3i motion = getClingDirection().getNormal();
            entity.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(blockPos), pos.x, pos.y, pos.z, motion.getX() * -1.0, -1.0, motion.getZ() * -1.0);
        }
    }

    static {
        stopSlid = false;
        lastJumpY = Double.MAX_VALUE;
        walls = new HashSet<>();
        staleWalls = new HashSet<>();
    }
}