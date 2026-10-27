import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;
import model.FoundItem;
import model.Item;
import model.LostItem;
import model.MatchResult;
import service.ItemManager;
import service.ItemMatcher;
import service.SearchService;
import storage.FileItemRepository;

/**
 * Interactive Command-Line Console for OOP Evaluation Defense
 * Allows real-time terminal testing without requiring a web browser:
 * - Adding lost/found items
 * - Searching items
 * - Running matching engine
 * - Verifying and claiming items
 * - Demonstrating persistence across restarts
 */
public class InteractiveConsole {

    private static final String STORAGE_FILE = "items.dat";
    private static ItemManager manager;
    private static FileItemRepository repository;
    private static SearchService searchService;
    private static ItemMatcher matcher;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        init();

        System.out.println("=================================================================");
        System.out.println("   MANIPAL UNIVERSITY JAIPUR - CAMPUS LOST & FOUND SYSTEM        ");
        System.out.println("   Interactive Java OOP Console Test Driver                      ");
        System.out.println("=================================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter your choice (1-8): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    reportLostItem();
                    break;
                case "2":
                    reportFoundItem();
                    break;
                case "3":
                    displayAllItems();
                    break;
                case "4":
                    searchMenu();
                    break;
                case "5":
                    runMatchingEngine();
                    break;
                case "6":
                    claimItem();
                    break;
                case "7":
                    System.out.println("\nRunning Automated Test Suite...\n");
                    JavaTestSuite.main(new String[0]);
                    break;
                case "8":
                    System.out.println("\nExiting. Thank you! All records saved in " + STORAGE_FILE);
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please select 1 to 8.");
            }
        }
    }

    private static void init() {
        manager = new ItemManager();
        repository = new FileItemRepository(STORAGE_FILE);
        matcher = new ItemMatcher();

        try {
            ArrayList<Item> loaded = repository.loadItems();
            for (Item item : loaded) {
                manager.addItem(item);
            }
        } catch (Exception e) {
            System.err.println("Note: Could not load initial items from " + STORAGE_FILE + ": " + e.getMessage());
        }
        searchService = new SearchService(manager);
    }

    private static void printMenu() {
        System.out.println("\n-------------------------------------------------------------");
        System.out.println(" [1] Report a Lost Item (Add LostItem)");
        System.out.println(" [2] Report a Found Item (Add FoundItem)");
        System.out.println(" [3] View All Campus Records (Polymorphic Display)");
        System.out.println(" [4] Search Records (by Name, Category, Location, ID)");
        System.out.println(" [5] Run Intelligent Match Engine (Preference-Wise Order)");
        System.out.println(" [6] Verify & Mark Item as Claimed (State Transition)");
        System.out.println(" [7] Run Automated Java Test Suite (Rubric Verification)");
        System.out.println(" [8] Exit");
        System.out.println("-------------------------------------------------------------");
    }

    private static void reportLostItem() {
        System.out.println("\n--- REPORT LOST ITEM ---");
        System.out.print("Item Name (e.g. Blue HydroFlask Bottle): ");
        String name = scanner.nextLine().trim();
        System.out.print("Category (Electronics/Accessories/Wallets & Bags/Other): ");
        String cat = scanner.nextLine().trim();
        System.out.print("MUJ Campus Location (Library/AB1/AB2/AB3/LHC/Hostels/Food Court): ");
        String loc = scanner.nextLine().trim();
        System.out.print("Student Registration Number (e.g. 229301045): ");
        String regNo = scanner.nextLine().trim();
        System.out.print("Contact Email or Phone: ");
        String contact = scanner.nextLine().trim();
        System.out.print("Description & Marks: ");
        String desc = scanner.nextLine().trim();

        long count = manager.getAllItems().stream().filter(i -> "LOST".equalsIgnoreCase(i.getType())).count() + 1;
        String id = "L" + (100 + count);

        Item lost = new LostItem(id, name, cat, desc, loc, LocalDate.now(), "Student", contact, regNo);
        manager.addItem(lost);
        try {
            repository.saveItems(manager.getAllItems());
            System.out.println(">> SUCCESS: Lost notice registered with ID " + id + " and persisted to " + STORAGE_FILE);
        } catch (Exception e) {
            System.err.println(">> ERROR: Failed to save to disk: " + e.getMessage());
        }
    }

    private static void reportFoundItem() {
        System.out.println("\n--- REPORT FOUND ITEM ---");
        System.out.print("Item Name (e.g. Blue HydroFlask Bottle): ");
        String name = scanner.nextLine().trim();
        System.out.print("Category (Electronics/Accessories/Wallets & Bags/Other): ");
        String cat = scanner.nextLine().trim();
        System.out.print("MUJ Campus Location Found: ");
        String loc = scanner.nextLine().trim();
        System.out.print("Finder Student Reg No / Staff ID: ");
        String regNo = scanner.nextLine().trim();
        System.out.print("Safe Custody Contact Details: ");
        String contact = scanner.nextLine().trim();
        System.out.print("Description & Condition: ");
        String desc = scanner.nextLine().trim();

        long count = manager.getAllItems().stream().filter(i -> "FOUND".equalsIgnoreCase(i.getType())).count() + 1;
        String id = "F" + (100 + count);

        Item found = new FoundItem(id, name, cat, desc, loc, LocalDate.now(), "Finder/Security", contact, regNo);
        manager.addItem(found);
        try {
            repository.saveItems(manager.getAllItems());
            System.out.println(">> SUCCESS: Found item registered with ID " + id + " and persisted to " + STORAGE_FILE);
        } catch (Exception e) {
            System.err.println(">> ERROR: Failed to save to disk: " + e.getMessage());
        }
    }

    private static void displayAllItems() {
        System.out.println("\n=== ALL CAMPUS RECORDS (" + manager.getAllItems().size() + " Total) ===");
        manager.displayAllItems();
    }

    private static void searchMenu() {
        System.out.println("\n--- SEARCH RECORDS ---");
        System.out.println(" [A] Search by Name keyword");
        System.out.println(" [B] Search by Category");
        System.out.println(" [C] Search by Campus Location");
        System.out.println(" [D] Search by Item ID");
        System.out.print("Select search type (A-D): ");
        String sub = scanner.nextLine().trim().toUpperCase();

        ArrayList<Item> results = new ArrayList<>();
        switch (sub) {
            case "A":
                System.out.print("Enter name keyword: ");
                results = searchService.searchByName(scanner.nextLine().trim());
                break;
            case "B":
                System.out.print("Enter category: ");
                results = searchService.searchByCategory(scanner.nextLine().trim());
                break;
            case "C":
                System.out.print("Enter location keyword: ");
                results = searchService.searchByLocation(scanner.nextLine().trim());
                break;
            case "D":
                System.out.print("Enter exact Item ID: ");
                Item it = manager.findById(scanner.nextLine().trim());
                if (it != null) results.add(it);
                break;
            default:
                System.out.println("Invalid search choice.");
                return;
        }

        System.out.println("\nResults Found: " + results.size());
        for (Item item : results) {
            System.out.println(" -> " + item);
        }
    }

    private static void runMatchingEngine() {
        System.out.println("\n=== RUNNING INTELLIGENT MATCH ENGINE (PREFERENCE-WISE ORDER) ===");
        ArrayList<Item> lostItems = new ArrayList<>();
        for (Item it : manager.getAllItems()) {
            if ("LOST".equalsIgnoreCase(it.getType()) && !"CLAIMED".equalsIgnoreCase(it.getStatus())) {
                lostItems.add(it);
            }
        }

        if (lostItems.isEmpty()) {
            System.out.println("No active lost items currently in database.");
            return;
        }

        int totalPairsFound = 0;
        for (Item lost : lostItems) {
            ArrayList<MatchResult> matches = matcher.findMatches(lost, manager.getAllItems());
            if (!matches.isEmpty()) {
                System.out.println("\n=======================================================");
                System.out.println(" Matches for Lost Item [" + lost.getId() + "] " + lost.getName() + " (Reg: " + lost.getRegNo() + ")");
                System.out.println("=======================================================");
                int rank = 1;
                for (MatchResult mr : matches) {
                    System.out.printf(" >> PREFERENCE RANK #%d (Highest to Lowest Compatibility):\n", rank);
                    System.out.println("    Found Item: [" + mr.getFoundItem().getId() + "] " + mr.getFoundItem().getName());
                    System.out.println("    Category  : " + mr.getFoundItem().getCategory());
                    System.out.println("    Location  : " + mr.getFoundItem().getLocation());
                    System.out.println("    Finder ID : " + mr.getFoundItem().getRegNo());
                    System.out.println("    Contact   : " + mr.getFoundItem().getContact());
                    System.out.println("    Desc      : " + mr.getFoundItem().getDescription());
                    System.out.println("    ---------------------------------------------------");
                    rank++;
                    totalPairsFound++;
                }
            }
        }

        if (totalPairsFound == 0) {
            System.out.println("No candidate pairs scored >= 30% threshold. All records are currently distinct.");
        }
    }

    private static void claimItem() {
        System.out.println("\n--- VERIFY & MARK ITEM AS CLAIMED ---");
        System.out.print("Enter Item ID to claim (e.g. L101 or F101): ");
        String id = scanner.nextLine().trim();
        Item item = manager.findById(id);

        if (item == null) {
            System.out.println("Item ID " + id + " not found.");
            return;
        }

        if ("CLAIMED".equalsIgnoreCase(item.getStatus())) {
            System.out.println("Item " + id + " is ALREADY marked as CLAIMED by: " + item.getClaimedBy());
            return;
        }

        System.out.print("Enter Claimant Student Reg No / Name: ");
        String claimant = scanner.nextLine().trim();

        item.markClaimed(claimant, LocalDate.now().toString());
        try {
            repository.saveItems(manager.getAllItems());
            System.out.println(">> SUCCESS: Item " + id + " status updated to CLAIMED and saved to " + STORAGE_FILE);
            System.out.println(">> Notice: This item is now archived and will be excluded from the active matching queue.");
        } catch (Exception e) {
            System.err.println(">> ERROR: Failed to save to disk: " + e.getMessage());
        }
    }
}
