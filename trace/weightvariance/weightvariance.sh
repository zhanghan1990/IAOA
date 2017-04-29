#!/bin/bash
TYPE=$1
for((i=1;i<11;i+=1));
	do
		echo $i
		../../run coflowsim.experiments.weight.CoflowSim_Weight $TYPE 11-$i-REAL.tr $TYPE-$i.rt
	done
