package com.elthisboy.cryonix.client.state;

import com.elthisboy.cryonix.client.util.RGBA;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Config de X-Ray para MOBS (por cliente):
 * - enabled (on/off)
 * - range (sincronizable externamente si quieres)
 * - lista de entidades con color RGBA y enabled
 *
 * Archivo: config/cryonix_mobxray.json
 */
public final class MobXrayState {

    private MobXrayState() {}

    private static boolean enabled = true;
    public static boolean enabled() { return enabled; }
    public static void setEnabled(boolean v) { enabled = v; }

    private static int range = 48; // se puede sincronizar desde la pistola si quieres
    public static int range() { return range; }
    public static void setRange(int r) { range = Math.max(8, Math.min(256, r)); }

    public static final class Target {
        public final Identifier id; public final RGBA color; public final boolean on;
        public Target(Identifier id, RGBA c, boolean on) { this.id = id; this.color = c; this.on = on; }
    }
    // --- nuevos campos ---
    private static boolean renderUnknown = true; // dibujar mobs no listados
    public static boolean renderUnknown() { return renderUnknown; }
    public static void setRenderUnknown(boolean v) { renderUnknown = v; }

    private static RGBA defaultColor = new RGBA(255, 255, 0, 200); // amarillo semi
    public static RGBA defaultColor() { return defaultColor; }
    public static void setDefaultColor(RGBA c) { defaultColor = c; }

    // --- helpers cómodos ---
    public static boolean shouldRender(Identifier id) {
        Target t = TARGETS.get(id);
        return (t != null) ? t.on : renderUnknown;   // <- si no está, decide con renderUnknown
    }
    public static RGBA colorForOrDefault(Identifier id) {
        Target t = TARGETS.get(id);
        return (t != null) ? t.color : defaultColor; // <- usa el color por defecto
    }
    private static final Map<Identifier, Target> TARGETS = new HashMap<>();
    public static Set<Identifier> targetIds() {
        HashSet<Identifier> s = new HashSet<>();
        for (Target t : TARGETS.values()) if (t.on) s.add(t.id);
        return s;
    }
    public static boolean isWanted(Identifier id) {
        Target t = TARGETS.get(id); return t != null && t.on;
    }
    public static RGBA colorFor(Identifier id, RGBA fallback) {
        Target t = TARGETS.get(id); return t != null ? t.color : fallback;
    }

    public static final Path CONFIG = Path.of("config", "cryonix", "mobxray.json");
    private static boolean loadedOnce = false;

    public static void loadTargetsFromConfig() {

        if (loadedOnce) return;
        loadedOnce = true;

        try {
            if (!Files.exists(CONFIG)) {
                TARGETS.clear();
                // ======= DEFAULTS (hostiles más comunes) =======
                addDefault("minecraft:zombie",      new RGBA(255,  80,  80, 220));
                addDefault("minecraft:husk",        new RGBA(255, 120,  80, 220));
                addDefault("minecraft:drowned",     new RGBA(120, 200, 255, 220));
                addDefault("minecraft:skeleton",    new RGBA(255, 255, 255, 220));
                addDefault("minecraft:stray",       new RGBA(180, 220, 255, 220));
                addDefault("minecraft:creeper",     new RGBA(120, 255, 120, 220));
                addDefault("minecraft:spider",      new RGBA(255,  80, 255, 220));
                addDefault("minecraft:cave_spider", new RGBA(200,  60, 255, 220));
                addDefault("minecraft:enderman",    new RGBA(220,   0, 255, 220));
                addDefault("minecraft:witch",       new RGBA(180,  60, 255, 220));
                addDefault("minecraft:slime",       new RGBA(120, 255, 160, 220));
                addDefault("minecraft:magma_cube",  new RGBA(255, 120,  60, 220));
                addDefault("minecraft:blaze",       new RGBA(255, 200,  60, 220));
                addDefault("minecraft:guardian",    new RGBA( 60, 200, 200, 220));
                addDefault("minecraft:elder_guardian", new RGBA( 40, 160, 160, 220));
                addDefault("minecraft:phantom",     new RGBA(120, 180, 255, 220));
                addDefault("minecraft:warden",      new RGBA(  0, 255, 255, 240));

                // ======= Nether =======
                addDefault("minecraft:piglin",        new RGBA(255, 190, 120, 220));
                addDefault("minecraft:piglin_brute",  new RGBA(255, 140,  60, 240));
                addDefault("minecraft:zombified_piglin", new RGBA(180, 255, 180, 220));
                addDefault("minecraft:ghast",         new RGBA(255, 255, 255, 220));
                addDefault("minecraft:wither_skeleton", new RGBA(60, 60, 60, 220));
                addDefault("minecraft:hoglin",      new RGBA(255, 140, 120, 220));

                // ======= Pasivos comunes =======
                addDefault("minecraft:cow",         new RGBA(160, 120,  80, 180));
                addDefault("minecraft:pig",         new RGBA(255, 170, 170, 180));
                addDefault("minecraft:sheep",       new RGBA(240, 240, 240, 180));
                addDefault("minecraft:chicken",     new RGBA(255, 255, 255, 180));
                addDefault("minecraft:villager",    new RGBA(160, 255, 160, 200));
                addDefault("minecraft:horse",         new RGBA(160, 120,  80, 180));
                addDefault("minecraft:donkey",        new RGBA(120, 100,  80, 180));
                addDefault("minecraft:mule",          new RGBA(140, 110,  80, 180));
                addDefault("minecraft:llama",         new RGBA(200, 200, 160, 180));
                addDefault("minecraft:trader_llama",  new RGBA(200, 160, 120, 180));
                addDefault("minecraft:cat",           new RGBA(200, 200, 200, 180));
                addDefault("minecraft:wolf",          new RGBA(200, 200, 200, 180));
                addDefault("minecraft:parrot",        new RGBA(255, 100, 100, 200));
                addDefault("minecraft:rabbit",        new RGBA(240, 200, 180, 180));
                addDefault("minecraft:mooshroom",     new RGBA(255, 80, 80, 200));
                addDefault("minecraft:fox",           new RGBA(255, 140, 80, 200));
                addDefault("minecraft:ocelot",        new RGBA(255, 220, 120, 200));
                addDefault("minecraft:bee",           new RGBA(255, 220, 80, 200));
                addDefault("minecraft:panda",         new RGBA(255, 255, 255, 200));
                addDefault("minecraft:polar_bear",    new RGBA(240, 240, 240, 200));
                addDefault("minecraft:turtle",        new RGBA(120, 200, 120, 200));
                addDefault("minecraft:axolotl",       new RGBA(255, 180, 200, 200));
                addDefault("minecraft:frog",          new RGBA(200, 180, 120, 200));
                addDefault("minecraft:allay",         new RGBA(120, 200, 255, 220));

                // ======= Aldeanos variante =======
                addDefault("minecraft:wandering_trader", new RGBA(160, 200, 255, 200));

                // ======= Jefes =======
                addDefault("minecraft:wither",        new RGBA(80, 0, 80, 240));
                addDefault("minecraft:ender_dragon",  new RGBA(40, 0, 80, 240));

                // ======= Illagers =======
                addDefault("minecraft:illusioner",    new RGBA(120, 120, 255, 220));
                addDefault("minecraft:pillager",    new RGBA(200, 200, 200, 220));
                addDefault("minecraft:vindicator",  new RGBA(200, 180, 180, 220));
                addDefault("minecraft:evoker",      new RGBA(255, 180, 255, 220));
                addDefault("minecraft:ravager",     new RGBA(180, 180, 160, 220));

                // ======= Peces =======
                addDefault("minecraft:cod",           new RGBA(180, 120, 80, 180));
                addDefault("minecraft:salmon",        new RGBA(255, 120, 120, 180));
                addDefault("minecraft:tropical_fish", new RGBA(255, 200, 80, 200));
                addDefault("minecraft:pufferfish",    new RGBA(255, 220, 120, 200));


                // ======= Otros =======
                addDefault("minecraft:bat",           new RGBA(120, 120, 120, 180));
                addDefault("minecraft:squid",         new RGBA(60, 60, 160, 200));
                addDefault("minecraft:glow_squid",    new RGBA(0, 255, 200, 220));
                addDefault("minecraft:strider",       new RGBA(255, 100, 80, 220));

                // Defaults para flags nuevas
                renderUnknown = true;
                defaultColor  = new RGBA(255, 255, 0, 200);

                save();
                return;



            }

            JsonObject root = JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();
            TARGETS.clear();

            enabled = root.has("enabled") ? root.get("enabled").getAsBoolean() : true;
            range   = root.has("range")   ? Math.max(8, Math.min(256, root.get("range").getAsInt())) : 48;

            //renderUnknown y defaultColor
            renderUnknown = root.has("renderUnknown") ? root.get("renderUnknown").getAsBoolean() : true;


            if (root.has("defaultColor")) {
                JsonObject dc = root.getAsJsonObject("defaultColor");
                defaultColor = new RGBA(
                        dc.get("r").getAsInt(),
                        dc.get("g").getAsInt(),
                        dc.get("b").getAsInt(),
                        dc.get("a").getAsInt()
                );
            } else {
                defaultColor = new RGBA(255, 255, 0, 200);
            }

            if (root.has("targets")) {
                for (var el : root.getAsJsonArray("targets")) {
                    JsonObject o = el.getAsJsonObject();
                    String s = o.get("id").getAsString();
                    Identifier id = Identifier.tryParse(s);
                    if (id == null) continue;
                    if (!Registries.ENTITY_TYPE.containsId(id)) continue;
                    TARGETS.put(id, new Target(
                            id,
                            new RGBA(o.get("r").getAsInt(), o.get("g").getAsInt(), o.get("b").getAsInt(), o.get("a").getAsInt()),
                            o.get("enabled").getAsBoolean()
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); TARGETS.clear();
        }
    }


    private static void addDefault(String id, RGBA c) {
        Identifier i = Identifier.tryParse(id);
        if (i != null) {
            TARGETS.put(i, new Target(i, c, true));
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            JsonArray arr = new JsonArray();
            for (Target t : TARGETS.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", t.id.toString());
                o.addProperty("r", t.color.r); o.addProperty("g", t.color.g);
                o.addProperty("b", t.color.b); o.addProperty("a", t.color.a);
                o.addProperty("enabled", t.on);
                arr.add(o);
            }
            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("range", range);

            // NUEVO: persistir renderUnknown y defaultColor
            root.addProperty("renderUnknown", renderUnknown);
            JsonObject dc = new JsonObject();
            dc.addProperty("r", defaultColor.r);
            dc.addProperty("g", defaultColor.g);
            dc.addProperty("b", defaultColor.b);
            dc.addProperty("a", defaultColor.a);
            root.add("defaultColor", dc);

            root.add("targets", arr);

            try (Writer w = new OutputStreamWriter(new FileOutputStream(CONFIG.toFile()), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}