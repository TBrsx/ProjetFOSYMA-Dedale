package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class MsgReceiverBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = 5871538328316209119L;
	private static final int NO_MSG = 0;
	private static final int SHARE_TOPO = 1;
	private static final int INTERLOCKING = 2;
	private static final int HANDSHAKE = 3;

	private int returnCode = NO_MSG;

	ExploreCoopAgent myAgent;

	public MsgReceiverBehaviour(ExploreCoopAgent ag) {
		super(ag);
		this.myAgent = ag;
	}

	public void action() {

		// ==== Daemons ====
		// Receive a position
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("SHARE-POSITION"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		if (msgReceived != null) {
			this.myAgent.getOtherAgents().get(msgReceived.getSender().getLocalName()).setLastKnownPosition(msgReceived.getContent());;
		}

		// ==== Messages that calls for a change of state====
		// First one read is the one treated
		msgReceived = null;

		// Handshake try
		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("HANDSHAKE"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		msgReceived = this.myAgent.receive(msgTemplate);

		if (msgReceived != null) {
			returnCode = HANDSHAKE;
			return;
		}
		// Map sharing
		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		msgReceived = this.myAgent.receive(msgTemplate);
		if (msgReceived != null) {
			returnCode = SHARE_TOPO;
			SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.myAgent.getMyMap().mergeMap(sgreceived);
			return;
			}
		
		// Interlocking
		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
				MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
		msgReceived = this.myAgent.receive(msgTemplate);
		if (msgReceived != null) {
			getDataStore().put("received-message", msgReceived);
			returnCode = INTERLOCKING;
			return;
		}

		
	}

	public int onEnd() {
		return returnCode;
	}

}