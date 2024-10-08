# Keycloak SCIM 2.0 outbound user provisioning

> JDK 17+
> 
> Keycloak 25.0.2

**Keycloak** is an open source Identity and Access management system for modern applications and services.

more info https://github.com/keycloak/keycloak


This is extension to keycloak, where it provides capability of user provisioning to external scim service providers from keycloak identity server.


## Installation

Use docker or build your own jar


### via Docker

```
# on Linux/Windows
docker run -it --rm --name keycloak-scim2-storage -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin suvera/keycloak-scim2-storage:v0.2

# on MAC OS (specify platform)
docker run -it --rm --name keycloak-scim2-storage -p 8080:8080 --platform linux/amd64 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin suvera/keycloak-scim2-storage:v0.2
```

Access keycloak server here
http://localhost:8080/
> User: admin Password: admin


```
# clone this repo

cd keycloak-scim2-storage
mvn clean install

# Take the backup of your Keyclock DB, this extension has new tables added.

# Copy jar file to keyclock server
 cp -f target/suvera-keycloak-scim2-outbound-provisioning-jar-with-dependencies.jar \
        /path/to/keycloak-25.0.2/provides/

# build & start keycloak server
 /path/to/keycloak-25.0.2/bin/kc.sh build
 /path/to/keycloak-25.0.2/bin/kc.sh start-dev
```

### Is your server is compliant to SCIM 2.0?

Here is the tool to test the compliance level  https://github.com/suvera/scim2-compliance-test-utility


## User Provisioning Steps 

1) Login to Keyclock as "admin"

2) Select "User Federation" Add SCIM 2.0 Service Provider 

![User Federation](https://suvera.github.io/assets/images/scim2_storage_01.png)


3) Add new SCIM 2.0 User federation 

    - **Bearer Token** Authentication also supported,  though it's not shown in the screenshot.

![Add SCIM 2.0 User Federation](https://suvera.github.io/assets/images/scim2_storage_02.png)


4) Now go to "Users -> Add New User" 

![Add New User](https://suvera.github.io/assets/images/scim2_storage_03.png)


5) That's it!, User will be added to your SCIM 2.0 Service provider too. 

