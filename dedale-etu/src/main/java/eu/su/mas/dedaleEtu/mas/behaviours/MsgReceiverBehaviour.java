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
	
	private int returnCode = NO_MSG;
	
	ExploreCoopAgent agent;

	public MsgReceiverBehaviour(ExploreCoopAgent ag) {
		super(ag);
		this.agent = ag;
	}

	public void action() {
		
		//====Daemons
		//Receive a position
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("SHARE-POSITION"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		if (msgReceived != null) {
			//TODO : Mettre à jour les connaissances sur les positions
		}
		
		
		//====Messages that calls for a change of state, first one read 
		msgReceived = null;
		
		//Map sharing
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
			this.agent.getMyMap().mergeMap(sgreceived);
		}else {
		//Interlocking
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
					MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			msgReceived = this.myAgent.receive(msgTemplate);
			returnCode = INTERLOCKING;
		}
		
		if (msgReceived != null) {
			getDataStore().put("received-message", msgReceived);
		}
	}
	
	public int onEnd() {
		return returnCode;
	}

}