@echo off
chcp 65001 >nul 2>&1
setlocal

set JAR_PATH=%~dp0target\agent-cli-1.0.0.jar
set JAVA_OPTS=-Dfile.encoding=UTF-8

if "%1"=="" (
    echo.
    echo  ============================================
    echo   Agent CLI - AI Programming Assistant
    echo  ============================================
    echo.
    echo  Usage:
    echo    agent.bat -p deepseek -k YOUR_API_KEY
    echo    agent.bat -p glm -k YOUR_API_KEY --rag
    echo    agent.bat --help
    echo.
    echo  Environment Variables (alternative to -k):
    echo    DEEPSEEK_API_KEY, GLM_API_KEY, MOONSHOT_API_KEY,
    echo    STEPFUN_API_KEY, SPARK_API_KEY, OPENAI_API_KEY
    echo.
    exit /b 0
)

java %JAVA_OPTS% -jar "%JAR_PATH%" %*

endlocal
