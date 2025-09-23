package com.elthisboy.cryonix.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

public record ScanResultsPayload(
        int energy, int max,
        List<String> blocksN, List<Double> blocksD, List<String> blocksId,
        List<String> mobsN,   List<Double> mobsD,   List<String> mobsId
) implements CustomPayload {

    public static final CustomPayload.Id<ScanResultsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("cryonix", "scan_results"));

    // Codec manual: escribimos/lemos campo por campo
    public static final PacketCodec<RegistryByteBuf, ScanResultsPayload> CODEC = new PacketCodec<>() {
        @Override
        public ScanResultsPayload decode(RegistryByteBuf buf) {
            int energy = buf.readVarInt();
            int max    = buf.readVarInt();

            List<String> blocksN  = readStringList(buf);
            List<Double> blocksD  = readDoubleList(buf);
            List<String> blocksId = readStringList(buf);

            List<String> mobsN    = readStringList(buf);
            List<Double> mobsD    = readDoubleList(buf);
            List<String> mobsId   = readStringList(buf);

            return new ScanResultsPayload(energy, max, blocksN, blocksD, blocksId, mobsN, mobsD, mobsId);
        }

        @Override
        public void encode(RegistryByteBuf buf, ScanResultsPayload value) {
            buf.writeVarInt(value.energy());
            buf.writeVarInt(value.max());

            writeStringList(buf, value.blocksN());
            writeDoubleList(buf, value.blocksD());
            writeStringList(buf, value.blocksId());

            writeStringList(buf, value.mobsN());
            writeDoubleList(buf, value.mobsD());
            writeStringList(buf, value.mobsId());
        }

        // ===== helpers =====
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
    };

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}