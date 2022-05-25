# -*- coding: utf-8 -*-
"""
Created on Fri Apr  1 17:43:20 2022

@author: Sergio
"""

import numpy as np
import scipy
import scipy.io
import matplotlib.pyplot as plt
from scipy.io import wavfile
from scipy import signal
import wave

plt.close('all')

Fs = 44100
#Longitud de la señal (2s)
len_tren = 88200
tren = np.zeros(len_tren)
#Longitud en muestras de cada impulso
len_impulso = 10
#Número de impulsos
Nimpulsos = 3
#margen de muestras para que suene el impulso correctamente
margen = 2000 

intervalo = int((len_tren - margen) / Nimpulsos)
pos_ini = 10000

for i in range(3):
    pos = pos_ini + intervalo*i
    for j in range(10):
        tren[pos + j] = np.iinfo(np.int16).max

plt.figure(1)
plt.plot(tren)
plt.title('Tren de impulsos')
plt.xlabel('Muestras')
plt.ylabel('Amplitud')

file_name1 = 'tren_impulsos'
wavfile.write('tren/' + file_name1 + '.wav', Fs, tren.astype('int16'))

def verImpulso(file):
    Fs, imp = wavfile.read(file + '.wav')
    
    L = len(imp)
    T = L / Fs;
    t0 = np.linspace(0, T, L)
    
    plt.figure(2)
    plt.plot(t0, imp)
    plt.xlabel('tiempo (s)')
    plt.ylabel('Amplitud')