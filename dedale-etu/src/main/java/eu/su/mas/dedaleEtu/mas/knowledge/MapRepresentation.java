package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import javafx.application.Platform;

/**
 * This simple topology representation only deals with the graph, not its content.</br>
 * The knowledge representation is not well written (at all), it is just given as a minimal example.</br>
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent, and has a claimedBy attribute which state which agent has claimed it
	 */


	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle = " node {" + "fill-color: black;" + " size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = " node.agent {" + "fill-color: forestgreen;" + "}";
	private String nodeStyle_agent = " node.open {" + "fill-color: blue;" + "}";
	private String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open;

	private Graph g; //data structure non serializable
	private Viewer viewer; //ref to the display,  non serializable
	private Integer nbEdges;//used to generate the edges ids

	private SerializableSimpleGraph<String, MapAttribute> sg;//used as a temporary dataStructure during migration


	public MapRepresentation() {
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		Platform.runLater(() -> {
			openGui();
		});
		//this.viewer = this.g.display();

		this.nbEdges = 0;
	}
	
	public synchronized MapAttribute getMapAttributeFromNodeId(String id) {
		Node treated = this.g.getNode(id);
		MapAttribute returnedAttribute = new MapAttribute((String)treated.getAttribute("ui.class"),
				(String)treated.getAttribute("claimant"),
				(Boolean)treated.getAttribute("blocked"),
				(Couple<Observation, Integer>)treated.getAttribute("treasure"));
				return returnedAttribute;
	}


	/**
	 * Give the claimant of a node given its id
	 *
	 * @param id id of the node
	 * @return The claimant of the given node, or null if this node doesn't exist in the map
	 */
	public synchronized String getNodeClaimant(String id) {
		if (this.g.getNode(id) == null) {
			return null;
		} else {
			return this.g.getNode(id).getAttribute("claimant").toString();
		}
	}

	/**
	 * Add or replace a node and its attribute
	 *
	 * @param id
	 * @param mapAttribute
	 */
	public synchronized Node addNode(String id, MapAttribute mapAttribute) {
		Node n;
		if (this.g.getNode(id) == null) {
			n = this.g.addNode(id);
		} else {
			n = this.g.getNode(id);
		}
		n.clearAttributes();
		n.setAttribute("ui.class", mapAttribute.getState());
		n.setAttribute("claimant", mapAttribute.getClaimant());
		n.setAttribute("blocked", mapAttribute.isBlocked());
		n.setAttribute("treasure", mapAttribute.getTreasure());

		if (mapAttribute.getClaimant().equalsIgnoreCase("")) {
			n.setAttribute("ui.label", id);
		} else {
			n.setAttribute("ui.label", id + "    " + mapAttribute.getClaimant().substring(0, mapAttribute.getClaimant().length() - 5));
		}
		return n;
	}

	/**
	 * Add a node to the graph. Do nothing if the node already exists.
	 * If new, it is labeled as open (non-visited) and there is no claimant (empty string)
	 *
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized Node addNewNode(String id) {
		if (this.g.getNode(id) == null) {
			MapAttribute mapAtt = new MapAttribute();
			Node added = addNode(id, mapAtt);
			return added;
		}
		return null;
	}

	/**
	 * Add a node to the graph, and add its claimant to its attribute. Do nothing if the node already exists.
	 * If new, it is labeled as open (non-visited)
	 *
	 * @param id       id of the node
	 * @param claimant name of the claimant
	 * @return added node if added, else null
	 */
	public synchronized Node addNewNode(String id, String claimant) {
		if (this.g.getNode(id) == null) {
			MapAttribute mapAtt = new MapAttribute("open", claimant, false, new Couple<Observation, Integer>(null, 0));
			Node added = addNode(id, mapAtt);
			return added;
		}
		return null;
	}
	
	//Same but also with blocked and treasure
	public synchronized Node addNewNode(String id, String claimant,String occupied,Couple<Observation, Integer> treasure) {
		if (this.g.getNode(id) == null) {
			MapAttribute mapAtt = new MapAttribute("open", claimant,false,treasure);
			Node added = addNode(id, mapAtt);
			return added;
		}
		return null;
	}
	
	public synchronized boolean setTreasures(String id, Couple<Observation, Integer> treasure) {
		Node treated = this.g.getNode(id);
		if (treated != null) {
			MapAttribute mapAtt = this.getMapAttributeFromNodeId(id);
			mapAtt.setTreasure(treasure);
			addNode(id, mapAtt);
			return true;
		}
		return false;
	}
	
	

	/**
	 * Add an undirect edge if not already existing.
	 *
	 * @param idNode1
	 * @param idNode2
	 * @return the added edge
	 */
	public synchronized Edge addEdge(String idNode1, String idNode2) {
		Edge e = null;
		this.nbEdges++;
		try {
			e = this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		} catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		} catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch (ElementNotFoundException e3) {

		}
		return e;
	}
	
	public synchronized LinkedList<String> getAdjacentsNodes(String center){
		Iterator<Edge> edges = g.getNode(center).edges().iterator();
		LinkedList<String> nodes = new LinkedList<String>();
		while (edges.hasNext()) {
			Edge e = edges.next();
			String tNode = e.getTargetNode().getId();
			String sNode = e.getSourceNode().getId();
			if (!tNode.equals(center)) {
				nodes.add(tNode);
			} else if (!sNode.equals(center)) {
				nodes.add(sNode);
			}
		}
		return nodes;
	}
	
	//BFS
	public synchronized LinkedList<String> getShortestPath(String idFrom, String idTo) {
		LinkedList<String> shortestPath = new LinkedList<String>();
		HashMap<String,String> parents = new HashMap<String,String>();
		Queue<String> bfsQ = new LinkedList<String>();
		Set<String> visited = new HashSet<String>();
		parents.put(idFrom, null);
		bfsQ.add(idFrom);
		visited.add(idFrom);
		while(!bfsQ.isEmpty()) {
			String node = bfsQ.remove();
			if(node.equalsIgnoreCase(idTo)) {
				String parent = node;
				while(parent!=null) {
					shortestPath.addFirst(parent);
					parent = parents.get(parent);
				}
			}else {
				for(String adjNode : getAdjacentsNodes(node)) {
					if(!visited.contains(adjNode)) { //Hash based so constant
						if(!getMapAttributeFromNodeId(adjNode).isBlocked()) {
							visited.add(adjNode);
							bfsQ.add(adjNode);
							parents.put(adjNode, node);
						}
					}
				}
			}
		}
		
		if (shortestPath.isEmpty()) {//The openNode is not currently reachable
			return null;
		} else {
			shortestPath.remove(0);//remove the current position
		}
		return shortestPath;
	}

	public LinkedList<String> getShortestPathToClosestOpenNodeNotBlocked(String myPosition, String askName) {
		//1) Get all openNodes
		List<String> opennodes = getOpenNodesNotBlocked(askName);
		//2) select the closest one that is
		List<Couple<String, Integer>> lc =
				opennodes.stream()
						.map(on -> (getShortestPath(myPosition, on) != null) ? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size()) : new Couple<String, Integer>(on, Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
						.collect(Collectors.toList());

		Optional<Couple<String, Integer>> closest = lc.stream().min(Comparator.comparing(Couple::getRight));
		//3) Compute shorterPath

		return getShortestPath(myPosition, closest.get().getLeft());
	}
	
	public LinkedList<String> getShortestPathToClosestOpenNode(String myPosition, String askName) {
		//1) Get all openNodes
		List<String> opennodes = getOpenNodes(askName);
		//2) select the closest one that is
		List<Couple<String, Integer>> lc =
				opennodes.stream()
						.map(on -> (getShortestPath(myPosition, on) != null) ? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size()) : new Couple<String, Integer>(on, Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
						.collect(Collectors.toList());

		Optional<Couple<String, Integer>> closest = lc.stream().min(Comparator.comparing(Couple::getRight));
		//3) Compute shorterPath

		return getShortestPath(myPosition, closest.get().getLeft());
	}
	
	public LinkedList<String> getShortestPathToClosestInList(String myPosition, LinkedList<String> nodesList) {
		List<Couple<String, Integer>> lc =
				nodesList.stream()
						.map(on -> (getShortestPath(myPosition, on) != null) ? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size()) : new Couple<String, Integer>(on, Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
						.collect(Collectors.toList());

		Optional<Couple<String, Integer>> closest = lc.stream().min(Comparator.comparing(Couple::getRight));
		if (closest.isEmpty()){
			return null;
		}

		return getShortestPath(myPosition, closest.get().getLeft());
	}


	public List<String> getOpenNodes(String askName) {
		List<String> computedList = this.g.nodes()
				.filter(x -> x.getAttribute("ui.class") == "open")
				.filter(x -> x.getAttribute("claimant").toString().equalsIgnoreCase(askName)
						|| x.getAttribute("claimant").toString().equalsIgnoreCase(""))
				.map(Node::getId)
				.collect(Collectors.toList());
		if (computedList.isEmpty()) {
			return this.g.nodes().filter(x -> x.getAttribute("ui.class") == "open")
					.map(Node::getId)
					.collect(Collectors.toList());
		} else {
			return computedList;
		}
	}
	
	public List<String> getOpenNodesNotBlocked(String askName) {
		List<String> computedList = this.g.nodes()
				.filter(x -> x.getAttribute("ui.class") == "open")
				.filter(x -> x.getAttribute("claimant").toString().equalsIgnoreCase(askName)
						|| x.getAttribute("claimant").toString().equalsIgnoreCase(""))
				.filter(x -> !((Boolean) x.getAttribute("blocked")))
				.map(Node::getId)
				.collect(Collectors.toList());
		if (computedList.isEmpty()) {
			return this.g.nodes().filter(x -> x.getAttribute("ui.class") == "open")
					.map(Node::getId)
					.collect(Collectors.toList());
		} else {
			return computedList;
		}
	}
	
	public List<String> getClaimedNodes(String askName) {
		List<String> computedList = this.g.nodes()
				.filter(x -> x.getAttribute("claimant").toString().equalsIgnoreCase(askName))
				.map(Node::getId)
				.collect(Collectors.toList());
		return computedList;
	}
	
	public List<String> getBlockedNodes() {
		List<String> computedList = this.g.nodes()
				.filter(x -> ((Boolean) x.getAttribute("blocked")))
				.map(Node::getId)
				.collect(Collectors.toList());
		return computedList;
	}
	
	public List<String> getTreasuresNodes(String treasure) {
		List<String> computedList = this.g.nodes()
				.filter(x -> ( (Couple<Observation,Integer>) x.getAttribute("treasure")).getLeft().getName().equalsIgnoreCase(treasure))
				.map(Node::getId)
				.collect(Collectors.toList());
		return computedList;
	}


	/**
	 * Before the migration we kill all non serializable components and store their data in a serializable form
	 */
	public void prepareMigration() {
		serializeGraphTopology();

		closeGui();

		this.g = null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private void serializeGraphTopology() {
		this.sg = new SerializableSimpleGraph<String, MapAttribute>();
		
		Iterator<Node> iter = this.g.iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			sg.addNode(n.getId(), this.getMapAttributeFromNodeId(n.getId()));
		}
		Iterator<Edge> iterE = this.g.edges().iterator();
		while (iterE.hasNext()) {
			Edge e = iterE.next();
			Node sn = e.getSourceNode();
			Node tn = e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}
	}


	public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializableGraph() {
		serializeGraphTopology();
		return this.sg;
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public synchronized void loadSavedData() {

		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		openGui();

		Integer nbEd = 0;
		for (SerializableNode<String, MapAttribute> n : this.sg.getAllNodes()) {
			addNode(n.getNodeId(), n.getNodeContent());
			for (String s : this.sg.getEdges(n.getNodeId())) {
				this.g.addEdge(nbEd.toString(), n.getNodeId(), s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream components
	 */
	private synchronized void closeGui() {
		//once the graph is saved, clear non serializable components
		if (this.viewer != null) {
			//Platform.runLater(() -> {
			try {
				this.viewer.close();
			} catch (NullPointerException e) {
				System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			//});
			this.viewer = null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private synchronized void openGui() {
		this.viewer = new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);//GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		g.display();
	}
	
	public String settleClaims(Node clashNode,String claimant1,String claimant2) {
		//Count the number of neighbors node for each claimant
		int neighbors1 = 0;
		int neighbors2 = 0;
		Iterator<Edge> neighborsEdge = clashNode.edges().iterator();
		while(neighborsEdge.hasNext()) {
			Edge treated = neighborsEdge.next();
			Node neighbor = treated.getSourceNode().getId() != clashNode.getId() ? treated.getSourceNode() : treated.getTargetNode();
			if(neighbor.getAttribute("claimant").toString().equalsIgnoreCase(claimant1)){
				neighbors1 += 1;
			}else if (neighbor.getAttribute("claimant").toString().equalsIgnoreCase(claimant2)){
				neighbors2 += 1;
			}
		}
		//The one with the more claimed neighbors is the true claimant, with priority to sender's given information to avoid another clash (claimant2)
		return neighbors1 > neighbors2 ? claimant1 : claimant2;
		
	}
		
	public void replaceMap(SerializableSimpleGraph<String, MapAttribute> sgreceived) {
		for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
			MapAttribute attributes = n.getNodeContent();
			addNewNode(n.getNodeId());
			addNode(n.getNodeId(),attributes);
		}
	}
	//TODO : Done : state, claimant. Not done : blocked. Need a synchronization
	public void mergeMap(SerializableSimpleGraph<String, MapAttribute> sgreceived,ExploreCoopAgent agent,String sender) {
		for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
			MapAttribute attributes = n.getNodeContent();
			String claimant = attributes.getClaimant();
			//Add it (Reminder : does nothing if already in the map)
			Node nodeAdded = null;
			nodeAdded = addNewNode(n.getNodeId(), claimant);
			
			//Treasures
			
			if(n.getNodeContent().getTreasure().getLeft()!=null) {
				attributes.setTreasure(n.getNodeContent().getTreasure());
			}
			Couple<Observation,Integer> possibleTreasure = getMapAttributeFromNodeId(n.getNodeId()).getTreasure();
			if(possibleTreasure.getLeft()!=null) {
				attributes.setTreasure(possibleTreasure);
			}
			//If there is a claimant clash
			if (( (String) this.g.getNode(n.getNodeId()).getAttribute("claimant")).equalsIgnoreCase(n.getNodeContent().getClaimant())){
				claimant = this.settleClaims(this.g.getNode(n.getNodeId()), (String) this.g.getNode(n.getNodeId()).getAttribute("claimant"), n.getNodeContent().getClaimant());
			}

			attributes.setClaimant(claimant);
			//check its state attribute. If I knew or just learned it was closed, it's now closed on my map. Otherwise, it's open.
			if (((String) this.g.getNode(n.getNodeId()).getAttribute("ui.class")).equalsIgnoreCase("closed") || n.getNodeContent().getState().equalsIgnoreCase("closed")) {
				attributes.setState("closed");
			}else {
				attributes.setState("open");
			}
			
			nodeAdded = addNode(n.getNodeId(), attributes);
			agent.addNodeOtherAgents(nodeAdded);
		}

		//now that all nodes are added, we can add edges
		for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
			for (String s : sgreceived.getEdges(n.getNodeId())) {
				Edge e = addEdge(n.getNodeId(), s);
				if (e!= null) {
					agent.addEdgeOtherAgents(e,sender);
				}
			}
		}
		//System.out.println("Merge done");
	}

	/**
	 * @return true if there exist at least one openNode on the graph
	 */
	public boolean hasOpenNode() {
		return (this.g.nodes()
				.filter(n -> n.getAttribute("ui.class") == "open")
				.findAny()).isPresent();
	}
	public boolean hasOpenNodeNotBlocked() {
		return (this.g.nodes()
				.filter(n -> n.getAttribute("ui.class") == "open")
				.filter(n -> !((Boolean) n.getAttribute("blocked")))
				.findAny()).isPresent();
	}
	
	public boolean hasNodeToCollect(String askName) {
		return (this.g.nodes()
				.filter(n -> ((String)n.getAttribute("collector")).equalsIgnoreCase(askName))
				.findAny()).isPresent();
	}

	public List<String> getNextNeighboringNodes(String centerNode, String prevNode) {
		Iterator<Edge> edges = g.getNode(centerNode).edges().iterator();
		List<String> nodes = new ArrayList<>();
		while (edges.hasNext()) {
			Edge e = edges.next();
			String tNode = e.getTargetNode().getId();
			String sNode = e.getSourceNode().getId();
			if (!tNode.equals(prevNode) && !tNode.equals(centerNode)) {
				nodes.add(tNode);
			} else if (!sNode.equals(prevNode) && !sNode.equals(centerNode)) {
				nodes.add(sNode);
			}
		}
		return nodes;
	}
	
	public LinkedList<String> getRandomPathFrom(String center,int range){
		LinkedList<String> path = new LinkedList<String>();
		path.add(center);
		for (int i = 0;i<range;i++) {
			Stream<Edge> edges = g.getNode(path.getLast()).edges();
			LinkedList<Edge> edgesL = new LinkedList<Edge>(edges.toList());
			Random rand = new Random();
			Edge chosenEdge = edgesL.get(rand.nextInt(edgesL.size()));
			String tNode = chosenEdge.getTargetNode().getId();
			String sNode = chosenEdge.getSourceNode().getId();
			if(tNode.equalsIgnoreCase(path.getLast())) {
				path.add(sNode);
			}else {
				path.add(tNode);
			}
		}
		return path;
	}

	public LinkedList<String> getNearestFork(String prevNode, String currentNode, List<String> forbiddenNodes) {
		LinkedList<String> path = new LinkedList<>();
		List<String> neighboringNodes = getNextNeighboringNodes(currentNode, prevNode);
		while (neighboringNodes.size() == 1) {
			prevNode = currentNode;
			currentNode = neighboringNodes.get(0);
			path.add(currentNode);
			neighboringNodes = getNextNeighboringNodes(currentNode, prevNode);
		}
		if (neighboringNodes.size() < 1) {
			return new LinkedList<>();
		} else {
			Random rand = new Random();
			neighboringNodes.removeAll(forbiddenNodes);
			path.add(neighboringNodes.get(rand.nextInt(neighboringNodes.size())));
			return path;
		}
	}

	public List<String> getAllNodes() {
		List<String> computedList = this.g.nodes()
				.map(Node::getId)
				.collect(Collectors.toList());
		return computedList;
	}
}