package agent;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public interface GameEventListener {

    void status(String name, String players, String spies, int mission, int failures);

    void proposedMission(String leader, String mission);

    void votes(String yays);

    void mission(String mission);

    void traitors(int traitors);

    void accusation(String accuser, String accused);

}
