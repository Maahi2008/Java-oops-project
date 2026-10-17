
package service;

import java.util.ArrayList;
import model.Item;

public class ItemManager {

    // Stores all lost and found items
    private final ArrayList<Item> items = new ArrayList<>();

    // Add an item
    public void addItem(Item item) {
        items.add(item);
    }

    // Return all items
    public ArrayList<Item> getAllItems() {
        return new ArrayList<>(items);
    }

    // Display all items
    public void displayAllItems() {
        if (items.isEmpty()) {
            System.out.println("No items recorded.");
            return;
        }

        for (Item item : items) {
            System.out.println(item);
        }
    }

    // Find an item by ID
    public Item findById(String id) {
        for (Item item : items) {
            if (item.getId().equalsIgnoreCase(id)) {
                return item;
            }
        }

        return null;
    }

    // Remove an item by ID
    public boolean removeItem(String id) {
        return items.removeIf(
            item -> item.getId().equalsIgnoreCase(id)
        );
    }
}
