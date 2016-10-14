package agent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class Player {

    private final char id;
    private final Probability suspect;
    private final Map<Player, Variable> friends;
    private final Variable supportSuspect;
    private final Variable suspiciousActions;
    private final Variable possibleGoodActions;
    private double bayesSuspicion;

    public Player(GameState data, char id, double initialSuspicion) {
        this.id = id;
        this.friends = new HashMap<Player, Variable>();
        this.suspect = new Probability((double) data.numberOfSpies() / data.numberOfPlayers());
        this.supportSuspect = new Variable((double) data.numberOfSpies() / data.numberOfPlayers(), 1);
        this.suspiciousActions = new Variable(0, 0);
        this.possibleGoodActions = new Variable(0, 0);
        this.bayesSuspicion = initialSuspicion;
    }

    public Variable friendship(Player player) {
        Variable friendship = friends.get(player);
        if (friendship == null) {
            friendship = new Variable(0, 0);
            friends.put(player, friendship);
        }
        return friendship;
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

    public double likelihoodToBetray(Mission mission) {
        return 0.5;
    }

    public boolean definitelyASpy() {
        return bayesSuspicion == 1;
    }

    public double spyness() {
/*
        double value = 0.75 + 0.25 * supportSuspect.estimate();
        value *= suspect.estimate();
        value *= 0.4 + 0.6 * suspiciousActions.estimate();
        value *= 1 - 0.1 * possibleGoodActions.estimate();
*/
        return bayesSuspicion;
    }

    public String toString() {
        return String.format(
                "%c[%s]",
                id, BigDecimal.valueOf(spyness()).round(new MathContext(4, RoundingMode.HALF_UP)).toEngineeringString()
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
