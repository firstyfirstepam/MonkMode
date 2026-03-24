package com.example.reelcounter.util;

import java.util.Random;

/**
 * Builds a tiny, reliable math puzzle for the friction flow.
 */
public final class PuzzleGenerator {

    private final Random random = new Random();

    public Puzzle newAdditionPuzzle() {
        int a = random.nextInt(9) + 1;
        int b = random.nextInt(9) + 1;
        int sum = a + b;
        String prompt = a + " + " + b + " = ?";
        return new Puzzle(prompt, Integer.toString(sum));
    }

    /**
     * Immutable puzzle prompt + expected answer (as string for simple TextView comparison).
     */
    public static final class Puzzle {
        private final String prompt;
        private final String expectedAnswer;

        public Puzzle(String prompt, String expectedAnswer) {
            this.prompt = prompt;
            this.expectedAnswer = expectedAnswer;
        }

        public String getPrompt() {
            return prompt;
        }

        public String getExpectedAnswer() {
            return expectedAnswer;
        }
    }
}
