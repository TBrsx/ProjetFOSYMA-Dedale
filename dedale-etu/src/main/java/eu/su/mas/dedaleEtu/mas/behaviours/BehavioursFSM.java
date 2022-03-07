package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.behaviours.FSMBehaviour;

public class BehavioursFSM extends FSMBehaviour {

	private static final long serialVersionUID = 2728229558462751181L;

	public BehavioursFSM(ExploreCoopAgent ag) {

		// Explore state = 1 movement to explore. It's the first state.
		this.registerFirstState(new ExploreMoveBehaviour(ag), "exploreMoves");

		// messageReceiver state
		this.registerState(new MsgReceiverBehaviour(ag), "msgReceiver");

		// shareMap state
		this.registerState(new ShareMapBehaviour(ag,ag.getListAgentNames()), "shareMap");

		// Blocked state

		// Decision state, currently the only valid decision is to explore again.

		// Transitions

		this.registerTransition("exploreMoves", "msgReceiver", 1);

		this.registerDefaultTransition("msgReceiver", "shareMap");

		this.registerDefaultTransition("shareMap", "exploreMoves");
	}

}
