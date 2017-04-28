#!/bin/bash
TYPE=$1
for((i=100;i<5000;i+=500));
	do
		echo $i
		../../run coflowsim.experiments.weight.CoflowSim_Weight $TYPE 20-$i-REAL.tr $TYPE-$i.rt
	done
