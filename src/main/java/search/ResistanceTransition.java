package search;

/**
 * The types of moves possible in the resistance game.
 */
public abstract class ResistanceTransition implements MCTS.Transition {

    /**
     * A nomination move. Holds a string representing the players on a mission.
     */
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Nomination that = (Nomination) o;
            return selection != null ? selection.equals(that.selection) : that.selection == null;

        }

        @Override
        public int hashCode() {
            return selection != null ? selection.hashCode() : 0;
        }

    }

    /**
     * A sabotage move, either {@code true} or {@code false}.
     */
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Sabotage sabotage1 = (Sabotage) o;
            return sabotage == sabotage1.sabotage;

        }

        @Override
        public int hashCode() {
            return (sabotage ? 1 : 0);
        }

    }

    /**
     * A vote move, either {@code true} or {@code false}.
     */
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vote vote = (Vote) o;
            return yes == vote.yes;

        }

        @Override
        public int hashCode() {
            return (yes ? 1 : 0);
        }

    }

}
