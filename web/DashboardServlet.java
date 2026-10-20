package web;

import model.Item;
import model.MatchResult;
import service.ItemManager;
import service.ItemMatcher;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet(name = "DashboardServlet", urlPatterns = {"/api/dashboard"})
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ItemManager manager = (ItemManager) getServletContext().getAttribute(AppContextListener.ATTR_MANAGER);
        ItemMatcher matcher = (ItemMatcher) getServletContext().getAttribute(AppContextListener.ATTR_MATCHER);

        if (manager == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"ItemManager not initialized\"}");
            return;
        }

        ArrayList<Item> allItems = manager.getAllItems();
        int lostCount = 0;
        int foundCount = 0;
        int claimedCount = 0;
        int totalCount = allItems.size();

        ArrayList<Item> activeLostItems = new ArrayList<>();
        for (Item item : allItems) {
            if ("CLAIMED".equalsIgnoreCase(item.getStatus())) {
                claimedCount++;
            } else if ("LOST".equalsIgnoreCase(item.getType())) {
                lostCount++;
                activeLostItems.add(item);
            } else if ("FOUND".equalsIgnoreCase(item.getType())) {
                foundCount++;
            }
        }

        int matchCount = 0;
        if (matcher != null) {
            for (Item lost : activeLostItems) {
                ArrayList<MatchResult> matches = matcher.findMatches(lost, allItems);
                matchCount += matches.size();
            }
        }

        String json = JsonUtil.dashboardToJson(lostCount, foundCount, matchCount, totalCount, claimedCount);
        PrintWriter out = resp.getWriter();
        out.write(json);
        out.flush();
    }
}
