package agent.mcts;

/**
 * the types of moves possible in the resistance game
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

        @Override
        public String toString() {
            return "Nomination[" + selection + "]";
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

        @Override
        public String toString() {
            return "Sabotage[" + Boolean.toString(sabotage) + "]";
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

        @Override
        public String toString() {
            return "Vote[" + Boolean.toString(yes) + "]";
        }

    }

}
