package org.cap.model;

import java.util.Arrays;
import java.util.List;

public class NiceToWeight {
    public static final List<Integer> niceToWeight = Arrays.asList(
            88761, 71755, 56483, 46273, 36291,
            29154, 23254, 18705, 14949, 11916,
            9548, 7620, 6100, 4904, 3906,
            3121, 2501, 1991, 1586, 1277,
            1024, 820, 655, 526, 423,
            335, 272, 215, 172, 137,
            110, 87, 70, 56, 45,
            36, 29, 23, 18, 15
    );

    public static int getWeight(int nice) {
        return niceToWeight.get(nice+20);
    }

}
