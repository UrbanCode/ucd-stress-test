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


if [ "$#" -ne 2 ]; then
  echo "./create-app.sh <number of apps> <number of components>"
  echo "Input required for number of applications and components"
  exit
fi

#number of apps
x=$1
#number of components
n=$2

ARTIFACT_PATH='\\\/home\\\/spkavana\\\/shared'


for ((j=1; j<=$x; j++)); do

	rm -rf tmp
	rm -rf dist
	mkdir tmp
	cd tmp

	echo "Generate components from template : $n-Load-$j"
	echo "["  > $n-component_props.json
	for ((i=1; i<=$n; i++)); do

		cp ../lib/components-template.json $n-components-$i.json
		#create components based on template
		sed -i -e "s/\${COMPONENTNAME}/$n-Load-$j-Component-$i/g" $n-components-$i.json
		sed -i -e "s/shared/$ARTIFACT_PATH/g" $n-components-$i.json
	
		#create tags for components
		sed -i -e "s/\${TAG}/$n-load-$j/g" $n-components-$i.json

		#create properties with component name
		echo "{" >> $n-component_props.json
		echo	"\"properties\": []," >> $n-component_props.json
		echo	"\"componentName\": \"$n-Load-$j-Component-$i\"" >> $n-component_props.json
		echo "}" >> $n-component_props.json


		if [ "$i" -lt "$n" ]; then
			echo ","  >> $n-component_props.json
		fi

	done;
	echo "]"  >> $n-component_props.json

	mkdir ../dist

	echo "[" > ../dist/$n-components.json
	for ((i=1; i<=$n; i++)); do

		cat $n-components-$i.json  >> ../dist/$n-components.json
		if [ "$i" -lt "$n" ]; then
			echo "," >> ../dist/$n-components.json
		fi

	done;
	echo "]" >> ../dist/$n-components.json

	#create application json to import

	echo "Generate application from template : $n-Load-$j"
	cat ../lib/application-template.json > ../dist/$n-application.json

	sed -i -e "s/\${APPNAME}/$n-Load-$j/g" ../dist/$n-application.json

	sed -i -e "/\${COMPONENTS}/ r ../dist/$n-components.json" ../dist/$n-application.json 
	sed -i -e "s/\${COMPONENTS}//g" ../dist/$n-application.json 

	sed -i -e "/\${COMPONENT_PROP_SHEETS}/ r $n-component_props.json" ../dist/$n-application.json 
	sed -i -e "s/\${COMPONENT_PROP_SHEETS}//g" ../dist/$n-application.json 

	sed -i -e "s/\${TAG}/$n-load-$j/g" ../dist/$n-application.json 

	rm ../dist/$n-components.json

	cd ../


	#import application
	echo "Create application : $n-Load-$j"
	echo "create application" `date`;
	curl -k "$DS_WEB_URL/rest/deploy/application/import?upgradeType=UPGRADE_IF_EXISTS&compTempUpgradeType=UPGRADE_IF_EXISTS&processUpgradeType=UPGRADE_IF_EXISTS" --user $DS_USERNAME:$DS_PASSWORD -i -F file=@dist/$n-application.json -F processUpgradeTypeInput=UPGRADE_IF_EXIST -F componentUpgradeTypeInput=UPGRADE_IF_EXIST
	echo "finish application" `date`;

	#create versions and artifacts
	for ((i=1; i<=$n; i++)); do
		#create version	
		echo "Create version : $n-Load-$j-Component-$i"
		java -jar udclient.jar createVersion -name 1.0 -component $n-Load-$j-Component-$i
	
		#import artifacts
		echo "Import artifacts : $n-Load-$j-Component-$i"
		java -jar udclient.jar addVersionFiles -version 1.0 -base lib/artifact/1.0 -component $n-Load-$j-Component-$i
		
	done;

	echo "Create snapshot : $n-Load-$j"
	echo \{\"name\"\:\"1.0\"\,\"application\"\:\"$n-Load-$j\"\, > tmp/create-snapshot.json
	echo \"versions\"\: \[ >> tmp/create-snapshot.json
	for ((i=1; i<=$n; i++)); do

		echo \{ \"$n-Load-$j-Component-$i\": \"1.0\"\} >> tmp/create-snapshot.json
		if [ "$i" -lt "$n" ]; then
			echo ","  >> tmp/create-snapshot.json
		fi
	done;
	echo \] >> tmp/create-snapshot.json
	echo \} >> tmp/create-snapshot.json

    echo "create snapshot" `date`;
	java -jar udclient.jar createSnapshot tmp/create-snapshot.json
    echo "finish snapshot" `date`;

	rm -rf tmp

done;

java -jar ucd-create-app.jar $DS_WEB_URL $DS_USERNAME $DS_PASSWORD $x $n
