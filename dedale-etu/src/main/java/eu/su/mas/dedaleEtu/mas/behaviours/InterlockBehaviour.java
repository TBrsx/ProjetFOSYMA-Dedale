package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;

public class InterlockBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -5610039770213100761L;
	private AbstractDedaleAgent myAgent;


	public InterlockBehaviour(AbstractDedaleAgent ag) {
		this.myAgent = ag;
	}

	@Override
	public void action() {
		System.out.println(this.myAgent.getLocalName() + " - Interlock !");
	}

}
