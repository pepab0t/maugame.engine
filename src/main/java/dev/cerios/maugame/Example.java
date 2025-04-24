package dev.cerios.maugame;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class Example {
    public static void main(String[] args) {
        SequencedMap<String, Integer> map = new LinkedHashMap<>();

        map.put("A", 10);
        map.put("B", 2);
        map.put("C", 3);

        System.out.println(map.lastEntry());
    }
}
