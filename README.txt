Script to create applications with a user defined number of components using the JPetStore application. Used to create applications for load testing the workflow engine with a various number of components. 

Prerequisite: Export environment variables for UCD server 

export DS_WEB_URL=https://<ucd server>:<port>
export DS_USERNAME=<username>
export DS_PASSWORD=<password>

Run: 

./create-app.sh <# of components>
