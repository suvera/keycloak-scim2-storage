# Keycloak SCIM 2.0 outbound user provisioning

**Keycloak** is an open source Identity and Access management system for modern applications and services.

more info https://github.com/keycloak/keycloak


This is extension to keycloak, where it provides capability of user provisioning to external scim service providers from keycloak identity server.


## Installation

```
# clone this repo

cd keycloak-scim2-storage

mvn clean install


# Take the backup of your Keyclock DB, this extension has new tables added.

# Copy jar file to keyclock server

 cp -f target/suvera-keycloak-scim2-outbound-provisioning-jar-with-dependencies.jar \
        /path/to/keycloak-11.0.2/standalone/deployments/


# Restart keycloak server

```

## User Provisioning

1) Login to Keyclock as "admin"

2) Select "User Federation" Add SCIM 2.0 Service Provider 

![User Federation](https://suvera.github.io/assets/images/scim2_storage_01.png)


3) Add new SCIM 2.0 User federation 

![Add SCIM 2.0 User Federation](https://suvera.github.io/assets/images/scim2_storage_02.png)


4) Now go to "Users -> Add New User" 

![Add New User](https://suvera.github.io/assets/images/scim2_storage_03.png)


5) That's it!, User will be added to your SCIM 2.0 Service provider too. 

