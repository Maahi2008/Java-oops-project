
package service;

import model.Item;
import java.util.ArrayList;

public class SearchService {

    private final ItemManager manager;

    public SearchService(ItemManager manager) {
        this.manager = manager;
    }

    // Search by item name
    public ArrayList<Item> searchByName(String name) {
        ArrayList<Item> results = new ArrayList<>();

        for (Item item : manager.getAllItems()) {
            if (item.getName().toLowerCase()
                    .contains(name.toLowerCase())) {
                results.add(item);
            }
        }

        return results;
    }

    // Search by category
    public ArrayList<Item> searchByCategory(String category) {
        ArrayList<Item> results = new ArrayList<>();

        for (Item item : manager.getAllItems()) {
            if (item.getCategory().equalsIgnoreCase(category)) {
                results.add(item);
            }
        }

        return results;
    }

    // Search by location
    public ArrayList<Item> searchByLocation(String location) {
        ArrayList<Item> results = new ArrayList<>();

        for (Item item : manager.getAllItems()) {
            if (item.getLocation().toLowerCase()
                    .contains(location.toLowerCase())) {
                results.add(item);
            }
        }

        return results;
    }
}
