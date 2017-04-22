
import random
import math
import numpy
# generate weight according to different weight variance
# the exceptation is that, larger variance , better
def generateWeightVariance(file,weightaverage,weightvariance):
	f=open(file,"r")
	desfile=open(str(weightaverage)+"-"+str(weightvariance)+"-REAL.tr","a+")
	totaline=f.readlines()
	f.close()
	index=0

	rarray=numpy.random.normal(weightaverage,weightvariance,527)



	for line in totaline:
		line=line.strip()
		if index==0:
			index+=1
			#get number of the job
			desfile.write(line+"\n")
			continue

		index+=1
		arrayline=line.split()
		jobname=arrayline[0]
		weight=1
		start=int(arrayline[2])
		mappernumber=int(arrayline[3])
		mappers=[]


		for i in range(0,mappernumber):
			mappers.append(arrayline[4+i])

		reducenumber=int(arrayline[4+mappernumber])
		reduces=[]
		maxlength=0
		for i in range(0,reducenumber):
			reduce= arrayline[4+mappernumber+1+i]
			reduceposition=reduce.split(':')[0]
			reducelength=(float)(reduce.split(':')[1])
			if reducelength > maxlength:
				maxlength=reducelength
			reduces.append(reduce)




		#set weight according to maxlength
		if rarray[index-1] <=0:
			weight=1
		else:
			weight=rarray[index-1]


		desfile.write(jobname+" ")
		desfile.write(str(weight)+" ")
		desfile.write(str(start)+ " ")
		desfile.write(str(mappernumber)+" ")
		for i in range(0,mappernumber):
			desfile.write(mappers[i]+" ")
		desfile.write(str(reducenumber)+" ")
		for i in range(0,reducenumber):
			desfile.write(reduces[i]+" ")
		desfile.write("\n")
	desfile.close()


#FB2010-weight0.txt

if __name__ == "__main__":
	generateWeightVariance("FB2010-weight0.txt",8,300)