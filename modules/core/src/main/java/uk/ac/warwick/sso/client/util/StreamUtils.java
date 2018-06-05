package uk.ac.warwick.sso.client.util;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtils {
    private StreamUtils() {}

    public static <K,V> Map<K,V> filterMapValues(Map<K,V> map, Predicate<V> predicate) {
        return filterMapValues(map.entrySet().stream(), predicate);
    }

    public static <K,V> Map<K,V> filterMapValues(Stream<Map.Entry<K, V>> values, Predicate<V> predicate) {
        return values
                .filter(e -> predicate.test(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
