#!/bin/bash
TYPE=$1
for((i=5;i<100;i+=5));
	do
		echo $i
		../../run coflowsim.experiments.length.CoflowSim_Length_frac $TYPE $i.tr $TYPE-$i.rt
	done
