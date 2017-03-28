package coflowsim.traceproducers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import coflowsim.datastructures.Job;
import coflowsim.datastructures.Machine;
import coflowsim.datastructures.MapTask;
import coflowsim.datastructures.ReduceTask;
import coflowsim.datastructures.Task;
import coflowsim.utils.Constants;
import coflowsim.utils.Utils;

/**
 * @author zhanghan trace generator: use random to generate traffic for coflow
 *         trace is stored in this format:
 *         <p>
 *         Expected trace format:
 *         <ul>
 *         <li>Line 1: &lt;Number of Racks&gt; &lt;Number of Jobs&gt;
 *         <li>Line i: &lt;Job ID&gt; &lt; weight gt; &lt;Job Arrival Time &gt;
 *         &lt;Number of Mappers&gt; &lt;Location of each Mapper&gt; &lt;Number
 *         of Reducers&gt; &lt;Location:ShuffleMB of each Reducer&gt;
 *         </ul>
 * 
 */
public class TraceGenerator extends TraceProducer {

	private int NUM_RACKS;
	private int MACHINES_PER_RACK = 1;

	private int REDUCER_ARRIVAL_TIME = 0;

	public int numJobs;

	private int numJobClasses;
	private JobClassDescription[] jobClass;

	private double sumFracs;
	private double[] fracsOfClasses;

	private Random ranGen;

	private String filename;

	/**
	 * Constructor and input validator.
	 * 
	 * @param numRacks
	 *            Number of racks in the trace.
	 * @param numJobs
	 *            Number of jobs to create.
	 * @param jobClassDescs
	 *            Description of job classes
	 *            ({@link coflowsim.traceproducers.JobClassDescription}).
	 * @param fracsOfClasses
	 *            Fractions of jobs from each job class.
	 * @param randomSeed
	 *            Random seed to use for all randomness inside.
	 */

	public TraceGenerator(int numRacks, int numJobs, JobClassDescription[] jobClassDescs, double[] fracsOfClasses,
			int randomSeed, String filename) {

		ranGen = new Random(randomSeed);
		this.NUM_RACKS = numRacks;
		this.numJobs = numJobs;
		this.numJobClasses = jobClassDescs.length;
		this.jobClass = jobClassDescs;
		this.fracsOfClasses = fracsOfClasses;
		this.sumFracs = Utils.sum(fracsOfClasses);

		this.filename = filename;
		// Check input validity
		assert (jobClassDescs.length == numJobClasses);
		assert (fracsOfClasses.length == numJobClasses);
	}

	public TraceGenerator() {

	}

	public void setParams(int numRacks, int numJobs, JobClassDescription[] jobClassDescs, double[] fracsOfClasses,
			int randomSeed, String filename) {

		ranGen = new Random(randomSeed);
		this.NUM_RACKS = numRacks;
		this.numJobs = numJobs;
		this.numJobClasses = jobClassDescs.length;
		this.jobClass = jobClassDescs;
		this.fracsOfClasses = fracsOfClasses;
		this.sumFracs = Utils.sum(fracsOfClasses);
		this.filename=filename;

	}

	@Override
	public void prepareTrace() {
		try {
			File file = new File(this.filename);// 指定要写入的文件
			if (!file.exists()) {// 如果文件不存在则创建
				System.out.println("creating file now...");
				file.createNewFile();

			}
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
			bufferedWriter.write(NUM_RACKS + " ");

			// Create the tasks
			int jID = 0;
			int sum = 0;
			for (int i = 0; i < numJobClasses; i++) {
				sum += (int) (1.0 * numJobs * fracsOfClasses[i] / sumFracs);
			}
			bufferedWriter.write(sum + "");
			bufferedWriter.newLine();
			for (int i = 0; i < numJobClasses; i++) {

				int numJobsInClass = (int) (1.0 * numJobs * fracsOfClasses[i] / sumFracs);

				while (numJobsInClass-- > 0) {
					// Find corresponding job
					String jobName = "JOB-" + jID;
					jID++;
					bufferedWriter.write(jID + " ");
					double weight = ranGen.nextInt(Constants.WEIGHTRANGE) + 1;
					bufferedWriter.write(weight + " ");
					// generate arrival time, within 1 hour
					int arrival = ranGen.nextInt(3600000);
					bufferedWriter.write(arrival + " ");
					// #region: Create mappers
					int numMappers = ranGen.nextInt(jobClass[i].maxWidth - jobClass[i].minWidth + 1)
							+ jobClass[i].minWidth;
					bufferedWriter.write(numMappers + " ");
					boolean[] rackChosen = new boolean[NUM_RACKS];
					Arrays.fill(rackChosen, false);
					for (int mID = 0; mID < numMappers; mID++) {
						// generate the machine id
						int maplocation = selectMachine(rackChosen);
						bufferedWriter.write(maplocation + " ");
					}
					// #endregion

					// #region: Create reducers
					int numReducers = ranGen.nextInt(jobClass[i].maxWidth - jobClass[i].minWidth + 1)
							+ jobClass[i].minWidth;
					bufferedWriter.write(numReducers + " ");
					// Mark racks so that there is at most one reducer per rack
					rackChosen = new boolean[NUM_RACKS];
					Arrays.fill(rackChosen, false);
					for (int rID = 0; rID < numReducers; rID++) {
						int numMB = ranGen.nextInt(jobClass[i].maxLength - jobClass[i].minLength + 1)
								+ jobClass[i].minLength;
						// shuffleBytes for each mapper
						double shuffleBytes = numMB * numMappers;
						String taskName = "REDUCER-" + rID;
						int reduceid = selectMachine(rackChosen);
						bufferedWriter.write(reduceid + ":" + shuffleBytes + " ");
					}
					bufferedWriter.newLine();
				}
			}
			// #endregion
			bufferedWriter.flush();// 清空缓冲区
			bufferedWriter.close();// 关闭输出流
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Selects a rack that has no tasks, returns its index, and updates
	 * bookkeeping.
	 * <p>
	 * Because CustomTraceProducer essentially has one machine per rack,
	 * selecting rack is equivalent to selecting a machine.
	 * 
	 * @param racksAlreadyChosen
	 *            keeps track of racks that have already been used.
	 * @return the selected rack's index
	 */
	private int selectMachine(boolean[] racksAlreadyChosen) {
		int rackIndex = -1;
		while (rackIndex == -1) {
			rackIndex = ranGen.nextInt(NUM_RACKS);
			if (racksAlreadyChosen[rackIndex]) {
				rackIndex = -1;
			}
		}
		racksAlreadyChosen[rackIndex] = true;

		return rackIndex;
	}

	/** {@inheritDoc} */
	@Override
	public int getNumRacks() {
		return NUM_RACKS;
	}

	/** {@inheritDoc} */
	@Override
	public int getMachinesPerRack() {
		return MACHINES_PER_RACK;
	}

	public void trace_simulate_1() {
		int numRacks = 50;
		int numJobs = 200;
		int randomSeed = 13;
		String destDirName = numRacks + "-" + numJobs;
		File dir = new File(destDirName);

		if (dir.exists() == false)
			dir.mkdir();

		double[][] fracswOfClasses = new double[Constants.I][Constants.J];
		for (int j1 = 0; j1 < Constants.J; j1++)
			for (int i1 = 0; i1 < Constants.I; i1++) {
				for (int k = 0; k < Constants.J; k++) {
					double dynamic = (i1 + 1) * 10;
					if (j1 == k)
						fracswOfClasses[i1][k] = dynamic;
					else {
						double others = (100 - dynamic) / 3;
						fracswOfClasses[i1][k] = others;
					}
				}
				int id1 = (int) fracswOfClasses[i1][0];
				int id2 = (int) fracswOfClasses[i1][1];
				int id3 = (int) fracswOfClasses[i1][2];
				int id4 = (int) fracswOfClasses[i1][3];

				String tracename = dir.getAbsolutePath() + "/" + id1 + "-" + id2 + "-" + id3 + "-" + id4 + ".tr";
				System.out.println(tracename);
				JobClassDescription[] jobClassDescs = new JobClassDescription[] { new JobClassDescription(1, 5, 1, 10),
						new JobClassDescription(1, 5, 10, 1000), new JobClassDescription(5, numRacks, 1, 10),
						new JobClassDescription(5, numRacks, 10, 1000) };
				setParams(numRacks, numJobs, jobClassDescs, fracswOfClasses[i1], randomSeed, tracename);
				prepareTrace();

			}
	}

	public void trace_simulate_2() {
		int widthinitial = 40;
		int lengthinitial = 400;
		int randomSeed = 13;
		String destDirName = "simtrace2";
		File dir = new File(destDirName);
		if (dir.exists() == false)
			dir.mkdir();

		for (int i = widthinitial; i <= 100; i += 10)
			for (int j = lengthinitial; j < 1000; j += 100) {
				String tracename = dir.getAbsolutePath() + "/" + i + "-" + j + ".tr";

				double[] fracsOfClasses = new double[] { 41, 29, 9, 21 };

				JobClassDescription[] jobClassDescs = new JobClassDescription[] { new JobClassDescription(1, 5, 1, 10),
						new JobClassDescription(1, 5, 10, j), new JobClassDescription(5, i, 1, 10),
						new JobClassDescription(5, i, 10, j) };
				setParams(i, numJobs, jobClassDescs, fracsOfClasses, randomSeed, tracename);
				prepareTrace();
			}
	}


	public static void main(String[] argv) {
		// Create TraceProducer
		TraceGenerator traceProducer = new TraceGenerator();
		traceProducer.trace_simulate_1();
		traceProducer.trace_simulate_2();
	}
		
}
