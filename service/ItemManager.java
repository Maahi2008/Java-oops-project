package service;

import java.time.LocalDate;
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

    // Return active (unclaimed) items
    public ArrayList<Item> getActiveItems() {
        ArrayList<Item> active = new ArrayList<>();
        for (Item item : items) {
            if ("ACTIVE".equalsIgnoreCase(item.getStatus())) {
                active.add(item);
            }
        }
        return active;
    }

    // Return claimed/resolved items
    public ArrayList<Item> getClaimedItems() {
        ArrayList<Item> claimed = new ArrayList<>();
        for (Item item : items) {
            if ("CLAIMED".equalsIgnoreCase(item.getStatus())) {
                claimed.add(item);
            }
        }
        return claimed;
    }

    // Mark an item as claimed
    public boolean markAsClaimed(String id, String claimant) {
        Item item = findById(id);
        if (item != null) {
            item.markClaimed(claimant, LocalDate.now().toString());
            return true;
        }
        return false;
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
