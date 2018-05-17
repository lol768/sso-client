package uk.ac.warwick.sso.client.util;


import java.util.List;

public class ListUtils {
    /**
     * Get the first item of a list, or null if the list is empty.
     */
    public static <E> E head(List<E> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }
}
