@echo off
rem
rem ----- MS-DOS batch script that will configure an exisiting Windows-based MySQL
rem ----- database installation for use with Fedora. It assumes that MySQL has been 
rem ----- successfully installed and has a MySQL username with dba privileges. If this
rem ----- is a new installation of mySQL, the default DBA username is usually "root"
rem ----- with no password(use "" to indicate no password). If this is an existing
rem ----- mySQL installation, use the appropriate username and password for the user
rem ----- that has DBA authority on the mySQL installation. The following local 
rem ----- environment variables are set based on the input arguments to the script.
rem
rem ----- mysql_home      - the location where MySQL is installed
rem ----- mysql_dba_user  - the name of the MySQL user with dba privileges (default: root)
rem ----- mysql_dba_pass  - the password for the MySQL user with dba privileges (default" none)
rem ----- fedoradba_user  - the name of MySQL user admin for Fedora database (default: fedoraAdmin)
rem ----- fedora_dba_pass - the password of MySQL user admin for Fedora database (default: fedoraAdmin)
rem ----- mysql_db_name   - the name of the Fedora database (default: FedoraObjects)

rem ----- check for input arguments

if "%1" == "" goto showUsage
if "%2" == "" goto showUsage
if "%3" == "" goto showUsage
if "%4" == "" goto showUsage
if "%5" == "" goto showUsage
if "%6" == "" goto showUsage

set mysql_home=%1
echo MySQL install directory: %mysql_home%

set mysql_dba_user=%2
echo MySQL dba username: %mysql_dba_user%

set mysql_dba_pass=%3
echo Mysql dba password: %mysql_dba_password%

set fedora_dba_user=%4
echo Fedora dba username: %fedora_dba_user%

set fedora_dba_pass=%5
echo Fedora dba password: %fedora_dba_pass%

set fedora_db_name=%6
echo Fedora database name: %fedora_db_name%

set path=%path%;%mysql_home%\bin
echo mySQL home: %mysql_home%

echo.
echo Creating Fedora database: %fedora_db_name%
echo.

rem ----- Create Fedora database using specified database name
mysqladmin -u %mysql_dba_user% -p%mysql_dba_pass% -h localhost create %fedora_db_name%

rem ----- Generate MySQL commands to assign username and password to Fedora database
echo >mysqlConfig.sql #
echo >>mysqlConfig.sql # Configure MySQL with the proper username and password for the Fedora database by 
echo >>mysqlConfig.sql # 1) adding a username of %fedora_dba_user% with no global permissions
echo >>mysqlConfig.sql # 2) assigning initial password of %fedora_dba_pass% for username %fedora_dba_user%
echo >>mysqlConfig.sql # 3) granting the fedoraAdmin username DBA permissions on the Fedora Database named 'FedoraObjects'
echo >>mysqlConfig.sql #
echo >>mysqlConfig.sql grant all privileges on %fedora_db_name%.* to %fedora_dba_user%@localhost identified by '%fedora_dba_pass%' with grant option;
echo >>mysqlConfig.sql grant all privileges on %fedora_db_name%.* to %fedora_dba_user%@'%%' identified by '%fedora_dba_pass%' with grant option;
echo >>mysqlConfig.sql #
echo >>mysqlConfig.sql # Display results for verification
echo >>mysqlConfig.sql #
echo >>mysqlConfig.sql select * from user;
echo >>mysqlConfig.sql select * from db;

echo.
echo Assigning username and passwords for Fedora database
echo.
rem ----- Assign specified username and password for Fedora database
mysql -u %mysql_dba_user% -p%mysql_dba_pass% -h localhost -D mysql <mysqlConfig.sql

echo.
echo Database initialization complete!
echo.
del mysqlConfig.sql
goto end


:showUsage
echo.
echo Usage: mySQLConfig.bat mysqlHome mysqlDBAUser mysqlDBAPass fedoraUser fedoraPass fedoraDbName
echo.
echo mysqlHome - The location where mySQL is installed (e.g., c:\mysql)
echo mysqlDBAUser - mySQL username with DBA authority  (root if new install)
echo mysqlDBAPass - mySQL password for DBA username    (none if new install)
echo fedoraUser   - username for Fedora admin user (use fedoraAdmin as default)
echo fedoraPass   - password for Fedora admin user (use fedoraAdmin as default)
echo fedoraDbName - Fedora database name (use FedoraObjects as default)
echo.
goto end

:end