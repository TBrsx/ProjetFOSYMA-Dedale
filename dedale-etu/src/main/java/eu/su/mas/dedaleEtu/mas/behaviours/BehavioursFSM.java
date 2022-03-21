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
		
		// Interlocking state, emitter or receiver
		b = new InterlockBehaviour(ag,false);
		b.setDataStore(this.getDataStore());
		this.registerState(b,"interlockEmitter");
		
		b = new InterlockBehaviour(ag,true);
		b.setDataStore(this.getDataStore());
		this.registerState(b,"interlockReceiver");
		
		//Information sharing state
		b = new InformationSharingBehaviour(ag,false);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "infoSharingEmitter");
		
		b = new InformationSharingBehaviour(ag,true);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "infoSharingReceiver");

		// Decision state, currently the only valid decision is to explore again.
		
		// End state, currently do nothing
		b = new JobDoneBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerLastState(b, "jobDone");

		// Transitions

		this.registerDefaultTransition("exploreMoves", "msgReceiver");
		this.registerDefaultTransition("msgReceiver", "infoSharingEmitter");
		this.registerDefaultTransition("infoSharingEmitter", "exploreMoves");
		this.registerDefaultTransition("infoSharingReceiver", "exploreMoves");
		this.registerDefaultTransition("interlock", "msgReceiver");
		
		this.registerTransition("msgReceiver", "infoSharingReceiver", 3);
		this.registerTransition("msgReceiver", "interlockReceiver", 2);
		this.registerTransition("exploreMoves", "interlockEmitter", 0);
		this.registerTransition("exploreMoves", "jobDone", 2);
	}

}
