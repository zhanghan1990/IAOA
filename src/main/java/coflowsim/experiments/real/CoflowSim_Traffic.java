package coflowsim.experiments.real;

import java.io.File;
import java.io.IOException;

import coflowsim.datastructures.Job;
import coflowsim.simulators.CoflowSimulator;
import coflowsim.simulators.CoflowSimulatorDark;
import coflowsim.simulators.FlowSimulator;
import coflowsim.simulators.Simulator;
import coflowsim.traceproducers.CoflowBenchmarkTraceProducer;
import coflowsim.traceproducers.CustomTraceProducer;
import coflowsim.traceproducers.JobClassDescription;
import coflowsim.traceproducers.TraceProducer;
import coflowsim.utils.Constants;
import coflowsim.utils.Constants.SHARING_ALGO;

public class CoflowSim_Traffic {
	public static void main(String[] args) {

		if (args.length < 3) {
			System.out.println(
					"usage java -cp target/coflowsim-0.2.0-SNAPSHOT.jar coflowsim.experiments.real.offline_online.CoflowSim_Traffic type tracepath destipath");

			System.exit(1);
		}
		System.out.println(args[0]+" "+args[1]+" "+args[2]);
		SHARING_ALGO sharingAlgo = SHARING_ALGO.WEIGHT;
		if(args[0].equals("Yosemite")){
			System.out.println("Select Yosemite");
			sharingAlgo = SHARING_ALGO.WEIGHT;
		}
		else if(args[0].equals("Varys")){
			System.out.println("Select Varys");
			sharingAlgo=SHARING_ALGO.SEBF;
		}
		else if(args[0].equals("Barrat")){
			System.out.println("Select Barrat");
			sharingAlgo=SHARING_ALGO.FIFO;
		}
		else if(args[0].equals("FAIR")){
			System.out.println("Select FAIR");
			sharingAlgo=SHARING_ALGO.FAIR;
		}
		else if(args[0].equals("pFabric")){
			System.out.println("Select pFabric");
			sharingAlgo=SHARING_ALGO.PFP;
		}
		else if(args[0].equals("DARK")){
			System.out.println("Select DARK");
			sharingAlgo=SHARING_ALGO.DARK;
		}
		
		boolean isOffline = false;
		int simulationTimestep = 10 * Constants.SIMULATION_SECOND_MILLIS;
		boolean considerDeadline = false;
		double deadlineMultRandomFactor = 1;

		// Create TraceProducer
		TraceProducer traceProducer = null;
		// read the tracefrom the file
		traceProducer = new CoflowBenchmarkTraceProducer(args[1]);
		traceProducer.prepareTrace();
		
		for (int k = 0; k < traceProducer.jobs.size(); k++) {
			Job temp = traceProducer.jobs.elementAt(k);
			System.out.println(temp.jobName + " " + temp.jobID + " " + temp.weight);
		}

		Simulator nlpl = null;
		if(args[0].equals("Yosemite")|| args[0].equals("Varys") ||args[0].equals("Barrat")){
			System.out.println("Select coflow simulator");
			nlpl = new CoflowSimulator(sharingAlgo, traceProducer, isOffline, considerDeadline, deadlineMultRandomFactor);
		}
		else if(args[0].equals("pFabric") || args[0].equals("FAIR")){
			nlpl= new FlowSimulator(sharingAlgo, traceProducer, isOffline, considerDeadline, deadlineMultRandomFactor);
			System.out.println("Select flow simulator");
		}
		else if(args[0].equals("DARK")){
			nlpl= new CoflowSimulatorDark(sharingAlgo, traceProducer);
			System.out.println("Select DARK Simulator");
		}
		else{
			System.out.println("Wrong parameters,only Yosemite,Varys,Barrat,pFabric,FAIR are supported");
			System.exit(1);
		}
		nlpl.simulate(simulationTimestep);

		try {
			nlpl.printStats(true, args[2]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
