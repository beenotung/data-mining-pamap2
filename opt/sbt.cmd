set SCRIPT_DIR=%~dp0
set "PATH=%PATH%;C:\Program Files (x86)\Java\jdk1.8.0_66\bin"
java -Dsbt.ivy.home=d:\.ivy2 -Dsbt.global.base=d:\.sbt\0.13 -Dsbt.boot.directory=d:\.sbt\boot -Dsbt.repository.config=d:\.sbt\repositories -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M -jar "%SCRIPT_DIR%sbt-launch.jar" %*