@echo off
echo ==========================================
echo    Enhanced ATM - Compilacion y Copia
echo ==========================================

cd /d "f:\Projects\pixcodev\Minecraft\InfinityPixel\Custom mods\forge-infinixmc-enhancedatm"

echo Compilando mod...
jar -cf build/libs/enhancedatm-1.0.0.jar -C src/main/resources . -C src/main/java .

echo Verificando archivo JAR...
if not exist "build\libs\enhancedatm-1.0.0.jar" (
    echo ERROR: No se pudo crear el archivo JAR
    pause
    exit /b 1
)

echo Eliminando versiones anteriores...

REM Limpiar directorio de CurseForge
if exist "C:\Users\ASUS-DESKTOP\curseforge\minecraft\Instances\test mods\mods\" (
    del /q "C:\Users\ASUS-DESKTOP\curseforge\minecraft\Instances\test mods\mods\enhancedatm-*"
) else (
    mkdir "C:\Users\ASUS-DESKTOP\curseforge\minecraft\Instances\test mods\mods\"
)

REM Limpiar directorio del servidor
if exist "C:\Users\ASUS-DESKTOP\Desktop\server\mods\" (
    del /q "C:\Users\ASUS-DESKTOP\Desktop\server\mods\enhancedatm-*"
) else (
    mkdir "C:\Users\ASUS-DESKTOP\Desktop\server\mods\"
)

echo Copiando mod a directorios de destino...

REM Copiar a CurseForge
copy "build\libs\enhancedatm-1.0.0.jar" "C:\Users\ASUS-DESKTOP\curseforge\minecraft\Instances\test mods\mods\"

REM Copiar al servidor
copy "build\libs\enhancedatm-1.0.0.jar" "C:\Users\ASUS-DESKTOP\Desktop\server\mods\"

echo ==========================================
echo   🎉 Enhanced ATM MOD COMPILADO EXITOSAMENTE!
echo ==========================================
echo Mod copiado exitosamente a:
echo   🎮 CurseForge: C:\Users\ASUS-DESKTOP\curseforge\minecraft\Instances\test mods\mods\
echo   🖥️  Servidor: C:\Users\ASUS-DESKTOP\Desktop\server\mods\
echo   📦 Archivo: enhancedatm-1.0.0.jar
echo ==========================================
echo ✅ ¡Listo para usar! Reinicia Minecraft/Servidor
echo ==========================================

pause