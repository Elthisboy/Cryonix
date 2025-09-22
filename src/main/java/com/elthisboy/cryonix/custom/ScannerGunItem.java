package com.elthisboy.cryonix.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

public class ScannerGunItem extends Item {
    private static final String NBT_ENERGY = "ScannerEnergy";
    private static final int MAX_ENERGY = 300;
    private static final int ENERGY_PER_USE = 5;
    private static final double RANGE = 50.0;
    private static final int COOLDOWN_TICKS = 10;

    // ===== Barra de energía en el ítem =====
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getEnergy(stack) < MAX_ENERGY;
    }
    @Override
    public int getItemBarStep(ItemStack stack) {
        float pct = (float) getEnergy(stack) / (float) MAX_ENERGY;
        return Math.round(pct * 13.0f);
    }
    @Override
    public int getItemBarColor(ItemStack stack) {
        float pct = (float) getEnergy(stack) / (float) MAX_ENERGY;
        float hue = MathHelper.lerp(pct, 0.55f, 0.60f); // cian->azul
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.9f, 1.0f);
        return ColorHelper.Argb.getArgb(255, (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
    }

    public ScannerGunItem(Settings settings) { super(settings); }

    // ===== Helpers CUSTOM_DATA =====
    private static NbtCompound readCustom(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        return comp != null ? comp.copyNbt() : new NbtCompound();
    }
    private static void writeCustom(ItemStack stack, NbtCompound tag) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
    }
    private int getEnergy(ItemStack stack) {
        NbtCompound tag = readCustom(stack);
        if (!tag.contains(NBT_ENERGY)) {
            tag.putInt(NBT_ENERGY, MAX_ENERGY);
            writeCustom(stack, tag);
        }
        return tag.getInt(NBT_ENERGY);
    }
    private void setEnergy(ItemStack stack, int value) {
        NbtCompound tag = readCustom(stack);
        tag.putInt(NBT_ENERGY, MathHelper.clamp(value, 0, MAX_ENERGY));
        writeCustom(stack, tag);
    }

    // ===== Recarga desde inventario =====
    private boolean tryReloadFromInventory(PlayerEntity player, ItemStack scanner) {
        int current = getEnergy(scanner);
        if (current >= MAX_ENERGY) return false;
        int[] prios = {200, 100, 50}; // Lv3->Lv2->Lv1
        for (int want : prios) {
            int slot = findChargeSlot(player, want);
            if (slot != -1) {
                ItemStack charge = player.getInventory().getStack(slot);
                if (charge.getItem() instanceof LaserChargeItem lci) {
                    int add = lci.getEnergyAmount();
                    int space = MAX_ENERGY - current;
                    int applied = Math.min(add, space);
                    if (applied > 0) {
                        setEnergy(scanner, current + applied);
                        charge.decrement(1);
                        player.getInventory().markDirty();
                        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
                        player.sendMessage(Text.translatable(
                                "message.cryonix.reload.success",
                                String.valueOf(applied),
                                String.valueOf(getEnergy(scanner)),
                                String.valueOf(MAX_ENERGY)
                        ), true);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private int findChargeSlot(PlayerEntity player, int amount) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() instanceof LaserChargeItem lci && lci.getEnergyAmount() == amount) return i;
        }
        return -1;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.isSneaking()) {
            boolean reloaded = tryReloadFromInventory(player, stack);
            if (!reloaded) player.sendMessage(Text.translatable("message.cryonix.reload.none"), true);
            return TypedActionResult.success(stack, world.isClient());
        }
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            player.sendMessage(Text.translatable("message.cryonix.cooldown"), true);
            return TypedActionResult.success(stack, world.isClient());
        }
        int energy = getEnergy(stack);
        if (energy < ENERGY_PER_USE) {
            player.sendMessage(Text.translatable("message.cryonix.low_battery"), true);
            return TypedActionResult.success(stack, world.isClient());
        }

        // ===== 1) Raycast de detección: SIEMPRE desde los OJOS hacia donde miras =====
        float tickDelta = 0f;
        Vec3d eye = player.getCameraPosVec(tickDelta);
        Vec3d look = player.getRotationVec(tickDelta).normalize();
        Vec3d aimEnd = eye.add(look.multiply(RANGE));

        // Bloques
        BlockHitResult blockHit = world.raycast(new RaycastContext(
                eye, aimEnd,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));
        double blockDist2 = blockHit != null ? blockHit.getPos().squaredDistanceTo(eye) : Double.POSITIVE_INFINITY;

        // Entidades (hitbox)
        EntityHitResult entityHit = raycastEntities(world, player, eye, aimEnd);
        double entityDist2 = entityHit != null ? entityHit.getPos().squaredDistanceTo(eye) : Double.POSITIVE_INFINITY;

        // Elegir el impacto más cercano al ojo
        HitResult finalHit;
        if (entityDist2 < blockDist2) finalHit = entityHit;
        else finalHit = (blockHit != null ? blockHit : null);

        // ===== 2) Partículas: desde la MANO usada (izq/der) hasta el punto impactado =====
        if (world.isClient()) {
            Vec3d beamStart = getHandPos(player, tickDelta, hand);
            Vec3d beamEnd = (finalHit != null) ? finalHit.getPos() : eye.add(look.multiply(RANGE));
            spawnBeamParticles(world, beamStart, beamEnd);
        }

        // ===== 3) Resultado + efectos =====
        if (finalHit == null || finalHit.getType() == HitResult.Type.MISS) {
            player.sendMessage(Text.translatable("message.cryonix.scan.none"), true);

        } else if (finalHit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bh = (BlockHitResult) finalHit;
            BlockPos pos = bh.getBlockPos();
            var state = world.getBlockState(pos);
            double dist = Math.sqrt(eye.squaredDistanceTo(Vec3d.ofCenter(pos)));
            player.sendMessage(Text.translatable("message.cryonix.scan.block", state.getBlock().getName(), String.format("%.1f", dist)), true);

            // “Glow” simulado en bloque (contorno de partículas, cliente)
            if (world.isClient()) spawnBlockOutlineParticles(world, pos);

        } else if (finalHit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult eh = (EntityHitResult) finalHit;
            Entity e = eh.getEntity();
            double dist = Math.sqrt(eye.squaredDistanceTo(e.getPos()));
            player.sendMessage(Text.translatable("message.cryonix.scan.entity", e.getDisplayName(), String.format("%.1f", dist)), true);

            // Glow real en mobs
            if (!world.isClient() && e instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 5, 0, false, true));
            }
        }

        world.playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_IN, SoundCategory.PLAYERS, 1.0F, 1.0F);
        setEnergy(stack, energy - ENERGY_PER_USE);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack, world.isClient());
    }

    /** Posición de la mano que DISPARA (respeta main/offhand y zurdo/diestro). */
    private Vec3d getHandPos(PlayerEntity player, float tickDelta, Hand usedHand) {
        // Base: ojos y ejes derivados
        Vec3d eye = player.getCameraPosVec(tickDelta);
        Vec3d forward = player.getRotationVec(tickDelta).normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up).normalize();

        // ¿La mano usada está a la derecha o a la izquierda?
        boolean rightSide;
        if (usedHand == Hand.MAIN_HAND) {
            rightSide = (player.getMainArm() == Arm.RIGHT);
        } else { // OFF_HAND
            rightSide = (player.getMainArm() == Arm.LEFT);
        }
        if (!rightSide) right = right.multiply(-1);

        // Offsets (ajustables): lateral, vertical y hacia delante
        double side = 0.35;   // separación lateral
        double down = 0.25;   // un poquito abajo de los ojos
        double fwd  = 0.20;   // un poquito delante de la cara

        return eye.add(right.multiply(side)).add(0, -down, 0).add(forward.multiply(fwd));
    }

    /** Raycast de entidades por la línea de mira (ojo->destino). */
    private EntityHitResult raycastEntities(World world, PlayerEntity player, Vec3d start, Vec3d end) {
        // Caja a lo largo del rayo (expand para no perder entidades pequeñas)
        Box search = new Box(start, end).expand(1.0);

        // ✅ Predicado compatible con Yarn 1.21.x
        List<Entity> entities = world.getOtherEntities(
                player,
                search,
                e -> e != player
                        && e.isAlive()
                        && !e.isSpectator()
                        && e.isAttackable() // <- en vez de e.collides()
        );

        Entity closest = null;
        Vec3d closestHitPos = null;
        double closestDist2 = Double.POSITIVE_INFINITY;

        for (Entity e : entities) {
            // Intersección del segmento con la AABB de la entidad
            Box eb = e.getBoundingBox().expand(e.getTargetingMargin());
            var opt = eb.raycast(start, end);
            if (opt.isPresent()) {
                Vec3d hitPos = opt.get();
                double d2 = hitPos.squaredDistanceTo(start);
                // Asegura que el punto está dentro del segmento
                if (d2 < closestDist2 && start.distanceTo(hitPos) <= start.distanceTo(end)) {
                    closestDist2 = d2;
                    closest = e;
                    closestHitPos = hitPos;
                }
            }
        }
        return (closest != null) ? new EntityHitResult(closest, closestHitPos) : null;
    }

    /** Partículas del haz entre un origen (mano) y un destino (impacto). */
    private void spawnBeamParticles(World world, Vec3d start, Vec3d end) {
        int steps = Math.max(1, (int) (start.distanceTo(end) / 0.3));
        double dx = (end.x - start.x) / steps;
        double dy = (end.y - start.y) / steps;
        double dz = (end.z - start.z) / steps;
        for (int i = 0; i <= steps; i++) {
            double px = start.x + dx * i;
            double py = start.y + dy * i;
            double pz = start.z + dz * i;
            world.addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0, 0);
        }
    }

    /** Contorno de partículas para resaltar un bloque. */
    private void spawnBlockOutlineParticles(World world, BlockPos pos) {
        double min = 0.002, max = 0.998, step = 0.2;
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (double t = 0; t <= 1.0001; t += step) {
            double lerp = Math.min(1.0, t);
            world.addParticle(ParticleTypes.GLOW, x + MathHelper.lerp(lerp, min, max), y + min, z + min, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + MathHelper.lerp(lerp, min, max), y + min, z + max, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + MathHelper.lerp(lerp, min, max), y + max, z + min, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + MathHelper.lerp(lerp, min, max), y + max, z + max, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + min, y + MathHelper.lerp(lerp, min, max), z + min, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + min, y + MathHelper.lerp(lerp, min, max), z + max, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + max, y + MathHelper.lerp(lerp, min, max), z + min, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + max, y + MathHelper.lerp(lerp, min, max), z + max, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + min, y + min, z + MathHelper.lerp(lerp, min, max), 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + min, y + max, z + MathHelper.lerp(lerp, min, max), 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + max, y + min, z + MathHelper.lerp(lerp, min, max), 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, x + max, y + max, z + MathHelper.lerp(lerp, min, max), 0, 0, 0);
        }
    }
}


