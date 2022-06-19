% Patrón de directividad del beamformer
close all;

f= [100,400,700,1000,2000,3000,4000,5000,6000,7000,8000];
angulos_patron = 0:0.5:180; %vector de angulos del patron de directividad
phi = 45; %direccion de procedencia del sonido
D_aux = 0;
D = zeros(length(f),length(angulos_patron));
N=7;
% Para cada un de las frecuencias...
for i = 1:length(f)
    % Para cada una de las direcciones...
    for j = 1:length(angulos_patron)
        % Para cada uno de los canales...
        for n=1:N
            % Formula de los pesos (Determinan la directividad)
            diferencia = (cosd(angulos_patron(j))-cosd(phi));
            D_aux=exp(1i*2*pi*f(i)*(n-1)*d*(1/c)*diferencia) + D_aux;
        end
        % Directividad acumulada de cada canal
        D(i,j)=D_aux/N;
        D_aux = 0; %inicializamos de nuevo la directividad calculada
    end
end

for l = 1:length(f)
    subplot(4,3,l)
    figure(1);
    phi_polar = linspace(0,2*pi,721);
    %Hasta ahora solo hemos calculado hasta 180, hacemos la parte simetrica
    D_polar = horzcat(D(l,:),D(l,end:-1:2));
    polar(phi_polar,abs(D_polar)); %funcion utilizada para pintar en pola
    title(['frecuencia:',num2str(f(l))]);
end