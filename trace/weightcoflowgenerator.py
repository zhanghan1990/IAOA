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












def generateLengthTrace(file,minlength,maxlength,frac):
	handler=open(file,"a+")
	jobs=[]
	sumjob=0
	
	for element in frac:
		jobn=(int)((float)(element*TOTAL)/100.0)
		jobs.append(jobn)
		sumjob+=jobn
	print jobs
	handler.write(str(RACKS)+" "+str(sumjob)+"\n")
	index=0
	for element in jobs:
		for i in range(0,element):
			jobname=str(index)+" "
			handler.write(jobname)
			
			#generate weight and arrival time
			weight=random.randint(1, MAXWEIGHT)
			arrivaltime=random.randint(0,ARRIVALMAX)
			handler.write(str(weight)+" ")
			handler.write(str(arrivaltime)+" ")

			#generate mapper, in this section, the number of mapper is fixed
			numMappers=random.randint(1,RACKS-1)
			handler.write(str(numMappers)+" ")
			rack=[0 for x in range (0,RACKS)]
			rack=fillArray(rack)
			
			for j in range(0,numMappers):
				rack,machine=selectMachine(rack)
				handler.write(str(machine)+" ")
			#generate reduce

			numReducers=random.randint(1,RACKS-1)
			handler.write(str(numReducers)+" ")
			rack=[0 for x in range (0,RACKS)]
			rack=fillArray(rack)

			for j in range(0,numReducers):
				numMB=random.randint(minlength,maxlength)
				shuffleBytes = numMB * numMappers
				rack,reduceid=selectMachine(rack)
				handler.write(str(reduceid)+":"+str(shuffleBytes)+" ")
			handler.write("\n")
			index+=1
	handler.close()


# def generateArrival(file,within):
	#generate different arrival time of coflows






'''
Generate the trace for different factor of width of coflows
'''
def generateWidthFactor(file,mid,small,rackwidth):

	handler=open(file,"a+")
	jobs=[]
	sumjob=0

	RACKS=rackwidth
	jobshort=(int)((float)(small*TOTAL)/100.0)
	joblarge=TOTAL-jobshort
	handler.write(str(RACKS)+" "+str(TOTAL)+"\n")
	
	for i in range(0,TOTAL):

		jobname=str(i)+" "
		handler.write(jobname)
		
		#generate weight and arrival time
		weight=random.randint(1, MAXWEIGHT)
		arrivaltime=random.randint(0,ARRIVALMAX)
		handler.write(str(weight)+" ")
		handler.write(str(arrivaltime)+" ")

		#generate mapper, in this section, the number of mapper is fixed

		if i < jobshort:
			numMappers=random.randint(1,mid)
		else:
			numMappers=random.randint(mid,RACKS-1)

		handler.write(str(numMappers)+" ")
		rack=[0 for x in range (0,RACKS)]
		rack=fillArray(rack)
		
		for j in range(0,numMappers):
			rack,machine=selectMachineN(rack,RACKS)
			handler.write(str(machine)+" ")
		#generate reduce
		if i < jobshort:
			numReducers=random.randint(1,mid)
		else:
			numReducers=random.randint(mid,RACKS-1)
	
		handler.write(str(numReducers)+" ")
		rack=[0 for x in range (0,RACKS)]
		rack=fillArray(rack)

		for j in range(0,numReducers):
			numMB=random.randint(1,1000)
			shuffleBytes = numMB * numMappers
			rack,reduceid=selectMachineN(rack,RACKS)
			handler.write(str(reduceid)+":"+str(shuffleBytes)+" ")
		handler.write("\n")
















def generateFactor(file,mid,short):
	handler=open(file,"a+")
	jobs=[]
	sumjob=0

    
	jobshort=(int)((float)(short*TOTAL)/100.0)
	joblarge=TOTAL-jobshort
	handler.write(str(RACKS)+" "+str(TOTAL)+"\n")
	
	
	for i in range(0,TOTAL):
		print i
		jobname=str(i)+" "
		handler.write(jobname)
		
		#generate weight and arrival time
		weight=random.randint(1, MAXWEIGHT)
		arrivaltime=random.randint(0,ARRIVALMAX)
		handler.write(str(weight)+" ")
		handler.write(str(arrivaltime)+" ")

		#generate mapper, in this section, the number of mapper is fixed
		numMappers=random.randint(1,RACKS-1)
		handler.write(str(numMappers)+" ")
		rack=[0 for x in range (0,RACKS)]
		rack=fillArray(rack)
		
		for j in range(0,numMappers):
			rack,machine=selectMachine(rack)
			handler.write(str(machine)+" ")
		#generate reduce

		numReducers=random.randint(1,RACKS-1)
		handler.write(str(numReducers)+" ")
		rack=[0 for x in range (0,RACKS)]
		rack=fillArray(rack)

		for j in range(0,numReducers):
			if i < jobshort:
				numMB=random.randint(10,mid)
			else:
				numMB=random.randint(mid,1000)
			shuffleBytes = numMB * numMappers
			rack,reduceid=selectMachine(rack)
			handler.write(str(reduceid)+":"+str(shuffleBytes)+" ")
		handler.write("\n")


















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



def generateLengthVariance(file,average,variancecoflow,varianceflow):
	f=open(file,"r")
	desfile=open(str(average)+"-"+str(variancecoflow)+"-"+str(varianceflow)+".tr","a+")
	totaline=f.readlines()
	f.close()
	index=0
	rarray=numpy.random.normal(average,variancecoflow,TOTAL)
	
	for line in totaline:
		line=line.strip()
		if index==0:
			desfile.write(line+"\n")
			#get the number of jobs
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
		#set length according to the array value
		if rarray[index-1] <=0:
			rarray[index-1]=0
		lengthrange=rarray[index-1]
		flowlength=numpy.random.normal(lengthrange,varianceflow,reducenumber)
		for i in range(0,reducenumber):
			reduce= arrayline[4+mappernumber+1+i]
			reduceposition=reduce.split(':')[0]
			reducelength=(int)(reduce.split(':')[1])
			if flowlength[i] <=0:
				flowlength[i]=1
			shufflebytes=int(flowlength[i])*mappernumber
			reduces.append(reduceposition+":"+str(shufflebytes))
			desfile.write(reduceposition+":"+str(shufflebytes)+" ")
		index+=1
		desfile.write("\n")
	desfile.close()
	
	










def generateWeightSpecial(file, THRESHOLD):
	f=open(file,"r")
	desfile=open(str(THRESHOLD)+"-REAL.tr","a+")
	totaline=f.readlines()
	f.close()
	index=0
	for line in totaline:
		line=line.strip()
		if index==0:
			index+=1
			desfile.write(line+"\n")
			continue
		index+=1
		arrayline=line.split()
		jobname=arrayline[0]
		
		#generate different weight
		weight=random.randint(1,MAXWEIGHT)
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
		weight=maxlength/THRESHOLD 


		if weight <=1:
			weight=1
		elif weight >=20:
			weight=20
		else:
			weight=math.ceil(weight)

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









#generate weight for particular set of coflows
def generateWeightReal(file,jobnumber,weightSet):
	f=open(file,"r")
	desfile=open(str(jobnumber)+"-REAL.tr","a+")
	totaline=f.readlines()
	f.close()
	index=0

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
		if index % jobnumber==0:
			weight=weightSet
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













def generateWeight(file,MAXWEIGHT):
	f=open(file,"r")
	desfile=open(str(MAXWEIGHT)+"-REAL.tr","a+")
	totaline=f.readlines()
	f.close()
	index=0
	for line in totaline:
		line=line.strip()
		if index==0:
			index+=1
			desfile.write(line+"\n")
			continue
		index+=1
		arrayline=line.split()
		jobname=arrayline[0]
		desfile.write(jobname+" ")
		#generate different weight
		weight=random.randint(1,MAXWEIGHT)
		print weight
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
		reduces=[]
		for i in range(0,reducenumber):
			reduce= arrayline[4+mappernumber+1+i]
			desfile.write(reduce+" ")
		desfile.write("\n")
	desfile.close()



if __name__ == "__main__":
	for i in range(0,10):
		variance=100+500*i
		avarage=20
		generateWeightVariance("FB2010-weight0.txt",avarage,variance)
	# for i in range(0,10):
	# 	variancecoflow=100+500*i
	# 	generateLengthVariance("10-200.tr",400, variancecoflow,1000)
# 		generateLength("10.tr",10,maxlength)
	#generateWeightSpecial("FB2010-weight0.txt",200)
	#generateLengthTrace("test.txt",50,1000,[12,88])]

	# i=5
	# while i <=95:
	# 	filename=str(i)+".tr"
	# 	generateWidthFactor(filename,20,i，60)
	# 	i+=5
# 	for i in range(2,10):
# 		generateWeightReal("FB2010-weight0.txt",i,8)
	# i=10
	# while i < 60:
	# 	filename=str(i)+".tr"
	# 	generateWidthFactor(filename,i,30,60)
	# 	i+=5
	# i=5
	# while i <= 95:
	# 	print i
	# 	filename=str(i)+".tr"
	# 	generateWidth(filename,30,i)
	# 	i+=5
# 	for i in range(0,17):
# 		maxlength=200+50*i
# 		generateLength("10.tr",10,maxlength)
	# i=5
	# while i <=95:
	# 	filename=str(i)+".tr"
	# 	generateFactor(filename,300,i)
	# 	i+=5

	# i=3
	# while i <= 20:
	# 	generateWeight("FB2010-weight0.txt",i)
	# 	i+=2