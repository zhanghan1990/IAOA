#!/bin/bash
TYPE=$1
for((i=200;i<1000;i+=50));
	do
		echo $i
		../../run coflowsim.experiments.length.CoflowSim_Length $TYPE 10-$i.tr $TYPE-10-$i.rt
	done
