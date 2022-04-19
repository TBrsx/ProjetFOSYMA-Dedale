package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
		while (msgReceived == null && (cpt < 10)) {
			this.myAgent.doWait(250);
			msgReceived = this.myAgent.receive(msgTemplate);
			if (doTimeout)
				cpt++;
		}
		if (cpt >= 10) {
			return null;
		}
		return msgReceived;
	}

	private ACLMessage setupMessage(ACLMessage msg, String content, String protocol) {
		msg.setProtocol("INTERLOCKING" + protocol);
		msg.setSender(this.myAgent.getAID());
		msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
		if (content != null) {
			msg.setContent(content);
		}
		return msg;
	}

	private void continuePath() {
		this.myAgent.doWait(500);
		System.out.println(this.myAgent.getLocalName() + " starts to move.");

		// Receive the target from the other agent
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-TARGET"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msg = waitForMessage(msgTemplate, false);
		assert msg != null;
		String target = msg.getContent();

		// Continue the path
		int cpt = 0;
		while (!this.myAgent.getCurrentPosition().equals(target) && !this.myAgent.getPathToFollow().isEmpty() && cpt <= 5) {
			if (this.myAgent.moveTo(this.myAgent.getNextPosition())) {
				this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
				cpt = 0;
			} else {
				cpt++;
			}
			this.myAgent.doWait(500);
		}
//		if (cpt >= 5 || this.myAgent.getPathToFollow().isEmpty()) return;

//		System.out.println(this.myAgent.getLocalName() + " finished its interlocking, sending go message...");
//		msg = setupMessage(new ACLMessage(ACLMessage.QUERY_IF), "done?", "-DONE");
//		this.myAgent.sendMessage(msg);
//		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-DONE"),
//				MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
//		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
//		while (msgReceived == null) {
//			this.myAgent.sendMessage(msg);
//			this.myAgent.doWait(250);
//			msgReceived = this.myAgent.receive(msgTemplate);
//		}
	}

	private void backtrackPath(LinkedList<String> path) {
		this.myAgent.setTargetPosition(path.getLast());
		// Send the target to the other agent
		String target = path.size() > 1 ? path.get(path.size() - 2) : this.myAgent.getCurrentPosition();
		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), target, "-TARGET");
		this.myAgent.sendMessage(msg);

		// Backtrack to the target
		System.out.println(this.myAgent.getLocalName() + " begins backtracking to " + path + ".");
		moveTo(path);
//		msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "-DONE");
//		this.myAgent.sendMessage(msg);
//		this.myAgent.doWait(1500);
	}

	private void defaultInterlocking() {
		// Send the agent's path to the receiver
		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), this.myAgent.getPathToFollow().toString(), "-SHARE_PATH");
		this.myAgent.send(msg);

		// Wait for the receiver's path
		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-SHARE_PATH"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = waitForMessage(msgTemplate, false);

		// Parsing of the received path
		String msgContent = msgReceived.getContent().replaceAll("\\[|\\]", "");
		List<String> otherAgentPath = List.of(msgContent.split(", *"));

		// Compute the shortest backtracking path
		System.out.println(this.myAgent.getLocalName() + " is computing its backtracking path.");
		LinkedList<String> path = this.myAgent.getMyMap()
				.getNearestFork(this.myAgent.getNextPosition(), this.myAgent.getCurrentPosition(), otherAgentPath);
		System.out.println(this.myAgent.getLocalName() + " backtracking path is " + path.toString() + ".");
		System.out.println(this.myAgent.getLocalName() + " path is " + this.myAgent.getPathToFollow() + ".");

		// Send the backtracking path to the receiver
		String content = path.size() == 0 ? Integer.toString(Integer.MAX_VALUE) : Integer.toString(path.size());
		System.out.println(this.myAgent.getLocalName() + " backtracking path's length is " + content + ".");
		msg = setupMessage(new ACLMessage(ACLMessage.INFORM), content, "-PATH_SIZE");
		this.myAgent.sendMessage(msg);

		// Wait for the receiver's backtracking path
		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-PATH_SIZE"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		msgReceived = waitForMessage(msgTemplate, false);
		int otherAgentPathSize = Integer.parseInt(msgReceived.getContent());

		// Choose which agent has to move backward
		if (otherAgentPathSize < path.size() || (otherAgentPathSize <= path.size() && isReceiver)) {
			backtrackPath(path);
		} else {
			continuePath();
		}
	}

	private void alreadyInterlocked() {
		// Wait for the other agent target
		ACLMessage msgReceived = waitForMessage(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-ALREADY_INTERLOCKED"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM)), false);
		String otherAgentTarget = msgReceived.getContent();

		// Get the path to the other agent target
		LinkedList<String> path = this.myAgent.getMyMap().getShortestPath(this.myAgent.getCurrentPosition(), otherAgentTarget);

		// Go to the other agent target
		moveTo(path);

		// Backtrack one more time
		String nextPos = this.myAgent.getMyMap().getNextNeighboringNodes(this.myAgent.getCurrentPosition(), this.myAgent.getPathToFollow().getFirst()).get(0);
		this.myAgent.getPathToFollow().addFirst(this.myAgent.getCurrentPosition());
		this.myAgent.moveTo(nextPos);
		int cpt = 0;
		while (!this.myAgent.getCurrentPosition().equals(nextPos) && cpt < 10) {
			this.myAgent.doWait(500);
			this.myAgent.moveTo(nextPos);
			cpt++;
		}
		this.myAgent.doWait(2000);
	}

	private void moveTo(LinkedList<String> path) {
		for (String pos : path) {
			this.myAgent.getPathToFollow().addFirst(this.myAgent.getCurrentPosition());
			this.myAgent.setNextPosition(pos);
			this.myAgent.moveTo(pos);
			boolean moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
			System.out.println(this.myAgent.getLocalName() + " next backtracking target: " + pos + ", status: " + moveSuccess + ".");
			int cpt = 0;
			while (!moveSuccess && cpt < 5) {
				this.myAgent.doWait(500);
				this.myAgent.moveTo(pos);
				moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
				cpt++;
			}
			if (cpt >= 5) return;
			this.myAgent.doWait(500);
		}
	}

	@Override
	public void action() {
		// Override potential error in selection of sender/receiver
		// Not optimal
		boolean needsFix = false;
		ACLMessage bandage = this.myAgent.receive(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-START"),
				MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)));
		if (bandage != null) {
			this.isReceiver = true;
			needsFix = true;
		}
		if (isReceiver) {
			System.out.println(this.myAgent.getLocalName() + " is the receiver.");

			// Receive the interlocking data
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

			// Reply to the sender
			ACLMessage msg;
			boolean isInterlocking = data[0].equals(this.myAgent.getNextPosition()) && data[1].equals(this.myAgent.getCurrentPosition());
			if (isInterlocking) {
				msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "-START");
				System.out.println(this.myAgent.getLocalName() + " is indeed interlocked.");
			} else {
				msg = setupMessage(new ACLMessage(ACLMessage.DISCONFIRM), null, "-START");
				System.out.println(this.myAgent.getLocalName() + " isn't interlocked.");
			}
			this.myAgent.sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + " sent its reply.");
			if (!isInterlocking) {
				return;
			}

			// Receive the other agent's query about the current interlocking
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-CURRENT"),
					MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
			ACLMessage msgReceived = waitForMessage(msgTemplate, false);
			if (this.myAgent.isInterlocking()) {
				msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "-CURRENT");
				this.myAgent.sendMessage(msg);
				this.myAgent.doWait(500);
				msg = setupMessage(new ACLMessage(ACLMessage.INFORM), this.myAgent.getTargetPosition(), "-CURRENT");
				this.myAgent.sendMessage(msg);
				return;
			} else {
				msg = setupMessage(new ACLMessage(ACLMessage.DISCONFIRM), null, "-CURRENT");
				this.myAgent.sendMessage(msg);
			}
			defaultInterlocking();
		} else {
			// Send a message to ask if the agents around are interlocked with me
			List<String> otherAgents = this.myAgent.getListAgentNames();
			//System.out.println(this.myAgent.getLocalName() + " is the sender.");
			ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
			msg.setProtocol("INTERLOCKING-START");
			msg.setSender(this.myAgent.getAID());
			for (String name : otherAgents) {
				msg.addReceiver(new AID(name, AID.ISLOCALNAME));
			}
			msg.setContent(this.myAgent.getCurrentPosition() + "," + this.myAgent.getNextPosition());
			this.myAgent.sendMessage(msg);

			// Wait for the answer
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-START"),
					MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
					MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
			ACLMessage msgReceived = waitForMessage(msgTemplate, true);
			if (msgReceived == null || msgReceived.getPerformative() == ACLMessage.DISCONFIRM) {
				return;
			}
			this.receiver = msgReceived.getSender().getLocalName();

			// Send a message to ask if the receiver is interlocked with someone else
			msg = setupMessage(new ACLMessage(ACLMessage.QUERY_IF), null, "-CURRENT");
			this.myAgent.send(msg);

			// Wait for the answer
			msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-CURRENT"),
					MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
					MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
			msgReceived = waitForMessage(msgTemplate, false);
			if (msgReceived.getPerformative() == ACLMessage.DISCONFIRM) {
				defaultInterlocking();
			} else {
				alreadyInterlocked();
			}
		}
	}
//	private void backtrackPath(LinkedList<String> path) {
//		String target = path.size() > 1 ? path.get(path.size() - 2) : this.myAgent.getCurrentPosition();
//		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), target, "-TARGET");
//		this.myAgent.sendMessage(msg);
//		System.out.println(this.myAgent.getLocalName() + " begins backtracking to " + path + ".");
//		for (String pos : path) {
//			this.myAgent.getPathToFollow().addFirst(this.myAgent.getCurrentPosition());
//			this.myAgent.moveTo(pos);
//			boolean moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
//			System.out.println(this.myAgent.getLocalName() + " next backtracking target: " + pos + ", status: " + moveSuccess + ".");
//			int cpt = 0;
//			while (!moveSuccess && cpt < 5) {
//				this.myAgent.doWait(500);
//				this.myAgent.moveTo(pos);
//				moveSuccess = this.myAgent.getCurrentPosition().equals(pos);
//				cpt++;
//			}
//			if (cpt >= 5) return;
//			this.myAgent.doWait(500);
//		}
//		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-DONE"),
//				MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
//		waitForMessage(msgTemplate, false);
//		msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "-DONE");
//		this.myAgent.sendMessage(msg);
//		this.myAgent.doWait(1000);
//	}
//
//	private void continuePath() {
//		this.myAgent.doWait(1000);
//		System.out.println(this.myAgent.getLocalName() + " starts to move.");
//		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-TARGET"),
//				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
//		ACLMessage msg = waitForMessage(msgTemplate, false);
//		assert msg != null;
//		String target = msg.getContent();
//		int cpt = 0;
//		while (!this.myAgent.getCurrentPosition().equals(target) && !this.myAgent.getPathToFollow().isEmpty() && cpt <= 5) {
//			if (this.myAgent.moveTo(this.myAgent.getNextPosition())) {
//				this.myAgent.setNextPosition(this.myAgent.getPathToFollow().removeFirst());
//				cpt = 0;
//			} else {
//				cpt++;
//			}
//			this.myAgent.doWait(500);
//		}
//		if (cpt >= 5) return;
//		System.out.println(this.myAgent.getLocalName() + " finished its interlocking, sending go message...");
//		msg = setupMessage(new ACLMessage(ACLMessage.QUERY_IF), "done?", "-DONE");
//		this.myAgent.sendMessage(msg);
//		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-DONE"),
//				MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
//		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
//		while (msgReceived == null) {
//			this.myAgent.sendMessage(msg);
//			this.myAgent.doWait(250);
//			msgReceived = this.myAgent.receive(msgTemplate);
//		}
//	}
//
//	@Override
//	public void action() {
//		this.myAgent.doWait(500); // Remove this, only for display in debug
//		System.out.println(this.myAgent.getLocalName() + " enters interlocking state");
//		System.out.println(this.myAgent.getLocalName() + " current node is " + this.myAgent.getCurrentPosition() + " and next node is " + this.myAgent.getNextPosition() + ".");
//
//		// Override potential error in selection of sender/receiver
//		// Not optimal
//		boolean needsFix = false;
//		ACLMessage bandage = this.myAgent.receive(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-START"),
//				MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)));
//		if (bandage != null) {
//			this.isReceiver = true;
//			needsFix = true;
//		}
//		if (isReceiver) {
//			System.out.println(this.myAgent.getLocalName() + " is the receiver.");
//			String[] data;
//			if (needsFix) {
//				data = bandage.getContent().split(",");
//				this.receiver = bandage.getSender().getLocalName();
//			} else {
//				ACLMessage msgReceived = (ACLMessage) this.getDataStore().get("received-message");
//				if (msgReceived == null) {
//					return;
//				}
//				data = msgReceived.getContent().split(",");
//				this.receiver = msgReceived.getSender().getLocalName();
//			}
//
//			ACLMessage msgReceived = this.myAgent.receive(MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-CURRENT"),
//					MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)));
//
//			if (msgReceived != null) {
//				System.out.println("Other is currently interlocked");
//			} else {
//				// Send confirm message to the sender if the agents are interlocking,
//				// send disconfirm message else
//				System.out.println(this.myAgent.getLocalName() + " loaded the sender message from its DataStore.");
//				ACLMessage msg;
//				boolean isInterlocking = data[0].equals(this.myAgent.getNextPosition()) && data[1].equals(this.myAgent.getCurrentPosition());
//				if (isInterlocking) {
//					msg = setupMessage(new ACLMessage(ACLMessage.CONFIRM), null, "");
//					System.out.println(this.myAgent.getLocalName() + " is indeed interlocked.");
//				} else {
//					msg = setupMessage(new ACLMessage(ACLMessage.DISCONFIRM), null, "");
//					System.out.println(this.myAgent.getLocalName() + " isn't interlocked.");
//				}
//				this.myAgent.sendMessage(msg);
//				System.out.println(this.myAgent.getLocalName() + " sent its reply.");
//				if (!isInterlocking) {
//					return;
//				}
//			}
//		} else {
//			// Ping agents around
//			List<String> otherAgents = this.myAgent.getListAgentNames();
//			System.out.println(this.myAgent.getLocalName() + " is the sender.");
//			ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
//			msg.setProtocol("INTERLOCKING-START");
//			msg.setSender(this.myAgent.getAID());
//			for (String name : otherAgents) {
//				msg.addReceiver(new AID(name, AID.ISLOCALNAME));
//			}
//			msg.setContent(this.myAgent.getCurrentPosition() + "," + this.myAgent.getNextPosition());
//			this.myAgent.sendMessage(msg);
//
//			System.out.println(this.myAgent.getLocalName() + " asked if " + otherAgents + " is/are interlocking.");
//
//			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING"),
//					MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
//							MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
//			msg = waitForMessage(msgTemplate, true);
//			if (msg == null) {
//				return;
//			}
//			this.receiver = msg.getSender().getLocalName();
//			System.out.println(this.myAgent.getLocalName() + " got its reply.");
//			if (msg.getPerformative() == ACLMessage.DISCONFIRM) {
//				System.out.println(this.myAgent.getLocalName() + " knows that " + this.receiver + " wasn't interlocking.");
//				return;
//			}
//			System.out.println(this.myAgent.getLocalName() + " knows that " + this.receiver + " is interlocked.");
//		}
//
//		ACLMessage msg = setupMessage(new ACLMessage(ACLMessage.INFORM), this.myAgent.getPathToFollow().toString(), "-PATH_DATA");
//		this.myAgent.sendMessage(msg);
//		MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-PATH_DATA"),
//				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
//		ACLMessage msgReceived = waitForMessage(msgTemplate, false);
//
//		assert msgReceived != null;
//		String msgContent = msgReceived.getContent().replaceAll("\\[|\\]", "");
//		List<String> otherAgentPath = List.of(msgContent.split(", *"));
//
//		System.out.println(this.myAgent.getLocalName() + " is computing its backtracking path.");
//		LinkedList<String> path = this.myAgent.getMyMap()
//				.getNearestFork(this.myAgent.getNextPosition(), this.myAgent.getCurrentPosition(), otherAgentPath);
//		System.out.println(this.myAgent.getLocalName() + " backtracking path is " + path.toString() + ".");
//		System.out.println(this.myAgent.getLocalName() + " path is " + this.myAgent.getPathToFollow() + ".");
//		String content = path.size() == 0 ? Integer.toString(Integer.MAX_VALUE) : Integer.toString(path.size());
//		System.out.println(this.myAgent.getLocalName() + " backtracking path's length is " + content + ".");
//		msg = setupMessage(new ACLMessage(ACLMessage.INFORM), content, "-PATH_SIZE");
//		this.myAgent.sendMessage(msg);
//
//		msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("INTERLOCKING-PATH_SIZE"),
//				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
//		msgReceived = waitForMessage(msgTemplate, false);
//		if (msgReceived == null) {
//			return;
//		}
//		System.out.println(this.myAgent.getLocalName() + " knows that " + this.receiver + " backtracking path's length is " + msgReceived.getContent() + ".");
//		if (path.size() == 0) {
//			return;
//		}
//		if (Integer.parseInt(msgReceived.getContent()) > path.size() ||
//				(Integer.parseInt(msgReceived.getContent()) >= path.size() && this.isReceiver)) {
//			backtrackPath(path);
//		} else {
//			continuePath();
//		}
//	}
}
