import agent.Team;
import agent.bayes.BayesAgent;
import core.Game;
import core.RandomAgent;

/**
 * Author: Sam Marsh
 * Date: 15/10/2016
 */
public class Tournament {

    public static void main(String[] args) {
        int resistanceWins = 0;
        int resistanceTotal = 0;
        int spyWins = 0;
        int spyTotal = 0;
        for (int i = 0; i < 1000; ++i) {
            if (i % 50 == 0) System.out.println((double) 100 * i / 1000 + "%");
            Game game = new Game("fuckoff.txt");
            BayesAgent agent = new BayesAgent();
            game.addPlayer(new RandomAgent());
            game.addPlayer(new RandomAgent());
            game.addPlayer(new RandomAgent());
            game.addPlayer(new RandomAgent());
            game.addPlayer(agent);
            game.setup();
            game.play();
            boolean won = agent.won();
            Team team = agent.team();
            if (team == Team.RESISTANCE) {
                resistanceTotal += 1;
                if (won) resistanceWins += 1;
            } else {
                spyTotal += 1;
                if (won) spyWins += 1;
            }
        }
        System.out.printf("Resistance: %.2f%n", (double) 100 * resistanceWins / resistanceTotal);
        System.out.printf("Government: %.2f%n", (double) 100 * spyWins / spyTotal);
    }

}
