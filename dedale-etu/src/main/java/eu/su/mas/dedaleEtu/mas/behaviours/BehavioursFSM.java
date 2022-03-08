package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

public class BehavioursFSM extends FSMBehaviour {

	private static final long serialVersionUID = 2728229558462751181L;

	public BehavioursFSM(ExploreCoopAgent ag) {
		
		// messageReceiver state
		Behaviour b = new MsgReceiverBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerFirstState(b, "msgReceiver");

		// Explore state = 1 movement to explore.
		b = new ExploreMoveBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "exploreMoves");

		// shareMap state
		b = new ShareMapBehaviour(ag,ag.getListAgentNames());
		b.setDataStore(this.getDataStore());
		this.registerState(b, "shareMap");
		
		// Interlocking state
		b = new InterlockBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b,"interlock");

		// Decision state, currently the only valid decision is to explore again.
		
		// End state, currently do nothing
		b = new JobDoneBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerLastState(b, "jobDone");

		// Transitions

		this.registerDefaultTransition("msgReceiver", "exploreMoves");
		this.registerDefaultTransition("shareMap", "msgReceiver");
		this.registerDefaultTransition("interlock", "msgReceiver");
		
		
		this.registerTransition("exploreMoves", "shareMap",1);
		this.registerTransition("exploreMoves", "interlock", 0);
		this.registerTransition("exploreMoves", "jobDone", 2);
	}

}
