#encoding:utf-8
import numpy as np
import matplotlib.pyplot as plt
import random
import math
import numpy
import sys
from mpl_toolkits.axes_grid.axislines import SubplotZero
import matplotlib 
font = {'family' : 'normal', 'weight' : 'bold', 'size' : 15 }

matplotlib.rc('font', **font)




def frac(v,x):
	n=0
	for i in v:
		if i<x:
			n=n+1
	return float(n)/float(len(v))

foffline = open("offline")
line = foffline.readline()
offlineresult=[]
onlineresult=[]    

while line:
	t=line.strip()
	words=t.split(' ')
	offlineresult.append(float(words[2])*float(words[10])/1000)
	line = foffline.readline()
foffline.close()



fonline = open("online")
line = fonline.readline()
onlineresult=[]
while line:
	t=line.strip()
	words=t.split(' ')
	onlineresult.append(float(words[2])*float(words[10])/1000)
	line = fonline.readline()
fonline.close()


fig, ax = plt.subplots()

x = np.linspace(0, 5000, 100)
ax.spines['right'].set_color('none')
ax.spines['top'].set_color('none')

ax.xaxis.set_ticks_position('bottom')
ax.spines['bottom'].set_position(('data',0)) # set position of x spine to x=0

ax.yaxis.set_ticks_position('left')
ax.spines['left'].set_position(('data',2500))   # set position of y spine to y=0

offlinefactor=[]
onlinefactor=[]

for v in x:
	offlinefactor.append(frac(offlineresult,v))
	onlinefactor.append(frac(onlineresult,v))

ax.plot(x, offlinefactor,linewidth=2,label='2-approximate-algorithm')
ax.plot(x, onlinefactor,linewidth=2,label='online-algorithm')
ax.legend(loc='lower right')
plt.ylabel('fraction')
plt.xlabel('weight completion time(s)')
fig
plt.show()
fig.savefig("online_offline.pdf")

# x = np.linspace(0, 5000000, 10000)
# offlinefactor=[]
# onlinefactor=[]

# for v in x:
# 	offlinefactor.append(frac(offlineresult,v))
# 	onlinefactor.append(frac(onlineresult,v))

# fig, ax = plt.subplots()
# line1, = ax.plot(x, offlinefactor, '--', linewidth=2,
#                  label='Dashes set retroactively')

# line2, = ax.plot(x, onlinefactor, dashes=[30, 5, 10, 5],
#                  label='Dashes set proactively')

# ax.legend(loc='lower right')
# plt.show()
