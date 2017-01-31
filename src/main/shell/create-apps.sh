#!/bin/bash
echo -n "Enter the external user URL for your UrbanCode Deploy environment: "
read DS_WEB_URL
echo -n "Enter a user ID: "
read DS_USERNAME
echo -n "Enter a corresponding password: "
read -s DS_PASSWORD
echo && echo -n "Enter the number of applications to generate: "
read number_apps
echo -n "Enter the number of components to generate per application: "
read number_comps
# echo -n "Enter the number of versions to create per component: "
# read number_versions
# echo -n "Enter the base artifact path for your component versions: "
# read ARTIFACT_PATH

export DS_WEB_URL=$DS_WEB_URL
export DS_USERNAME=$DS_USERNAME
export DS_PASSWORD=$DS_PASSWORD
ARTIFACT_PATH='lib/artifact/1.0'

# For each application ...
for ((j=1; j<=$number_apps; j++)); do
	rm -rf tmp
	rm -rf dist
	mkdir tmp
	cd tmp

  # Start the creation of a component property sheet for the application
  echo "[" > load$number_comps-props.json

  # Build the JSON for each component in the application
  echo "Assembling component JSON for application LOAD$number_comps-APP$j"
  for ((i=1; i<=$number_comps; i++)); do
    cp ../lib/components-template.json load$number_comps-component$i.json
		sed -i -e "s/\${COMPONENTNAME}/LOAD$number_comps-APP$j-COMP$i/g" load$number_comps-component$i.json
    sed -i -e "s/\${TAG}/LOAD$number_comps-APP$j/g" load$number_comps-component$i.json

		# Add a property w/component name to the application's component property sheet
		echo "{" >> load$number_comps-props.json
		echo	"\"properties\": []," >> load$number_comps-props.json
		echo	"\"componentName\": \"LOAD$number_comps-APP$j-COMP$i\"" >> load$number_comps-props.json
		echo "}" >> load$number_comps-props.json

    # Add a comma to the component proprty sheet in case of additional components
    if [ "$i" -lt "$number_comps" ]; then
			echo ","  >> load$number_comps-props.json
		fi
  done;
  echo "]"  >> load$number_comps-props.json
  # JSON for all components is ready in tmp/

  # Now create a new JSON object that's a combination of all component JSON
  mkdir ../dist
  echo "[" > ../dist/$number_comps-components.json
  for ((i=1; i<=$number_comps; i++)); do
		cat load$number_comps-component$i.json >> ../dist/$number_comps-components.json

    # Add a comma for additional components if needed
		if [ "$i" -lt "$number_comps" ]; then
			echo "," >> ../dist/$number_comps-components.json
		fi
  done;
	echo "]" >> ../dist/$number_comps-components.json
  # All component JSON collated into $number_comps-components.json

	# Copy application-template.json for new application and modify
  echo "Assembling JSON for application LOAD$number_comps-APP$j"
	cat ../lib/application-template.json > ../dist/$number_comps-application.json
  sed -i -e "s/\${APPNAME}/LOAD$number_comps-APP$j/g" ../dist/$number_comps-application.json
  sed -i -e "/\${COMPONENTS}/ r ../dist/$number_comps-components.json" ../dist/$number_comps-application.json
	sed -i -e "s/\${COMPONENTS}//g" ../dist/$number_comps-application.json
  sed -i -e "/\${COMPONENT_PROP_SHEETS}/ r load$number_comps-props.json" ../dist/$number_comps-application.json
	sed -i -e "s/\${COMPONENT_PROP_SHEETS}//g" ../dist/$number_comps-application.json
	sed -i -e "s/\${TAG}/LOAD$number_comps-APP$j/g" ../dist/$number_comps-application.json

	rm ../dist/$number_comps-components.json
  cd ../

	# Import the application
	echo "Attempting to import application LOAD$number_comps-APP$j"
  echo "Import submitted ..." `date`;
	curl -k "$DS_WEB_URL/rest/deploy/application/import?upgradeType=UPGRADE_IF_EXISTS&compTempUpgradeType=UPGRADE_IF_EXISTS&processUpgradeType=UPGRADE_IF_EXISTS" --user $DS_USERNAME:$DS_PASSWORD -i -F file=@dist/$number_comps-application.json -F processUpgradeTypeInput=UPGRADE_IF_EXIST -F componentUpgradeTypeInput=UPGRADE_IF_EXIST
	echo "Application imported ..." `date`;

	# Create versions and artifacts for components
	for ((i=1; i<=$number_comps; i++)); do
		echo "Creating version for LOAD$number_comps-APP$j-COMP$i"
		java -jar udclient.jar createVersion -name 1.0 -component LOAD$number_comps-APP$j-COMP$i

		echo "Importing artifacts for LOAD$number_comps-APP$j-COMP$i"
		java -jar udclient.jar addVersionFiles -version 1.0 -base $ARTIFACT_PATH -component LOAD$number_comps-APP$j-COMP$i
  done;

	echo "Creating snapshot for LOAD$number_comps-APP$j"
	echo \{\"name\"\:\"1.0\"\,\"application\"\:\"LOAD$number_comps-APP$j\"\, > tmp/create-snapshot.json
	echo \"versions\"\: \[ >> tmp/create-snapshot.json

  for ((i=1; i<=$number_comps; i++)); do
    echo \{ \"LOAD$number_comps-APP$j-COMP$i\": \"1.0\"\} >> tmp/create-snapshot.json

    # Add a comma for additional components if they exist
    if [ "$i" -lt "$number_comps" ]; then
			echo ","  >> tmp/create-snapshot.json
		fi
	done;

	echo \] >> tmp/create-snapshot.json
	echo \} >> tmp/create-snapshot.json
  echo "Import submitted ..." `date`;
	java -jar udclient.jar createSnapshot tmp/create-snapshot.json
  echo "Snapshot created ..." `date`;

	rm -rf tmp
done;

java -jar ucd-stress-test.jar $DS_WEB_URL $DS_USERNAME $DS_PASSWORD $number_apps $number_comps
echo "Resource creation and mapping utility is exiting successfully"
