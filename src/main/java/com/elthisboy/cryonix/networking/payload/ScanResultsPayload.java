package com.elthisboy.cryonix.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.ArrayList;


public record ScanResultsPayload(
        int energy, int max,
        //centro y radio del escaneo
        long scanCenter, int scanRadius,
        // HUD: bloques
        List<String> blocksN, List<Double> blocksD, List<String> blocksId,
        // POSICIONES DE MENAS
        List<Long> orePos,
        // HUD: mobs
        List<String> mobsN, List<Double> mobsD, List<String> mobsId
) implements CustomPayload {

    public static final CustomPayload.Id<ScanResultsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("cryonix", "scan_results"));

    public static final PacketCodec<RegistryByteBuf, ScanResultsPayload> CODEC = new PacketCodec<>() {
        @Override
        public ScanResultsPayload decode(RegistryByteBuf buf) {
            int energy = buf.readVarInt();
            int max    = buf.readVarInt();
            long center= buf.readLong();
            int radius = buf.readVarInt();

            List<String> blocksN  = readStringList(buf);
            List<Double> blocksD  = readDoubleList(buf);
            List<String> blocksId = readStringList(buf);

            List<Long> orePos     = readLongList(buf);

            List<String> mobsN    = readStringList(buf);
            List<Double> mobsD    = readDoubleList(buf);
            List<String> mobsId   = readStringList(buf);

            return new ScanResultsPayload(energy, max, center, radius, blocksN, blocksD, blocksId, orePos, mobsN, mobsD, mobsId);
        }

        @Override
        public void encode(RegistryByteBuf buf, ScanResultsPayload v) {
            buf.writeVarInt(v.energy());
            buf.writeVarInt(v.max());
            buf.writeLong(v.scanCenter());
            buf.writeVarInt(v.scanRadius());

            writeStringList(buf, v.blocksN());
            writeDoubleList(buf, v.blocksD());
            writeStringList(buf, v.blocksId());

            writeLongList(buf, v.orePos());

            writeStringList(buf, v.mobsN());
            writeDoubleList(buf, v.mobsD());
            writeStringList(buf, v.mobsId());
        }

        private List<String> readStringList(RegistryByteBuf buf) {
            int n = buf.readVarInt();
            List<String> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(PacketCodecs.STRING.decode(buf));
            return out;
        }
        private void writeStringList(RegistryByteBuf buf, List<String> list) {
            buf.writeVarInt(list.size());
            for (String s : list) PacketCodecs.STRING.encode(buf, s);
        }

        private List<Double> readDoubleList(RegistryByteBuf buf) {
            int n = buf.readVarInt();
            List<Double> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(PacketCodecs.DOUBLE.decode(buf));
            return out;
        }
        private void writeDoubleList(RegistryByteBuf buf, List<Double> list) {
            buf.writeVarInt(list.size());
            for (Double d : list) PacketCodecs.DOUBLE.encode(buf, d);
        }

        private List<Long> readLongList(RegistryByteBuf buf) {
            int n = buf.readVarInt();
            List<Long> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(buf.readLong());
            return out;
        }
        private void writeLongList(RegistryByteBuf buf, List<Long> list) {
            buf.writeVarInt(list.size());
            for (Long l : list) buf.writeLong(l == null ? 0L : l);
        }
    };

    @Override public Id<? extends CustomPayload> getId() { return ID; }

    // Helpers cliente
    public BlockPos scanCenterBlockPos() { return BlockPos.fromLong(this.scanCenter()); }
    public int scanRadiusBlocks() { return Math.max(0, this.scanRadius()); }

    public List<BlockPos> oreBlockPos() {
        if (orePos == null || orePos.isEmpty()) return List.of();
        List<BlockPos> out = new ArrayList<>(orePos.size());
        for (Long packed : orePos) if (packed != null) out.add(BlockPos.fromLong(packed));
        return out;
    }
    public List<Vec3d> orePosAsVec3dCenters() {
        if (orePos == null || orePos.isEmpty()) return List.of();
        List<Vec3d> out = new ArrayList<>(orePos.size());
        for (Long packed : orePos) if (packed != null) {
            BlockPos bp = BlockPos.fromLong(packed);
            out.add(new Vec3d(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
        }
        return out;
    }
}