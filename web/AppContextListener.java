package web;

import model.FoundItem;
import model.Item;
import model.LostItem;
import service.ItemManager;
import service.ItemMatcher;
import service.SearchService;
import storage.FileItemRepository;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;

@WebListener
public class AppContextListener implements ServletContextListener {

    public static final String ATTR_MANAGER = "itemManager";
    public static final String ATTR_REPOSITORY = "fileRepository";
    public static final String ATTR_SEARCH = "searchService";
    public static final String ATTR_MATCHER = "itemMatcher";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        // Determine storage path
        String storagePath = context.getInitParameter("storageFilePath");
        if (storagePath == null || storagePath.trim().isEmpty()) {
            // Check WEB-INF or project root
            String realPath = context.getRealPath("/WEB-INF/items.dat");
            if (realPath != null) {
                storagePath = realPath;
            } else {
                storagePath = "items.dat";
            }
        }

        System.out.println("[AppContextListener] Initializing Lost & Found System with storage: " + storagePath);

        ItemManager manager = new ItemManager();
        FileItemRepository repository = new FileItemRepository(storagePath);

        try {
            ArrayList<Item> savedItems = repository.loadItems();
            if (savedItems.isEmpty()) {
                System.out.println("[AppContextListener] No saved records in " + storagePath + ". Initializing sample data.");
                Item lostSample = new LostItem(
                        "L101",
                        "Black Wallet",
                        "Accessories",
                        "Black leather wallet with student ID and transit pass",
                        "Library",
                        LocalDate.now(),
                        "Student A",
                        "studentA@example.com"
                );

                Item foundSample = new FoundItem(
                        "F101",
                        "Black Wallet",
                        "Accessories",
                        "Black wallet near reading desk on 2nd floor",
                        "Library",
                        LocalDate.now(),
                        "Student B",
                        "studentB@example.com"
                );

                manager.addItem(lostSample);
                manager.addItem(foundSample);
                repository.saveItems(manager.getAllItems());
            } else {
                System.out.println("[AppContextListener] Loaded " + savedItems.size() + " items from " + storagePath);
                for (Item item : savedItems) {
                    manager.addItem(item);
                }
            }
        } catch (Exception e) {
            System.err.println("[AppContextListener] Error initializing repository: " + e.getMessage());
            e.printStackTrace();
        }

        SearchService searchService = new SearchService(manager);
        ItemMatcher matcher = new ItemMatcher();

        context.setAttribute(ATTR_MANAGER, manager);
        context.setAttribute(ATTR_REPOSITORY, repository);
        context.setAttribute(ATTR_SEARCH, searchService);
        context.setAttribute(ATTR_MATCHER, matcher);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        ItemManager manager = (ItemManager) context.getAttribute(ATTR_MANAGER);
        FileItemRepository repository = (FileItemRepository) context.getAttribute(ATTR_REPOSITORY);

        if (manager != null && repository != null) {
            try {
                repository.saveItems(manager.getAllItems());
                System.out.println("[AppContextListener] Final items saved successfully upon context shutdown.");
            } catch (Exception e) {
                System.err.println("[AppContextListener] Failed to persist items on shutdown: " + e.getMessage());
            }
        }
    }
}
