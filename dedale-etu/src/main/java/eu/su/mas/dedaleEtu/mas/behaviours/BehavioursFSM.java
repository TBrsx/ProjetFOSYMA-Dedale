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
		
		//Information sharing and new try to move before interlocking
		
		b = new MsgReceiverBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "msgReceiverMaybeBlocked");
		
		b = new InformationSharingBehaviour(ag,false);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "infoSharingEmitterMaybeBlocked");
		
		b = new ExploreMoveBehaviour(ag,true);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "exploreMovesAfterBlock");
		
		// End state, currently do nothing
		b = new JobDoneBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerLastState(b, "jobDone");
		
		// Decision state
		b = new CollectDecisionBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "collectDecision");
		
		// Collect state
		b = new ShareAndCollectBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "shareCollect");

		// Transitions
		
		this.registerTransition("msgReceiver","exploreMoves",0);
		this.registerTransition("msgReceiver","infoSharingEmitter",1);
		this.registerTransition("msgReceiver", "interlockReceiver", 2);
		this.registerTransition("msgReceiver", "infoSharingReceiver", 3);
		
		this.registerTransition("exploreMoves", "msgReceiver", 1);
		this.registerTransition("exploreMoves", "collectDecision", 2);
		this.registerTransition("exploreMoves", "msgReceiverMaybeBlocked", 3);
		
		this.registerDefaultTransition("interlockEmitter", "msgReceiver");
		this.registerDefaultTransition("interlockReceiver", "msgReceiver");
		
		this.registerDefaultTransition("infoSharingEmitter", "exploreMoves");
		this.registerDefaultTransition("infoSharingReceiver", "exploreMoves");
		
		this.registerTransition("msgReceiverMaybeBlocked", "infoSharingEmitterMaybeBlocked", 0);
		this.registerTransition("msgReceiverMaybeBlocked", "interlockReceiver", 2);
		this.registerTransition("msgReceiverMaybeBlocked", "infoSharingReceiver", 3);
		
		this.registerDefaultTransition("infoSharingEmitterMaybeBlocked", "exploreMovesAfterBlock");
		
		this.registerTransition("exploreMovesAfterBlock", "interlockEmitter", 0);
		this.registerTransition("exploreMovesAfterBlock", "msgReceiver", 1);
		this.registerTransition("exploreMovesAfterBlock", "collectDecision", 2);
		
		this.registerTransition("collectDecision", "shareCollect", 1);
		
		this.registerTransition("shareCollect", "shareCollect", 0);
		this.registerTransition("shareCollect", "jobDone", 1);
		
		
		// Init dataStore content 
		getDataStore().put("movesWithoutSharing",0);
		getDataStore().put("decision-master", "1stAgent");
	}

}
