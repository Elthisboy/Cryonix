package com.elthisboy.cryonix.client.state;

import com.elthisboy.cryonix.client.util.RGBA;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XrayState {
    private static boolean enabled = true;
    public static boolean enabled(){ return enabled; }
    public static void setEnabled(boolean v){ enabled = v; }

    private static int range = 48; //se sincroniza desde la pistola
    public static int range(){ return range; }
    public static void setRange(int r){ range = Math.max(8, Math.min(256, r)); }

    private static RGBA DEFAULT_ORE_COLOR = new RGBA(140, 255, 220, 160);
    public static RGBA getDefaultOreColor(){ return DEFAULT_ORE_COLOR; }
    public static void setDefaultOreColor(RGBA c){ DEFAULT_ORE_COLOR = c; }

    private static final TagKey<net.minecraft.block.Block> C_ORES     =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));
    private static final TagKey<Block> FORGE_ORES =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of("forge", "ores"));

    public static boolean isOre(BlockState st){
        if (st == null) return false;
        if (st.isIn(C_ORES) || st.isIn(FORGE_ORES)) return true;
        Identifier id = Registries.BLOCK.getId(st.getBlock());
        String p = id.getPath();
        return p.contains("ore") || p.contains("debris");
    }

    public static class Target {
        public final Identifier id; public final RGBA color; public final boolean on;
        public Target(Identifier id, RGBA c, boolean on){ this.id=id; this.color=c; this.on=on; }
    }


    private static final Map<Identifier,Target> TARGETS = new HashMap<>();

    public static Set<Identifier> targetIds(){
        HashSet<Identifier> s = new HashSet<>();
        for (Target t: TARGETS.values()) if (t.on) s.add(t.id);
        return s;
    }

    public static boolean isWanted(Identifier id){
        Target t = TARGETS.get(id); return t!=null && t.on;
    }

    public static boolean matches(BlockState st){
        Identifier id = Registries.BLOCK.getId(st.getBlock());
        Target t = TARGETS.get(id);
        if (t != null) return t.on;
        return isOre(st); // si no está en JSON pero es ore → sí se muestra
    }

    public static RGBA colorFor(BlockState st, RGBA fb){
        Identifier id = Registries.BLOCK.getId(st.getBlock());
        Target t = TARGETS.get(id);
        if (t != null) return t.color;
        return isOre(st) ? DEFAULT_ORE_COLOR : fb;
    }

    public static RGBA colorFor(Identifier id, RGBA fb){
        Target t = TARGETS.get(id); return t!=null ? t.color : fb;
    }






    public static final Path CONFIG = Path.of("config", "cryonix", "block_xray.json");

    public static void loadTargetsFromConfig(){
        try{
            if (!Files.exists(CONFIG)){
                TARGETS.clear();

                //SPAWNERS
                addDefault("minecraft:trial_spawner",    new RGBA(255,128,0,200));
                addDefault("minecraft:spawner",          new RGBA(255,128,0,200));

                //COAL
                addDefault("minecraft:coal_ore",             new RGBA(30,30,30,180));
                addDefault("minecraft:deepslate_coal_ore",   new RGBA(30,30,30,180));

                //IRON
                addDefault("minecraft:iron_ore",             new RGBA(200,130,80,180));
                addDefault("minecraft:deepslate_iron_ore",   new RGBA(200,130,80,180));

                //COPPER
                addDefault("minecraft:copper_ore",           new RGBA(255,120,50,180));
                addDefault("minecraft:deepslate_copper_ore", new RGBA(255,120,50,180));

                //GOLD
                addDefault("minecraft:gold_ore",             new RGBA(255,220,40,180));
                addDefault("minecraft:deepslate_gold_ore",   new RGBA(255,220,40,180));

                //REDSTONE
                addDefault("minecraft:redstone_ore",         new RGBA(255,0,0,200));
                addDefault("minecraft:deepslate_redstone_ore", new RGBA(255,0,0,200));

                //LAPIS
                addDefault("minecraft:lapis_ore",            new RGBA(0,0,255,200));
                addDefault("minecraft:deepslate_lapis_ore",  new RGBA(0,0,255,200));

                //DIAMOND
                addDefault("minecraft:diamond_ore",          new RGBA(0,255,255,200));
                addDefault("minecraft:deepslate_diamond_ore",new RGBA(0,255,255,200));

                //EMERALD
                addDefault("minecraft:emerald_ore",          new RGBA(0,255,0,200));
                addDefault("minecraft:deepslate_emerald_ore",new RGBA(0,255,0,200));

                //NETHER
                addDefault("minecraft:nether_quartz_ore",    new RGBA(255,255,255,180));
                addDefault("minecraft:nether_gold_ore",      new RGBA(255,200,40,180));
                addDefault("minecraft:ancient_debris",       new RGBA(255,128,0,200));
                save();
                return;
            }

            JsonObject root = JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();
            TARGETS.clear();
            for (var el: root.getAsJsonArray("targets")){
                JsonObject o = el.getAsJsonObject();
                String s = o.get("id").getAsString();
                Identifier id = Identifier.tryParse(s);
                if (id == null) continue;                if (!Registries.BLOCK.containsId(id)) continue;
                TARGETS.put(id, new Target(
                        id,
                        new RGBA(o.get("r").getAsInt(), o.get("g").getAsInt(), o.get("b").getAsInt(), o.get("a").getAsInt()),
                        o.get("enabled").getAsBoolean()
                ));
            }
        }catch (Exception e){ e.printStackTrace(); TARGETS.clear(); }
    }
    private static void addDefault(String id, RGBA c){
        Identifier i = Identifier.tryParse(id);
        if (i != null) {
            TARGETS.put(i, new Target(i, c, true));
        }    }
    public static void save(){
        try{
            Files.createDirectories(CONFIG.getParent());
            JsonArray arr = new JsonArray();
            for (Target t: TARGETS.values()){
                JsonObject o = new JsonObject();
                o.addProperty("id", t.id.toString());
                o.addProperty("r", t.color.r); o.addProperty("g", t.color.g);
                o.addProperty("b", t.color.b); o.addProperty("a", t.color.a);
                o.addProperty("enabled", t.on);
                arr.add(o);
            }
            JsonObject root = new JsonObject(); root.add("targets", arr);
            try(Writer w = new OutputStreamWriter(new FileOutputStream(CONFIG.toFile()), StandardCharsets.UTF_8)){
                new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
            }
        }catch (IOException e){ e.printStackTrace(); }
    }
}