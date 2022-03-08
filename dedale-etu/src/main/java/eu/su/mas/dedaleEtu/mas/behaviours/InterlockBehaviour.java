package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.LinkedList;

public class InterlockBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -5610039770213100761L;
	private final ExploreCoopAgent myAgent;
	private final String receiver;
	private final boolean isReceiver;

	/**
	 * The agent and receiver debates about which agent as to move backward
	 *
	 * @param ag         The agent owning this behaviour
	 * @param isReceiver Chose the state of the agent
	 */
	public InterlockBehaviour(ExploreCoopAgent ag, boolean isReceiver) {
		super(ag);
		this.myAgent = ag;
		this.receiver = ""; // get this from the datastore
		this.isReceiver = isReceiver;
	}

	private ACLMessage waitForMessage(MessageTemplate msgTemplate, int timer) {
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		while (msgReceived == null) {
			this.myAgent.doWait(timer);
			msgReceived = this.myAgent.receive(msgTemplate);
		}
		return msgReceived;
	}

	private ACLMessage setupMessage(ACLMessage msg, String protocol, String content) {
		msg.setProtocol(protocol);
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		if (content != null) {
			msg.setContent(content);
		}
		return msg;
	}

	@Override
	public void action() {
		if (isReceiver) {
			// Wait for query-if from the sender
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
					MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			ACLMessage msgReceived = waitForMessage(msgTemplate, 500);

			// Send confirm message to the sender if the agents are interlocking,
			// send disconfirm message else
			ACLMessage msg;
			boolean isInterlocking = msgReceived.getContent().equals(this.myAgent.getNextPosition());
			if (isInterlocking) {
				msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), "INTERLOCKING", null);
			} else {
				msg = setupMessage(new ACLMessage(ACLMessage.DISCONFIRM), "INTERLOCKING", null);
			}
			this.myAgent.sendMessage(msg);
			if (!isInterlocking) {
				return;
			}
		} else {
			ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.QUERY_IF),
					"INTERLOCKING", this.myAgent.getCurrentPosition());
			this.myAgent.sendMessage(msg);

			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
					MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
							MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
			msg = waitForMessage(msgTemplate, 500);
			if (msg.getPerformative() == ACLMessage.DISCONFIRM) {
				return;
			}
		}
		LinkedList<String> path = this.myAgent.getMyMap()
				.getNearestFork(this.myAgent.getNextPosition(), this.myAgent.getCurrentPosition());
		String content = path.size() == 0 ? "inf" : Integer.toString(path.size());
		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), "INTERLOCK", content);
		this.myAgent.sendMessage(msg);
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = waitForMessage(msgTemplate, 500);
		if (Integer.parseInt(msgReceived.getContent().toString()) < path.size()) {
			// continue à avancer
		} else {
			// recule 
		}
	}
}
