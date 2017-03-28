# encoding: utf-8
import numpy as np
import matplotlib.pyplot as plt
import sys
from scipy import stats

from matplotlib import rcParams
rcParams.update({'font.size': 18,'font.weight':'bold'})

patterns = ('/','//','-', '+', 'x', '\\', '\\\\', '*', 'o', 'O', '.')

##first read from file
fairpath="FAIR"
sebfpath="SEBF"
fifopath="FIFO"
weightpath="WEIGHT"

SHORT=5*1024*1024



def getAverage(arralylist):
	return np.mean(arralylist)


def getRange(arraylist,element):
	return stats.percentileofscore(arraylist, element)


def getElements(arraylist,percentage):
	result=[]
	for element in arraylist:
		pos=getRange(arraylist,element)
		if pos <= percentage:
			result.append(element)
	return result



def getPercentile(arraylist,percentage):
	a=np.array(arraylist)
	p=np.percentile(a,percentage)
	return p



def getPercentageResult(path,percentage):
	f=open(path,"r")
	totaline=f.readlines()
	bin1=[]
	bin2=[]
	wc1=0
	wc2=0
	wc3=0
	wc4=0
	wc=0
	for line in totaline:
		if line[0]=='J':
			arrayline=line.split()
			#analyze job 
			jobname=arrayline[0]
			starttime=float(arrayline[1])
			finishtime=float(arrayline[2])
			mappers=int(arrayline[3])
			reducers=int(arrayline[4])
			totalshuffle=float(arrayline[5])
			maxshuffle=float(arrayline[6])
			duration=float(arrayline[7])
			deadlineduration=float(arrayline[8])
			shufflesum=float(arrayline[9])
			weight=float(arrayline[10])


			if maxshuffle < SHORT:
				bin1.append(weight*duration)
			else:
				bin2.append(weight*duration)
	#now get the percentage result
	bin1=getElements(bin1,percentage)
	bin2=getElements(bin2,percentage)

	wc1=0
	wc2=0
	wc=0
	for e in bin1:
		wc1+=e
	for e in bin2:
		wc2+=e

	wc=wc1+wc2
	return wc1,wc2,wc

	




def getResult(path):
		f=open(path,"r")
		totaline=f.readlines()
		wctotal=0
		wcweight=0
		wcnonweight=0
		for line in totaline:
			if line[0]=='J':
				arrayline=line.split()
				#analyze job 
				jobname=arrayline[0]
				starttime=float(arrayline[1])
				finishtime=float(arrayline[2])
				mappers=int(arrayline[3])
				reducers=int(arrayline[4])
				totalshuffle=float(arrayline[5])
				maxshuffle=float(arrayline[6])
				duration=float(arrayline[7])
				deadlineduration=float(arrayline[8])
				shufflesum=float(arrayline[9])
				weight=float(arrayline[10])

				if weight >=2:
					wcweight+=duration

				wctotal+=weight*duration
				wcnonweight+=duration

		return wcweight,wctotal,wcnonweight

	


if __name__=='__main__':
	sebfactor=[]
	wf=[]
	i=2
	
	fifolm=[]
	vary=[]
	yo=[]
	pwf=[]
	while i<= 9:
		fifo=fifopath+"/"+str(i)+".rt"
		sebf=sebfpath+"/"+str(i)+".rt"
		weight=weightpath+"/"+str(i)+".rt"


		fwc1,fwc2,fwc=getResult(fifo)

		sebfwc1,sebfwc2,sebfwc=getResult(sebf)
		wc1,wc2,wc=getResult(weight)

		fifolm.append([fwc1,fwc2,fwc])
		vary.append([sebfwc1,sebfwc2,sebfwc])
		yo.append([wc1,wc2,wc])

		#wf.append(fwc/wc)
		#pwf.append(pfwc/pwc)
		#wf.append(fwc/wc)
		i+=1


	sebfresult=[]
	sebfshort=[]
	sebflarge=[]
	wfresult=[]
	pwfresult=[]
	wfshort=[]
	wflarge=[]
	fiforesult=[]
	fifoshort=[]
	fifolarge=[]
	varyresult=[]
	varyshort=[]
	varylarge=[]
	yoresult=[]
	yoshort=[]
	yolarge=[]


	j=0
	while j <=7:
		fiforesult.append(fifolm[j][2])
		fifoshort.append(fifolm[j][0])
		fifolarge.append(fifolm[j][1])
		varyresult.append(vary[j][2])
		varyshort.append(vary[j][0])
		varylarge.append(vary[j][1])
		yoresult.append(yo[j][2])
		yoshort.append(yo[j][0])
		yolarge.append(yo[j][1])

		j+=1


	N=8
	ind = np.arange(N)  # the x locations for the groups
	width = 0.2       # the width of the bars
	fig, ax = plt.subplots(dpi=1000)
	rects1 = ax.bar(ind, fifoshort, width, hatch="//",color='red',ecolor='k')
	rects2 = ax.bar(ind+width, varyshort, width, hatch='-',color='b',ecolor='k')
	rects3 = ax.bar(ind+2*width, yoshort, width, hatch='-',color='k',ecolor='k')
	ax.set_xticks(ind+width)
	ax.set_xticklabels(('200','300','400','500','600','700','800'))
	ax.legend((rects1[0],rects2[0],rects3[0]), ('Baraat','Vary','Yosemite'),loc=0)
	ax.set_ylabel('weight completion time (ms)')
	ax.set_xlabel('max length of coflows(MB)')
	ax.set_ylim([0,1e7])
	fig.savefig("total_completion_time.eps",dpi=1000)



	N=8
	ind = np.arange(N)  # the x locations for the groups
	width = 0.2       # the width of the bars
	fig, ax = plt.subplots(dpi=1000)
	rects1 = ax.bar(ind, fifolarge, width, hatch="//",color='red',ecolor='k')
	rects2 = ax.bar(ind+width, varylarge, width, hatch='-',color='b',ecolor='k')
	rects3 = ax.bar(ind+2*width, yolarge, width, hatch='-',color='k',ecolor='k')
	ax.set_xticks(ind+width)
	ax.set_xticklabels(('200','300','400','500','600','700','800'))
	ax.legend((rects1[0],rects2[0],rects3[0]), ('Baraat','Vary','Yosemite'),loc=0)
	ax.set_ylabel('weight completion time (ms)')
	ax.set_xlabel('max length of coflows(MB)')
	#ax.set_ylim([0,3])
	fig.savefig("2.eps",dpi=1000)





	N=8
	ind = np.arange(N)  # the x locations for the groups
	width = 0.2       # the width of the bars
	fig, ax = plt.subplots(dpi=1000)
	rects1 = ax.bar(ind, fiforesult, width, hatch="//",color='red',ecolor='k')
	rects2 = ax.bar(ind+width, varyresult, width, hatch='-',color='b',ecolor='k')
	rects3 = ax.bar(ind+2*width, yoresult, width, hatch='-',color='k',ecolor='k')
	ax.set_xticks(ind+width)
	ax.set_xticklabels(('200','300','400','500','600','700','800'))
	ax.legend((rects1[0],rects2[0],rects3[0]), ('Baraat','Vary','Yosemite'),loc=0)
	ax.set_ylabel('weight completion time (ms)')
	ax.set_xlabel('max length of coflows(MB)')
	ax.set_ylim([0,1e8])
	fig.savefig("3.eps",dpi=1000)





