# -*- coding: utf-8 -*-
"""
Created on Sun Mar 20 17:37:35 2022

@author: Usuario
"""

import numpy as np
import scipy
import scipy.io
import matplotlib.pyplot as plt
from scipy.io import wavfile
from scipy import signal
import wave

Fs, beep0 = wavfile.read('beep.wav')
beep0 = beep0.astype(np.float)
 
beep1 = beep0[:13230]
file_name1 = 'beep1'
wavfile.write(file_name1 + '.wav', Fs, beep1.astype('int16'))
