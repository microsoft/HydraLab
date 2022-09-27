#!/bin/sh
echo newfile = $1
brew services stop supervisor
if [ -z "$1" ]
then
  echo "No need to update"
else
  if [ ! -f "$1" ]
  then
    echo "$1 not exist"
  else
    echo "Updating"
    rm -rf agent.jar
    mv "$1" agent.jar
  fi
fi
brew services reload supervisor
brew services start supervisor
