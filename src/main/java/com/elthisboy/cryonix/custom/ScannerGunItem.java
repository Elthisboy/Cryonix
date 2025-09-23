package com.elthisboy.cryonix.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.registry.tag.BlockTags;


import java.util.List;

public class ScannerGunItem extends Item {
    /* ====== Config ====== */
    private static final String NBT_ENERGY = "ScannerEnergy";
    private static final int MAX_ENERGY = 300;
    private static final int ENERGY_PER_USE = 5;
    private static final double RANGE = 50.0;
    private static final int COOLDOWN_TICKS = 10;

    // AOE (escaneo en radio alrededor del impacto)
    private static final int AOE_RADIUS = 6;            // radio del barrido
    private static final int AOE_GLOW_TICKS = 20 * 4;   // glow a mobs del AOE (4s)
    private static final int AOE_MAX_POINTS = 60;       // límite de partículas

    public ScannerGunItem(Settings settings) { super(settings); }

    /* ====== Barra de energía (durability bar) ====== */
    @Override
    public boolean isItemBarVisible(ItemStack stack) { return getEnergy(stack) < MAX_ENERGY; }

    @Override
    public int getItemBarStep(ItemStack stack) {
        float pct = (float) getEnergy(stack) / (float) MAX_ENERGY;
        return Math.round(pct * 13.0f);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float pct = (float) getEnergy(stack) / (float) MAX_ENERGY;
        float hue = MathHelper.lerp(pct, 0.55f, 0.60f); // cian -> azul
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.9f, 1.0f);
        return ColorHelper.Argb.getArgb(255, (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
    }

    /* ====== Data Components (CUSTOM_DATA) ====== */
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
            tag.putInt(NBT_ENERGY, MAX_ENERGY); // por defecto llena
            writeCustom(stack, tag);
        }
        return tag.getInt(NBT_ENERGY);
    }

    private void setEnergy(ItemStack stack, int value) {
        NbtCompound tag = readCustom(stack);
        tag.putInt(NBT_ENERGY, MathHelper.clamp(value, 0, MAX_ENERGY));
        writeCustom(stack, tag);
    }

    /* ====== Recarga desde inventario ====== */
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

    /* ====== Uso (disparo / escaneo) ====== */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Shift para recargar
        if (player.isSneaking()) {
            boolean reloaded = tryReloadFromInventory(player, stack);
            if (!reloaded) player.sendMessage(Text.translatable("message.cryonix.reload.none"), true);
            return TypedActionResult.success(stack, world.isClient());
        }

        // Cooldown y energía
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            player.sendMessage(Text.translatable("message.cryonix.cooldown"), true);
            return TypedActionResult.success(stack, world.isClient());
        }
        int energy = getEnergy(stack);
        if (energy < ENERGY_PER_USE) {
            player.sendMessage(Text.translatable("message.cryonix.low_battery"), true);
            return TypedActionResult.success(stack, world.isClient());
        }

        // 1) Raycast de detección EXACTO a donde miras (desde los ojos)
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

        // Elige el impacto más cercano al ojo
        HitResult finalHit = (entityDist2 < blockDist2) ? entityHit : blockHit;

        // 2) Partículas: desde la MANO usada hasta el punto impactado
        if (world.isClient()) {
            Vec3d beamStart = getHandPos(player, tickDelta, hand);
            Vec3d beamEnd = (finalHit != null) ? finalHit.getPos() : aimEnd;
            spawnBeamParticles(world, beamStart, beamEnd);
        }

        // 3) Resultado + efectos
        if (finalHit == null || finalHit.getType() == HitResult.Type.MISS) {
            player.sendMessage(Text.translatable("message.cryonix.scan.none"), true);

        } else if (finalHit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bh = (BlockHitResult) finalHit;
            BlockPos pos = bh.getBlockPos();
            var state = world.getBlockState(pos);
            double dist = Math.sqrt(eye.squaredDistanceTo(Vec3d.ofCenter(pos)));
            player.sendMessage(Text.translatable("message.cryonix.scan.block",
                    state.getBlock().getName(), String.format("%.1f", dist)), true);

            // “Glow” simulado (contorno de partículas) en el bloque
            if (world.isClient()) spawnBlockOutlineParticles(world, pos);

            // Escaneo en área alrededor del impacto
            scanSurroundings(world, pos, player);

        } else if (finalHit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult eh = (EntityHitResult) finalHit;
            Entity e = eh.getEntity();
            double dist = Math.sqrt(eye.squaredDistanceTo(e.getPos()));
            player.sendMessage(Text.translatable("message.cryonix.scan.entity",
                    e.getDisplayName(), String.format("%.1f", dist)), true);

            // Glow real a mobs
            if (!world.isClient() && e instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 5, 0, false, true));
            }

            // Escaneo en área alrededor de la entidad impactada
            scanSurroundings(world, e.getBlockPos(), player);
        }

        // 4) Sonido + consumo + cooldown
        world.playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_IN, SoundCategory.PLAYERS, 1.0F, 1.0F);
        setEnergy(stack, energy - ENERGY_PER_USE);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack, world.isClient());
    }

    /* ====== Mano usada (origen del láser) ====== */
    private Vec3d getHandPos(PlayerEntity player, float tickDelta, Hand usedHand) {
        Vec3d eye = player.getCameraPosVec(tickDelta);
        Vec3d forward = player.getRotationVec(tickDelta).normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up).normalize();

        boolean rightSide;
        if (usedHand == Hand.MAIN_HAND) rightSide = (player.getMainArm() == Arm.RIGHT);
        else rightSide = (player.getMainArm() == Arm.LEFT);
        if (!rightSide) right = right.multiply(-1);

        double side = 0.35;   // lateral
        double down = 0.25;   // bajo la vista
        double fwd  = 0.20;   // delante de la cara
        return eye.add(right.multiply(side)).add(0, -down, 0).add(forward.multiply(fwd));
    }

    /* ====== Raycast de entidades (hitbox) a lo largo de la línea de mira ====== */
    private EntityHitResult raycastEntities(World world, PlayerEntity player, Vec3d start, Vec3d end) {
        Box search = new Box(start, end).expand(1.0);
        List<Entity> entities = world.getOtherEntities(
                player,
                search,
                e -> e != player
                        && e.isAlive()
                        && !e.isSpectator()
                        && e.isAttackable()
        );

        Entity closest = null;
        Vec3d closestHitPos = null;
        double closestDist2 = Double.POSITIVE_INFINITY;

        for (Entity e : entities) {
            Box eb = e.getBoundingBox().expand(e.getTargetingMargin());
            var opt = eb.raycast(start, end);
            if (opt.isPresent()) {
                Vec3d hitPos = opt.get();
                double d2 = hitPos.squaredDistanceTo(start);
                if (d2 < closestDist2 && start.distanceTo(hitPos) <= start.distanceTo(end)) {
                    closestDist2 = d2;
                    closest = e;
                    closestHitPos = hitPos;
                }
            }
        }
        return (closest != null) ? new EntityHitResult(closest, closestHitPos) : null;
    }

    /* ====== Partículas del láser ====== */
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

    /* ====== “Glow” simulado en bloque (contorno de partículas) ====== */
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

    /* ====== AOE: escaneo en radio alrededor del impacto ====== */
    private void scanSurroundings(World world, BlockPos center, PlayerEntity player) {
        int foundOres = 0;
        int foundMobs = 0;

        int r = AOE_RADIUS;
        int budget = AOE_MAX_POINTS;

        // BLOQUES importantes (ores + cobre/variantes)
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = center.add(dx, dy, dz);
                    if (center.getSquaredDistance(p) > (r * r)) continue;

                    if (isImportantBlock(world, p)) {
                        foundOres++;
                        if (world.isClient() && budget-- > 0) {
                            Vec3d c = Vec3d.ofCenter(p);
                            world.addParticle(ParticleTypes.GLOW, c.x, c.y, c.z, 0, 0, 0);
                        }
                    }
                }
            }
        }

        // MOBS (LivingEntity)
        Box box = new Box(
                center.getX() - r, center.getY() - r, center.getZ() - r,
                center.getX() + r + 1, center.getY() + r + 1, center.getZ() + r + 1
        );
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator());
        foundMobs = entities.size();

        // Glow a mobs (server) + partículas cliente
        if (!world.isClient()) {
            for (LivingEntity living : entities) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, AOE_GLOW_TICKS, 0, false, true));
            }
        } else {
            int mobBudget = Math.min(entities.size(), AOE_MAX_POINTS / 2);
            for (int i = 0; i < mobBudget; i++) {
                LivingEntity e = entities.get(i);
                Vec3d c = e.getPos().add(0, e.getHeight() * 0.6, 0);
                world.addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, 0, 0.01, 0);
            }
        }

        // Mensaje resumen
        player.sendMessage(Text.translatable("message.cryonix.scan.aoe",
                String.valueOf(foundOres), String.valueOf(foundMobs)), true);
    }

    // Tag global de ores (common tags)
    private static final TagKey<Block> ALL_ORES =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));

    private boolean isImportantBlock(World world, BlockPos p) {
        BlockState state = world.getBlockState(p);
        if (state.isAir()) return false;

        // 1) Cualquier ore vía tag c:ores (stone, deepslate, nether, end, y de otros mods)
        if (state.isIn(ALL_ORES)) return true;

        // 2) (Opcional) también considerar bloques compactados como importantes
        var b = state.getBlock();
        return b == Blocks.COPPER_BLOCK
                || b == Blocks.RAW_COPPER_BLOCK
                || b == Blocks.COAL_BLOCK
                || b == Blocks.IRON_BLOCK
                || b == Blocks.GOLD_BLOCK
                || b == Blocks.DIAMOND_BLOCK
                || b == Blocks.EMERALD_BLOCK
                || b == Blocks.LAPIS_BLOCK
                || b == Blocks.REDSTONE_BLOCK
                || b == Blocks.NETHERITE_BLOCK;
    }
}