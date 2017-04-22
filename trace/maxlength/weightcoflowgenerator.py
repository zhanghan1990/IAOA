#encoding:utf-8

import random
import math
import numpy

TOTAL=200
RACKS=50
ARRIVALMAX=1000000
MAXWEIGHT=4

def fillArray(rack):
	length=len(rack)
	for i in range(0,length):
		rack[i]=0
	return rack

def selectMachine(rack):
	while True:
		machine=random.randint(0, RACKS-1)
		if rack[machine]==0:
			rack[machine]=1
			return rack,machine

def selectMachineN(rack,racknumber):
	while True:
		machine=random.randint(0, racknumber-1)
		if rack[machine]==0:
			rack[machine]=1
			return rack,machine

def generateLength(file,minlength,maxlength):
	f=open(file,"r")
	desfile=open(str(minlength)+"-"+str(maxlength)+".tr","a+")
	totaline=f.readlines()
	f.close()
	index=0
	for line in totaline:
		line=line.strip()
		if index==0:
			desfile.write(line+"\n")
			index+=1
			continue
		arrayline=line.split()
		jobname=arrayline[0]
		desfile.write(jobname+" ")
		weight=int(arrayline[1])
		desfile.write(str(weight)+" ")
		start=int(arrayline[2])
		desfile.write(str(start)+ " ")
		mappernumber=int(arrayline[3])
		desfile.write(str(mappernumber)+" ")
		mappers=[]
		for i in range(0,mappernumber):
			desfile.write(arrayline[4+i]+" ")
			mappers.append(arrayline[4+i])
		reducenumber=int(arrayline[4+mappernumber])
		desfile.write(str(reducenumber)+" ")
		#print reducenumber
		reduces=[]
		for i in range(0,reducenumber):
			reduce= arrayline[4+mappernumber+1+i]
			reduceposition=reduce.split(':')[0]
			reducelength=(int)(reduce.split(':')[1])
			# generate length
			lengthrange=random.randint(minlength,maxlength)

			shufflebytes=lengthrange*mappernumber
			reduces.append(reduceposition+":"+str(shufflebytes))
			desfile.write(reduceposition+":"+str(shufflebytes)+" ")
		index+=1
		desfile.write("\n")
	desfile.close()



if __name__ == "__main__":
	for i in range(0,17):
		maxlength=200+50*i
		generateLength("10.tr",10,maxlength)