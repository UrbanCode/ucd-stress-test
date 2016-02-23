#!/bin/bash

if [ -z "$DS_WEB_URL" ]; then
    DS_WEB_URL=http://localhost:8080
    export DS_WEB_URL
fi

if [ -z "$DS_USERNAME" ]; then
    DS_USERNAME=admin
    export DS_USERNAME
fi

if [ -z "$DS_PASSWORD" ]; then
    DS_PASSWORD=admin
    export DS_PASSWORD
fi


if [ "$#" -ne 3 ]; then
  echo "./create-app.sh <number of apps> <number of components> <number of versions>"
  echo "Input required for number of applications and components"
  exit
fi

#number of apps
x=$1
#number of components
n=$2


for ((j=1; j<=$x; j++)); do

	#create versions and artifacts
	for ((i=1; i<=$n; i++)); do
		#create version
        for ((t=1; t<=$3; t++)); do
		    echo "Create version : $t.0 - $n-Load-$j-Component-$i"
		    java -jar bin/udclient.jar createVersion -name $t.0 -component $n-Load-$j-Component-$i

		    #import artifacts
		    echo "Import version : $t.0 - $n-Load-$j-Component-$i"
		    java -jar bin/udclient.jar addVersionFiles -version $t.0 -base lib/artifact/1.0 -component $n-Load-$j-Component-$i
		done;

	done;



	rm -rf tmp

done;


