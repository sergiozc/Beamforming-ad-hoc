
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
    
    'Explicación offset de la prueba 1 (en todas las pruebas he realizado el mismo procedimiento'
    #En estas grabaciones en específico, la diferencia de marcas de tiempo cuando empiezan a grabar es
    #de 177 ms. La diferencia de tiempo cuando paran de grabar es de 404 ms.
    #En muestras esto se traduce como nmuestras = Fs * t ==> 7805.7 muestras en el comienzo y unas
    #17816.4 al final de la grabación
    #El device0 empieza antes a grabar por lo que acortamos al principio. El device 1 acaba más tarde
    #de grabar, por lo que acortamos al final (len(Device1_1) - 17817 muestras = 124647)
    
    #Acortamos las grabaciones con el criterio de las marcas temporales en cada caso
    
    # if (prueba == 1):
    #     offset_inicial = 8423
    #     offset_final = 20242
    #     cut2 = Device1[offset_inicial:]
    #     cut1 = Device0[:(len(Device0) - offset_final)]
    # elif (prueba == 2):
    #     offset_inicial = 5777
    #     offset_final = 9570
    #     cut2 = Device1
    #     cut1 = Device0[offset_inicial:(len(Device0) - offset_final)]
    # elif (prueba == 3):
    #     offset_inicial = 3087
    #     offset_final = 14906
    #     cut1 = Device0[offset_inicial:]
    #     cut2 = Device1
    # elif (prueba == 4):
    #     offset_inicial = 8688
    #     offset_final = 29106
    #     cut1 = Device0[offset_inicial:]
    #     cut2 = Device1[:(len(Device1) - offset_final)]

        
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
    

    
    #Correlación de las dos señales
    
    rx = scipy.signal.correlate(Device0[:88200], Device1[:88200], mode = 'full') #2s para que pille únicamente el pitido
    N = len(rx)
    #k = np.linspace(-N+1,N-1,2*N) 
    k = np.linspace(-(N/2) +1, (N/2)-1, N)
    
    plt.figure(4)
    plt.plot(k, rx)
    plt.title('Correlación cruzada')
    
    peak = np.where(rx == max(rx))
    delay = int(np.ceil(float(peak[0]) - N / 2.0))
    print('El delay es de:', delay, 'muestras')
    
    #Buscamos donde la amplitud sea mayor que 200
    nonull_0 = np.where(np.abs(Device0) > 200)
    nonull_1 = np.where(np.abs(Device1) > 200)
    start0 = nonull_0[0][0]
    start1 = nonull_1[0][0]
    if(start0 > start1): #Empieza antes Device1
        Device1 = Device1[delay:]
        L1 = len(Device1)
        T_1 = L1 / Fs1;
        t1 = np.linspace(0, T_1, L1)
    else:
        Device0 = Device0[delay:]
        L0 = len(Device0)
        T_0 = L0 / Fs0;
        t0 = np.linspace(0, T_0, L0)
        
    plt.figure(5)
    plt.plot(t0, Device0)
    plt.plot(t1, Device1)
    plt.xlabel('Tiempo (s)')
    plt.ylabel('Amplitud')
    plt.title('Representación temporal de las grabaciones AJUSTADAS')
    plt.legend(['Device0', 'Device1'])
    

    
    

#----------------------------------------------------------------------------------------------

plt.close('all')

#sincro('Device0_1', 'Device1_1', 1)
#sincro('Device0_2', 'Device1_2', 2)
sincro('Device0_3', 'Device1_3', 3)
#sincro('Device0_4', 'Device1_4', 4)


#RawToWav('Device0')
#RawToWav('Device1')
#RawToWav('Device2')