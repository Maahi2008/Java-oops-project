
import java.time.LocalDate;
import java.util.ArrayList;
import model.FoundItem;
import model.Item;
import model.LostItem;
import model.MatchResult;
import service.ItemManager;
import service.ItemMatcher;
import service.SearchService;
import storage.FileItemRepository;

public class Main {

    public static void main(String[] args) {

        try {
            // 1. Create manager and file repository
            ItemManager manager = new ItemManager();

            FileItemRepository repository =
                new FileItemRepository("items.dat");

            // 2. Load saved records
            ArrayList<Item> savedItems = repository.loadItems();

            if (savedItems.isEmpty()) {

                System.out.println("No saved records found. Creating sample data.");

                Item lostSample = new LostItem(
                    "L101",
                    "Black Wallet",
                    "Accessories",
                    "Black leather wallet",
                    "Library",
                    LocalDate.now(),
                    "Student A",
                    "studentA@example.com"
                );

                Item foundSample = new FoundItem(
                    "F101",
                    "Black Wallet",
                    "Accessories",
                    "Black wallet near reading desk",
                    "Library",
                    LocalDate.now(),
                    "Student B",
                    "studentB@example.com"
                );

                manager.addItem(lostSample);
                manager.addItem(foundSample);

                // Save initial sample records
                repository.saveItems(manager.getAllItems());

            } else {

                System.out.println("Loading saved records...");

                for (Item item : savedItems) {
                    manager.addItem(item);
                }
            }

            // 3. Display all records
            System.out.println("\n=== ALL ITEMS ===");
            manager.displayAllItems();

            // 4. Search by name
            SearchService search = new SearchService(manager);

            System.out.println("\n=== SEARCH BY NAME ===");

            ArrayList<Item> nameResults =
                search.searchByName("wallet");

            for (Item item : nameResults) {
                System.out.println(item);
            }

            // 5. Search by category
            System.out.println("\n=== SEARCH BY CATEGORY ===");

            for (Item item :
                    search.searchByCategory("Accessories")) {
                System.out.println(item);
            }

            // 6. Search by location
            System.out.println("\n=== SEARCH BY LOCATION ===");

            for (Item item :
                    search.searchByLocation("Library")) {
                System.out.println(item);
            }

            // 7. Search by ID
            System.out.println("\n=== SEARCH BY ID ===");

            Item result = manager.findById("L101");

            if (result != null) {
                System.out.println("Found: " + result);
            } else {
                System.out.println("Item not found.");
            }

            // 8. Find the lost item for matching
            System.out.println("\n=== POSSIBLE MATCHES ===");

            Item lostItem = manager.findById("L101");

            if (lostItem != null) {

                ItemMatcher matcher = new ItemMatcher();

                ArrayList<MatchResult> matches =
                    matcher.findMatches(
                        lostItem,
                        manager.getAllItems()
                    );

                if (matches.isEmpty()) {
                    System.out.println("No possible matches found.");
                } else {
                    for (MatchResult match : matches) {
                        System.out.println(match);
                        System.out.println("--------------------");
                    }
                }

            } else {
                System.out.println(
                    "Lost item L101 was not found, so matching was skipped."
                );
            }

            // 9. Confirm that records are stored
            System.out.println("\n=== STORAGE STATUS ===");
            System.out.println("Records currently stored: "
                    + manager.getAllItems().size());
            System.out.println("Storage file: items.dat");

        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
