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
NARROW=50



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




def getResult(path):
		f=open(path,"r")
		totaline=f.readlines()
		bin1=0
		bin2=0
		bin3=0
		bin4=0
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
				width=mappers
				if mappers < reducers:
					width=reducers
				else:
					width=mappers
				if maxshuffle < SHORT and width < NARROW:
					wc1+=weight*duration
					bin1+=1
				elif maxshuffle >= SHORT and width < NARROW:
					wc2+=weight*duration
					bin2+=1
				elif maxshuffle < SHORT and width > NARROW:
					wc3+=weight*duration
					bin3+=1
				else:
					wc4+=weight*duration
					bin4+=1
		wc=wc1+wc2+wc3+wc4
		return wc1,wc2,wc3,wc4,wc

	


if __name__=='__main__':
	sebfactor1=[]
	sebfactor2=[]
	sebfactor3=[]
	sebfactor4=[]
	sebfactor=[]
	wf1=[]
	wf2=[]
	wf3=[]
	wf4=[]
	wf=[]
	wf1=[]
	wf2=[]
	wf3=[]
	wf4=[]
	wf=[]

	for i in range(0,8):
		fifo=fifopath+"/"+str(i)+".rt"
		sebf=sebfpath+"/"+str(i)+".rt"
		weight=weightpath+"/"+str(i)+".rt"
		
		fwc1,fwc2,fwc3,fwc4,fwc=getResult(fifo)
		sebfwc1,sebfwc2,sebfwc3,sebfwc4,sebfwc=getResult(sebf)
		wc1,wc2,wc3,wc4,wc=getResult(weight)
		sebfactor1.append(fwc1/sebfwc1)
		sebfactor2.append(fwc2/sebfwc2)
		sebfactor3.append(fwc3/sebfwc3)
		sebfactor4.append(fwc4/sebfwc4)
		sebfactor.append(fwc/sebfwc)
		wf1.append(fwc1/wc1)
		wf2.append(fwc2/wc2)
		wf3.append(fwc3/wc3)
		wf4.append(fwc4/wc4)
		wf.append(fwc/wc)

	# wf_average=[getAverage(wf),getAverage(wf1),getAverage(wf2),getAverage(wf3),getAverage(wf4)]
	# wf_upper=[max(wf)-getAverage(wf),max(wf1)-getAverage(wf1),max(wf2)-getAverage(wf2),max(wf3)-getAverage(wf3),max(wf4)-getAverage(wf4)]
	# wf_down=[getAverage(wf)-min(wf),getAverage(wf1)-min(wf1),getAverage(wf2)-min(wf2),getAverage(wf3)-min(wf3),getAverage(wf4)-min(wf4)]

	# sf_average=[getAverage(sebfactor),getAverage(sebfactor1),getAverage(sebfactor2),getAverage(sebfactor3),getAverage(sebfactor4)]
	# sf_upper=[max(sebfactor)-getAverage(sebfactor),max(sebfactor1)-getAverage(sebfactor1),max(sebfactor2)-getAverage(sebfactor2),max(sebfactor3)-getAverage(sebfactor3),max(sebfactor4)-getAverage(sebfactor4)]		
	# sf_down=[getAverage(sebfactor)-min(sebfactor),getAverage(sebfactor1)-min(sebfactor1),getAverage(sebfactor2)-min(sebfactor2),getAverage(sebfactor3)-min(sebfactor3),getAverage(sebfactor4)-min(sebfactor4)]
	N=8
	ind = np.arange(N)  # the x locations for the groups
	width = 0.2       # the width of the bars
	fig, ax = plt.subplots(dpi=1000)
	rects1 = ax.bar(ind, sebfactor3, width, hatch="//",color='red',ecolor='k')
	rects2 = ax.bar(ind+width, wf3, width, hatch='-',color='k',ecolor='k')
	ax.set_xticks(ind+width)
	ax.set_xticklabels(('0','1','2','3','4','5','6','7'))
	ax.legend((rects1[0],rects2[0]), ('Vary','Yosemite'),loc=2)
	ax.set_ylabel('improvement over fifo allocation')
	ax.set_ylim([0,1000])
	fig.savefig("3.eps",dpi=1000)


		# for j in range(0,8):
		# 	fifo=fifopath+"/"+str(i)+"-"+str(j)+".rt"
		# 	fwc1,fwc2,fwc3,fwc4,fwc=getResult(fifo)
		# 	sebf=sebfpath+"/"+str(i)+"-"+str(j)+".rt"
		# 	sebfwc1,sebfwc2,sebfwc3,sebfwc4,sebfwc=getResult(sebf)
		# 	weight=weightpath+"/"+str(i)+"-"+str(j)+".rt"
		# 	wc1,wc2,wc3,wc4,wc=getResult(weight)
		# 	sebfactor1.append(fwc1/sebfwc1)
		# 	sebfactor2.append(fwc2/sebfwc2)
		# 	sebfactor3.append(fwc3/sebfwc3)
		# 	sebfactor4.append(fwc4/sebfwc4)
		# 	sebfactor.append(fwc/sebfwc)
		# 	wf1.append(fwc1/wc1)
		# 	wf2.append(fwc2/wc2)
		# 	wf3.append(fwc3/wc3)
		# 	wf4.append(fwc4/wc4)
		# 	wf.append(fwc/wc)
		# wf95th=getElements(wf,95)
		# wf195th=getElements(wf1,95)
		# wf295th=getElements(wf2,95)
		# wf395yh=getElements(wf3,95)
		# wf495th=getElements(wf4,95)

		# sf95th=getElements(sebfactor,95)
		# sf195th=getElements(sebfactor1,95)
		# sf295th=getElements(sebfactor2,95)
		# sf395th=getElements(sebfactor3,95)
		# sf495th=getElements(sebfactor4,95)

		# wf_average=[getAverage(wf),getAverage(wf1),getAverage(wf2),getAverage(wf3),getAverage(wf4)]
		# wf_upper=[max(wf)-getAverage(wf),max(wf1)-getAverage(wf1),max(wf2)-getAverage(wf2),max(wf3)-getAverage(wf3),max(wf4)-getAverage(wf4)]
		# wf_down=[getAverage(wf)-min(wf),getAverage(wf1)-min(wf1),getAverage(wf2)-min(wf2),getAverage(wf3)-min(wf3),getAverage(wf4)-min(wf4)]

		# sf_average=[getAverage(sebfactor),getAverage(sebfactor1),getAverage(sebfactor2),getAverage(sebfactor3),getAverage(sebfactor4)]
		# sf_upper=[max(sebfactor)-getAverage(sebfactor),max(sebfactor1)-getAverage(sebfactor1),max(sebfactor2)-getAverage(sebfactor2),max(sebfactor3)-getAverage(sebfactor3),max(sebfactor4)-getAverage(sebfactor4)]		
		# sf_down=[getAverage(sebfactor)-min(sebfactor),getAverage(sebfactor1)-min(sebfactor1),getAverage(sebfactor2)-min(sebfactor2),getAverage(sebfactor3)-min(sebfactor3),getAverage(sebfactor4)-min(sebfactor4)]

		# N=5
		# ind = np.arange(N)  # the x locations for the groups
		# width = 0.2       # the width of the bars
		# fig, ax = plt.subplots(dpi=1000)
		# rects1 = ax.bar(ind, sf_average, width, hatch="//",color='red',ecolor='k')
		# rects2 = ax.bar(ind+width, wf_average, width, hatch='-',color='k',ecolor='k')
		# ax.set_xticks(ind+width)
		# #ax.set_xticklabels(('ALL'))
		# ax.legend((rects1[0],rects2[0]), ('Vary','Yosemite'),loc=2)
		# ax.set_ylabel('improvement over fifo allocation')
		# ax.set_ylim([0,200])
		# fig.savefig(str(i)+".eps",dpi=1000)

