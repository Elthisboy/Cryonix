package com.elthisboy.cryonix.custom;

import com.elthisboy.cryonix.networking.CryonixNetworking;
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
import net.minecraft.server.network.ServerPlayerEntity;
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
import com.elthisboy.cryonix.networking.CryonixNetworking;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScannerGunItem extends Item {
    /* ====== Config ====== */
    private static final String NBT_ENERGY = "ScannerEnergy";
    private static final int MAX_ENERGY = 300;
    private static final int ENERGY_PER_USE = 5;
    private static final double RANGE = 50.0;
    private static final int COOLDOWN_TICKS = 10;

    // AOE (escaneo en radio alrededor del impacto)
    private static final int AOE_RADIUS = 6;            // radio del barrido
    private static final int AOE_GLOW_TICKS = 20 * 3;   // glow a mobs del AOE (3s)
    private static final int AOE_MAX_POINTS = 60;       // límite de partículas

    // Tag de "todos los ores" (common tags: c:ores)
    private static final TagKey<Block> ALL_ORES =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));

    /* ====== Datos para construir el HUD (solo nombres/distancias) ====== */
    public static class FoundBlock {
        public final String name; public final double dist;
        public FoundBlock(String name, double dist) { this.name = name; this.dist = dist; }
    }
    public static class FoundMob {
        public final String name; public final double dist;
        public FoundMob(String name, double dist) { this.name = name; this.dist = dist; }
    }

    public ScannerGunItem(Settings settings) { super(settings.maxCount(1)); }

    /* ====== Barra de energía (durability bar) ====== */
    @Override public boolean isItemBarVisible(ItemStack stack) { return getEnergy(stack) < MAX_ENERGY; }
    @Override public int getItemBarStep(ItemStack stack) {
        float pct = (float) getEnergy(stack) / (float) MAX_ENERGY;
        return Math.round(pct * 13.0f);
    }
    @Override public int getItemBarColor(ItemStack stack) {
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
    /** Público por si quieres leer energía desde otros lados. */
    public int getEnergy(ItemStack stack) {
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

    /* ====== Recarga desde inventario ====== */
    private boolean tryReloadFromInventory(PlayerEntity player, ItemStack scanner) {
        int current = getEnergy(scanner);
        if (current >= MAX_ENERGY) return false;
        int[] prios = {200, 100, 50};
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

        // Cooldown + energía
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            player.sendMessage(Text.translatable("message.cryonix.cooldown"), true);
            return TypedActionResult.success(stack, world.isClient());
        }
        int energy = getEnergy(stack);
        if (energy < ENERGY_PER_USE) {
            player.sendMessage(Text.translatable("message.cryonix.low_battery"), true);
            return TypedActionResult.success(stack, world.isClient());
        }

        // 1) Raycast exacto desde los ojos
        float tickDelta = 0f;
        Vec3d eye = player.getCameraPosVec(tickDelta);
        Vec3d look = player.getRotationVec(tickDelta).normalize();
        Vec3d aimEnd = eye.add(look.multiply(RANGE));

        // Bloque
        BlockHitResult blockHit = world.raycast(new RaycastContext(
                eye, aimEnd,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));
        double blockDist2 = blockHit != null ? blockHit.getPos().squaredDistanceTo(eye) : Double.POSITIVE_INFINITY;

        // Entidades
        EntityHitResult entityHit = raycastEntities(world, player, eye, aimEnd);
        double entityDist2 = entityHit != null ? entityHit.getPos().squaredDistanceTo(eye) : Double.POSITIVE_INFINITY;

        // Escoge el impacto más cercano
        HitResult finalHit = (entityDist2 < blockDist2) ? entityHit : blockHit;

        // 2) Partículas desde la mano usada
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

            if (world.isClient()) spawnBlockOutlineParticles(world, pos);
            scanSurroundings(world, pos, player, stack);

        } else if (finalHit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult eh = (EntityHitResult) finalHit;
            Entity e = eh.getEntity();
            double dist = Math.sqrt(eye.squaredDistanceTo(e.getPos()));
            player.sendMessage(Text.translatable("message.cryonix.scan.entity",
                    e.getDisplayName(), String.format("%.1f", dist)), true);

            if (!world.isClient() && e instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 5, 0, false, true));
            }
            scanSurroundings(world, e.getBlockPos(), player, stack);

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

        boolean rightSide = (usedHand == Hand.MAIN_HAND) ? (player.getMainArm() == Arm.RIGHT) : (player.getMainArm() == Arm.LEFT);
        if (!rightSide) right = right.multiply(-1);

        double side = 0.35;   // lateral
        double down = 0.25;   // bajo la vista
        double fwd  = 0.20;   // delante de la cara
        return eye.add(right.multiply(side)).add(0, -down, 0).add(forward.multiply(fwd));
    }

    /* ====== Raycast de entidades ====== */
    private EntityHitResult raycastEntities(World world, PlayerEntity player, Vec3d start, Vec3d end) {
        Box search = new Box(start, end).expand(1.0);
        List<Entity> entities = world.getOtherEntities(
                player, search,
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isAttackable()
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

    /* ====== “Glow” simulado en bloque ====== */
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


    // ===== Aux internos para agrupar =====
    private static final class BlockAgg {
        final net.minecraft.util.Identifier id;
        final String display;
        int count = 1;
        double minDist;
        BlockAgg(net.minecraft.util.Identifier id, String display, double d) {
            this.id = id; this.display = display; this.minDist = d;
        }
    }

    private static final class MobAgg {
        final net.minecraft.util.Identifier id; // p.ej. minecraft:zombie
        final String baseName;                  // "Zombie", "Unemployed", etc.
        int count = 1;
        double minDist;
        MobAgg(net.minecraft.util.Identifier id, String baseName, double d) {
            this.id = id; this.baseName = baseName; this.minDist = d;
        }
    }

    private static String prettyName(String path) {
        if (path == null || path.isEmpty()) return "Unknown";
        String s = path.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }


    /** Escanea alrededor del impacto: ores y mobs en un radio.
     *  - Agrupa menas/mobs por nombre con "×N", guardando distancia mínima por grupo.
     *  - Spawnea partículas en ores y perímetro del AOE (cliente).
     *  - Aplica GLOW a mobs y coloca luz temporal sobre ores y cerca de mobs (servidor).
     *  - Hace un "burst" de luz temporal en varias direcciones desde el centro.
     *  - Envía al HUD nombres, distancias e IDs para dibujar íconos.
     */
    /** Escanea alrededor del impacto: ores y mobs en un radio.
     *  - Agrupa menas/mobs por nombre con "×N", guardando distancia mínima por grupo.
     *  - Spawnea partículas en ores y perímetro del AOE (cliente).
     *  - Aplica GLOW a mobs y coloca luz temporal sobre ores y cerca de mobs (servidor).
     *  - Envía al HUD nombres, distancias e IDs ya ORDENADOS/ALINEADOS para dibujar íconos.
     */
    private void scanSurroundings(World world, BlockPos center, PlayerEntity player, ItemStack scannerStack) {
        final int r = AOE_RADIUS;
        int budget = AOE_MAX_POINTS;

        // ====== MENAS AGRUPADAS (nombre -> grupo) con id de bloque representativo ======
        class GroupB {
            int count = 0;
            double minDist = Double.POSITIVE_INFINITY;
            net.minecraft.util.Identifier blockId = null;
        }
        java.util.Map<String, GroupB> oreGroups = new java.util.HashMap<>();
        int totalOres = 0;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = center.add(dx, dy, dz);
                    if (center.getSquaredDistance(p) > (r * r)) continue;

                    if (isImportantBlock(world, p)) {
                        totalOres++;

                        // Partículas visibles en la mena (CLIENTE)
                        if (world.isClient() && budget-- > 0) {
                            Vec3d c = Vec3d.ofCenter(p);
                            world.addParticle(ParticleTypes.GLOW, c.x, c.y, c.z, 0, 0, 0);
                            world.addParticle(ParticleTypes.END_ROD, c.x, c.y + 0.1, c.z, 0, 0.01, 0);
                            // halo simple
                            for (int i = 0; i < 6; i++) {
                                double a = (i / 6.0) * Math.PI * 2.0;
                                world.addParticle(ParticleTypes.END_ROD,
                                        c.x + Math.cos(a) * 0.35, c.y + 0.1, c.z + Math.sin(a) * 0.35,
                                        0, 0.01, 0);
                            }
                        }

                        // Luz temporal encima (SERVIDOR)
                        if (!world.isClient() && world instanceof net.minecraft.server.world.ServerWorld sw) {
                            BlockPos above = p.up();
                            com.elthisboy.cryonix.util.TempLightManager.placeTemporaryLight(sw, above, 12, 20 * 5);
                        }

                        // Agrupación + ID de bloque
                        BlockState st = world.getBlockState(p);
                        String baseName = st.getBlock().getName().getString();
                        double dist = Math.sqrt(player.squaredDistanceTo(
                                p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
                        var id = net.minecraft.registry.Registries.BLOCK.getId(st.getBlock());

                        GroupB g = oreGroups.computeIfAbsent(baseName, k -> new GroupB());
                        g.count++;
                        if (dist < g.minDist) g.minDist = dist;
                        if (g.blockId == null) g.blockId = id;
                    }
                }
            }
        }

        // ====== MOBS AGRUPADOS (nombre -> grupo) con id de entity type representativo ======
        class GroupM {
            int count = 0;
            double minDist = Double.POSITIVE_INFINITY;
            net.minecraft.util.Identifier typeId = null;
        }
        java.util.Map<String, GroupM> mobGroups = new java.util.HashMap<>();

        Box box = new Box(
                center.getX() - r, center.getY() - r, center.getZ() - r,
                center.getX() + r + 1, center.getY() + r + 1, center.getZ() + r + 1
        );
        java.util.List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator() && !(e instanceof PlayerEntity)
        );

        if (!world.isClient()) {
            // Glow a mobs + luz cerca de la cabeza (SERVIDOR)
            for (LivingEntity living : entities) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, AOE_GLOW_TICKS, 0, false, true));
            }
            if (world instanceof net.minecraft.server.world.ServerWorld sw) {
                for (LivingEntity living : entities) {
                    placeLightNearEntity(sw, living, 12, 20 * 5);
                }
            }
        } else {
            // Marcadores de mobs (CLIENTE)
            int mobBudget = Math.min(entities.size(), Math.max(4, AOE_MAX_POINTS / 2));
            for (int i = 0; i < mobBudget; i++) {
                LivingEntity e = entities.get(i);
                Vec3d c = e.getPos().add(0, e.getHeight() * 0.6, 0);
                world.addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, 0, 0.01, 0);
            }
        }

        for (LivingEntity e : entities) {
            String baseName = e.getDisplayName().getString();
            double dist = e.distanceTo(player);
            var id = net.minecraft.registry.Registries.ENTITY_TYPE.getId(e.getType());

            GroupM g = mobGroups.computeIfAbsent(baseName, k -> new GroupM());
            g.count++;
            if (dist < g.minDist) g.minDist = dist;
            if (g.typeId == null) g.typeId = id;
        }

        // ====== Construir filas y ORDENAR manteniendo ID alineado ======
        final class BlockHudRow {
            final String name; final double dist; final String id;
            BlockHudRow(String name, double dist, String id) { this.name = name; this.dist = dist; this.id = id; }
        }
        final class MobHudRow {
            final String name; final double dist; final String id;
            MobHudRow(String name, double dist, String id) { this.name = name; this.dist = dist; this.id = id; }
        }

        java.util.List<BlockHudRow> blockRows = new java.util.ArrayList<>();
        for (var entry : oreGroups.entrySet()) {
            String baseName = entry.getKey();
            GroupB g = entry.getValue();
            String label = (g.count >= 2) ? (baseName + " ×" + g.count) : baseName;
            String idStr = (g.blockId != null) ? g.blockId.toString() : "minecraft:stone";
            blockRows.add(new BlockHudRow(label, g.minDist, idStr));
        }
        blockRows.sort(java.util.Comparator.comparingDouble(rw -> rw.dist));

        java.util.List<MobHudRow> mobRows = new java.util.ArrayList<>();
        for (var entry : mobGroups.entrySet()) {
            String baseName = entry.getKey();
            GroupM g = entry.getValue();
            String label = (g.count >= 2) ? (baseName + " ×" + g.count) : baseName;
            String idStr = (g.typeId != null) ? g.typeId.toString() : "minecraft:pig";
            mobRows.add(new MobHudRow(label, g.minDist, idStr));
        }
        mobRows.sort(java.util.Comparator.comparingDouble(rw -> rw.dist));

        // Ahora sí: separar en listas paralelas ALINEADAS
        java.util.List<String> blocksN = new java.util.ArrayList<>();
        java.util.List<Double> blocksD = new java.util.ArrayList<>();
        java.util.List<String> blocksId = new java.util.ArrayList<>();
        for (BlockHudRow r0 : blockRows) { blocksN.add(r0.name); blocksD.add(r0.dist); blocksId.add(r0.id); }

        java.util.List<String> mobsN = new java.util.ArrayList<>();
        java.util.List<Double> mobsD = new java.util.ArrayList<>();
        java.util.List<String> mobsId = new java.util.ArrayList<>();
        for (MobHudRow r1 : mobRows) { mobsN.add(r1.name); mobsD.add(r1.dist); mobsId.add(r1.id); }

        // ====== Mensaje resumen ======
        player.sendMessage(
                Text.translatable("message.cryonix.scan.aoe",
                        String.valueOf(totalOres),                 // total menas (sin agrupar)
                        String.valueOf(entities.size())),          // total mobs (sin agrupar)
                true
        );

        // ====== Enviar al HUD (SOLO SERVIDOR) ======
        if (!world.isClient() && player instanceof ServerPlayerEntity spe) {
            int energyNow = getEnergy(scannerStack); // aunque ya no lo muestres
            CryonixNetworking.sendScanResults(
                    spe, blocksN, blocksD, blocksId, mobsN, mobsD, mobsId, energyNow, MAX_ENERGY
            );
        }

        // ====== Perímetro visual del radio (SOLO CLIENTE) ======
        if (world.isClient()) {
            spawnScanPerimeterParticles(world, center, AOE_RADIUS);
        }
    }




    /** Luz cerca de la cabeza del mob: busca aire cercano y coloca Blocks.LIGHT temporal. */
    private static boolean placeLightNearEntity(net.minecraft.server.world.ServerWorld sw,
                                                LivingEntity e, int level, int ttlTicks) {
        BlockPos base = BlockPos.ofFloored(e.getX(), e.getY() + e.getStandingEyeHeight(), e.getZ());
        BlockPos[] candidates = new BlockPos[] {
                base, base.up(), base.up(2),
                base.north(), base.south(), base.east(), base.west(),
                base.north().up(), base.south().up(), base.east().up(), base.west().up(),
                base.add(1, 1, 1), base.add(-1, 1, 1), base.add(1, 1, -1), base.add(-1, 1, -1)
        };
        for (BlockPos pos : candidates) {
            if (sw.getBlockState(pos).isAir()) {
                return com.elthisboy.cryonix.util.TempLightManager.placeTemporaryLight(sw, pos, level, ttlTicks);
            }
        }
        return false;
    }


    private boolean isImportantBlock(World world, BlockPos p) {
        BlockState state = world.getBlockState(p);
        if (state.isAir()) return false;
        if (state.isIn(ALL_ORES)) return true; // todos los ores (incluye mods que usen c:ores)

        // (Opcional) bloques compactados
        Block b = state.getBlock();
        return b == Blocks.RAW_COPPER_BLOCK
                || b == Blocks.COAL_BLOCK
                || b == Blocks.IRON_BLOCK
                || b == Blocks.GOLD_BLOCK
                || b == Blocks.DIAMOND_BLOCK
                || b == Blocks.EMERALD_BLOCK
                || b == Blocks.LAPIS_BLOCK
                || b == Blocks.REDSTONE_BLOCK
                || b == Blocks.NETHERITE_BLOCK
                || b == Blocks.COPPER_BLOCK;
    }

    /** Dibuja un anillo de partículas marcando el radio de escaneo (plano horizontal). */
    private void spawnScanPerimeterParticles(World world, BlockPos center, int radius) {
        if (!world.isClient()) return;

        // Ajusta densidad (más alto = más puntos)
        int points = Math.max(24, radius * 10); // 10 puntos por bloque de radio aprox
        double y = center.getY() + 0.5;         // altura del anillo

        double cx = center.getX() + 0.5;
        double cz = center.getZ() + 0.5;

        for (int i = 0; i < points; i++) {
            double t = (i / (double) points) * (Math.PI * 2.0);
            double px = cx + Math.cos(t) * radius;
            double pz = cz + Math.sin(t) * radius;

            world.addParticle(ParticleTypes.END_ROD, px, y, pz, 0, 0.01, 0);
        }

        // (Opcional) dos anillos extra para “volumen” ligero
        // comenta/borra si quieres ultra minimalista
        double yUp = y + 0.8;
        double yDn = y - 0.8;
        int points2 = points / 2;
        for (int i = 0; i < points2; i++) {
            double t = (i / (double) points2) * (Math.PI * 2.0);
            double px = cx + Math.cos(t) * radius;
            double pz = cz + Math.sin(t) * radius;

            world.addParticle(ParticleTypes.GLOW, px, yUp, pz, 0, 0, 0);
            world.addParticle(ParticleTypes.GLOW, px, yDn, pz, 0, 0, 0);
        }
    }
}