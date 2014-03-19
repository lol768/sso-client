package uk.ac.warwick.sso.client.cache;

import java.util.ArrayList;
import java.util.List;

public class SerializeUtils {
    private SerializeUtils() {}

    /**
     * Converts a List to an ArrayList if it isn't already one.
     * This is necessary for caching lists of items because List
     * is not itself serializable
     */
    public static <T> ArrayList<T> arrayList(final List<T> a) {
        if (a instanceof ArrayList<?>) {
            return (ArrayList<T>) a;
        } else {
            return new ArrayList<T>(a);
        }
    }
}