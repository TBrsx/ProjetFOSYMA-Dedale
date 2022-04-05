package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Set;

import java.util.LinkedList;
import java.util.List;

public class InterlockBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -5610039770213100761L;
	private final ExploreCoopAgent myAgent;
	private String receiver;
	private boolean isReceiver;

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

	private ACLMessage waitForMessage(MessageTemplate msgTemplate, boolean doTimeout) {
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		int cpt = 0;
		while (msgReceived == null && (cpt < 10 || !doTimeout)) {
			this.myAgent.doWait(250);
			msgReceived = this.myAgent.receive(msgTemplate);
			if (doTimeout)
				cpt++;
		}
		if (cpt >= 10 && doTimeout) {
			return null;
		}
		return msgReceived;
	}

	private LinkedList<String> reverse(LinkedList<String> list) {
		LinkedList<String> newList = new LinkedList<>();
		list.forEach(
				newList::addFirst
		);
		return newList;
	}

	private ACLMessage setupMessage(ACLMessage msg, String content, String protcol) {
		msg.setProtocol("INTERLOCKING" + protcol);
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		if (content != null) {
			msg.setContent(content);
		}
		return msg;
	}

	@Override
	public void action() {
		this.myAgent.doWait(500); // Remove this, only for display in debug
		System.out.println(this.myAgent.getLocalName() + " enters interlocking state");
		System.out.println(this.myAgent.getLocalName() + " current node is " + this.myAgent.getCurrentPosition() + " and next node is " + this.myAgent.getNextPosition() + ".");

		// Override potential error in selection of sender/receiver
		// Not optimal
		boolean needsFix = false;
		ACLMessage bandage = this.myAgent.receive(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
				MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)));
		if (bandage != null) {
			this.isReceiver = true;
			needsFix = true;
		}
		if (isReceiver) {
			System.out.println(this.myAgent.getLocalName() + " is the receiver.");
			String[] data;
			if (needsFix) {
				data = bandage.getContent().split(",");
				this.receiver = bandage.getSender().getLocalName();
			} else {
				ACLMessage msgReceived = (ACLMessage) this.getDataStore().get("received-message");
				if (msgReceived == null) {
					return;
				}
				data = msgReceived.getContent().split(",");
				this.receiver = msgReceived.getSender().getLocalName();
			}

			// Send confirm message to the sender if the agents are interlocking,
			// send disconfirm message else
			System.out.println(this.myAgent.getLocalName() + " loaded the sender message from its DataStore.");
			ACLMessage msg;
			boolean isInterlocking = data[0].equals(this.myAgent.getNextPosition()) && data[1].equals(this.myAgent.getCurrentPosition());
			if (isInterlocking) {
				msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "");
				System.out.println(this.myAgent.getLocalName() + " is indeed interlocked.");
			} else {
				msg = setupMessage(new ACLMessage(ACLMessage.DISCONFIRM), null, "");
				System.out.println(this.myAgent.getLocalName() + " isn't interlocked.");
			}
			((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + " sent its reply.");
			if (!isInterlocking) {
				return;
			}
		} else {
			// Ping agents around
			List<String> otherAgents = this.myAgent.getListAgentNames();
			System.out.println(this.myAgent.getLocalName() + " is the sender.");
			ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
			msg.setProtocol("INTERLOCKING");
			msg.setSender(this.myAgent.getAID());
			for (String name : otherAgents) {
				msg.addReceiver(new AID(name, AID.ISLOCALNAME));
			}
			msg.setContent(this.myAgent.getCurrentPosition() + "," + this.myAgent.getNextPosition());
			((AbstractDedaleAgent) this.myAgent).sendMessage(msg);

			System.out.println(this.myAgent.getLocalName() + " asked if " + otherAgents + " is/are interlocking.");

			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
					MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
							MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
			msg = waitForMessage(msgTemplate, true);
			if (msg == null) {
				return;
			}
			this.receiver = msg.getSender().getLocalName();
			System.out.println(this.myAgent.getLocalName() + " got its reply.");
			if (msg.getPerformative() == ACLMessage.DISCONFIRM) {
				System.out.println(this.myAgent.getLocalName() + " knows that " + this.receiver + " wasn't interlocking.");
				return;
			}
			System.out.println(this.myAgent.getLocalName() + " knows that " + this.receiver + " is interlocked.");
		}

		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), this.myAgent.getPathToFollow().toString(), "-PATH_DATA");
		((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-PATH_DATA"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = waitForMessage(msgTemplate, false);

		String msgContent = msgReceived.getContent().replaceAll("\\[|\\]", "");
		List<String> otherAgentPath = List.of(msgContent.split(", *"));

		System.out.println(this.myAgent.getLocalName() + " is computing its backtracking path.");
		LinkedList<String> path = this.myAgent.getMyMap()
				.getNearestFork(this.myAgent.getNextPosition(), this.myAgent.getCurrentPosition(), otherAgentPath);
		System.out.println(this.myAgent.getLocalName() + " backtracking path is " + path.toString() + ".");
		System.out.println(this.myAgent.getLocalName() + " path is " + this.myAgent.getPathToFollow() + ".");
		String content = path.size() == 0 ? Integer.toString(Integer.MAX_VALUE) : Integer.toString(path.size());
		System.out.println(this.myAgent.getLocalName() + " backtracking path's length is " + content + ".");
		msg = setupMessage(new ACLMessage(ACLMessage.INFORM), content, "-PATH_SIZE");
		((AbstractDedaleAgent) this.myAgent).sendMessage(msg);

		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-PATH_SIZE"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		msgReceived = waitForMessage(msgTemplate, false);
		if (msgReceived == null) {
			return;
		}
		System.out.println(this.myAgent.getLocalName() + " knows that " + this.receiver + " backtracking path's length is " + msgReceived.getContent() + ".");
		if (path.size() == 0) {
			return;
		}
		if (Integer.parseInt(msgReceived.getContent()) > path.size() ||
				(Integer.parseInt(msgReceived.getContent()) >= path.size() && this.isReceiver)) {
			String target = path.size() > 1 ? path.get(path.size()-2) : this.myAgent.getCurrentPosition();
			msg = setupMessage(new ACLMessage(ACLMessage.INFORM), target, "-TARGET");
			((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + " begins backtracking to " + path + ".");
			for (String pos : path) {
				this.myAgent.getPathToFollow().addFirst(this.myAgent.getCurrentPosition());
				((AbstractDedaleAgent) this.myAgent).moveTo(pos);
				boolean moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
				System.out.println(this.myAgent.getLocalName() + " next bactracking target: " + pos + ", status: " + moveSuccess + ".");
				int cpt = 0;
				while (!moveSuccess && cpt < 5) {
					this.myAgent.doWait(500);
					((AbstractDedaleAgent) this.myAgent).moveTo(pos);
					moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
					cpt++;
				}
				if (cpt >= 5) return;
				this.myAgent.doWait(500);
			}
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-DONE"),
					MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			msgReceived = waitForMessage(msgTemplate, false);
			msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "-DONE");
			((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
			this.myAgent.doWait(1000);
		} else {
			this.myAgent.doWait(1000);
			System.out.println(this.myAgent.getLocalName() + " starts to move.");
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-TARGET"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			msg = waitForMessage(msgTemplate, false);
			String target = msg.getContent();
			int cpt = 0;
			while (!this.myAgent.getCurrentPosition().equals(target) && !this.myAgent.getPathToFollow().isEmpty() && cpt <= 5) {
				if (((AbstractDedaleAgent) this.myAgent).moveTo(this.myAgent.getNextPosition())) {
					this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
					cpt = 0;
				} else {
					cpt++;
				}
				this.myAgent.doWait(500);
			}
			if (cpt >= 5) return;
			System.out.println(this.myAgent.getLocalName() + " finished its interlocking, sending go message...");
			msg = setupMessage(new ACLMessage(ACLMessage.QUERY_IF), "done?", "-DONE");
			((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-DONE"),
					MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
			msgReceived = this.myAgent.receive(msgTemplate);
			while (msgReceived == null) {
				((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
				this.myAgent.doWait(250);
				msgReceived = this.myAgent.receive(msgTemplate);
			}
		}
	}
}
