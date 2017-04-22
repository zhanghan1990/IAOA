# YosemiteSim README
This is the simulator of Yosemite,Yosemite tries to minimize coflow weight completion time, Thanks to Varys, we borrow many codes from them.

##  How to compile and use
 We use maven to construct the project, compile the project:
 - mvn compile
 - mvn package
 
To clean your project:
- mvn clean
 
## ex1: Compare the performance of online and offline algorithm

- ./run coflowsim.experiments.offline_online.CoflowSim_offline trace-tr dest-dr on
- ./run coflowsim.experiments.offline_online.CoflowSim_offline trace-tr dest-dr off


on is online-algorithm and off is offline algorithm. The dest-dr will include the trace we will analyze

## ex2: Use facebook trace to compare the performance of Yosemite,Varys and Barrat


- ./run coflowsim.experiments.real.CoflowSim_Traffic Barrat trace/real/8-300-REAL.tr trace/real/Barrat
 - ./run coflowsim.experiments.real.CoflowSim_Traffic Varys trace/real/8-300-REAL.tr trace/real/Varys  
  
 - ./run coflowsim.experiments.real.CoflowSim_Traffic Yosemite trace/real/8-300-REAL.tr trace/real/Yosemite

 - ./run coflowsim.experiments.real.CoflowSim_Traffic pFabric trace/real/8-300-REAL.tr trace/real/pFabric
 
  - ./run coflowsim.experiments.real.CoflowSim_Traffic FAIR trace/real/8-300-REAL.tr trace/real/FAIR
 
- If you want to generate other trace, just run python generatetraffic.py
