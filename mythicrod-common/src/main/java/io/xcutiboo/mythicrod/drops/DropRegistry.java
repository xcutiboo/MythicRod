package io.xcutiboo.mythicrod.drops;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DropRegistry {
    private final Map<String, List<CustomDrop>> categories = new ConcurrentHashMap<>();

    public void registerCategory(String category, List<CustomDrop> drops) {
        categories.put(category, new ArrayList<>(drops));
    }

    public List<CustomDrop> getDrops(String category) {
        return categories.getOrDefault(category, Collections.emptyList());
    }

    public Set<String> getCategories() {
        return categories.keySet();
    }

    public void clear() {
        categories.clear();
    }

    public int getTotalDropCount() {
        return categories.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    public int getCategoryCount() {
        return categories.size();
    }

    public List<CustomDrop> getAllDrops() {
        List<CustomDrop> allDrops = new ArrayList<>();
        categories.values().forEach(allDrops::addAll);
        return allDrops;
    }
}
