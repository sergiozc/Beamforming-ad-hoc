
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
import wave

def RawToWav(file_raw):
    
    'Función que convierte datos en bruto de audio en formato wav, conociendo'
    'previamente sus parámetros'

    with open(file_raw + ".raw", "rb") as inp_f:
        data = inp_f.read()
        with wave.open(file_raw + ".wav", "wb") as out_f:
            out_f.setnchannels(1)
            out_f.setsampwidth(2)
            out_f.setframerate(44100)
            out_f.writeframesraw(data)
        
        with wave.open(file_raw + ".wav", "rb") as in_f:
            print(repr(in_f.getparams()))
            

def sincro(file0_wav, file1_wav, prueba):
    
    'Función que acorta las señales coherentemente a las marcas de tiempo'
    'obtenidas y realiza la correlación cruzada de los dos dispositivos'

    Fs0, Device0 = wavfile.read(file0_wav + '.wav')
    Device0 = Device0.astype(np.float)
    
    Fs1, Device1 = wavfile.read(file1_wav + '.wav')
    Device1 = Device1.astype(np.float)
    
        
    #Creamos el vector de tiempos
    L0 = len(Device0)
    L1 = len(Device1)
    T_0 = L0 / Fs0;
    T_1 = L1 / Fs1;
    t0 = np.linspace(0, T_0, L0)
    t1 = np.linspace(0, T_1, L1)
    
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
    plt.plot(t0, Device0)
    plt.plot(t1, Device1)
    plt.xlabel('Tiempo (s)')
    plt.ylabel('Amplitud')
    plt.title('Representación temporal de las grabaciones')
    plt.legend(['Device0', 'Device1'])
    
    #Acortamos a 1.5 segundos para captar el beep
    beep0 = Device0[:66150]
    beep1 = Device1[:66150]
    plt.figure(4)
    plt.subplot(211)
    plt.plot(beep0)
    plt.title('Pitido')
    plt.subplot(212)
    plt.plot(beep1)
    plt.xlabel('Muestras')
    plt.ylabel('Amplitud')
    
    
    #Correlación de las dos señales
    
    rx = scipy.signal.correlate(beep0, beep1, mode = 'full') #2s para que pille únicamente el pitido
    N = len(rx)
    #k = np.linspace(-N+1,N-1,2*N) 
    k = np.linspace(-(N/2) +1, (N/2)-1, N)
    
    plt.figure(5)
    plt.plot(k, rx)
    plt.title('Correlación cruzada')
    
    peak = np.where(rx == max(rx))
    delay = int(np.ceil(float(peak[0]) - N / 2.0))
    print('El delay inicial es de:', delay, 'muestras')
    
    #Buscamos donde la amplitud sea mayor que 200
    nonull_0 = np.where(np.abs(Device0) > 200)
    nonull_1 = np.where(np.abs(Device1) > 500)
    start0 = nonull_0[0][0]
    start1 = nonull_1[0][0]
    if(start0 > start1): #Empieza antes Device0
        Device0 = Device0[delay:]
        print('Device 0 empieza antes')
        L0 = len(Device0)
        T_0 = L0 / Fs0;
        t0 = np.linspace(0, T_0, L0)
    else:
        Device1 = Device1[delay:]
        print('el 0 antes')
        L0 = len(Device1)
        T_1 = L1 / Fs1;
        t1 = np.linspace(0, T_1, L1)
        
    plt.figure(6)
    plt.plot(t0, Device0)
    plt.plot(t1, Device1)
    plt.xlabel('Tiempo (s)')
    plt.ylabel('Amplitud')
    plt.title('Representación temporal de las grabaciones AJUSTADAS')
    plt.legend(['Device0', 'Device1'])
    
    
    rx2 = scipy.signal.correlate(Device0, Device1, mode = 'full') #2s para que pille únicamente el pitido
    N = len(rx2)
    #k = np.linspace(-N+1,N-1,2*N) 
    k = np.linspace(-(N/2) +1, (N/2)-1, N)
    
    plt.figure(7)
    plt.plot(k, rx2)
    plt.title('Correlación cruzada señales ajustadas')
    
    peak2 = np.where(rx2 == max(rx2))
    delay2 = int(np.ceil(float(peak2[0]) - N / 2.0))
    print('El delay final es de:', delay2, 'muestras')
    

#----------------------------------------------------------------------------------------------

plt.close('all')

sincro('Device0_1', 'Device1_1', 1)
#sincro('Device0_2', 'Device1_2', 2)
#sincro('Device0_3', 'Device1_3', 3)
#sincro('Device0_4', 'Device1_4', 4)


#RawToWav('Device0')
#RawToWav('Device1')
#RawToWav('Device2')