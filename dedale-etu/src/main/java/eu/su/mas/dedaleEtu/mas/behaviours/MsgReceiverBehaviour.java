package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MsgReceiverBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = 5871538328316209119L;
	private static final int NO_MSG = 0;
	private static final int SEND_HANDSHAKE = 1;
	private static final int INTERLOCKING = 2;
	private static final int INFOSHARE = 3;
	
	private static final int SHARINGFREQUENCE = 3;
	

	private int returnCode = NO_MSG;

	ExploreCoopAgent myAgent;

	public MsgReceiverBehaviour(ExploreCoopAgent ag) {
		super(ag);
		this.myAgent = ag;
	}

	public void action() {

		//System.out.println(this.myAgent.getLocalName() + " - started behavior " + this.getBehaviourName());

		// ==== Messages that calls for a change of state====
		// First one read is the one treated
		ACLMessage msgReceived = null;
		MessageTemplate msgTemplate = null;

		// Handshake try
		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INFOSHARE"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		msgReceived = this.myAgent.receive(msgTemplate);

		if (msgReceived != null) {
			getDataStore().put("received-message", msgReceived);
			getDataStore().put("timeSinceSharing", 0);
			this.returnCode = INFOSHARE;
			return;
		}
		
		// Interlocking
		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
				MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
		msgReceived = this.myAgent.receive(msgTemplate);
		if (msgReceived != null) {
			getDataStore().put("received-message", msgReceived);
			this.returnCode = INTERLOCKING;
			return;
		}
		
		if (msgReceived == null) {
			if ((int)getDataStore().get("movesWithoutSharing")>SHARINGFREQUENCE) {
				this.returnCode = SEND_HANDSHAKE;
			}else {
				this.returnCode = NO_MSG;
			}
			
		}
		//System.out.println(this.myAgent.getLocalName() + " - ended behavior " + this.getBehaviourName());

	}

	public int onEnd() {
		//System.out.println(this.myAgent.getLocalName() + " " + this.returnCode);
		return this.returnCode;
	}

}