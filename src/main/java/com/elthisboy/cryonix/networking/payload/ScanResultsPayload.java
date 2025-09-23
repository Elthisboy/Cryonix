package com.elthisboy.cryonix.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

public record ScanResultsPayload(
        int energy,
        int max,
        List<String> blocksN, List<Double> blocksD,
        List<String> mobsN,   List<Double> mobsD
) implements CustomPayload {

    public static final CustomPayload.Id<ScanResultsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("cryonix", "scan_results"));

    public static final PacketCodec<RegistryByteBuf, ScanResultsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ScanResultsPayload::energy,
            PacketCodecs.VAR_INT, ScanResultsPayload::max,
            PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), ScanResultsPayload::blocksN,
            PacketCodecs.collection(ArrayList::new, PacketCodecs.DOUBLE), ScanResultsPayload::blocksD,
            PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), ScanResultsPayload::mobsN,
            PacketCodecs.collection(ArrayList::new, PacketCodecs.DOUBLE), ScanResultsPayload::mobsD,
            ScanResultsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}