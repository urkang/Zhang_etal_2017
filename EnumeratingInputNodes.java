import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

//Calculating All Possible Drivers
public class EnumeratingInputNodes {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {

		// explanations of parameters
		// nodeNum: node number of the network
		// edgeNum: edge number of the network
		// nodeSrc: source node list of the bipartite graph
		// nodeDes: destination node list of the bipartite graph
		// edge: edge list of the network

		// ==================================================================
		// these processes belong to data processing of the network file
		// its target is to store all necessary information into this program
		// ==================================================================

		// define a list to store node & edge data from the network file
		ArrayList<String> list = new ArrayList<String>();
		File file = new File(".//" + args[0] + "//" + args[1] + ".net");
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[2] + ".txt", true));
		try {

			// get the path of a network file from the content of file
			// "Path.txt" and then save the result into the file "ResultNew.txt"

			BufferedReader reader = new BufferedReader(new FileReader(file));
			// BufferedWriter writer = new BufferedWriter(new FileWriter(
			// Thread.currentThread().getContextClassLoader().getResource("").getPath()
			// + "/ResultNew.txt"));

			// with "BEGIN" occupying the first useless element
			list.add("BEGIN");
			// add each line of the file data into list
			String data = reader.readLine();
			while (null != data) {
				list.add(data);
				data = reader.readLine();
			}
			reader.close();

			// receive the contents in different ways according to different
			// types of networks
			// type 1 contains a space between letters and node numbers
			// type 2 contains a tab between letters and node numbers
			// change the tab to space and split to get node number

			// get & save node quantity & edge quantity
			final int nodeNum = new Integer(list.get(1).replaceAll("\t", " ").replaceAll(" +", " ").split(" ")[1]);
			final int edgeNum = list.size() - nodeNum - 3;
			// define two node arrays to represent source & destination nodes of
			// the bipartite graph
			Node[] nodeSrc = new Node[nodeNum + 1];
			Node[] nodeDes = new Node[nodeNum + 1];

			// define an edge array
			Edge[] edge = new Edge[edgeNum + 1];
			// give a source node and a destination node to each edge
			for (int i = 1; i <= edgeNum; i++) {
				// delete the front and back spaces and shrink the length of
				// series spaces according to different kinds of network files
				String array = list.get(i + nodeNum + 2).trim().replaceAll("\t", " ").replaceAll(" +", " ");
				// delete the following unnecessary data except the first two
				// source and destination nodes
				if (array.indexOf(" ", array.indexOf(" ") + 1) == -1) {
					edge[i] = new Edge(new Integer(array.split(" ")[0]), new Integer(array.split(" ")[1]));
				} else {
					array = array.substring(0, array.indexOf(" ", array.indexOf(" ") + 1));
					edge[i] = new Edge(new Integer(array.split(" ")[0]), new Integer(array.split(" ")[1]));
				}
			}

			// parameters initializing
			// add every id of source & destination node
			for (int i = 1; i <= nodeNum; i++) {
				nodeSrc[i] = new Node(i);
				nodeDes[i] = new Node(i);
				nodeSrc[i].nodeEdges.add(0);
				nodeDes[i].nodeEdges.add(0);
			}
			// add edge number of source & destination node
			for (int i = 1; i <= edgeNum; i++) {
				nodeSrc[edge[i].src].nodeEdges.add(i);
				nodeDes[edge[i].des].nodeEdges.add(i);
			}

			// =============================================================
			// down until here the network data saving & writing &processing
			// ends and network is totally saved into this program
			// =============================================================

			// =============================================================
			// the following is an instantiation of HK algorithm with a new
			// method to get all possible driver nodes
			// =============================================================

			// initialize the network data
			HopcroftKarpNew hka = new HopcroftKarpNew(nodeSrc, nodeDes, edge, nodeNum, edgeNum);
			// get all matching pairs while marking all possible driver nodes
			long start = System.currentTimeMillis();
			hka.maxMatching();
			long end = System.currentTimeMillis();
			System.out.println("\n" + "File Name:" + args[1] + ".net");
			System.out.println("Core Time:" + (end - start) + "ms");
			// write the result into a file
			writer.write("File Name:" + args[1] + ".net" + "\r\n");
			hka.writeResult(writer);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class HopcroftKarpNew {
	// define the same parameters of the network
	public int nodeNum;
	public int edgeNum;
	public Node[] nodeSrc;
	public Node[] nodeDes;
	public Edge[] edge;
	// matching sign of every source & destination node
	public int[] markedSrc;
	public int[] markedDes;
	// levels of every source & destination node
	// to show whether it has an alternate path or not
	public int[] distSrc;
	public int[] distDes;
	// unMatchedNode collection to store all unmatched nodes
	// the unMatchedNode collection happens to contain all possible driver nodes
	public ArrayList<Integer> unMatchedNode;

	// initialization of HopcroftKarp algorithm
	// all indexes of nodes and edges are from 1
	public HopcroftKarpNew(Node[] nodeSrc, Node[] nodeDes, Edge[] edge, int nodeNum, int edgeNum) {
		// receive basic information of the network
		this.nodeSrc = nodeSrc;
		this.nodeDes = nodeDes;
		this.edge = edge;
		this.nodeNum = nodeNum;
		this.edgeNum = edgeNum;
		// initialize all useful parameters
		this.markedSrc = new int[nodeNum + 1];
		this.markedDes = new int[nodeNum + 1];
		this.distSrc = new int[nodeNum + 1];
		this.distDes = new int[nodeNum + 1];
		// make all marked signs zero
		for (int i = 1; i <= nodeNum; i++) {
			markedSrc[i] = 0;
			markedDes[i] = 0;
			distSrc[i] = 0;
			distDes[i] = 0;
		}
		// mark the unmatched node and possible driver node
		this.unMatchedNode = new ArrayList<Integer>();
	}

	// core function of this program based on HopcroftKarp algorithm
	public void maxMatching() {
		// basic HopcroftKarp algorithm
		// BFS() is used to find alternate path of unmatched nodes
		// DFS() is used to match nodes
		while (BFS()) {
			for (int des = 1; des <= nodeNum; des++) {
				if (markedDes[des] == 0) {
					DFS(des);
				}
			}
		}
	}

	public boolean BFS() {
		// there exists an augmenting path if flag equals true
		boolean flag = false;
		// the unMatchedNode collection represents nodes which aren't driver
		// nodes yet and are potentially driver nodes
		unMatchedNode.clear();
		for (int i = 1; i <= nodeNum; i++) {
			distSrc[i] = 0;
			distDes[i] = 0;
			if (markedDes[i] == 0) {
				// if not matched then add into this collection
				unMatchedNode.add(i);
			}
		}

		for (int des, i = 0; i < unMatchedNode.size(); i++) {
			// for all unmatched nodes find relative alternate path
			des = unMatchedNode.get(i);
			// for all destination nodes find their possible matched source
			// node
			for (int edg : nodeDes[des].nodeEdges) {
				if (edg == 0) {
					continue;
				}
				// possible matched source node
				int src = edge[edg].src;
				// if OK make a level number to the relationship of source &
				// destination nodes
				if (distSrc[src] == 0) {
					distSrc[src] = distDes[des] + 1;
					// if the source node isn't matched then there exists
					// new
					// augmenting path
					if (markedSrc[src] == 0) {
						flag = true;
					} else {
						// if the source node is already matched then find
						// another new augmenting path based on its matching
						// destination node
						distDes[markedSrc[src]] = distSrc[src] + 1;
						unMatchedNode.add(markedSrc[src]);
					}
				}
			}
		}
		return flag;
	}

	public boolean DFS(int des) {
		for (int edg : nodeDes[des].nodeEdges) {
			if (edg == 0) {
				continue;
			}
			// for every destination node find its first unmatched source
			// node
			int src = edge[edg].src;
			// if level number is correct then mark this match
			if (distSrc[src] == distDes[des] + 1) {
				distSrc[src] = 0;
				// if source node is not matched then mark this match
				// if source node is already matched then find its augment
				// path
				// from this very source node
				if ((markedSrc[src] == 0) || (DFS(markedSrc[src]))) {
					// make matches
					markedSrc[src] = des;
					markedDes[des] = src;
					return true;
				}
			}
		}
		return false;
	}

	// write out all necessary data into result file
	public void writeResult(BufferedWriter writer) throws Exception {
		// APD collection to store all possible driver nodes
		TreeSet<Integer> APD = new TreeSet<Integer>();
		APD.addAll(unMatchedNode);
		// APD nodes sorting
		APD.comparator();
		writer.write("Node Size:" + unMatchedNode.size() + "\r\n" + APD + "\r\n\r\n\r\n");
		System.out.println("Node Size:" + unMatchedNode.size() + "\n");
	}
}

// necessary node class for network storing
class Node {
	// node id beginning from 1
	public int id;
	// edge id beginning from 1
	public ArrayList<Integer> nodeEdges;

	public Node(int id) {
		this.id = id;
		this.nodeEdges = new ArrayList<Integer>();
	}
}

// necessary edge class for network storing
class Edge {
	// source & destination node of one edge
	public int src;
	public int des;

	public Edge(int src, int des) {
		this.src = src;
		this.des = des;
	}
}
