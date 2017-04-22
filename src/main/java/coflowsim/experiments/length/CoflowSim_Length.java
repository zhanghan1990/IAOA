package coflowsim.experiments.length;
import java.io.File;
import java.io.IOException;
import coflowsim.datastructures.Job;
import coflowsim.simulators.CoflowSimulator;
import coflowsim.simulators.FlowSimulator;
import coflowsim.simulators.Simulator;
import coflowsim.traceproducers.CoflowBenchmarkTraceProducer;
import coflowsim.traceproducers.TraceProducer;
import coflowsim.utils.Constants;
import coflowsim.utils.Constants.SHARING_ALGO;

public class CoflowSim_Length {
	public static void main(String[] args) {

		if (args.length < 3) {
			System.out.println(
					"usage./run coflowsim coflowsim.experiments.length.CoflowSim_Length type tracepath destipath");

			System.exit(1);
		}
		System.out.println(args[0]+" "+args[1]+" "+args[2]);
		SHARING_ALGO sharingAlgo = SHARING_ALGO.WEIGHT;
		String destFile="rt";
		
		File basedir = new File(args[0]);
		if (basedir.exists() == false)
			basedir.mkdir();
		
		if(args[0].equals("Yosemite")){
			System.out.println("Select Yosemite");
			destFile=basedir+"/"+args[2];
			sharingAlgo = SHARING_ALGO.WEIGHT;
		}
		else if(args[0].equals("Varys")){
			System.out.println("Select Varys");
			destFile=basedir+"/"+args[2];
			sharingAlgo=SHARING_ALGO.SEBF;
		}
		else if(args[0].equals("Barrat")){
			System.out.println("Select Barrat");
			destFile=basedir+"/"+args[2];
			sharingAlgo=SHARING_ALGO.FIFO;
		}
		else if(args[0].equals("FAIR")){
			System.out.println("Select FAIR");
			destFile=basedir+"/"+args[2];
			sharingAlgo=SHARING_ALGO.FAIR;
		}
		else if(args[0].equals("pFabric")){
			System.out.println("Select pFabric");
			destFile=basedir+"/"+args[2];
			sharingAlgo=SHARING_ALGO.PFP;
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
		else{
			System.out.println("Wrong parameters,only Yosemite,Varys,Barrat,pFabric,FAIR are supported");
			System.exit(1);
		}
		nlpl.simulate(simulationTimestep);
		
		try {
			nlpl.printStats(true, destFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
}
