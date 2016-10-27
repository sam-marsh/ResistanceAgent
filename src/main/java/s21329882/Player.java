package s21329882;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class Player {

    private static final double SUPPORT_WEIGHT = 0.25;
    private static final double SUSPICIOUS_ACTION_WEIGHT = 0.6;
    private static final double RESISTANCE_ACTION_WEIGHT = 0.1;

    private final char id;
    private final Map<Player, Probability> friends;
    private final Variable supportSuspect;
    private final Variable suspiciousActions;
    private final Variable possibleGoodActions;
    private double bayesSuspicion;

    public Player(GameState data, char id, double initialSuspicion) {
        this.id = id;
        this.friends = new HashMap<Player, Probability>();
        this.supportSuspect = new Variable((double) data.numberOfSpies() / data.numberOfPlayers(), 1);
        this.suspiciousActions = new Variable(0, 0);
        this.possibleGoodActions = new Variable(0, 0);
        this.bayesSuspicion = initialSuspicion;
    }

    public Probability friendship(Player player) {
        Probability friendship = friends.get(player);
        if (friendship == null) {
            friendship = new Probability(0.4);
            friends.put(player, friendship);
        }
        return friendship;
    }

    public Variable getSupportSuspect() {
        return supportSuspect;
    }

    public Variable suspiciousActions() {
        return suspiciousActions;
    }

    public Variable getPossibleGoodActions() {
        return possibleGoodActions;
    }

    public char id() {
        return id;
    }

    public double bayesSuspicion() {
        return bayesSuspicion;
    }

    public void bayesSuspicion(double _bayesSuspicion) {
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
    public double likelihoodToBetray(GameState state, Collection<Player> spiesOnMission) {
        if (spiesOnMission.size() == 1) return 0.95;
        return 1.0 / spiesOnMission.size();
    }

    public boolean definitelyASpy() {
        return bayesSuspicion == 1;
    }

    public double spyness() {
        if (bayesSuspicion == 0) return 0;
        if (bayesSuspicion == 1) return 1;

        double value = bayesSuspicion();
        value *= ((1 - SUPPORT_WEIGHT) + SUPPORT_WEIGHT * supportSuspect.estimate());
        value *= ((1 - SUSPICIOUS_ACTION_WEIGHT) + SUSPICIOUS_ACTION_WEIGHT * suspiciousActions.estimate());
        value *= (1 - RESISTANCE_ACTION_WEIGHT * possibleGoodActions.estimate());
        return value;
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

}
