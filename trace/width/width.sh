#!/bin/bash
TYPE=$1
for((i=10;i<60;i+=5));
	do
		echo $i
		../../run coflowsim.experiments.width.CoflowSim_Width $TYPE $i.tr $TYPE-$i.rt
	done
