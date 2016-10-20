package agent.mcts.impl;

import core.Agent;

/**
 * Author: Sam Marsh
 * Date: 19/10/2016
 */
public interface CloneableAgent extends Agent {

    CloneableAgent copy();

    char id();

}
