source local.rc
echo "an: ${acrName} an: ${appName} rgn: ${resourceGroupName}"

az login
az acr login --name ${acrName}
docker build -t ${acrName}.azurecr.io/networkcenterservice:$1 .
docker push ${acrName}.azurecr.io/networkcenterservice:$1
# run below for the first time deployment
az acr update -n ${acrName} --admin-enabled true
echo "start to update app service config, version is $1"
az webapp config container set --name ${appName} --resource-group ${resourceGroupName} --docker-custom-image-name ${acrName}.azurecr.io/networkcenterservice:$1 --docker-registry-server-url https://${acrName}.azurecr.io