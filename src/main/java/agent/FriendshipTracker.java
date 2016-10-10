package agent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class FriendshipTracker implements GameEventListener {

    private Map<Character, Integer> indices;
    int[][] friendship;
    char[] mission;
    char leader;

    public FriendshipTracker(String players) {
        indices = new HashMap<Character, Integer>();
        int i = 0;
        for (char c : players.toCharArray()) {
            indices.put(c, i++);
        }
        friendship = new int[players.length()][players.length()];
    }

    @Override
    public void status(String name, String players, String spies, int mission, int failures) {
        System.out.println(Arrays.deepToString(friendship));
    }

    @Override
    public void proposedMission(String leader, String mission) {
        char l = leader.charAt(0);
        char[] m = mission.toCharArray();
        this.leader = l;
        this.mission = m;
        for (char c : m) {
            ++friendship[i(l)][i(c)];
            ++friendship[i(c)][i(l)];
        }
    }

    private int i(char c) {
        return indices.get(c);
    }

    @Override
    public void votes(String yays) {
        for (char c : yays.toCharArray()) {
            for (char d : mission) {
                ++friendship[i(c)][i(d)];
                ++friendship[i(d)][i(c)];
            }
        }
    }

    @Override
    public void mission(String mission) {

    }

    @Override
    public void traitors(int traitors) {

    }

    @Override
    public void accusation(String accuser, String accused) {

    }
}
