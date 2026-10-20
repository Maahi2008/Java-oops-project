package web;

import model.Item;
import service.ItemManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AnalyticsServlet", urlPatterns = {"/api/analytics"})
public class AnalyticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ItemManager manager = (ItemManager) getServletContext().getAttribute(AppContextListener.ATTR_MANAGER);
        if (manager == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"ItemManager not initialized\"}");
            return;
        }

        ArrayList<Item> allItems = manager.getAllItems();
        int total = allItems.size();
        int lost = 0;
        int found = 0;
        int claimed = 0;

        Map<String, Integer> locMap = new HashMap<>();
        Map<String, Integer> catMap = new HashMap<>();

        for (Item item : allItems) {
            if ("CLAIMED".equalsIgnoreCase(item.getStatus())) {
                claimed++;
            } else if ("LOST".equalsIgnoreCase(item.getType())) {
                lost++;
            } else if ("FOUND".equalsIgnoreCase(item.getType())) {
                found++;
            }

            // Location grouping (Library, AB1, AB2, AB3, LHC, Food Court, Hostels, Sports Complex, Other)
            String loc = item.getLocation();
            String canonicalLoc = "Other";
            if (loc != null) {
                String lower = loc.toLowerCase();
                if (lower.contains("library")) canonicalLoc = "Library";
                else if (lower.contains("ab1") || lower.contains("academic block 1")) canonicalLoc = "AB1";
                else if (lower.contains("ab2") || lower.contains("academic block 2")) canonicalLoc = "AB2";
                else if (lower.contains("ab3") || lower.contains("academic block 3")) canonicalLoc = "AB3";
                else if (lower.contains("lhc") || lower.contains("lecture")) canonicalLoc = "LHC";
                else if (lower.contains("food") || lower.contains("mess")) canonicalLoc = "Food Court";
                else if (lower.contains("hostel") || lower.contains("b1") || lower.contains("g1")) canonicalLoc = "Hostels";
                else if (lower.contains("sports") || lower.contains("gym")) canonicalLoc = "Sports Complex";
                else canonicalLoc = "Other";
            }
            locMap.put(canonicalLoc, locMap.getOrDefault(canonicalLoc, 0) + 1);

            String cat = item.getCategory();
            if (cat == null || cat.trim().isEmpty()) cat = "Other";
            catMap.put(cat, catMap.getOrDefault(cat, 0) + 1);
        }

        double recoveryRate = total > 0 ? ((double) claimed / total) * 100.0 : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalItems\":").append(total).append(",");
        sb.append("\"lostCount\":").append(lost).append(",");
        sb.append("\"foundCount\":").append(found).append(",");
        sb.append("\"claimedCount\":").append(claimed).append(",");
        sb.append("\"recoveryRate\":").append(String.format(java.util.Locale.US, "%.1f", recoveryRate)).append(",");

        sb.append("\"locationStats\":{");
        int li = 0;
        for (Map.Entry<String, Integer> entry : locMap.entrySet()) {
            if (li > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            li++;
        }
        sb.append("},");

        sb.append("\"categoryStats\":{");
        int ci = 0;
        for (Map.Entry<String, Integer> entry : catMap.entrySet()) {
            if (ci > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            ci++;
        }
        sb.append("}");
        sb.append("}");

        PrintWriter out = resp.getWriter();
        out.write(sb.toString());
        out.flush();
    }
}
