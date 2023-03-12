## Server module for Blue's Prox Chat

---

### Intro

This subproject contains the server side code for the prox chat plugin.  
To build the server, simply run `./gradlew build` in the root of the repository, the server jar file will be in `server/build/libs/blues-prox-chat-xxx-server.jar` where `xxx` is the version number of the project.  
None of the code in this submodule is included in the plugin hub build, so using it from the plugin will crash. Common code should go in the root src tree, so both the server and client can access it.

---

### Running the server

Running the server is simple. Just run `java -jar blues-prox-chat-xxx-server.jar` where `xxx` is the version number.  
On first load the server will create a config file and then exit. Change the password in the config.json, then restart the server.  
You can also change the bind address and port in the config, if you have special network requirements, but the defaults should be fine for most people.