#!/bin/bash
TYPE=$1
for((i=50;i<600;i+=50));
	do
		echo $i
		../../run coflowsim.experiments.concurrent.CoflowSim_Concurrent $TYPE $i.tr $TYPE-$i.rt
	done
