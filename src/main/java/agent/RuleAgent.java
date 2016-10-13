package agent;

import core.Agent;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class RuleAgent implements Agent {

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {

    }

    @Override
    public String do_Nominate(int number) {
        return null;
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {

    }

    @Override
    public boolean do_Vote() {
        return false;
    }

    @Override
    public void get_Votes(String yays) {

    }

    @Override
    public void get_Mission(String mission) {

    }

    @Override
    public boolean do_Betray() {
        return false;
    }

    @Override
    public void get_Traitors(int traitors) {

    }

    @Override
    public String do_Accuse() {
        return null;
    }

    @Override
    public void get_Accusation(String accuser, String accused) {

    }
}
