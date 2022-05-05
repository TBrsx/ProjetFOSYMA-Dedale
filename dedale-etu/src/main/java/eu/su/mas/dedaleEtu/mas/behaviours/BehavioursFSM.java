package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.LinkedList;

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

		// Explore state
		b = new ExploreMoveBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "exploreMoves");
		
		// Interlocking state, emitter or receiver, during initial explore
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
		b = new CollectBehavior(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "Collect");
		
		// Interlocking state, emitter or receiver, during decision
		b = new InterlockBehaviour(ag,false);
		b.setDataStore(this.getDataStore());
		this.registerState(b,"interlockEmitter2");
		
		b = new InterlockBehaviour(ag,true);
		b.setDataStore(this.getDataStore());
		this.registerState(b,"interlockReceiver2");
		
		
		//Msg receiver states during decision (used mainly for interlocking)
		
		b = new MsgReceiverBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "msgReceiver2");
		
		//Idem but during collect
		
		b = new InterlockBehaviour(ag,false);
		b.setDataStore(this.getDataStore());
		this.registerState(b,"interlockEmitter3");
		
		b = new InterlockBehaviour(ag,true);
		b.setDataStore(this.getDataStore());
		this.registerState(b,"interlockReceiver3");
		
		b = new MsgReceiverBehaviour(ag);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "msgReceiver3");
	
		
		//Infosharing during decision state
		
		b = new InformationSharingBehaviour(ag,false);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "infoSharingEmitter2");
		
		b = new InformationSharingBehaviour(ag,true);
		b.setDataStore(this.getDataStore());
		this.registerState(b, "infoSharingReceiver2");

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
		
		this.registerTransition("collectDecision", "msgReceiver2", 0);
		this.registerTransition("collectDecision", "Collect", 1);
		this.registerTransition("collectDecision", "interlockEmitter2", 2);
		this.registerTransition("collectDecision", "jobDone", 3);
		
		this.registerTransition("msgReceiver2", "collectDecision", 0);
		this.registerTransition("msgReceiver2","infoSharingEmitter2",1);
		this.registerTransition("msgReceiver2","interlockReceiver2",2);
		this.registerTransition("msgReceiver2", "infoSharingReceiver2", 3);
		
		this.registerDefaultTransition("interlockEmitter2", "msgReceiver2");
		this.registerDefaultTransition("interlockReceiver2", "msgReceiver2");
		
		this.registerDefaultTransition("infoSharingReceiver2", "msgReceiver2");
		this.registerDefaultTransition("infoSharingEmitter2", "msgReceiver2");
		
		this.registerTransition("Collect", "msgReceiver3", 0);
		this.registerTransition("Collect", "msgReceiver", 1);
		this.registerTransition("Collect","interlockEmitter3",2);
		this.registerTransition("Collect", "jobDone", 3);

		
		this.registerTransition("msgReceiver3", "Collect", 0);
		this.registerTransition("msgReceiver3", "Collect", 1);
		this.registerTransition("msgReceiver3","interlockReceiver3",2);
		this.registerTransition("msgReceiver3", "infoSharingReceiver2", 3);
		
		this.registerDefaultTransition("interlockEmitter3", "msgReceiver3");
		this.registerDefaultTransition("interlockReceiver3", "msgReceiver3");
		
		
		// Init dataStore content 
		getDataStore().put("movesWithoutSharing",0);
		getDataStore().put("awareOfPlan", new LinkedList<String>());
		getDataStore().put("decision-master", "1stAgent");
	}

}
