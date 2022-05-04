# -*- coding: utf-8 -*-
"""
Created on Thu Mar 24 09:34:01 2022

@author: Usuario
"""

import numpy as np
import scipy
import scipy.io
import matplotlib.pyplot as plt
from scipy.io import wavfile
from scipy import signal
import wave

plt.close('all')

f = 2000
Fs = 44100
Ts = 1 / Fs
t = np.arange(0., 0.65, Ts)


amplitude = np.iinfo(np.int16).max
beep = amplitude * np.sin(2. * np.pi * f * t)
plt.figure(1)
plt.plot(beep)

file_name1 = 'Beep_mod'
wavfile.write('elBeep/' + file_name1 + '.wav', 44100, beep.astype('int16'))
