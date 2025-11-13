@echo off
echo ========================================
echo 快速生成覆盖率报告
echo ========================================
echo.

echo 运行测试并生成覆盖率报告...
call mvn clean test jacoco:report

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✅ 覆盖率报告生成成功！
    echo ========================================
    echo.
    echo 📊 报告位置: target\site\jacoco\index.html
    echo.
    
    start target\site\jacoco\index.html
) else (
    echo.
    echo ❌ 生成失败！请检查测试代码
)

echo.
pause
