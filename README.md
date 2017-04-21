## YosemiteSim README
This is the simulator of Yosemite,Yosemite tries to minimize coflow weight completion time, Thanks to Varys, we borrow many codes from them.

# How to compile and use
 We use maven to construct the project, compile the project:
 - maven compile
 - maven package
 - In the following experiment, just replace Yourpath to the path that in your computer
 
# ex1: Compare the performance of online and offline algorithm

- java -cp target/coflowsim-0.2.0-SNAPSHOT.jar coflowsim.experiments.offline_online.CoflowSim_offline Yourpath/5.tr Yourpath/5.off  off


- java -cp target/coflowsim-0.2.0-SNAPSHOT.jar coflowsim.experiments.offline_online.CoflowSim_offline Yourpath/5.tr Yourpath/5.on  on

# ex2: Use facebook trace to compare the performance of Yosemite and Varys and Barrat


