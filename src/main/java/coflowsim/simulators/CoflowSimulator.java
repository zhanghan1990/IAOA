package coflowsim.simulators;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import coflowsim.datastructures.Flow;
import coflowsim.datastructures.Job;
import coflowsim.datastructures.MapTask;
import coflowsim.datastructures.ReduceTask;
import coflowsim.datastructures.Task;
import coflowsim.datastructures.Task.TaskType;
import coflowsim.traceproducers.TraceProducer;
import coflowsim.utils.Constants;
import coflowsim.utils.Constants.SHARING_ALGO;
import coflowsim.utils.Utils;

/**
 * Implements {@link coflowsim.simulators.Simulator} for coflow-level scheduling
 * policies (FIFO, SCF, NCF, LCF, SEBF, WIGHT).
 */
public class CoflowSimulator extends Simulator {

	Vector<Job> sortedJobs;

	double[] sendBpsFree;
	double[] recvBpsFree;

	int processingtimeSend[][];
	int processingtimeReceive[][];

	/**
	 * {@inheritDoc}
	 */
	public CoflowSimulator(SHARING_ALGO sharingAlgo, TraceProducer traceProducer, boolean offline,
			boolean considerDeadline, double deadlineMultRandomFactor) {

		super(sharingAlgo, traceProducer, offline, considerDeadline, deadlineMultRandomFactor);
		assert (sharingAlgo == SHARING_ALGO.FIFO || sharingAlgo == SHARING_ALGO.SCF || sharingAlgo == SHARING_ALGO.NCF
				|| sharingAlgo == SHARING_ALGO.LCF || sharingAlgo == SHARING_ALGO.SEBF
				|| sharingAlgo == SHARING_ALGO.WEIGHT || sharingAlgo == SHARING_ALGO.WEIGHTOFFLINE);
	}

	/** {@inheritDoc} */
	@Override
	protected void initialize(TraceProducer traceProducer) {
		super.initialize(traceProducer);

		this.sendBpsFree = new double[NUM_RACKS];
		this.recvBpsFree = new double[NUM_RACKS];

		resetSendRecvBpsFree();

		this.sortedJobs = new Vector<Job>();
	}

	protected void resetSendRecvBpsFree() {
		Arrays.fill(this.sendBpsFree, Constants.RACK_BITS_PER_SEC);
		Arrays.fill(this.recvBpsFree, Constants.RACK_BITS_PER_SEC);
	}

	/**
	 * Admission control for the deadline-sensitive case.
	 */
	@Override
	protected boolean admitThisJob(Job j) {
		if (considerDeadline) {
			updateRatesDynamicAlpha(Constants.VALUE_UNKNOWN, true);
			double currentAlpha = calcAlphaOnline(j, sendBpsFree, recvBpsFree);
			if (currentAlpha == Constants.VALUE_UNKNOWN || currentAlpha > j.deadlineDuration / 1000.0)
				return false;
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	protected void uponJobAdmission(Job j) {
		for (Task t : j.tasks) {
			if (t.taskType == TaskType.REDUCER) {
				ReduceTask rt = (ReduceTask) t;

				// Update start stats for the task and its parent job
				rt.startTask(CURRENT_TIME);

				// Add the parent job to the collection of active jobs
				if (!activeJobs.containsKey(rt.parentJob.jobName)) {
					activeJobs.put(rt.parentJob.jobName, rt.parentJob);
					addToSortedJobs(j);
				}
				incNumActiveTasks();
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void afterJobAdmission(long curTime) {
		layoutFlowsInJobOrder();
		updateRatesDynamicAlpha(curTime, false);
	}

	/** {@inheritDoc} */
	@Override
	protected void onSchedule(long curTime) {
		proceedFlowsInAllRacks(curTime, Constants.SIMULATION_QUANTA);
	}

	/** {@inheritDoc} */
	@Override
	protected void afterJobDeparture(long curTime) {
		updateRatesDynamicAlpha(curTime, false);
	}

	/**
	 * Calculate dynamic alpha (in seconds)
	 * 
	 * @param job
	 *            coflow to calculate for
	 * @param sFree
	 *            free bps in uplinks
	 * @param rFree
	 *            free bps in downlinks
	 * @return
	 */
	private double calcAlphaOnline(Job job, double[] sFree, double[] rFree) {
		double[] sendBytes = new double[NUM_RACKS];
		Arrays.fill(sendBytes, 0.0);
		double[] recvBytes = new double[NUM_RACKS];
		Arrays.fill(recvBytes, 0.0);

		// Calculate based on bytes remaining
		for (Task t : job.tasks) {
			if (t.taskType == TaskType.REDUCER) {
				ReduceTask rt = (ReduceTask) t;
				recvBytes[rt.taskID] = rt.shuffleBytesLeft;
				for (Flow f : rt.flows) {
					sendBytes[f.mapper.taskID] += f.bytesRemaining;
				}
			}
		}

		// Scale by available capacity
		for (int i = 0; i < NUM_RACKS; i++) {
			if ((sendBytes[i] > 0 && sFree[i] <= Constants.ZERO) || (recvBytes[i] > 0 && rFree[i] <= Constants.ZERO)) {
				return Constants.VALUE_UNKNOWN;
			}

			sendBytes[i] = sendBytes[i] * 8 / sFree[i];
			recvBytes[i] = recvBytes[i] * 8 / rFree[i];
		}

		return Math.max(Utils.max(sendBytes), Utils.max(recvBytes));
	}

	/**
	 * Update rates of flows for each coflow using a dynamic alpha, calculated
	 * based on remaining bytes of the coflow and current condition of the
	 * network
	 * 
	 * @param curTime
	 *            current time
	 * @param trialRun
	 *            if true, do NOT change any jobs allocations
	 */
	private void updateRatesDynamicAlpha(final long curTime, boolean trialRun) {
		// Reset sendBpsFree and recvBpsFree
		resetSendRecvBpsFree();

		// For keeping track of jobs with invalid currentAlpha
		Vector<Job> skippedJobs = new Vector<Job>();
		// System.out.println(sortedJobs.size());
		// Recalculate rates
		for (Job sj : sortedJobs) {
			// Reset ALL rates first
			for (Task t : sj.tasks) {
				if (t.taskType == TaskType.REDUCER) {
					ReduceTask rt = (ReduceTask) t;
					for (Flow f : rt.flows) {
						f.currentBps = 0;
					}
				}
			}

			double[] sendUsed = new double[NUM_RACKS];
			double[] recvUsed = new double[NUM_RACKS];
			Arrays.fill(sendUsed, 0.0);
			Arrays.fill(recvUsed, 0.0);

			double currentAlpha = calcAlphaOnline(sj, sendBpsFree, recvBpsFree);
			if (currentAlpha == Constants.VALUE_UNKNOWN) {
				skippedJobs.add(sj);
				continue;
			}
			// Use deadline instead of alpha when considering deadlines
			if (considerDeadline) {
				currentAlpha = sj.deadlineDuration / 1000;
			}

			updateRatesDynamicAlphaOneJob(sj, currentAlpha, sendUsed, recvUsed, trialRun);

			// Remove capacity from ALL sources and destination for the entire
			// job
			for (int i = 0; i < NUM_RACKS; i++) {
				sendBpsFree[i] -= sendUsed[i];
				recvBpsFree[i] -= recvUsed[i];
			}
		}

		// Work conservation
		if (!trialRun) {
			// First, consider all skipped ones and divide remaining bandwidth
			// between their flows
			double[] sendUsed = new double[NUM_RACKS];
			double[] recvUsed = new double[NUM_RACKS];
			Arrays.fill(sendUsed, 0.0);
			Arrays.fill(recvUsed, 0.0);

			updateRatesFairShare(skippedJobs, sendUsed, recvUsed);

			// Remove capacity from ALL sources and destination for the entire
			// job
			for (int i = 0; i < NUM_RACKS; i++) {
				sendBpsFree[i] -= sendUsed[i];
				recvBpsFree[i] -= recvUsed[i];
			}

			// Heuristic: Sort coflows by EDF and then refill
			// If there is no deadline, this simply sorts them by arrival time
			Vector<Job> sortedByEDF = new Vector<Job>(sortedJobs);
			Collections.sort(sortedByEDF, new Comparator<Job>() {
				public int compare(Job o1, Job o2) {
					int timeLeft1 = (int) (o1.simulatedStartTime + o1.deadlineDuration - curTime);
					int timeLeft2 = (int) (o2.simulatedStartTime + o2.deadlineDuration - curTime);
					return timeLeft1 - timeLeft2;
				}
			});

			for (Job sj : sortedByEDF) {
				for (Task t : sj.tasks) {
					if (t.taskType != TaskType.REDUCER) {
						continue;
					}

					ReduceTask rt = (ReduceTask) t;
					int dst = rt.taskID;
					if (recvBpsFree[dst] <= Constants.ZERO) {
						continue;
					}

					for (Flow f : rt.flows) {
						int src = f.mapper.taskID;

						double minFree = Math.min(sendBpsFree[src], recvBpsFree[dst]);
						if (minFree <= Constants.ZERO)
							minFree = 0.0;

						f.currentBps += minFree;
						sendBpsFree[src] -= minFree;
						recvBpsFree[dst] -= minFree;
					}
				}
			}
		}
	}

	/**
	 * Update rates of individual flows from a collection of coflows while
	 * considering them as a single coflow.
	 * 
	 * Modifies sendBpsFree and recvBpsFree
	 * 
	 * @param jobsToConsider
	 *            Collection of {@link coflowsim.datastructures.Job} under
	 *            consideration.
	 * @param sendUsed
	 *            initialized to zeros.
	 * @param recvUsed
	 *            initialized to zeros.
	 * @return
	 */
	private double updateRatesFairShare(Vector<Job> jobsToConsider, double[] sendUsed, double[] recvUsed) {
		double totalAlloc = 0.0;

		// Calculate the number of mappers and reducers in each port
		int[] numMapSideFlows = new int[NUM_RACKS];
		Arrays.fill(numMapSideFlows, 0);
		int[] numReduceSideFlows = new int[NUM_RACKS];
		Arrays.fill(numReduceSideFlows, 0);

		for (Job sj : jobsToConsider) {
			for (Task t : sj.tasks) {
				if (t.taskType != TaskType.REDUCER) {
					continue;
				}

				ReduceTask rt = (ReduceTask) t;
				int dst = rt.taskID;
				if (recvBpsFree[dst] <= Constants.ZERO) {
					continue;
				}

				for (Flow f : rt.flows) {
					int src = f.mapper.taskID;
					if (sendBpsFree[src] <= Constants.ZERO) {
						continue;
					}

					numMapSideFlows[src]++;
					numReduceSideFlows[dst]++;
				}
			}
		}

		for (Job sj : jobsToConsider) {
			for (Task t : sj.tasks) {
				if (t.taskType != TaskType.REDUCER) {
					continue;
				}

				ReduceTask rt = (ReduceTask) t;
				int dst = rt.taskID;
				if (recvBpsFree[dst] <= Constants.ZERO || numReduceSideFlows[dst] == 0) {
					continue;
				}

				for (Flow f : rt.flows) {
					int src = f.mapper.taskID;
					if (sendBpsFree[src] <= Constants.ZERO || numMapSideFlows[src] == 0) {
						continue;
					}

					// Determine rate based only on this job and available
					// bandwidth
					double minFree = Math.min(sendBpsFree[src] / numMapSideFlows[src],
							recvBpsFree[dst] / numReduceSideFlows[dst]);
					if (minFree <= Constants.ZERO) {
						minFree = 0.0;
					}

					f.currentBps = minFree;

					// Remember how much capacity was allocated
					sendUsed[src] += f.currentBps;
					recvUsed[dst] += f.currentBps;
					totalAlloc += f.currentBps;
				}
			}
		}

		return totalAlloc;
	}

	/**
	 * Update rates of individual flows of a given coflow.
	 * 
	 * @param sj
	 *            {@link coflowsim.datastructures.Job} under consideration.
	 * @param currentAlpha
	 *            dynamic alpha is in Seconds (Normally, alpha is in Bytes).
	 * @param sendUsed
	 *            initialized to zeros.
	 * @param recvUsed
	 *            initialized to zeros.
	 * @param trialRun
	 *            if true, do NOT change any jobs allocations.
	 * @return
	 */
	private double updateRatesDynamicAlphaOneJob(Job sj, double currentAlpha, double[] sendUsed, double[] recvUsed,
			boolean trialRun) {

		double jobAlloc = 0.0;

		for (Task t : sj.tasks) {
			if (t.taskType != TaskType.REDUCER) {
				continue;
			}

			ReduceTask rt = (ReduceTask) t;
			int dst = rt.taskID;
			if (recvBpsFree[dst] <= Constants.ZERO) {
				continue;
			}

			for (Flow f : rt.flows) {
				int src = f.mapper.taskID;

				double curBps = f.bytesRemaining * 8 / currentAlpha;
				if (curBps > sendBpsFree[src] || curBps > recvBpsFree[dst]) {
					curBps = Math.min(sendBpsFree[src], recvBpsFree[dst]);
				}

				// Remember how much capacity was allocated
				sendUsed[src] += curBps;
				recvUsed[dst] += curBps;
				jobAlloc += curBps;

				// Update f.currentBps if required
				if (!trialRun) {
					f.currentBps = curBps;
				}
			}
		}

		return jobAlloc;
	}

	protected void addToSortedJobs(Job j) {
		if (considerDeadline) {
			sortedJobs.add(j);
			return;
		}

		if (sharingAlgo == SHARING_ALGO.FIFO) {
			sortedJobs.add(j);
			// System.out.println(sortedJobs.size());
		} else if (sharingAlgo == SHARING_ALGO.WEIGHT) {
			int index = 0;
			for (Job sj : sortedJobs) {
				if (WEIGHTComparator.compare(j, sj) < 0)
					break;
				index++;
			}
			sortedJobs.insertElementAt(j, index);
		} else if (sharingAlgo == SHARING_ALGO.WEIGHTOFFLINE) {
			updateJobOrder(j);
		} else {
			int index = 0;
			for (Job sj : sortedJobs) {
				if (sharingAlgo == SHARING_ALGO.SCF && SCFComparator.compare(j, sj) < 0) {
					break;
				} else if (sharingAlgo == SHARING_ALGO.NCF && NCFComparator.compare(j, sj) < 0) {
					break;
				} else if (sharingAlgo == SHARING_ALGO.LCF && LCFComparator.compare(j, sj) < 0) {
					break;
				} else if (sharingAlgo == SHARING_ALGO.SEBF && SEBFComparator.compare(j, sj) < 0) {
					break;
				}
				index++;
			}
			sortedJobs.insertElementAt(j, index);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void removeDeadJob(Job j) {
		activeJobs.remove(j.jobName);
		sortedJobs.remove(j);
	}

	private void proceedFlowsInAllRacks(long curTime, long quantaSize) {
		for (int i = 0; i < NUM_RACKS; i++) {
			double totalBytesMoved = 0;
			Vector<Flow> flowsToRemove = new Vector<Flow>();
			for (Flow f : flowsInRacks[i]) {
				ReduceTask rt = f.reducer;

				if (totalBytesMoved >= Constants.RACK_BYTES_PER_SEC) {
					break;
				}

				double bytesPerTask = (f.currentBps / 8) * (1.0 * quantaSize / Constants.SIMULATION_SECOND_MILLIS);
				bytesPerTask = Math.min(bytesPerTask, f.bytesRemaining);
				f.bytesRemaining -= bytesPerTask;
				if (f.bytesRemaining <= Constants.ZERO) {
					rt.flows.remove(f);
					flowsToRemove.add(f);

					// Give back to src and dst links
					sendBpsFree[f.mapper.taskID] += f.currentBps;
					recvBpsFree[f.reducer.taskID] += f.currentBps;
				}

				totalBytesMoved += bytesPerTask;
				rt.shuffleBytesLeft -= bytesPerTask;
				rt.parentJob.decreaseShuffleBytesPerRack(rt.taskID, bytesPerTask);
				rt.parentJob.shuffleBytesCompleted += bytesPerTask;

				// If no bytes remaining, mark end and mark for removal
				if ((rt.shuffleBytesLeft <= Constants.ZERO || rt.flows.size() == 0) && !rt.isCompleted()) {
					rt.cleanupTask(curTime + quantaSize);
					if (!rt.parentJob.jobActive) {
						removeDeadJob(rt.parentJob);
					}
					decNumActiveTasks();
				}
			}
			flowsInRacks[i].removeAll(flowsToRemove);
		}
	}

	protected void layoutFlowsInJobOrder() {
		for (int i = 0; i < NUM_RACKS; i++) {
			flowsInRacks[i].clear();
		}

		for (Job j : sortedJobs) {
			for (Task r : j.tasks) {
				if (r.taskType != TaskType.REDUCER) {
					continue;
				}

				ReduceTask rt = (ReduceTask) r;
				if (rt.isCompleted()) {
					continue;
				}

				flowsInRacks[r.taskID].addAll(rt.flows);
			}
		}
	}

	/**
	 * Comparator used by {@link CoflowSimulator#addToSortedJobs(Job)} to add
	 * new job in list sorted by coflow length.
	 */
	private static Comparator<Job> SCFComparator = new Comparator<Job>() {
		public int compare(Job o1, Job o2) {
			if (o1.maxShuffleBytes / o1.numMappers == o2.maxShuffleBytes / o2.numMappers)
				return 0;
			return o1.maxShuffleBytes / o1.numMappers < o2.maxShuffleBytes / o2.numMappers ? -1 : 1;
		}
	};

	/**
	 * Comparator used by {@link CoflowSimulator#addToSortedJobs(Job)} to add
	 * new job in list sorted by coflow size.
	 */
	private static Comparator<Job> LCFComparator = new Comparator<Job>() {
		public int compare(Job o1, Job o2) {
			double n1 = o1.calcShuffleBytesLeft();
			double n2 = o2.calcShuffleBytesLeft();
			if (n1 == n2)
				return 0;
			return n1 < n2 ? -1 : 1;
		}
	};

	/**
	 * Comparator used by {@link CoflowSimulator#addToSortedJobs(Job)} to add
	 * new job in list sorted by the minimum number of endpoints of a coflow.
	 */
	private static Comparator<Job> NCFComparator = new Comparator<Job>() {
		public int compare(Job o1, Job o2) {
			int n1 = (o1.numMappers < o1.numReducers) ? o1.numMappers : o1.numReducers;
			int n2 = (o2.numMappers < o2.numReducers) ? o2.numMappers : o2.numReducers;
			return n1 - n2;
		}
	};

	/**
	 * Comparator used by {@link CoflowSimulator#addToSortedJobs(Job)} to add
	 * new job in list sorted by static coflow skew.
	 */
	private static Comparator<Job> SEBFComparator = new Comparator<Job>() {
		public int compare(Job o1, Job o2) {
			// System.out.println(o1.alpha);
			if (o1.alpha == o2.alpha)
				return 0;
			return o1.alpha < o2.alpha ? -1 : 1;
		}
	};

	/**
	 * Comparator used by {@link CoflowSimulator#addToSortedJobs(Job)} to add
	 * new job in list sorted by static coflow skew.
	 */
	private static Comparator<Job> WEIGHTComparator = new Comparator<Job>() {
		public int compare(Job o1, Job o2) {
			if (o1.weight / o1.alpha == o2.weight / o2.alpha)
				return 0;
			return o1.weight / o1.alpha < o2.weight / o2.alpha ? 1 : -1;
		}
	};

	/***
	 * 
	 * @param load
	 * @return get the max load of the return value and the corresponding index
	 */
	private int[] findMax(int load[][], Vector<Job> result) {
		int ret[] = new int[2];
		int maxload = 0;
		int index = 0;
		for (int i = 0; i < NUM_RACKS; i++) {
			int sumload = 0;
			for (Job j : sortedJobs) {
				if (result.contains(j))
					continue;
				sumload += load[j.jobID][i];
			}

			if (maxload <= sumload) {
				maxload = sumload;
				index = i;
			}

		}
		ret[0] = index;
		ret[1] = maxload;
		return ret;
	}

	private String[] findJobIndex(int[][] load, double[] weight, int rack, Vector<Job> result) {
		String[] ret = new String[2];
		int index = 0;
		double minfactor = 1000000;
		for (Job j : sortedJobs) {
			
			if (result.contains(j))
				continue;
			
			int jobindex = j.jobID;
			if(load[jobindex][rack]<=Constants.ZERO)
				continue;
			
			//System.out.println(load[jobindex][rack]);
			
			double factor = (weight[jobindex]) / (double) (load[jobindex][rack]);
			if (factor < minfactor) {
				minfactor = factor;
				index = jobindex;

			}
		}
		ret[0] = Integer.toString(index);
		ret[1] = Double.toString(minfactor);
		return ret;

	}

	private double[] adWeight(double[] adjust, double sita, int machine, int[][] load) {
		for (Job j : sortedJobs)
			adjust[j.jobID] -= sita * load[j.jobID][machine];
		return adjust;
	}

	
	
	
	
	
	private void computeLoad(Vector<Job> result) {

		for (int i = 0; i < Constants.MAXJOBS; i++)
			for (int k = 0; k < NUM_RACKS; k++) {
				processingtimeSend[i][k] = 0;
				processingtimeReceive[i][k] = 0;
			}

		// compute the load of jobs to ports
		int sortedJobsize = sortedJobs.size();
		for (int i = 0; i < sortedJobsize; i++) {

			Job j = sortedJobs.get(i);
			// first compute the load of every port

			// only computes the job that is not in the result
			if (result.contains(j))
				continue;
			int tasksize = j.tasks.size();
			for (Task t : j.tasks) {

				double perMapperBytes = (j.totalShuffleBytes - j.shuffleBytesCompleted) / j.numMappers;

				if (t.taskType == TaskType.MAPPER) {
					MapTask mt = (MapTask) t;
					processingtimeSend[j.jobID][mt.taskID] += perMapperBytes;
				}

				if (t.taskType == TaskType.REDUCER) {
					ReduceTask rt = (ReduceTask) t;
					processingtimeReceive[j.jobID][rt.taskID] += rt.shuffleBytesLeft;
				}
			}
		}

	}

	// give the job index and return the corresponding job
	private Job findByIndex(Vector<Job> v, int jobindex) {

		for (Job J : v) {
			if (J.jobID == jobindex)
				return J;
		}
		return null;
	}

	/**
	 * <p>
	 * Update the sequence of the task sFree are the send port available
	 * capacity rFree are the receive port available capacity
	 * </p>
	 */
	private void updateJobOrder(Job J) {

		sortedJobs.add(J);

		// for the new sorted jobs order
		Vector<Job> result = new Vector<Job>();

		// the total number of jobs to be scheduled
		int jobsize = sortedJobs.size();

		// initital adjust weight size
		double adjustweight[] = new double[Constants.MAXJOBS];

		// processing time of job i to the particular ports
		processingtimeSend = new int[Constants.MAXJOBS][NUM_RACKS];
		processingtimeReceive = new int[Constants.MAXJOBS][NUM_RACKS];

		// initiate weight of tasks
		for (int i = 0; i < jobsize; i++) {
			adjustweight[(int) sortedJobs.get(i).jobID] = (sortedJobs.get(i)).weight;
		}

		computeLoad(result);

		for (int i = 0; i < jobsize; i++) {

			// find the largest load port
			int maxsendindex = 0, maxrecvindex = 0, maxindex = 0;
			int maxsendload = 0, maxrecvload = 0, maxload = 0;

			int[] sendret = findMax(processingtimeSend, result);
			int[] recvret = findMax(processingtimeReceive, result);
			maxsendindex = sendret[0];
			maxsendload = sendret[1];
			maxrecvindex = recvret[0];
			maxrecvload = recvret[1];

			// find the corresponding job
			boolean flag = true;
			int jobindex = 0;
			double factor = 0;
			if (maxsendload >= maxrecvload) {
				maxindex = maxsendindex;
				maxload = maxsendload;
				String[] ret = findJobIndex(processingtimeSend, adjustweight, maxindex, result);
				jobindex = Integer.parseInt(ret[0]);
				factor = Double.parseDouble(ret[1]);
				flag = true;
			} else {
				maxindex = maxrecvindex;
				maxload = maxrecvload;
				String[] ret = findJobIndex(processingtimeReceive, adjustweight, maxindex, result);
				jobindex = Integer.parseInt(ret[0]);
				factor = Double.parseDouble(ret[1]);
				flag = false;
			}

			result.add(findByIndex(sortedJobs, jobindex));

			// now adjust the load
			if (flag == true)
				adjustweight = adWeight(adjustweight, factor, maxindex, processingtimeSend);
			else
				adjustweight = adWeight(adjustweight, factor, maxindex, processingtimeReceive);

			computeLoad(result);
		}

		int size = result.size();
		for (int i = size - 1; i >= 0; i--) {
			sortedJobs.set(size - 1 - i, result.get(i));
		}

	}

}
