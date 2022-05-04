# -*- coding: utf-8 -*-
"""
Created on Sun Apr 24 09:05:35 2022

@author: Usuario
"""

import numpy as np
import scipy
import scipy.io
import matplotlib.pyplot as plt
from scipy.io import wavfile
from scipy import signal
from scipy.optimize import fsolve
from scipy.optimize import least_squares
import wave

def correlaMax(rx, N):
    
    'Función que devuelve el pico de la correlación de dos señales. Si tiene'
    'decimales, redondea este máximo hacia arriba'
    
    peak = np.where(rx == max(rx))
   # print(peak[0])
    delay = int(np.ceil(float(peak[0]) - N / 2.0))
    return delay

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
            
    





            
RawToWav('Device0')
RawToWav('Device1')
RawToWav('Device2')

plt.close('all')
            
#def sincro_chirp(Ndevices):
Ndevices = 3
record = np.zeros((661500, Ndevices)) #Matriz de las grabaciones    
tam = np.arange(Ndevices) #Tamaño de cada grabación
tam_postdelay = np.arange(Ndevices) #Tamaño después de acortar con el delay inicial
correla = np.arange(Ndevices)
lim_exc = 44100 * 4 #Acortamos a 3 segundos para captar el impulso
Lexc = 4410 #Longitud en muestras del chirp
exc = np.zeros((lim_exc, Ndevices)) #Matriz donde guardamos el impulso de cada señal
ntoa = np.zeros((Ndevices, Ndevices)) #Tiempo de llegada de los chirp para cada móvil (en muestras)
toamed = np.zeros((Ndevices, Ndevices))
tdoamed = np.zeros((Ndevices, Ndevices, Ndevices))
tf_est = np.zeros((Ndevices, Ndevices)) #Tiempos de vuelo
tcest = np.zeros((Ndevices)) #Tiempos de comienzo
delay_final = np.arange(Ndevices)
mu = 0.001    # Factor de convergencia
RelInc = 0.01 # Criterio de convergencia

#Chirp original de 0.1s de duración    
Fs, chirp = wavfile.read('chirp' + '.wav')
    
for i in range(Ndevices):
    #Guardamos en volatil el contenido de la grabación i
    Fs, volatil = wavfile.read('Device'+ str(i) + '.wav')
    #Guardamos el número de muestras de la grabación i
    tam[i] = len(volatil)
    #Guardamos la parte de la excitación (los 3 pitidos únicamente)
    exc[:, i] = volatil[:lim_exc]
    #Guardamos en cada columna de 'record' las grabaciones (hasta la fila correspondiente
    #al número de muestras de cada grabación, el resto son ceros)
    record[:tam[i], i] = volatil
    
    #MEDIMOS LOS TOAs haciendo la correlación de cada señal con el chirp original
    #para ver en qué instante comienza cada pitido
    correla = scipy.signal.correlate(exc[:,i], chirp, mode = 'full')
    correla = correla[Lexc:]
    
    plt.figure(i+1)
    plt.subplot(211)
    plt.plot(volatil[:lim_exc])
    plt.subplot(212)
    plt.plot(correla)
    
    
    #Nos quedamos con los índices de los valores de la correlación ordenados
    #de mayor a menor, es decir, el primer índice o instante se corresponde con el índice
    #del máximo valor de la correlación
    indices = np.argsort(correla) #Se ordena de menor a mayor
    indices = indices[::-1] #Ordenado de mayor a menor
    ntoa[i, 0] = indices[0] #El valor máximo de la correlación se corresponde con un TOA
    nfound = 1 #Número de TOAs encontrados
    nsearch = 1 #Por donde empieza la búsqueda
    
    #Iremos comparando valor por valor, en los máximos.
    #Si la distancia es mayor que Lexc, se cumple la condición y se incrementa el contador.
    #Al incrementarse el contador, se incorpora el valor a los toas.
    #Por último se ordenan por orden de llegada
    while nfound < Ndevices:
        cont = 0
        for n in range(nfound):
            cond = np.abs(indices[nsearch] - ntoa[i, n]) >= Lexc #Booleana
            cont = cont + cond
        if cont == (nfound):
            nfound = nfound + 1
            ntoa[i, nfound-1] = indices[nsearch]
        else:
            nsearch = nsearch + 1
    
    ntoa[i, :] = np.sort(ntoa[i, :])
    #Expresado en tiempo
    toamed = (ntoa - 1) / Fs 
##################Fin del primer bucle#####################################

#DIFERENCIA DE LOS TIEMPOS DE LLEGADA
for k in range (Ndevices):
    for i in range (Ndevices):
        for j in range (Ndevices):
            tdoamed[k, i, j] = toamed[i, k] - toamed[j, k]

#TIEMPOS DE VUELO            
for i in range (Ndevices):
    for j in range (Ndevices):
        tf_est[i, j] = (tdoamed[j, i, j] - tdoamed[i, i, j]) / 2.0
        
#INSTANTES DE COMIENZO (tcest[0] = 0)
for i in range(1, Ndevices):
    tcest[i] = (toamed[i-1,i-1] - toamed[i,i-1] + toamed[i-1,i] - toamed[i,i]) / 2 + tcest[i-1]
    

# %%

#x = fsolve(sys, tcest, options)
   
    
# #Ordenamos de menor a mayor las señales según su instante de comienzo (el primer elemento cpmienza antes)
# toaRef = np.argsort(toamed[:, 0]) #Hace referencia a las señales ordenadas por instante de comienzo
# #Determinamos las muestras de comienzo de cada señal
# mcest = np.ceil(tcest * Fs)

# #Determinamos el nuevo tamaño de cada señal aplicando el corte del tiempo de comienzo en cada caso
# for i in range(Ndevices):
#     tam_postdelay[i] = tam[i] - mcest[toaRef[i]]

# #Adaptamos el tamaño de las señales con la señal de menos muestras (acortamos por abajo)
# tam_final = min(tam_postdelay)

# #En esta matriz se guardarán las señales sincronizadas
# sincronizadas = np.zeros((tam_final, Ndevices))

# plt.figure(4)
# plt.title('Señales sincronizadas')
# plt.xlabel('Muestras')
# plt.ylabel('Amplitud')

# #Recortamos las señales
# for i in range(Ndevices):
#     limite = tam_final + int(mcest[toaRef[i]])
#     sincronizadas[:, i] = record[int(mcest[toaRef[i]]):limite, i] #Acortamos por arriba
#     plt.plot(sincronizadas[:,i])


# #Hacemos la correlación de las señales
# for z in range(Ndevices):
#     rx_sincro = scipy.signal.correlate(sincronizadas[:,z], sincronizadas[:,toaRef[1]], mode = 'full')
#     N = len(rx_sincro)
#     k = np.linspace(-(N/2) +1, (N/2)-1, N)
    
#     plt.figure(z+5)
#     plt.plot(k, rx_sincro)
#     plt.title('Correlación cruzada señales ajustadas')
#     # Calculamos el delay final para todas las señales
#     delay_final[z] = correlaMax(rx_sincro, N)
#     print('El delay es de:', delay_final[z], 'muestras')    
#----------------------------------------------------------------------------------------------------


