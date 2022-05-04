function [F,tdoaest]=fcriterion(tdoamed,Sest,Mest,Tcest,c)

N=length(Tcest);
F=0;
for k=1:N
    for i=1:N
        for j=1:N
            tdoaest{k}(i,j)=norm(Sest(:,k)-Mest(:,i)) - norm(Sest(:,k)-Mest(:,j));
            tdoaest{k}(i,j)=tdoaest{k}(i,j)/c;
            tdoaest{k}(i,j)=tdoaest{k}(i,j)-Tcest(i)+Tcest(j);
            F=F+(tdoaest{k}(i,j)-tdoamed{k}(i,j))^2;
        end
    end
end