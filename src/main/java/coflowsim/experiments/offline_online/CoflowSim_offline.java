package coflowsim.experiments.offline_online;

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

public class CoflowSim_offline {

	public static void main(String[] args) {

		if (args.length < 3) {
			System.out.println(
					"usage java -cp target/coflowsim-0.2.0-SNAPSHOT.jar coflowsim.experiments.offline_online.CoflowSim_offline tracepath destipath off(or on)");

			System.exit(1);
		}
		int curArg = 0;
		SHARING_ALGO sharingAlgo = SHARING_ALGO.WEIGHT;

		if (args[2].equals("off")) {
			sharingAlgo = SHARING_ALGO.WEIGHTOFFLINE;
			System.out.println("set offline");
		} else {
			sharingAlgo = SHARING_ALGO.WEIGHT;
			System.out.println("set online");
		}

		boolean isOffline = false;
		int simulationTimestep = 10 * Constants.SIMULATION_SECOND_MILLIS;
		boolean considerDeadline = false;
		double deadlineMultRandomFactor = 1;

		// Create TraceProducer
		TraceProducer traceProducer = null;
		traceProducer = new CoflowBenchmarkTraceProducer(args[0]);
		traceProducer.prepareTrace();
		for (int k = 0; k < traceProducer.jobs.size(); k++) {
			Job temp = traceProducer.jobs.elementAt(k);
			System.out.println(temp.jobName + " " + temp.jobID + " " + temp.weight);
		}

		Simulator nlpl = null;
		nlpl = new CoflowSimulator(sharingAlgo, traceProducer, isOffline, considerDeadline, deadlineMultRandomFactor);
		nlpl.simulate(simulationTimestep);

		try {
			nlpl.printStats(true, args[1]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
