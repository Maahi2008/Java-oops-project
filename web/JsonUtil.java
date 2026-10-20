package web;

import model.Item;
import model.MatchResult;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight JSON utility for serialization and deserialization
 * without requiring external JSON libraries.
 */
public class JsonUtil {

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String itemToJson(Item item) {
        if (item == null) {
            return "null";
        }
        return "{"
                + "\"id\":\"" + escape(item.getId()) + "\","
                + "\"name\":\"" + escape(item.getName()) + "\","
                + "\"category\":\"" + escape(item.getCategory()) + "\","
                + "\"description\":\"" + escape(item.getDescription()) + "\","
                + "\"location\":\"" + escape(item.getLocation()) + "\","
                + "\"date\":\"" + (item.getDate() != null ? item.getDate().toString() : "") + "\","
                + "\"reporterName\":\"" + escape(item.getReporterName()) + "\","
                + "\"regNo\":\"" + escape(item.getRegNo()) + "\","
                + "\"contact\":\"" + escape(item.getContact()) + "\","
                + "\"type\":\"" + escape(item.getType()) + "\","
                + "\"status\":\"" + escape(item.getStatus()) + "\","
                + "\"imageUrl\":\"" + escape(item.getImageUrl()) + "\","
                + "\"claimedBy\":\"" + escape(item.getClaimedBy()) + "\","
                + "\"claimDate\":\"" + escape(item.getClaimDate()) + "\""
                + "}";
    }

    public static String itemsToJson(List<Item> items) {
        if (items == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append(itemToJson(items.get(i)));
            if (i < items.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String matchResultToJson(MatchResult mr) {
        if (mr == null) {
            return "null";
        }
        return "{"
                + "\"score\":" + String.format(java.util.Locale.US, "%.1f", mr.getScore()) + ","
                + "\"lostItem\":" + itemToJson(mr.getLostItem()) + ","
                + "\"foundItem\":" + itemToJson(mr.getFoundItem())
                + "}";
    }

    public static String matchesToJson(List<MatchResult> matches) {
        if (matches == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < matches.size(); i++) {
            sb.append(matchResultToJson(matches.get(i)));
            if (i < matches.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String dashboardToJson(int lostCount, int foundCount, int matchCount, int totalCount, int claimedCount) {
        return "{"
                + "\"lostCount\":" + lostCount + ","
                + "\"foundCount\":" + foundCount + ","
                + "\"matchCount\":" + matchCount + ","
                + "\"totalCount\":" + totalCount + ","
                + "\"claimedCount\":" + claimedCount
                + "}";
    }

    public static String dashboardToJson(int lostCount, int foundCount, int matchCount, int totalCount) {
        return dashboardToJson(lostCount, foundCount, matchCount, totalCount, 0);
    }

    public static String extractJsonField(String json, String field) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        String key = "\"" + field + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) {
            return "";
        }
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx == -1) {
            return "";
        }
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = quoteStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                if (c == '"') sb.append('"');
                else if (c == '\\') sb.append('\\');
                else if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else if (c == '/') sb.append('/');
                else sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
