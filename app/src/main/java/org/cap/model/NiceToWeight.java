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

    public static final List<Long> niceToWMult = Arrays.asList(
        /* -20 */     48388L,     59856L,     76040L,     92818L,    118348L,
        /* -15 */    147320L,    184698L,    229616L,    287308L,    360437L,
        /* -10 */    449829L,    563644L,    704093L,    875809L,   1099582L,
        /*  -5 */   1376151L,   1717300L,   2157191L,   2708050L,   3363326L,
        /*   0 */   4194304L,   5237765L,   6557202L,   8165337L,  10153587L,
        /*   5 */  12820798L,  15790321L,  19976592L,  24970740L,  31350126L,
        /*  10 */  39045157L,  49367440L,  61356676L,  76695844L,  95443717L,
        /*  15 */ 119304647L, 148102320L, 186737708L, 238609294L, 286331153L
    );

    public static int getWeight(int nice) {
        int index = nice + 20;
        int weight = niceToWeight.get(index);
        return weight;
    }

    public static long getWeightMul(int nice) {
        int index = nice + 20;
        long weight_mult = niceToWMult.get(index);
        return weight_mult;
    }

}
