clear all;
clc;
close all;

%---------PARAMETROS DEL SISTEMA-------------------------------------------

fs = 16000;
c = 340; % velocidad de propagacion del sonido 
N=7; %numero de elementos del array
d = 0.04; %separacion de elementos del array (metros)
angulo = pi/4; %direccion de procedencia de la voz
load('signals_array.mat'); %señales de entrada -> xc(7 elementos), xorg16(señal original)

L = 256; % tramas de la señal

%--------------------------------------------------------------------------

%------------------CALCULO DEL RETARDO-------------------------------------

n = 0:N-1;
retardoTiempo =(n*d*cos(angulo))/c; % Retardos de cada canal

%------------------ CALCULO DE LOS PESOS (Frecuencia) ---------------------
Wn = zeros(N,L/2+1);
%Las frecuencias llegan solo hasta 128 por el teorema de Nyquist
%(2Fmax >= Fs)

% Para cada canal...
for ncanal=1:N
    % Para cada muestra temporal...
    for k = 1 : (L/2)+1
        % Calculo de pesos
        Wn(ncanal,k)=exp(1i*(2*pi*(k-1))*(fs/L)*retardoTiempo(ncanal))/N; 
    end
end

%--------------------------------------------------------------------------

% Numero de tramas entre las que se divide el mensaje original
Ntram = floor(length(xc{1,1})/(L/2));
% Ventana de hanning que se aplicará a cada una de las tramas
Wh = sqrt(hanning(L,'periodic'));
% Matriz que contiene la informacion de todos los canales
% Se aplica este comando para tener una matriz procesable
xc = cell2mat(xc)'; %Matriz de xc(n)

% Matriz que contiene la información procesa de una trama para todos los
% canales
Xout=zeros(N,L);
MitadVentana=zeros(1,L); %vector que almacena la segunda parte de la trama

%Recorremos el numero de tramas que tiene el msg original
for ntram = 1:Ntram-1 
    k = ((ntram-1)*L/2)+(1:L); % Calculo de las posiciones frecuenciales
    for nc = 1:N
        %-------- FFT -----------------------------------------------------
        Xc=fft(xc(nc,k)).*Wh';
        %---- BEAMFORMING -------------------------------------------------
        PesoPorFFT=(conj(Wn(nc,:)).*Xc(1:L/2+1));
        Xout(nc,:) = horzcat(PesoPorFFT,conj(PesoPorFFT(L/2:-1:2))); % Información simetrica en la trama
        %------------------------------------------------------------------
    end
    %Acumular
    Xout = sum(Xout);
    %IFFT
    x=(ifft(Xout).*Wh');
    %Overlap and sum
    xout(k)=x+MitadVentana;
    MitadVentana = horzcat(x(L/2+1:end),zeros(1,L/2)); % Segunda mitad de la trama procesada
end

soundsc(real(xout),fs);



% Cálculos de la SNR antes y después del beamforming

%las 3000 primeras muestras son ruido --> de ahí calculamos su SNR

%%---------SNR ANTES DE APLICAR EL BEAMFORMING--------------------- 
%Debido a que xc es por canales sacamos la media de todos ellos.
ruido_antes = var((xc(1,1:3000)));
potencia_antes = var((xc(1,3001:end)));
snr_antes = 10*log10(potencia_antes/ruido_antes)

%%-----------SNR DESPUES DE APLICAR EL BEAMFORMING-----------------

ruido_despues = var(real(xout(1:3000)));
potencia_despues = var(real(xout));
snr_despues = 10*log10(potencia_despues/ruido_despues)

