package agent.expert;

import agent.Mission;
import agent.expert.transition.ResistanceTransition;
import agent.expert.transition.SelectionTransition;
import agent.expert.transition.VoteTransition;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 15/10/2016
 */
public class SpyGameState {

    private String playerString;
    private Phase phase;
    private int round;
    private int tries;
    private int failures;
    private MCTSPlayer leader;
    private List<MCTSPlayer> team;
    private List<MCTSPlayer> players;
    private List<Boolean> votes;
    private int sabotages;
    private MCTSPlayer currentPlayer;

    private SpyGameState(String players, String spies, char firstLeader) {
        this.playerString = players;
        this.players = new ArrayList<MCTSPlayer>(players.length());
        for (char id : players.toCharArray()) {
            this.players.add(new MCTSPlayer(id, spies.indexOf(id) != -1));
        }
        phase = Phase.SELECTING;
        round = 1;
        tries = 0;
        failures = 0;
        leader = findByID(firstLeader);
        this.team = new ArrayList<MCTSPlayer>();
        this.currentPlayer = leader;
    }

    public boolean done() {
        return round == 6 || won() || lost();
    }

    public boolean won() {
        return (round - failures) >= 3;
    }

    public boolean lost() {
        return failures >= 3;
    }

    public MCTSPlayer nextLeader() {
        return findByID(playerString.charAt((playerString.indexOf(leader.id()) + 1) % playerString.length()));
    }

    public void step(ResistanceTransition transition) {
        switch (phase) {
            case SELECTING:
                select((SelectionTransition) transition);
                break;

        }
    }

    public void select(SelectionTransition transition) {
        team.clear();
        for (char c : transition.selection().toCharArray())
            team.add(findByID(c));
        phase = Phase.VOTING;
        currentPlayer = players.get(0);
    }

    public void vote(VoteTransition vote) {
        if (currentPlayer.equals(players.get(players.size() - 1))) {

        }
    }

    private MCTSPlayer findByID(char id) {
        for (MCTSPlayer player : players) {
            if (player.id() == id) {
                return player;
            }
        }
        throw new IllegalArgumentException("no such id: " + id);
    }

    public enum Phase {
        SELECTING, VOTING, MISSION
    }

}
