package agent.mcts;

import java.util.*;

/**
 * A Class to represent a single game of resistance
 * @author Tim French
 * */

public class Simulator {

    private State state;

    /**
     * Creates an empty game.
     * core.GameSimulator log printed to stdout
     * */
    public Simulator(State state){
        this.state = new State(state);
    }

    /**
     * Sends a status update to all players.
     * The status includes the players name, the player string, the spys (or a string of ? if the player is not a spy, the number of rounds played and the number of rounds failed)
     * @param round the current round
     * @param fails the number of rounds failed
     **/
    private void statusUpdate(int round, int fails){
        for(Character c: state.players.keySet()){
            if (state.spies.contains(c)) {
                state.players.get(c).get_status(""+c,state.playerString,state.spyString,round,fails);
            } else{
                state.players.get(c).get_status(""+c,state.playerString,state.resString,round,fails);
            }
        }
    }

    /**
     * This method picks a random leader for the next round and has them nominate a mission team.
     * If the leader does not pick a legitimate mission team (wrong number of agents, or agents that are not in the game) a default selection is given instead.
     * @param round the round in the game the mission is for.
     * @return a String containing the names of the agents being sent on the mission
     * */
    private String nominate(int round, Character leader){
        int mNum = State.MISSION_NUM[state.numPlayers-5][round-1];
        String team = state.players.get(leader).do_Nominate(mNum);
        char[] tA = team.toCharArray();
        Arrays.sort(tA);
        boolean legit = tA.length==mNum;
        for(int i = 0; i<mNum && legit; i++){
            if(!state.players.keySet().contains(tA[i])) legit = false;
            if(i>0 && tA[i]==tA[i-1]) legit=false;
        }
        if(!legit){
            team = "";
            for(int i = 0; i< mNum; i++) team+=(char)(65+i);
        }
        for(Character c: state.players.keySet()){
            state.players.get(c).get_ProposedMission(leader+"", team);
        }
        return team;
    }

    /**
     * This method requests votes from all players on the most recently proposed mission teams, and reports whether a majority voted yes.
     * It counts the votes and reports a String of all agents who voted in favour to the each agent.
     * @return true if a strict majority supported the mission.
     * */
    private boolean vote(){
        int votes = 0;
        String yays = "";
        for(Character c: state.players.keySet()){
            if(state.players.get(c).do_Vote()){
                votes++;
                yays+=c;
            }
        }
        for(Character c: state.players.keySet()){
            state.players.get(c).get_Votes(yays);
        }
        return (votes>state.numPlayers/2);
    }

    /**
     * Polls the mission team on whether they betray or not, and reports the result.
     * First it informs all players of the team being sent on the mission.
     * Then polls each agent who goes on the mission on whether or not they betray the mission.
     * It reports to each agent the number of betrayals.
     * @param team A string with one character for each member of the team.
     * @return the number of agents who betray the mission.
     * */
    private int mission(String team){
        for(Character c: state.players.keySet()){
            state.players.get(c).get_Mission(team);
        }
        int traitors = 0;
        for(Character c: team.toCharArray()){
            if(state.spies.contains(c) && state.players.get(c).do_Betray()) traitors++;
        }
        for(Character c: state.players.keySet()){
            state.players.get(c).get_Traitors(traitors);
        }
        return traitors;
    }

    public void step() {
        switch (state.phase) {
            case NOT_STARTED:
                statusUpdate(1,0);
                state.phase = State.Phase.NOMINATING;
                state.leader = state.rand.nextInt(state.numPlayers);
                break;
            case NOMINATING:
                state.team = nominate(state.round, state.playerString.charAt(state.leader++%state.numPlayers));
                state.leader%=state.numPlayers;
                state.voteRound = 0;
                state.phase = State.Phase.VOTING;
                break;
            case VOTING:
                if (state.voteRound++ < 5) {
                    if (!vote()) {
                        state.team = nominate(state.round, state.playerString.charAt(state.leader++%state.numPlayers));
                        state.phase = State.Phase.MISSION;
                    }
                } else {
                    state.phase = State.Phase.MISSION;
                }
                break;
            case MISSION:
                int traitors = mission(state.team);
                if(traitors !=0 && (traitors !=1 || state.round !=4 || state.numPlayers<7)){
                    state.fails++;
                }
                statusUpdate(state.round + 1, state.fails);
                state.round += 1;
                if (state.round < 6) {
                    state.phase = State.Phase.NOMINATING;
                } else {
                    state.phase = State.Phase.COMPLETE;
                }
                break;
        }
    }

    public boolean play() {
        while (state.phase != State.Phase.COMPLETE)
            step();
       // System.out.println(state);
        return state.fails <= 2;
    }

}












