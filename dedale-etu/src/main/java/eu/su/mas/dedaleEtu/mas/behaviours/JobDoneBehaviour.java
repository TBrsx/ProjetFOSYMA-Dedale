package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;

public class JobDoneBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -5610039770213140761L;
	
	private AbstractDedaleAgent myAgent;

	public JobDoneBehaviour(AbstractDedaleAgent ag) {
		super(ag);
		this.myAgent=ag;
		
	}

	@Override
	public void action() {
		while(true) {
			System.out.println(this.myAgent.getLocalName() + " - Job Done !");
			this.myAgent.doWait(3000);
		}
		
	}

}
