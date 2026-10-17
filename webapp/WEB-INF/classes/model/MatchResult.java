
package model;

public class MatchResult {

    private final Item lostItem;
    private final Item foundItem;
    private final double score;

    public MatchResult(Item lostItem, Item foundItem, double score) {
        this.lostItem = lostItem;
        this.foundItem = foundItem;
        this.score = score;
    }

    public Item getLostItem() {
        return lostItem;
    }

    public Item getFoundItem() {
        return foundItem;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "Match score: " + String.format("%.1f", score) + "%"
                + "\nLost item: " + lostItem.getName()
                + "\nFound item: " + foundItem.getName()
                + "\nFound reporter: " + foundItem.getReporterName()
                + "\nContact: " + foundItem.getContact();
    }
}
