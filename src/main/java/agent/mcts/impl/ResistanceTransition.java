package agent.mcts.impl;

import agent.mcts.MCTS;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public abstract class ResistanceTransition implements MCTS.Transition {

    public static class Nomination extends ResistanceTransition {

        private String selection;

        public Nomination(String selection) {
            this.selection = selection;
        }

        public String selection() {
            return selection;
        }

    }

    public static class Sabotage extends ResistanceTransition {

        private final boolean sabotage;

        public Sabotage(boolean sabotage) {
            this.sabotage = sabotage;
        }

        public boolean sabotage() {
            return sabotage;
        }

    }

    public static class Vote extends ResistanceTransition {

        private final boolean yes;

        public Vote(boolean yes) {
            this.yes = yes;
        }

        public boolean yes() {
            return yes;
        }

    }

}
