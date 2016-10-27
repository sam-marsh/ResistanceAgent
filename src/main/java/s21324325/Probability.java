package s21324325;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class Probability {

    private double value;
    private int n;

    public Probability(double v0) {
        this.value = v0;
        this.n = 0;
    }

    public void sample(double value) {
        sample(value, 1);
    }

    public void sample(double value, int n) {
        this.value = 1 - (1 - value) * (1 - value / n);
        this.n += n;
    }

    public double estimate() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%.2f%", 100 * value);
    }

}
