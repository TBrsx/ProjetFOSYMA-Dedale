package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.behaviours.FSMBehaviour;

public class BehavioursFSM extends FSMBehaviour {

	private static final long serialVersionUID = 2728229558462751181L;

	public BehavioursFSM(ExploreCoopAgent ag) {
		
		// messageReceiver state
		this.registerFirstState(new MsgReceiverBehaviour(ag), "msgReceiver");

		// Explore state = 1 movement to explore.
		this.registerState(new ExploreMoveBehaviour(ag), "exploreMoves");

		// shareMap state
		this.registerState(new ShareMapBehaviour(ag,ag.getListAgentNames()), "shareMap");
		
		// Interlocking state
		this.registerState(new InterlockBehaviour(ag),"interlock");

		// Decision state, currently the only valid decision is to explore again.
		
		// End state, currently do nothing
		this.registerLastState(new JobDoneBehaviour(ag), "jobDone");

		// Transitions

		this.registerDefaultTransition("msgReceiver", "exploreMoves");
		this.registerDefaultTransition("shareMap", "msgReceiver");
		this.registerDefaultTransition("interlock", "msgReceiver");
		
		
		this.registerTransition("exploreMoves", "shareMap",1);
		this.registerTransition("exploreMoves", "interlock", 0);
		this.registerTransition("exploreMoves", "jobDone", 2);
	}

}
