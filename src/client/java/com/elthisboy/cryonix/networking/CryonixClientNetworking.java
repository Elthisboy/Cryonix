package com.elthisboy.cryonix.networking;

import com.elthisboy.cryonix.Cryonix;
import com.elthisboy.cryonix.client.hud.ScanHudData;
import com.elthisboy.cryonix.networking.payload.ScanResultsPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import com.elthisboy.cryonix.client.hud.ScanHudData;
import com.elthisboy.cryonix.networking.payload.ScanResultsPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class CryonixClientNetworking {
    private CryonixClientNetworking() {}

    public static void initClient() {
        Cryonix.LOGGER.info("[SCAN:CLIENT] Registering S2C handler...");

        // Handler del payload S2C
        ClientPlayNetworking.registerGlobalReceiver(
                ScanResultsPayload.ID,
                (ScanResultsPayload payload, ClientPlayNetworking.Context ctx) -> {
                    ctx.client().execute(() -> {
                        Cryonix.LOGGER.info(
                                "[SCAN:CLIENT] Received payload blocks={}, mobs={}",
                                payload.blocksN().size(),
                                payload.mobsN().size()
                        );
                        handleScanResults(payload);
                    });
                }
        );

        // Limpia el HUD al desconectar para que no quede “pegado”
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ScanHudData.clear();
            Cryonix.LOGGER.info("[SCAN:CLIENT] Cleared HUD on disconnect");
        });
    }

    // Ícono para bloque/ore por id
    private static ItemStack iconForBlockId(String blockIdStr) {
        try {
            Identifier id = Identifier.of(blockIdStr);
            var block = Registries.BLOCK.get(id);
            if (block != null) {
                Item item = block.asItem();
                if (item != null && item != Items.AIR) {
                    return new ItemStack(item);
                }
            }
        } catch (Exception ignored) {}
        return new ItemStack(Items.STONE);
    }

    // Ícono para entidad: usa el huevo si existe; si no, fallback por categoría
    private static ItemStack iconForEntityType(EntityType<?> type, boolean villager, boolean hostile) {
        // ✅ FIX: usar la API correcta en 1.21.1
        SpawnEggItem egg = SpawnEggItem.forEntity(type);
        if (egg != null) {
            return new ItemStack(egg);
        }
        // Fallbacks visuales
        if (villager)  return new ItemStack(Items.EMERALD);
        if (hostile)   return new ItemStack(Items.REDSTONE_TORCH);
        return new ItemStack(Items.WHEAT);
    }

    private static boolean isHostile(SpawnGroup group) {
        return !group.isPeaceful() && group != SpawnGroup.WATER_AMBIENT && group != SpawnGroup.AMBIENT;
    }

    private static void handleScanResults(ScanResultsPayload p) {
        List<ScanHudData.BlockEntry> blocks = new ArrayList<>();
        List<ScanHudData.MobEntry> mobs = new ArrayList<>();

        // === Bloques / Ores ===
        for (int i = 0; i < p.blocksN().size(); i++) {
            String name = p.blocksN().get(i);
            double dist = p.blocksD().get(i);
            String idStr = (i < p.blocksId().size()) ? p.blocksId().get(i) : "minecraft:stone";
            ItemStack icon = iconForBlockId(idStr);
            blocks.add(new ScanHudData.BlockEntry(name, dist, icon));
        }

        // === Mobs ===
        for (int i = 0; i < p.mobsN().size(); i++) {
            String baseName = p.mobsN().get(i);
            double dist = p.mobsD().get(i);
            String idStr = (i < p.mobsId().size()) ? p.mobsId().get(i) : "minecraft:pig";

            Identifier id = Identifier.of(idStr);
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);

            boolean villager = type == EntityType.VILLAGER;
            boolean hostile  = type != null && isHostile(type.getSpawnGroup());

            // Etiqueta: añade "Villager" si falta
            String label = baseName;
            if (villager) {
                String lower = baseName.toLowerCase();
                if (!lower.contains("villager") && !lower.contains("aldeano")) {
                    label = "Villager (" + baseName + ")";
                }
            }

            // Color por categoría
            int color;
            if (villager)        color = 0x00AAFF; // azul
            else if (hostile)    color = 0xFF6B6B; // rojo
            else                 color = 0x7CFFB2; // verde

            ItemStack icon = (type != null)
                    ? iconForEntityType(type, villager, hostile)
                    : new ItemStack(Items.WHEAT);

            mobs.add(new ScanHudData.MobEntry(label, dist, icon, color));
        }

        boolean nothing = blocks.isEmpty() && mobs.isEmpty();
        ScanHudData.update(blocks, mobs, nothing);
    }
}