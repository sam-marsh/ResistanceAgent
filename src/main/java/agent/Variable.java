package agent;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class Variable {

    private double total;
    private int samples;

    public Variable(double v0, int s0) {
        total = v0;
        samples = s0;
    }

    public void sample(double value) {
        sample(value, 1);
    }

    public void sample(double value, int samples) {
        this.total += value;
        this.samples += samples;
    }

    public void sample(boolean value) {
        sample(value ? 1 : 0);
    }

    public double estimate() {
        if (samples > 0) {
            return total / samples;
        }
        return 0;
    }

    @Override
    public String toString() {
        return samples > 0 ? String.format("%.2f%", estimate()) : "?";
    }

}
