#!/bin/sh
if [ "$USER" = "root" ]
then
    echo "Start this script as 'root' may cause errors when running services. Exit install process."
    exit 1
fi

scriptPath=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )

userHome=$(eval echo ~)

### install enviroment
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> "$userHome/.zprofile"
eval "$(/opt/homebrew/bin/brew shellenv)"
brew install carthage
brew install node
brew install awk
brew install android-platform-tools

# trace adb path and set ANDROID_HOME as env var
adb_path=`which adb`
# example value of this adb_path here: 
# lrwxr-xr-x  1 bsp  admin  71 Mar 10 18:47 /opt/homebrew/bin/adb -> /opt/homebrew/Caskroom/android-platform-tools/34.0.0/platform-tools/adb

adb_path=`ls -l $adb_path`
echo $adb_path > temp.txt
adb_path=`awk -F '-> ' '{print $2}' temp.txt`
echo $adb_path > temp.txt
export ANDROID_HOME=`awk -F '/platform-tools/' '{print $1}' temp.txt`
echo $ANDROID_HOME
echo "\nexport ANDROID_HOME=$ANDROID_HOME" >> export_android_home.sh
cat export_android_home.sh >> ~/.zshrc

brew install maven
brew install libimobiledevice
brew install --build-from-source python@3.9
brew install ffmpeg

brew install supervisor
brew install services

brew install openjdk@11

npm install -g ios-deploy
npm install -g appium

pip3 install -U "tidevice[openssl]"  # or 'pip3 install -U tidevice'

serverPath="$userHome/Library/Server/HydraLab"
supervisorPath="/opt/homebrew/etc/supervisor.d/"
iniFile="$scriptPath/HydraLabAgent.ini"
agentTaskFile="$scriptPath/HydraAgent.sh"

### create supervisor.ini file
rm -f "$iniFile"
touch "$iniFile"
echo "[program:HydraLabAgent]
directory = $userHome/Library/Server/HydraLab
command = $userHome/Library/Server/HydraLab/HydraAgent.sh
autostart = true
startsecs = 5
autorestart = true
startretries = 3
user = $USER
stdout_logfile = $userHome/Library/Server/HydraLab/supervisorstd.log
redirect_stderr = true" >> "$iniFile"


rm -f "$agentTaskFile"
touch "$agentTaskFile"
echo "#!/bin/sh
processid=\$(ps aux | grep caffeinate | grep -v \"grep\" | awk '{ print \$2}')
kill \$processid

osascript -e 'tell app \"Terminal\"
    do script \"caffeinate -s; exit\"
end tell'

processid=\$(ps aux | grep agent | grep -v \"grep\" | awk '{ print \$2}')
kill \$processid
export PATH=\"/opt/homebrew/opt/openjdk@11/bin:/opt/homebrew/bin:\$PATH\"
java -Xms1024m -Xmx2048m -jar ~/Library/Server/HydraLab/agent.jar --spring.config.location=config/
" >> "$agentTaskFile"

chmod +x "$scriptPath/restartAgent.sh"
chmod +x "$scriptPath/HydraAgent.sh"

### copy files 
echo "Copying Files......"
echo "Work Folder: $serverPath"
mkdir -p "$serverPath"
sudo mkdir -p "$supervisorPath"
sudo cp "$iniFile" "$supervisorPath/HydraLabAgent.ini"
jarPath=$(find "$scriptPath" -type f -name "*agent*")
cp "$scriptPath/HydraAgent.sh" "$serverPath/HydraAgent.sh"
cp "$scriptPath/restartAgent.sh" "$serverPath/restartAgent.sh"
cp "$jarPath" "$serverPath/agent.jar"
echo "Files copy finished."

### start agent
echo "Trying to start agent......"
"$scriptPath/restartAgent.sh"
echo "Agents should be started, please input \"supervisorctl status\" to check"
