package agent;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class Player {

    private final char identifier;
    private final String identifierString;
    private double suspicion;

    public Player(String identifier) {
        this.identifier = identifier.charAt(0);
        this.identifierString = identifier;
    }

    public void suspicion(double value) {

    }

    public double suspicion() {
        return suspicion;
    }

    @Override
    public String toString() {
        return String.format("%c[%.2f]", identifier, suspicion);
    }

}
