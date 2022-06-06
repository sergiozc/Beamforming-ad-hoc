# -*- coding: utf-8 -*-
"""
Created on Sun Mar  6 11:20:55 2022

@author: Sergio
"""

import numpy as np
import scipy
import scipy.io
import matplotlib.pyplot as plt
from scipy.io import wavfile
from scipy import signal

plt.close('all')

Fs0, Device0_1 = wavfile.read('Device0_1.wav')
Device0_1 = Device0_1.astype(np.float)

Fs1, Device1_1 = wavfile.read('Device1_1.wav')
Device1_1 = Device1_1.astype(np.float)

#Creamos el vector de tiempos
L0 = len(Device0_1)
L1 = len(Device1_1)
T_0 = L0 / Fs0; #2.98667 segundos
T_1 = L1 / Fs1; #3.23048 segundos
t0 = np.linspace(0, T_0, L0)
t1 = np.linspace(0, T_1, L1)


plt.figure(1)
plt.plot(t0, Device0_1)
plt.plot(t1, Device1_1)
plt.xlabel('Tiempo (s)')
plt.ylabel('Amplitud')
plt.title('Representación temporal de las grabaciones')
plt.legend(['Device0', 'Device1'])

#%%
#En estas grabaciones en específico, la diferencia de marcas de tiempo cuando empiezan a grabar es
#de 177 ms. La diferencia de tiempo cuando paran de grabar es de 404 ms.
#En muestras esto se traduce como nmuestras = Fs * t ==> 7805.7 muestras en el comienzo y unas
#17816.4 al final de la grabación

#El device0 empieza antes a grabar por lo que acortamos al principio. El device 1 acaba más tarde
#de grabar, por lo que acortamos al final (len(Device1_1) - 17817 muestras = 124647)
cut1 = Device0_1[7806:]
cut2 = Device1_1[:124647]

cut1_len = len(cut1);
cut1_T = cut1_len / Fs1;
cut1_t = np.linspace(0, cut1_T, cut1_len)

cut2_len = len(cut2);
cut2_T = cut2_len / Fs1;
cut2_t = np.linspace(0, cut2_T, cut2_len)

plt.figure(2)
plt.plot(cut1_t, cut1)
plt.plot(cut2_t, cut2)
plt.xlabel('Tiempo (s)')
plt.ylabel('Amplitud')
plt.title('Representación temporal AJUSTADA')
plt.legend(['Device0', 'Device1'])

#%%Correlación de las dos señales

rx = scipy.signal.correlate(cut1, cut2, mode = 'full')

N = len(rx)
#k = np.linspace(-N+1,N-1,2*N) 
k = np.linspace(-(N/2) +1, (N/2)-1, N)

plt.figure(3)
plt.plot(k, rx)
plt.title('Correlación cruzada')


