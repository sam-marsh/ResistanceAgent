package s21329882;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

class Player {

    static final double HELPED_SPY_WEIGHT = 0.25;
    static final double BEHAVED_LIKE_SPY_WEIGHT = 0.6;
    static final double BEHAVED_LIKE_RESISTANCE_WEIGHT = 0.1;

    private final char id;
    private final Map<Player, Double> friends;
    private final Variable helpedSpies;
    private final Variable behavedLikeSpy;
    private final Variable behavedLikeResistance;
    private final int nspies;
    private final int nplayers;
    private double bayesSuspicion;

    Player(GameState data, char id, double initialSuspicion) {
        this.id = id;
        this.friends = new HashMap<Player, Double>();
        this.nspies = data.numberOfSpies();
        this.nplayers = data.numberOfPlayers();
        this.helpedSpies = new Variable((double) nspies / (nplayers - 1), 1);
        this.behavedLikeSpy = new Variable(0, 0);
        this.behavedLikeResistance = new Variable(0, 0);
        this.bayesSuspicion = initialSuspicion;
    }

    void friendship(Player player, double value, int n) {
        Double current = friendship(player);
        friends.put(player, 1 - (1 - current) * (1 - value / n));
    }

    Variable helpedSpy() {
        return helpedSpies;
    }

    Variable behavedLikeSpy() {
        return behavedLikeSpy;
    }

    Variable behavedLikeResistance() {
        return behavedLikeResistance;
    }

    char id() {
        return id;
    }

    double bayesSuspicion() {
        return bayesSuspicion;
    }

    void bayesSuspicion(double _bayesSuspicion) {
        //avoid rounding error taking it above 1 or below 0
        bayesSuspicion = Math.min(Math.max(_bayesSuspicion, 0), 1);
    }

    /**
     * Gives the likelihood of this player to betray a mission, given that it is on the team and assuming it is a spy.
     *
     * @param state the game context
     * @param spiesOnMission the spies on the mission team (not including those left out of the team)
     * @return the likelihood that the player will betray the mission
     */
    double likelihoodToBetray(GameState state, Collection<Player> spiesOnMission) {
        if (spiesOnMission.size() == 1) return 0.95;
        return 1.0 / spiesOnMission.size();
    }

    boolean definitelyASpy() {
        return bayesSuspicion == 1;
    }

    double spyness() {
        if (bayesSuspicion == 0) return 0;
        if (bayesSuspicion == 1) return 1;

        double value = bayesSuspicion();
        value *= ((1 - HELPED_SPY_WEIGHT) + HELPED_SPY_WEIGHT * helpedSpies.value());
        value *= ((1 - BEHAVED_LIKE_SPY_WEIGHT) + BEHAVED_LIKE_SPY_WEIGHT * behavedLikeSpy.value());
        value *= (1 - BEHAVED_LIKE_RESISTANCE_WEIGHT * behavedLikeResistance.value());
        return value;
    }

    Double friendship(Player player) {
        Double friendship = friends.get(player);
        if (friendship == null) {
            friendship = (double) nspies / (nplayers - 1);
            friends.put(player, friendship);
        }
        return friendship;
    }

    public String toString() {
        return String.format(
                "%c[%s%%]",
                id, BigDecimal.valueOf(100 * spyness()).round(new MathContext(4, RoundingMode.HALF_UP)).toEngineeringString()
        );
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Player && ((Player) o).id() == id();
    }

    @Override
    public int hashCode() {
        return id();
    }

    class Variable {

        private double total;
        private int samples;

        Variable(double v0, int s0) {
            total = v0;
            samples = s0;
        }

        void sample(double value) {
            sample(value, 1);
        }

        void sample(boolean value) {
            sample(value ? 1 : 0);
        }

        double value() {
            return samples > 0 ? total / samples : 0;
        }

        private void sample(double value, int samples) {
            this.total += value;
            this.samples += samples;
        }

        @Override
        public String toString() {
            return samples > 0 ? String.format("%.2f%", value()) : "?";
        }

    }

}
