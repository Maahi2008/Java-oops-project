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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * High-Performance Standalone Web Server Launcher for MUJ Lost & Found.
 * Supports:
 * - Multi-threaded pooled executor (16 worker threads)
 * - In-memory RAM cache for static assets (index.html, css, js)
 * - Match engine result caching with instant invalidation
 * - Item Claim / Resolution lifecycle
 * - Admin Security Desk actions (delete, status filter)
 * - Interactive Analytics endpoint (/api/analytics)
 */
public class ServerLauncher {

    private static final int PORT = getPort();

    private static int getPort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }
    private static ItemManager manager;
    private static FileItemRepository repository;
    private static SearchService searchService;
    private static ItemMatcher matcher;

    // In-memory response caches for sub-millisecond response times
    private static volatile String cachedMatchesJson = null;
    private static volatile String cachedDashboardJson = null;
    private static volatile String cachedAnalyticsJson = null;

    public static void main(String[] args) {
        try {
            System.out.println("=================================================");
            System.out.println(" MUJ Campus Lost & Found System - Web Server");
            System.out.println(" Multi-Threaded Engine Initializing...");
            System.out.println("=================================================");

            // 1. Initialize data layer with items.dat
            manager = new ItemManager();
            repository = new FileItemRepository("items.dat");

            ArrayList<Item> saved = repository.loadItems();
            if (saved.isEmpty()) {
                System.out.println("No saved items in items.dat. Loading initial MUJ samples.");
                Item lostSample = new LostItem(
                        "L101",
                        "Black Leather Wallet",
                        "Wallets & Bags",
                        "Black leather wallet with MUJ student ID and transit pass",
                        "Library",
                        LocalDate.now().minusDays(1),
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
                        LocalDate.now().minusDays(1),
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

            // 2. Start HTTP Server with a pooled multi-threaded executor
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // API Endpoints
            server.createContext("/api/dashboard", new DashboardHandler());
            server.createContext("/api/items", new ItemHandler());
            server.createContext("/api/matches", new MatchHandler());
            server.createContext("/api/analytics", new AnalyticsHandler());

            // Static Web Content Handler with RAM caching
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();

            System.out.println("Server running at: http://localhost:" + PORT + "/");
            System.out.println("Press Ctrl+C to terminate.");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized void invalidateCaches() {
        cachedMatchesJson = null;
        cachedDashboardJson = null;
        cachedAnalyticsJson = null;
    }

    // Static files handler (serves directly from RAM cache for 0ms static latency)
    static class StaticFileHandler implements HttpHandler {
        private static final Map<String, CachedFile> fileCache = new ConcurrentHashMap<>();

        static class CachedFile {
            final byte[] data;
            final String mimeType;
            final long lastModified;

            CachedFile(byte[] data, String mimeType, long lastModified) {
                this.data = data;
                this.mimeType = mimeType;
                this.lastModified = lastModified;
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr.equals("/") || pathStr.isEmpty()) {
                pathStr = "/index.html";
            }

            Path filePath = Paths.get("webapp", pathStr.substring(1));
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                filePath = Paths.get("webapp", "index.html");
            }

            if (Files.exists(filePath)) {
                String cacheKey = filePath.toAbsolutePath().toString();
                long diskModified = Files.getLastModifiedTime(filePath).toMillis();

                CachedFile cached = fileCache.get(cacheKey);
                if (cached == null || cached.lastModified < diskModified) {
                    String mimeType = "text/plain";
                    String name = filePath.getFileName().toString().toLowerCase();
                    if (name.endsWith(".html")) mimeType = "text/html; charset=UTF-8";
                    else if (name.endsWith(".css")) mimeType = "text/css; charset=UTF-8";
                    else if (name.endsWith(".js")) mimeType = "application/javascript; charset=UTF-8";
                    else if (name.endsWith(".svg")) mimeType = "image/svg+xml";

                    byte[] bytes = Files.readAllBytes(filePath);
                    cached = new CachedFile(bytes, mimeType, diskModified);
                    fileCache.put(cacheKey, cached);
                }

                exchange.getResponseHeaders().set("Content-Type", cached.mimeType);
                exchange.getResponseHeaders().set("Cache-Control", "public, max-age=300");
                exchange.sendResponseHeaders(200, cached.data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(cached.data);
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

            String json = cachedDashboardJson;
            if (json == null) {
                ArrayList<Item> allItems = manager.getAllItems();
                int lostCount = 0;
                int foundCount = 0;
                int claimedCount = 0;
                int totalCount = allItems.size();

                ArrayList<Item> activeLostList = new ArrayList<>();
                for (Item item : allItems) {
                    if ("CLAIMED".equalsIgnoreCase(item.getStatus())) {
                        claimedCount++;
                    } else if ("LOST".equalsIgnoreCase(item.getType())) {
                        lostCount++;
                        activeLostList.add(item);
                    } else if ("FOUND".equalsIgnoreCase(item.getType())) {
                        foundCount++;
                    }
                }

                int matchCount = 0;
                for (Item lost : activeLostList) {
                    matchCount += matcher.findMatches(lost, allItems).size();
                }

                json = JsonUtil.dashboardToJson(lostCount, foundCount, matchCount, totalCount, claimedCount);
                cachedDashboardJson = json;
            }

            sendJsonResponse(exchange, 200, json);
        }
    }

    // GET /api/items, POST /api/items/lost, POST /api/items/found, POST /api/items/claim, POST /api/items/delete
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
                String status = query.get("status");

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
                    if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                        if (!item.getStatus().equalsIgnoreCase(status)) {
                            continue;
                        }
                    }
                    filtered.add(item);
                }

                sendJsonResponse(exchange, 200, JsonUtil.itemsToJson(filtered));

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                // 1. Handle Claim
                if (path.contains("/claim")) {
                    String itemId = JsonUtil.extractJsonField(body, "id");
                    String claimant = JsonUtil.extractJsonField(body, "claimedBy");
                    if (claimant == null || claimant.isEmpty()) {
                        claimant = JsonUtil.extractJsonField(body, "regNo");
                    }

                    Item item = manager.findById(itemId);
                    if (item != null) {
                        synchronized (manager) {
                            item.markClaimed(claimant, LocalDate.now().toString());
                            invalidateCaches();
                            try {
                                repository.saveItems(manager.getAllItems());
                            } catch (Exception e) {
                                System.err.println("Failed to write items.dat: " + e.getMessage());
                            }
                        }
                        sendJsonResponse(exchange, 200, JsonUtil.itemToJson(item));
                    } else {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Item not found\"}");
                    }
                    return;
                }

                // 2. Handle Delete
                if (path.contains("/delete")) {
                    String itemId = JsonUtil.extractJsonField(body, "id");
                    boolean removed;
                    synchronized (manager) {
                        removed = manager.removeItem(itemId);
                        if (removed) {
                            invalidateCaches();
                            try {
                                repository.saveItems(manager.getAllItems());
                            } catch (Exception e) {
                                System.err.println("Failed to write items.dat: " + e.getMessage());
                            }
                        }
                    }
                    if (removed) {
                        sendJsonResponse(exchange, 200, "{\"success\":true,\"id\":\"" + JsonUtil.escape(itemId) + "\"}");
                    } else {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Item not found\"}");
                    }
                    return;
                }

                // 3. Handle Lost / Found Submissions
                String id = JsonUtil.extractJsonField(body, "id");
                String name = JsonUtil.extractJsonField(body, "name");
                String category = JsonUtil.extractJsonField(body, "category");
                String description = JsonUtil.extractJsonField(body, "description");
                String location = JsonUtil.extractJsonField(body, "location");
                String dateStr = JsonUtil.extractJsonField(body, "date");
                String reporterName = JsonUtil.extractJsonField(body, "reporterName");
                String contact = JsonUtil.extractJsonField(body, "contact");
                String regNo = JsonUtil.extractJsonField(body, "regNo");
                String imageUrl = JsonUtil.extractJsonField(body, "imageUrl");

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
                        newItem = new FoundItem(id, name, category, description, location, date, reporterName, contact, regNo, "ACTIVE", imageUrl);
                    } else {
                        if (id == null || id.isEmpty()) {
                            long count = manager.getAllItems().stream().filter(i -> "LOST".equalsIgnoreCase(i.getType())).count();
                            id = "L" + (101 + count);
                        }
                        newItem = new LostItem(id, name, category, description, location, date, reporterName, contact, regNo, "ACTIVE", imageUrl);
                    }

                    manager.addItem(newItem);
                    invalidateCaches();

                    try {
                        repository.saveItems(manager.getAllItems());
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

            if (lostId != null && !lostId.isEmpty() && !"ALL".equalsIgnoreCase(lostId)) {
                Item lost = manager.findById(lostId);
                ArrayList<MatchResult> list = new ArrayList<>();
                if (lost != null && "LOST".equalsIgnoreCase(lost.getType()) && "ACTIVE".equalsIgnoreCase(lost.getStatus())) {
                    list.addAll(matcher.findMatches(lost, manager.getAllItems()));
                }
                list.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());
                sendJsonResponse(exchange, 200, JsonUtil.matchesToJson(list));
            } else {
                String json = cachedMatchesJson;
                if (json == null) {
                    ArrayList<Item> allItems = manager.getAllItems();
                    ArrayList<MatchResult> list = new ArrayList<>();
                    for (Item item : allItems) {
                        if ("LOST".equalsIgnoreCase(item.getType()) && "ACTIVE".equalsIgnoreCase(item.getStatus())) {
                            list.addAll(matcher.findMatches(item, allItems));
                        }
                    }
                    list.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());
                    json = JsonUtil.matchesToJson(list);
                    cachedMatchesJson = json;
                }
                sendJsonResponse(exchange, 200, json);
            }
        }
    }

    // GET /api/analytics
    static class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String json = cachedAnalyticsJson;
            if (json == null) {
                ArrayList<Item> allItems = manager.getAllItems();
                int total = allItems.size();
                int lost = 0;
                int found = 0;
                int claimed = 0;

                Map<String, Integer> locationStats = new HashMap<>();
                Map<String, Integer> categoryStats = new HashMap<>();

                for (Item item : allItems) {
                    if ("CLAIMED".equalsIgnoreCase(item.getStatus())) {
                        claimed++;
                    } else if ("LOST".equalsIgnoreCase(item.getType())) {
                        lost++;
                    } else if ("FOUND".equalsIgnoreCase(item.getType())) {
                        found++;
                    }

                    // Normalize location key (e.g. "AB2 (Room 310)" -> "AB2")
                    String rawLoc = item.getLocation();
                    String locKey = "Other";
                    if (rawLoc.contains("Library")) locKey = "Library";
                    else if (rawLoc.contains("AB1")) locKey = "AB1";
                    else if (rawLoc.contains("AB2")) locKey = "AB2";
                    else if (rawLoc.contains("AB3")) locKey = "AB3";
                    else if (rawLoc.contains("LHC")) locKey = "LHC";
                    else if (rawLoc.contains("Food Court") || rawLoc.contains("Mess")) locKey = "Food Court";
                    else if (rawLoc.contains("Hostel")) locKey = "Hostels";
                    else if (rawLoc.contains("Sports")) locKey = "Sports Complex";

                    locationStats.put(locKey, locationStats.getOrDefault(locKey, 0) + 1);

                    String cat = item.getCategory();
                    if (cat == null || cat.isEmpty()) cat = "Other";
                    categoryStats.put(cat, categoryStats.getOrDefault(cat, 0) + 1);
                }

                double recoveryRate = total > 0 ? ((double) claimed / total) * 100.0 : 0.0;

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"totalCount\":").append(total).append(",");
                sb.append("\"lostCount\":").append(lost).append(",");
                sb.append("\"foundCount\":").append(found).append(",");
                sb.append("\"claimedCount\":").append(claimed).append(",");
                sb.append("\"recoveryRate\":").append(String.format(java.util.Locale.US, "%.1f", recoveryRate)).append(",");

                // Location stats JSON
                sb.append("\"locationStats\":{");
                int li = 0;
                for (Map.Entry<String, Integer> entry : locationStats.entrySet()) {
                    sb.append("\"").append(JsonUtil.escape(entry.getKey())).append("\":").append(entry.getValue());
                    if (++li < locationStats.size()) sb.append(",");
                }
                sb.append("},");

                // Category stats JSON
                sb.append("\"categoryStats\":{");
                int ci = 0;
                for (Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
                    sb.append("\"").append(JsonUtil.escape(entry.getKey())).append("\":").append(entry.getValue());
                    if (++ci < categoryStats.size()) sb.append(",");
                }
                sb.append("}");

                sb.append("}");
                json = sb.toString();
                cachedAnalyticsJson = json;
            }

            sendJsonResponse(exchange, 200, json);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
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
