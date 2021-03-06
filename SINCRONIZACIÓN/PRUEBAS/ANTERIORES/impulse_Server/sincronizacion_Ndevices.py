# -*- coding: utf-8 -*-
"""
Created on Wed Apr  6 17:05:12 2022

@author: Usuario
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
            

def timeVector(signal, Fs):
    
    'Función que devuelve el vector de tiempos dada una señal y su frecuecia'
    'de muestreo'
    
    L = len(signal)
    T = L / Fs
    t = np.linspace(0, T, L)
    
    return t


def acorta_senial(Device0, Device1, delay):
    
    'Función que devuelve dos señales ajustadas inicialmente con un retardo'
    'calculado y recortadas posteriormente según el número de muestras que tenga'
    'cada señal'
    
    #Buscamos donde la amplitud sea mayor que 500, donde empieza el impulso
    nonull_0 = np.where(np.abs(Device0) > 500)
    nonull_1 = np.where(np.abs(Device1) > 500)
    start0 = nonull_0[0][0] #Comienzo para el Device0
    start1 = nonull_1[0][0] #Comienzo para el Device1
    
    if(start0 > start1): #Empieza antes Device0
        Device0 = Device0[delay:]
        print('Device 0 empieza antes')
    else:
        Device1 = Device1[delay:]
        print('el 0 antes')
    
    #Ahora acortamos la señal que sea más larga
    if(len(Device0) > len(Device1)):
        Device0 = Device0[:len(Device1)]
    elif(len(Device1) > len(Device0)):
        Device1 = Device1[:len(Device0)]
        
    return Device0, Device1
            


def correlaMax(rx, N):
    
    'Función que devuelve el pico de la correlación de dos señales. Si tiene'
    'decimales, redondea este máximo hacia arriba'
    
    peak = np.where(rx == max(rx))
   # print(peak[0])
    delay = int(np.ceil(float(peak[0]) - N / 2.0))
    return delay

##################################################################################

#def sincro(file0_wav, file1_wav, prueba):
def sincro(Ndevices):
    
    'Función que dadas dos grabaciones, las sincroniza coherentemente'

    
    record = np.zeros((661500, Ndevices)) #Matriz de las grabaciones
    tam = np.arange(Ndevices)
    tam_postdelay = np.arange(Ndevices) #Tamaño después de acortar con el delay inicial
    lim_beep = 44100 * 3 #Acortamos a 3 segundos para captar el impulso
    impulso = np.zeros((lim_beep, Ndevices)) #Matriz donde guardamos el impulso de cada señal
    start = np.zeros(Ndevices) #Comienzo del impulso de cada grabación
    delay = np.arange(Ndevices) #Vector de restraso de una señal respecto a la primera en comenzar
    delay_final = np.arange(Ndevices)
    
    for i in range(Ndevices):
        #Guardamos en volatil el contenido de la grabación i
        Fs, volatil = wavfile.read('Device'+ str(i) + '.wav')
        #Guardamos el número de muestras de la grabación i
        tam[i] = len(volatil)
        #Guardamos en cada columna de 'record' las grabaciones (hasta la fila correspondiente
        #al número de muestras de cada grabación)
        record[:tam[i], i] = volatil
        #Guardamos únicamente la señal impulsiva
        impulso[:, i] = volatil[:lim_beep]
        #Calculamos la muestra de comienzo para cada grabación (donde la amplitud es >500)
        nonull = np.where(np.abs(impulso[:,i]) > 500)
        start[i] = nonull[0][0]
        
        # plt.figure(i+1)
        # plt.plot(impulso[:,i])
        # plt.title('Pitido')
        # plt.xlabel('Muestras')
        # plt.ylabel('Amplitud')
        
    
    #Cuál es la grabación que comienza antes
    primera = np.argmin(start)
    
    for j in range(Ndevices):
        #Hacemos la correlación cruzada de la señal que comienza antes con todas las demás
        rx_impulso = scipy.signal.correlate(impulso[:,j], impulso[:,primera], mode = 'full')
        N = len(rx_impulso)
        #Calculamos el delay inicial para todas las señales
        delay[j] = correlaMax(rx_impulso, N)
        print('El delay inicial es de:', delay[j], 'muestras')
        #Tamaño de las señales después de compensar el delay
        tam_postdelay[j] = tam[j] - delay[j]
    
    #Tamaño de las señales después de aplicar el delay y ajustándose a la señal con menos muestras
    tam_final = min(tam_postdelay)
    #En esta matriz se guardarán las señales sincronizadas
    sincronizadas = np.zeros((tam_final, Ndevices))
    
    plt.figure(1)
    plt.title('Señales sincronizadas')
    plt.xlabel('Muestras')
    plt.ylabel('Amplitud')
    
    #Guardamos las señales en la nueva matriz
    for z in range(Ndevices):
        lim_final = tam_final + delay[z]
        sincronizadas[:, z] = record[delay[z]:lim_final, z]
        plt.plot(sincronizadas[:,z])
     
    #Hacemos la correlación de las señales
    for z in range(Ndevices):
        rx_sincro = scipy.signal.correlate(sincronizadas[:,z], sincronizadas[:,primera], mode = 'full')
        N = len(rx_sincro)
        k = np.linspace(-(N/2) +1, (N/2)-1, N)
    
        plt.figure(z+2)
        plt.plot(k, rx_sincro)
        plt.title('Correlación cruzada señales ajustadas')
        # Calculamos el delay final para todas las señales
        delay_final[z] = correlaMax(rx_sincro, N)
        print('El delay final es de:', delay_final[z], 'muestras')
        


##############################################################################################


plt.close('all')


#sincro(2)
sincro(3)


# RawToWav('Device0')
# RawToWav('Device1')
# RawToWav('Device2')