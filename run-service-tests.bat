@echo off
echo ========================================
echo 只运行 Service 层测试
echo ========================================
echo.

echo 运行测试...
call mvn clean test -Dtest=*ServiceTest

echo.
echo 生成覆盖率报告...
call mvn jacoco:report

echo.
echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 查看报告：
echo - 覆盖率报告: target\site\jacoco\index.html
echo.

pause
