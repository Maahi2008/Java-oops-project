import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.FoundItem;
import model.Item;
import model.LostItem;
import model.MatchResult;
import service.ItemManager;
import service.ItemMatcher;
import service.SearchService;
import storage.FileItemRepository;
import web.JsonUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Built-in Standalone Development Server Launcher.
 * Allows immediate demonstration and testing of the web application
 * using the standard JDK HTTP server without requiring Tomcat setup.
 * Uses the exact same ItemManager, FileItemRepository (items.dat), and ItemMatcher.
 */
public class ServerLauncher {

    private static final int PORT = 8080;
    private static ItemManager manager;
    private static FileItemRepository repository;
    private static SearchService searchService;
    private static ItemMatcher matcher;

    public static void main(String[] args) {
        try {
            System.out.println("=================================================");
            System.out.println(" Campus Lost & Found System - Web Server Starting");
            System.out.println("=================================================");

            // 1. Initialize data layer with items.dat
            manager = new ItemManager();
            repository = new FileItemRepository("items.dat");

            ArrayList<Item> saved = repository.loadItems();
            if (saved.isEmpty()) {
                System.out.println("No saved items in items.dat. Loading initial samples.");
                Item lostSample = new LostItem(
                        "L101",
                        "Black Leather Wallet",
                        "Wallets & Bags",
                        "Black leather wallet with MUJ student ID and transit pass",
                        "Library",
                        LocalDate.now(),
                        "Aryan Sharma",
                        "aryan.229301045@muj.manipal.edu",
                        "229301045"
                );

                Item foundSample = new FoundItem(
                        "F101",
                        "Black Leather Wallet",
                        "Wallets & Bags",
                        "Black wallet near reading desk on 2nd floor",
                        "Library",
                        LocalDate.now(),
                        "Kavita Rao",
                        "kavita.229301128@muj.manipal.edu",
                        "229301128"
                );

                manager.addItem(lostSample);
                manager.addItem(foundSample);
                repository.saveItems(manager.getAllItems());
            } else {
                for (Item item : saved) {
                    manager.addItem(item);
                }
                System.out.println("Successfully loaded " + saved.size() + " records from items.dat");
            }

            searchService = new SearchService(manager);
            matcher = new ItemMatcher();

            // 2. Start HTTP Server
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // API Endpoints
            server.createContext("/api/dashboard", new DashboardHandler());
            server.createContext("/api/items", new ItemHandler());
            server.createContext("/api/matches", new MatchHandler());

            // Static Web Content Handler
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(null); // default executor
            server.start();

            System.out.println("Server running at: http://localhost:" + PORT + "/");
            System.out.println("Press Ctrl+C to terminate.");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Static files handler (serves webapp/index.html, css, js)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr.equals("/") || pathStr.isEmpty()) {
                pathStr = "/index.html";
            }

            // Look in webapp folder
            Path filePath = Paths.get("webapp", pathStr.substring(1));
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                filePath = Paths.get("webapp", "index.html");
            }

            if (Files.exists(filePath)) {
                String mimeType = "text/plain";
                String name = filePath.getFileName().toString().toLowerCase();
                if (name.endsWith(".html")) mimeType = "text/html; charset=UTF-8";
                else if (name.endsWith(".css")) mimeType = "text/css; charset=UTF-8";
                else if (name.endsWith(".js")) mimeType = "application/javascript; charset=UTF-8";
                else if (name.endsWith(".svg")) mimeType = "image/svg+xml";

                byte[] bytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    // GET /api/dashboard
    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            ArrayList<Item> allItems = manager.getAllItems();
            int lostCount = 0;
            int foundCount = 0;
            int totalCount = allItems.size();

            ArrayList<Item> lostList = new ArrayList<>();
            for (Item item : allItems) {
                if ("LOST".equalsIgnoreCase(item.getType())) {
                    lostCount++;
                    lostList.add(item);
                } else if ("FOUND".equalsIgnoreCase(item.getType())) {
                    foundCount++;
                }
            }

            int matchCount = 0;
            for (Item lost : lostList) {
                matchCount += matcher.findMatches(lost, allItems).size();
            }

            String json = JsonUtil.dashboardToJson(lostCount, foundCount, matchCount, totalCount);
            sendJsonResponse(exchange, 200, json);
        }
    }

    // GET /api/items, POST /api/items/lost, POST /api/items/found
    static class ItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
                String name = query.get("name");
                String category = query.get("category");
                String location = query.get("location");
                String type = query.get("type");

                ArrayList<Item> results;
                if (name != null && !name.isEmpty()) {
                    results = searchService.searchByName(name);
                } else if (category != null && !category.isEmpty()) {
                    results = searchService.searchByCategory(category);
                } else if (location != null && !location.isEmpty()) {
                    results = searchService.searchByLocation(location);
                } else {
                    results = manager.getAllItems();
                }

                ArrayList<Item> filtered = new ArrayList<>();
                for (Item item : results) {
                    if (category != null && !category.isEmpty() && !item.getCategory().equalsIgnoreCase(category)) {
                        continue;
                    }
                    if (location != null && !location.isEmpty() && !item.getLocation().toLowerCase().contains(location.toLowerCase())) {
                        continue;
                    }
                    if (type != null && !type.isEmpty() && !item.getType().equalsIgnoreCase(type)) {
                        continue;
                    }
                    filtered.add(item);
                }

                sendJsonResponse(exchange, 200, JsonUtil.itemsToJson(filtered));

            } else if ("POST".equalsIgnoreCase(method)) {
                // Read request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                String id = JsonUtil.extractJsonField(body, "id");
                String name = JsonUtil.extractJsonField(body, "name");
                String category = JsonUtil.extractJsonField(body, "category");
                String description = JsonUtil.extractJsonField(body, "description");
                String location = JsonUtil.extractJsonField(body, "location");
                String dateStr = JsonUtil.extractJsonField(body, "date");
                String reporterName = JsonUtil.extractJsonField(body, "reporterName");
                String contact = JsonUtil.extractJsonField(body, "contact");
                String regNo = JsonUtil.extractJsonField(body, "regNo");

                LocalDate date;
                try {
                    date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
                } catch (Exception e) {
                    date = LocalDate.now();
                }

                Item newItem;
                synchronized (manager) {
                    if (path.contains("/found")) {
                        if (id == null || id.isEmpty()) {
                            long count = manager.getAllItems().stream().filter(i -> "FOUND".equalsIgnoreCase(i.getType())).count();
                            id = "F" + (101 + count);
                        }
                        newItem = new FoundItem(id, name, category, description, location, date, reporterName, contact, regNo);
                    } else {
                        if (id == null || id.isEmpty()) {
                            long count = manager.getAllItems().stream().filter(i -> "LOST".equalsIgnoreCase(i.getType())).count();
                            id = "L" + (101 + count);
                        }
                        newItem = new LostItem(id, name, category, description, location, date, reporterName, contact, regNo);
                    }

                    manager.addItem(newItem);

                    try {
                        repository.saveItems(manager.getAllItems());
                        System.out.println("[ServerLauncher] Saved new item to items.dat: " + newItem);
                    } catch (Exception e) {
                        System.err.println("Failed to write items.dat: " + e.getMessage());
                    }
                }

                sendJsonResponse(exchange, 201, JsonUtil.itemToJson(newItem));
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // GET /api/matches
    static class MatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String lostId = query.get("lostId");

            ArrayList<Item> allItems = manager.getAllItems();
            ArrayList<MatchResult> list = new ArrayList<>();

            if (lostId != null && !lostId.isEmpty() && !"ALL".equalsIgnoreCase(lostId)) {
                Item lost = manager.findById(lostId);
                if (lost != null && "LOST".equalsIgnoreCase(lost.getType())) {
                    list.addAll(matcher.findMatches(lost, allItems));
                }
            } else {
                for (Item item : allItems) {
                    if ("LOST".equalsIgnoreCase(item.getType())) {
                        list.addAll(matcher.findMatches(item, allItems));
                    }
                }
            }

            list.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());
            sendJsonResponse(exchange, 200, JsonUtil.matchesToJson(list));
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length > 0) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                map.put(key, val);
            }
        }
        return map;
    }
}
