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
import java.util.Comparator;

@WebServlet(name = "MatchServlet", urlPatterns = {"/api/matches"})
public class MatchServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ItemManager manager = (ItemManager) getServletContext().getAttribute(AppContextListener.ATTR_MANAGER);
        ItemMatcher matcher = (ItemMatcher) getServletContext().getAttribute(AppContextListener.ATTR_MATCHER);

        if (manager == null || matcher == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"ItemManager or Matcher not initialized\"}");
            return;
        }

        String lostId = req.getParameter("lostId");
        ArrayList<Item> allItems = manager.getAllItems();
        ArrayList<MatchResult> combinedMatches = new ArrayList<>();

        if (lostId != null && !lostId.trim().isEmpty() && !"ALL".equalsIgnoreCase(lostId.trim())) {
            Item specificLost = manager.findById(lostId.trim());
            if (specificLost != null && "LOST".equalsIgnoreCase(specificLost.getType())) {
                combinedMatches.addAll(matcher.findMatches(specificLost, allItems));
            }
        } else {
            // Find matches for all lost items
            for (Item item : allItems) {
                if ("LOST".equalsIgnoreCase(item.getType())) {
                    combinedMatches.addAll(matcher.findMatches(item, allItems));
                }
            }
        }

        // Sort descending by match score
        combinedMatches.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());

        PrintWriter out = resp.getWriter();
        out.write(JsonUtil.matchesToJson(combinedMatches));
        out.flush();
    }
}
