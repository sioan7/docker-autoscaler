echo Start removing!

FOR /f "tokens=*" %%i IN ('docker ps -a -q') DO docker rm %%i

echo All removed!
PAUSE