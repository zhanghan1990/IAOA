package coflowsim;

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

public class CoflowSim_11_weight_variance {

	public static void main(String[] args) {
		int curArg = 0;
		String dstbasedir = "weightvariance";
		File basedir = new File(dstbasedir);
		if (basedir.exists() == false)
			basedir.mkdir();

		SHARING_ALGO sharingAlgo = SHARING_ALGO.WEIGHT;
		if (args.length > curArg) {
			String UPPER_ARG = args[curArg++].toUpperCase();

			if (UPPER_ARG.contains("FAIR")) {
				sharingAlgo = SHARING_ALGO.FAIR;
			} else if (UPPER_ARG.contains("PFP")) {
				sharingAlgo = SHARING_ALGO.PFP;
			} else if (UPPER_ARG.contains("FIFO")) {
				sharingAlgo = SHARING_ALGO.FIFO;
			} else if (UPPER_ARG.contains("SCF") || UPPER_ARG.contains("SJF")) {
				sharingAlgo = SHARING_ALGO.SCF;
			} else if (UPPER_ARG.contains("NCF") || UPPER_ARG.contains("NJF")) {
				sharingAlgo = SHARING_ALGO.NCF;
			} else if (UPPER_ARG.contains("LCF") || UPPER_ARG.contains("LJF")) {
				sharingAlgo = SHARING_ALGO.LCF;
			} else if (UPPER_ARG.contains("SEBF")) {
				sharingAlgo = SHARING_ALGO.SEBF;
			} else if (UPPER_ARG.contains("DARK")) {
				sharingAlgo = SHARING_ALGO.DARK;
			} else {
				System.err.println("Unsupported or Wrong Sharing Algorithm");
				System.exit(1);
			}
		}

		boolean isOffline = false;
		int simulationTimestep = 10 * Constants.SIMULATION_SECOND_MILLIS;
		if (isOffline) {
			simulationTimestep = Constants.SIMULATION_ENDTIME_MILLIS;
		}

		boolean considerDeadline = false;
		double deadlineMultRandomFactor = 1;
		if (considerDeadline && args.length > curArg) {
			deadlineMultRandomFactor = Double.parseDouble(args[curArg++]);
		}

		// Create TraceProducer
		TraceProducer traceProducer = null;
		String destDirName = "weightvariance";
		File dir = new File(destDirName);
		if (dir.exists() == false)
			dir.mkdir();

		for (int i = 100; i <= 4600; i+=500) {
			String tracename = dir.getAbsolutePath() + "/20-" + i+"-REAL.tr";
			System.out.println(tracename);
			traceProducer = new CoflowBenchmarkTraceProducer(tracename);
			traceProducer.prepareTrace();
			for (int k = 0; k < traceProducer.jobs.size(); k++) {
				Job temp = traceProducer.jobs.elementAt(k);
				System.out.println(temp.jobName + " " + temp.jobID + " " + temp.weight);
			}
			Simulator nlpl = null;
			if (sharingAlgo == SHARING_ALGO.FAIR || sharingAlgo == SHARING_ALGO.PFP) {
				nlpl = new FlowSimulator(sharingAlgo, traceProducer, isOffline, considerDeadline,
						deadlineMultRandomFactor);
			} else if (sharingAlgo == SHARING_ALGO.DARK) {
				nlpl = new CoflowSimulatorDark(sharingAlgo, traceProducer);
			} else {
				nlpl = new CoflowSimulator(sharingAlgo, traceProducer, isOffline, considerDeadline,
						deadlineMultRandomFactor);
			}

			//nlpl.simulate(simulationTimestep);
			String stralgodir = basedir.getAbsolutePath() + "/" + sharingAlgo.toString();
			File algodir = new File(stralgodir);
			// System.out.println(algodir);
			if (algodir.exists() == false)
				algodir.mkdir();

			String fileName = algodir.getAbsolutePath() + "/400-" + i+"-20.rt";

			if (args.length > curArg) {
				fileName = args[curArg++];
			}
			try {
				nlpl.printStats(true, fileName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
