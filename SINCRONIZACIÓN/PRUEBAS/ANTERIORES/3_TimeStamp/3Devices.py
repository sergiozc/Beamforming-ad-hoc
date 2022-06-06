# -*- coding: utf-8 -*-
"""
Created on Mon Mar 14 10:03:19 2022

@author: Usuario
"""

import numpy as np
import scipy
import scipy.io
import matplotlib.pyplot as plt
from scipy.io import wavfile
from scipy import signal
import wave

file0_wav = 'Device0_5'
file1_wav = 'Device1_5'
file2_wav = 'Device2_5'

Fs0, Device0 = wavfile.read(file0_wav + '.wav')
Device0 = Device0.astype(np.float)
    
Fs1, Device1 = wavfile.read(file1_wav + '.wav')
Device1 = Device1.astype(np.float)

Fs2, Device2 = wavfile.read(file2_wav + '.wav')
Device2 = Device2.astype(np.float)

#Creamos el vector de tiempos
L0 = len(Device0)
L1 = len(Device1)
L2 = len(Device2)
T_0 = L0 / Fs0;
T_1 = L1 / Fs1;
T_2 = L2 / Fs2;
t0 = np.linspace(0, T_0, L0)
t1 = np.linspace(0, T_1, L1)
t2 = np.linspace(0, T_2, L2)
    
plt.figure(1)
plt.plot(t0, Device0)
plt.xlabel('Tiempo (s)')
plt.ylabel('Amplitud')
plt.title('Device 0')
plt.figure(2)
plt.plot(t1, Device1)
plt.xlabel('Tiempo (s)')
plt.ylabel('Amplitud')
plt.title('Device 1')
plt.figure(3)
plt.plot(t2, Device2)
plt.xlabel('Tiempo (s)')
plt.ylabel('Amplitud')
plt.title('Device 2')

    
plt.figure(4)
plt.plot(t0, Device0)
plt.plot(t1, Device1)
plt.plot(t2, Device2)
plt.xlabel('Tiempo (s)')
plt.ylabel('Amplitud')
plt.title('Representación temporal de las grabaciones')
plt.legend(['Device0', 'Device1', 'Device2'])