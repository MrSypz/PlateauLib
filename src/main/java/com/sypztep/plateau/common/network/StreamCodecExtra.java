package com.sypztep.plateau.common.network;

import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

public interface StreamCodecExtra {
    static <B, C,
            T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>
    StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> streamCodec,
            Function<C, T1> function,
            StreamCodec<? super B, T2> streamCodec2,
            Function<C, T2> function2,
            StreamCodec<? super B, T3> streamCodec3,
            Function<C, T3> function3,
            StreamCodec<? super B, T4> streamCodec4,
            Function<C, T4> function4,
            StreamCodec<? super B, T5> streamCodec5,
            Function<C, T5> function5,
            StreamCodec<? super B, T6> streamCodec6,
            Function<C, T6> function6,
            StreamCodec<? super B, T7> streamCodec7,
            Function<C, T7> function7,
            StreamCodec<? super B, T8> streamCodec8,
            Function<C, T8> function8,
            StreamCodec<? super B, T9> streamCodec9,
            Function<C, T9> function9,
            StreamCodec<? super B, T10> streamCodec10,
            Function<C, T10> function10,
            StreamCodec<? super B, T11> streamCodec11,
            Function<C, T11> function11,
            StreamCodec<? super B, T12> streamCodec12,
            Function<C, T12> function12,
            StreamCodec<? super B, T13> streamCodec13,
            Function<C, T13> function13,
            Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, C> constructor
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B object) {
                T1 v1 = streamCodec.decode(object);
                T2 v2 = streamCodec2.decode(object);
                T3 v3 = streamCodec3.decode(object);
                T4 v4 = streamCodec4.decode(object);
                T5 v5 = streamCodec5.decode(object);
                T6 v6 = streamCodec6.decode(object);
                T7 v7 = streamCodec7.decode(object);
                T8 v8 = streamCodec8.decode(object);
                T9 v9 = streamCodec9.decode(object);
                T10 v10 = streamCodec10.decode(object);
                T11 v11 = streamCodec11.decode(object);
                T12 v12 = streamCodec12.decode(object);
                T13 v13 = streamCodec13.decode(object);

                return constructor.apply(
                        v1, v2, v3, v4, v5, v6, v7,
                        v8, v9, v10, v11, v12, v13
                );
            }

            @Override
            public void encode(B object, C value) {
                streamCodec.encode(object, function.apply(value));
                streamCodec2.encode(object, function2.apply(value));
                streamCodec3.encode(object, function3.apply(value));
                streamCodec4.encode(object, function4.apply(value));
                streamCodec5.encode(object, function5.apply(value));
                streamCodec6.encode(object, function6.apply(value));
                streamCodec7.encode(object, function7.apply(value));
                streamCodec8.encode(object, function8.apply(value));
                streamCodec9.encode(object, function9.apply(value));
                streamCodec10.encode(object, function10.apply(value));
                streamCodec11.encode(object, function11.apply(value));
                streamCodec12.encode(object, function12.apply(value));
                streamCodec13.encode(object, function13.apply(value));
            }
        };
    }
    @FunctionalInterface
    interface Function13<A, B, C, D, E, F, G, H, I, J, K, L, M, R> {
        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m);
    }
}
