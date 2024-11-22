package com.searchengine.core.spell;

public class SpellCheckerStats {
    private int totalWordsChecked;
    private int correctWords;
    private int misspelledWords;
    private int suggestionsMade;
    private long totalCheckTime;

    public void recordCheck(boolean correct, long checkTime) {
        totalWordsChecked++;
        if (correct) {
            correctWords++;
        } else {
            misspelledWords++;
        }
        totalCheckTime += checkTime;
    }

    public void recordSuggestions() {
        suggestionsMade++;
    }

    public double getAccuracy() {
        return totalWordsChecked == 0 ? 0 :
                (double) correctWords / totalWordsChecked * 100;
    }

    public double getAverageCheckTime() {
        return totalWordsChecked == 0 ? 0 :
                (double) totalCheckTime / totalWordsChecked;
    }

    @Override
    public String toString() {
        return String.format("""
            Spell Checker Statistics:
            Total Words Checked: %d
            Correct Words: %d
            Misspelled Words: %d
            Suggestions Made: %d
            Accuracy: %.2f%%
            Average Check Time: %.2f ms""",
                totalWordsChecked,
                correctWords,
                misspelledWords,
                suggestionsMade,
                getAccuracy(),
                getAverageCheckTime()
        );
    }
}
