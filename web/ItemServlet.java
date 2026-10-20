package web;

import model.FoundItem;
import model.Item;
import model.LostItem;
import service.ItemManager;
import service.SearchService;
import storage.FileItemRepository;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;

@WebServlet(name = "ItemServlet", urlPatterns = {"/api/items", "/api/items/lost", "/api/items/found", "/api/items/claim", "/api/items/delete"})
public class ItemServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ItemManager manager = (ItemManager) getServletContext().getAttribute(AppContextListener.ATTR_MANAGER);
        SearchService searchService = (SearchService) getServletContext().getAttribute(AppContextListener.ATTR_SEARCH);

        if (manager == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"ItemManager not initialized\"}");
            return;
        }

        String name = req.getParameter("name");
        String category = req.getParameter("category");
        String location = req.getParameter("location");
        String type = req.getParameter("type");
        String status = req.getParameter("status"); // "ACTIVE", "CLAIMED", or null (all)

        ArrayList<Item> results;
        if (name != null && !name.trim().isEmpty() && searchService != null) {
            results = searchService.searchByName(name.trim());
        } else if (category != null && !category.trim().isEmpty() && searchService != null) {
            results = searchService.searchByCategory(category.trim());
        } else if (location != null && !location.trim().isEmpty() && searchService != null) {
            results = searchService.searchByLocation(location.trim());
        } else {
            results = manager.getAllItems();
        }

        // Apply secondary filters in memory
        ArrayList<Item> filtered = new ArrayList<>();
        for (Item item : results) {
            if (category != null && !category.trim().isEmpty()) {
                if (!item.getCategory().equalsIgnoreCase(category.trim())) {
                    continue;
                }
            }
            if (location != null && !location.trim().isEmpty()) {
                if (!item.getLocation().toLowerCase().contains(location.trim().toLowerCase())) {
                    continue;
                }
            }
            if (type != null && !type.trim().isEmpty()) {
                if (!item.getType().equalsIgnoreCase(type.trim())) {
                    continue;
                }
            }
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                if (!item.getStatus().equalsIgnoreCase(status.trim())) {
                    continue;
                }
            }
            filtered.add(item);
        }

        PrintWriter out = resp.getWriter();
        out.write(JsonUtil.itemsToJson(filtered));
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ItemManager manager = (ItemManager) getServletContext().getAttribute(AppContextListener.ATTR_MANAGER);
        FileItemRepository repository = (FileItemRepository) getServletContext().getAttribute(AppContextListener.ATTR_REPOSITORY);

        if (manager == null || repository == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"ItemManager or Repository not initialized\"}");
            return;
        }

        String body = "";
        String contentType = req.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            body = sb.toString();
        }

        String path = req.getServletPath();
        if (req.getPathInfo() != null) {
            path += req.getPathInfo();
        }

        PrintWriter out = resp.getWriter();

        // 1. Handle Claim Request
        if (path != null && path.contains("/claim")) {
            String itemId = getField(req, body, "id");
            String claimant = getField(req, body, "claimedBy");
            if (claimant == null || claimant.isEmpty()) {
                claimant = getField(req, body, "regNo");
            }

            Item item = manager.findById(itemId);
            if (item != null) {
                synchronized (manager) {
                    item.markClaimed(claimant, LocalDate.now().toString());
                    try {
                        repository.saveItems(manager.getAllItems());
                    } catch (Exception e) {
                        System.err.println("[ItemServlet] Error saving items.dat: " + e.getMessage());
                    }
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                out.write(JsonUtil.itemToJson(item));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Item not found\"}");
            }
            out.flush();
            return;
        }

        // 2. Handle Delete Request (for MUJ Security Admin)
        if (path != null && path.contains("/delete")) {
            String itemId = getField(req, body, "id");
            boolean removed;
            synchronized (manager) {
                removed = manager.removeItem(itemId);
                if (removed) {
                    try {
                        repository.saveItems(manager.getAllItems());
                    } catch (Exception e) {
                        System.err.println("[ItemServlet] Error saving items.dat: " + e.getMessage());
                    }
                }
            }
            if (removed) {
                resp.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"success\":true,\"id\":\"" + JsonUtil.escape(itemId) + "\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Item not found\"}");
            }
            out.flush();
            return;
        }

        // 3. Handle Lost / Found Submissions
        String id = getField(req, body, "id");
        String name = getField(req, body, "name");
        String category = getField(req, body, "category");
        String description = getField(req, body, "description");
        String location = getField(req, body, "location");
        String dateStr = getField(req, body, "date");
        String reporterName = getField(req, body, "reporterName");
        String contact = getField(req, body, "contact");
        String regNo = getField(req, body, "regNo");
        String imageUrl = getField(req, body, "imageUrl");

        LocalDate date;
        try {
            date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
        } catch (Exception e) {
            date = LocalDate.now();
        }

        Item newItem;
        synchronized (manager) {
            if (path != null && path.contains("/found")) {
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

            try {
                repository.saveItems(manager.getAllItems());
                System.out.println("[ItemServlet] Item persisted to storage: " + newItem);
            } catch (Exception e) {
                System.err.println("[ItemServlet] Error saving to items.dat: " + e.getMessage());
            }
        }

        resp.setStatus(HttpServletResponse.SC_CREATED);
        out.write(JsonUtil.itemToJson(newItem));
        out.flush();
    }

    private String getField(HttpServletRequest req, String jsonBody, String name) {
        String val = req.getParameter(name);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        if (!jsonBody.isEmpty()) {
            return JsonUtil.extractJsonField(jsonBody, name);
        }
        return "";
    }
}
