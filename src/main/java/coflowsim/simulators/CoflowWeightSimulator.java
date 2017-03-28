package coflowsim.simulators;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import coflowsim.datastructures.Flow;
import coflowsim.datastructures.Job;
import coflowsim.datastructures.ReduceTask;
import coflowsim.datastructures.Task;
import coflowsim.datastructures.Task.TaskType;
import coflowsim.traceproducers.TraceProducer;
import coflowsim.utils.Constants;
import coflowsim.utils.Constants.SHARING_ALGO;
import coflowsim.utils.Utils;

public class CoflowWeightSimulator extends CoflowSimulator {

	Vector<Job> sortedJobs;

	/**
	 * {@inheritDoc}
	 */
	public CoflowWeightSimulator(SHARING_ALGO sharingAlgo, TraceProducer traceProducer) {

		super(sharingAlgo, traceProducer, false, false, 0.0);
		assert (sharingAlgo == SHARING_ALGO.WEIGHT);
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	protected void initialize(TraceProducer traceProducer) {
		super.initialize(traceProducer);
		this.sortedJobs = new Vector<Job>();

	}

	/**
	 * <p>
	 * Adjust bandwidth according to job sequence in the queue
	 * </p>
	 * 
	 * @param curTime
	 *            current time
	 */
	private void updateRates(long curTime) {

		int[][] sendflows = new int[NUM_RACKS][Constants.N];
		int[][] recvflows = new int[NUM_RACKS][Constants.N];

		double[] sendUsed = new double[Constants.N];
		double[] recvUsed = new double[Constants.N];

		Arrays.fill(sendUsed, 0);
		Arrays.fill(recvUsed, 0);
		Arrays.fill(sendflows, 0);
		Arrays.fill(recvflows, 0);

		// refresh the bandwidth of flows in NIC level
		int sortsize = sortedJobs.size();
		for (int i = 0; i < sortsize; i++) {
			Job sj = sortedJobs.get(i);
			for (Task t : sj.tasks) {
				if (t.taskType != TaskType.REDUCER)
					continue;
				// for the tasks we have
				ReduceTask rt = (ReduceTask) t;
				int priority = (int) Math.log((double) i / (double) Constants.E + 1) + 1;
				if (priority >= Constants.N) {
					priority = Constants.N - 1;
				}

				// add receive port number flow information
				recvflows[rt.taskID][priority] += rt.flows.size();
				// for all the flows set the priority
				for (Flow f : rt.flows) {
					// add the number of flows that from the particular source.
					sendflows[f.mapper.taskID][priority]++;
				}
			}
		}

		for (int i = 0; i < sortedJobs.size(); i++) {
			// compute the bandwidth according to the sequence
			Job sj = sortedJobs.get(i);
			for (Task t : sj.tasks) {
				if (t.taskType != TaskType.REDUCER)
					continue;
				// for the tasks we have
				ReduceTask rt = (ReduceTask) t;
				int priority = (int) Math.log((double) i / (double) Constants.E + 1) + 1;
				if (priority >= Constants.N) {
					priority = Constants.N - 1;
				}

				for (Flow f : rt.flows) {
					// add the number of flows that from the particular source.
					sendflows[f.mapper.taskID][priority]++;
					int nSend = sendflows[f.mapper.taskID][priority];
					int nReceive = recvflows[rt.taskID][priority];

					int n = Math.max(nSend, nReceive);
					// after get he number of flows, compute the bandwidth
					// according to this
					double computebandwidth = (double) Constants.RACK_BITS_PER_SEC / Math.pow(2, priority) / n;
					if (sendUsed[f.mapper.taskID] + computebandwidth > Constants.RACK_BITS_PER_SEC) {
						System.out.println("no enough bandwidth at the sender side");
						f.currentBps = Constants.RACK_BITS_PER_SEC - sendUsed[f.mapper.taskID];
					} else if (recvUsed[rt.taskID] + computebandwidth > Constants.RACK_BITS_PER_SEC) {
						System.out.println("no enough at the receive side");
						f.currentBps = Constants.RACK_BITS_PER_SEC - recvUsed[rt.taskID];
					} else {
						f.currentBps = computebandwidth;
						sendUsed[f.mapper.taskID] += f.currentBps;
						recvUsed[f.reducer.taskID] += f.currentBps;
						System.out.println("bandwidth=" + f.currentBps);
					}

				}

			}
		}

		// work conservation, allocate the remaining bandwidth....
		if (Constants.bconservation) {
			double[] sendFree = new double[NUM_RACKS];
			double[] recvFree = new double[NUM_RACKS];
			for (int p = 0; p < NUM_RACKS; p++) {
				sendFree[p] = Constants.RACK_BITS_PER_SEC - sendUsed[p];
				recvFree[p] = Constants.RACK_BITS_PER_SEC - recvUsed[p];
			}

			for (int i = 0; i < sortedJobs.size(); i++) {
				// compute the bandwidth according to the sequence
				Job sj = sortedJobs.get(i);
				for (Task t : sj.tasks) {
					if (t.taskType != TaskType.REDUCER)
						continue;
					// for the tasks we have
					ReduceTask rt = (ReduceTask) t;
					int dst = rt.taskID;
					for (Flow f : rt.flows) {
						int src = f.mapper.taskID;
						double minFree = Math.min(sendBpsFree[src], recvBpsFree[dst]);
						if (minFree <= Constants.ZERO)
							minFree = 0.0;

						f.currentBps += minFree;
						sendFree[src] -= minFree;
						recvFree[dst] -= minFree;

					}
				}
			}

		}

	}

	/** {@inheritDoc} */
	@Override
	protected void afterJobAdmission(long curTime) {
		updateJobOrder(sendBpsFree, recvBpsFree);
		layoutFlowsInJobOrder();
		updateRates(curTime);
	}

	/** {@inheritDoc} */
	@Override
	protected void afterJobDeparture(long curTime) {
		updateJobOrder(sendBpsFree, recvBpsFree);
		layoutFlowsInJobOrder();
		updateRates(curTime);
	}

	@Override
	protected void addToSortedJobs(Job j) {
		if (sortedJobs.contains(j)) {
			return;
		}

		// Add to the end of the first queue
		sortedJobs.add(j);
	}

	/**
	 * <p>
	 * Update the sequence of the task sFree are the send port available
	 * capacity rFree are the receive port available capacity
	 * </p>
	 */
	private void updateJobOrder(double[] sFree, double[] rFree) {
		double recvBytes[] = new double[NUM_RACKS];
		double sendBytes[] = new double[NUM_RACKS];
		Arrays.fill(recvBytes, 0);
		Arrays.fill(sendBytes, 0);
		Vector<Job> result = new Vector<Job>();
		int jobsize = sortedJobs.size();
		double adjustweight[] = new double[jobsize];

		// processing time of job i task k, port
		double processingtimeSend[][][] = new double[25][50][100];
		double processingtimeReceive[][][] = new double[25][50][100];

		Arrays.fill(processingtimeSend, 0);
		Arrays.fill(processingtimeReceive, 0);

		// initiate weight of tasks
		for (int i = 0; i < jobsize; i++) {
			adjustweight[i] = ((Job) sortedJobs.get(i)).weight;
		}

		// compute the load of every tasks to the ports
		int sortedJobsize = sortedJobs.size();
		for (int i = 0; i < sortedJobsize; i++) {
			Job j = sortedJobs.get(i);
			// first compute the load of every port
			int tasksize = j.tasks.size();
			for (int k = 0; k < tasksize; k++) {
				Task t = j.tasks.get(k);
				if (t.taskType != TaskType.REDUCER)
					continue;
				// compute the processing time of each port
				ReduceTask rt = (ReduceTask) t;
				recvBytes[rt.taskID] += rt.shuffleBytesLeft;
				// check if rFree is zero
				if (rFree[rt.taskID] > Constants.ZERO)
					processingtimeReceive[i][k][rt.taskID] += rt.shuffleBytesLeft / rFree[rt.taskID];
				else
					processingtimeReceive[i][k][rt.taskID] = Constants.VALUE_UNKNOWN;
				int flowsize = rt.flows.size();
				for (int m = 0; m < flowsize; m++) {
					Flow f = rt.flows.get(m);
					sendBytes[f.mapper.taskID] += f.bytesRemaining;
					// check if sFree is zero
					if (sFree[f.mapper.taskID] > Constants.ZERO)
						processingtimeSend[i][k][f.mapper.taskID] += f.bytesRemaining / sFree[f.mapper.taskID];
					else
						processingtimeSend[i][k][f.mapper.taskID] = Constants.VALUE_UNKNOWN;
				}
			}
		}

		int k = sortedJobs.size() - 1;
		while (k >= 0) {
			// find the largest load port
			double maxrecv = 0;
			int maxrecvmachine = 0;
			double maxsend = 0;
			int maxsendmachine = 0;

			// get the heaviest load
			for (int i = 0; i < NUM_RACKS; i++) {
				if (maxsend <= sendBytes[i]) {
					maxsendmachine = i;
					maxsend = sendBytes[i];
				}

				if (maxrecv <= recvBytes[i]) {
					maxrecvmachine = i;
					maxrecv = recvBytes[i];
				}
			}

			// find the corresponding job who has max(weight/processing time)
			double minfactor = 1000000;
			int jobindex = 0;

			// refresh the job set size
			sortedJobsize = sortedJobs.size();
			for (int i = 0; i < sortedJobsize; i++) {
				Job job = sortedJobs.get(i);
				// only deals with the other jobs
				if (result.contains(job))
					continue;
				double taskmaplevel = 0;
				double taskreducelevel = 0;
				for (int j = 0; j < 50; j++) {
					taskmaplevel += processingtimeSend[i][j][maxsendmachine];
					taskreducelevel += processingtimeReceive[i][j][maxrecvmachine];
				}
				double level = 0;
				if (taskmaplevel < taskreducelevel) {
					level = taskmaplevel;
				} else {
					level = taskreducelevel;
				}

				if (minfactor > adjustweight[i] / level) {
					minfactor = adjustweight[i] / level;
					jobindex = i; // find the corresponding job
				}

			}

			// add the job to the result set
			result.add(sortedJobs.get(jobindex));
			// adjust the job weight in sorted Jobs
			for (int i = 0; i < sortedJobsize; i++) {
				if (result.contains(sortedJobs.get(i)))
					continue;
				double taskmaplevel = 0;
				double taskreducelevel = 0;
				for (int j = 0; j < 50; j++) {
					taskmaplevel += processingtimeSend[i][j][maxsendmachine];
					taskreducelevel += processingtimeReceive[i][j][maxrecvmachine];
				}
				double level = Math.max(taskmaplevel, taskreducelevel);
				adjustweight[i] -= minfactor * level;

			}
			k--;

		}

		int size = result.size();
		for (int i = size - 1; i >= 0; i--) {
			sortedJobs.set(size - 1 - i, result.get(i));
		}

	}

	/** {@inheritDoc} */
	@Override
	protected void removeDeadJob(Job j) {
		activeJobs.remove(j.jobName);
		sortedJobs.remove(j);
	}

}
