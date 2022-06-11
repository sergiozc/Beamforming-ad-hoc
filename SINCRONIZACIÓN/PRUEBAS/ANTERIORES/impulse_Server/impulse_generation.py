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

impulso = np.zeros(50000)
for i in range(10):
    impulso[i + 20000] = np.iinfo(np.int16).max

plt.figure(1)
plt.plot(impulso)
plt.title('Impulso')
plt.xlabel('Muestras')
plt.ylabel('Amplitud')

file_name1 = 'impulso'
wavfile.write('impulso/' + file_name1 + '.wav', Fs, impulso.astype('int16'))

def verImpulso(file):
    Fs, imp = wavfile.read(file + '.wav')
    
    L = len(imp)
    T = L / Fs;
    t0 = np.linspace(0, T, L)
    
    plt.figure(2)
    plt.plot(t0, imp)
    plt.xlabel('tiempo (s)')
    plt.ylabel('Amplitud')