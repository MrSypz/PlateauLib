package com.sypztep.plateau.common.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
@Deprecated(forRemoval = true)
public final class Vec3dCodecs {
    public static final Codec<Vec3> VEC3D_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(Vec3::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Vec3::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Vec3::z)
    ).apply(instance, Vec3::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3D_PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3D_PACKET_CODEC_COMPACT = StreamCodec.composite(
            ByteBufCodecs.FLOAT, vec3d -> (float) vec3d.x(),
            ByteBufCodecs.FLOAT, vec3d -> (float) vec3d.y(),
            ByteBufCodecs.FLOAT, vec3d -> (float) vec3d.z(),
            Vec3::new
    );
}
