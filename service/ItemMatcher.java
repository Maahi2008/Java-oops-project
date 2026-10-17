
package service;

import model.Item;
import model.MatchResult;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class ItemMatcher {

    public ArrayList<MatchResult> findMatches(
            Item lostItem, ArrayList<Item> allItems) {

        ArrayList<MatchResult> results = new ArrayList<>();

        if (!lostItem.getType().equals("LOST")) {
            return results;
        }

        for (Item candidate : allItems) {

            if (!candidate.getType().equals("FOUND")) {
                continue;
            }

            double score = calculateScore(lostItem, candidate);

            if (score >= 30) {
                results.add(
                    new MatchResult(lostItem, candidate, score)
                );
            }
        }

        results.sort(
            Comparator.comparingDouble(MatchResult::getScore)
                      .reversed()
        );

        return results;
    }

    private double calculateScore(Item lost, Item found) {

        double score = 0;

        score += nameSimilarity(
            lost.getName(), found.getName()
        ) * 20;

        if (lost.getCategory().equalsIgnoreCase(found.getCategory())) {
            score += 20;
        }

        if (lost.getLocation().equalsIgnoreCase(found.getLocation())) {
            score += 20;
        }

        score += descriptionSimilarity(
            lost.getDescription(), found.getDescription()
        ) * 25;

        long days = Math.abs(
            ChronoUnit.DAYS.between(
                lost.getDate(), found.getDate()
            )
        );

        if (days == 0) {
            score += 15;
        } else if (days <= 2) {
            score += 10;
        } else if (days <= 7) {
            score += 5;
        }

        return score;
    }

    private double nameSimilarity(String first, String second) {

        String a = first.toLowerCase().trim();
        String b = second.toLowerCase().trim();

        if (a.equals(b)) {
            return 1.0;
        }

        if (a.contains(b) || b.contains(a)) {
            return 0.7;
        }

        return 0.0;
    }

    private double descriptionSimilarity(
            String first, String second) {

        Set<String> wordsA = getWords(first);
        Set<String> wordsB = getWords(second);

        if (wordsA.isEmpty() || wordsB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);

        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> getWords(String text) {

        Set<String> words = new HashSet<>();

        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!word.isBlank()) {
                words.add(word);
            }
        }

        return words;
    }
}
