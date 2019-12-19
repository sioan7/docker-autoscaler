echo Start starting!

FOR /f "tokens=*" %%i IN ('docker ps -a -q') DO docker start %%i

echo All started!
PAUSE