# -*- coding: utf-8 -*-
"""
Created on Sat Jun 11 11:09:29 2022

@author: Sergio
"""

import numpy as np
import scipy
import scipy.io
import matplotlib.pyplot as plt
from scipy.io import wavfile
from scipy import signal
import wave
import sincronizacion_Ndevices as sincro


plt.close('all')

sincronizadas = sincro.sincro(3)


