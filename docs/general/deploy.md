# Deployment Instructions

## Requirements

* [Node](https://nodejs.org/)
  * [Bower](http://bower.io/)
  * [Gulp](http://gulpjs.com/)
* [MongoDB](https://www.mongodb.org/)
* [Typesafe-Activator](https://www.typesafe.com/get-started)
* [NGINX](https://www.nginx.com/)

#### Notes

For instructions about how to install the requirements see [Installation](/docs/general/install.md).

NGINX is necessary if only you host multiple web applications on the server and you need to do some port forwarding. 
Apache Web Server is not compatible with the WebSockets. If you use that the users won't have live notifications.

## Setup

1. Clone or pull Rosemary using git, then _navigate into the Rosemary directory_.

	``` bash
	git clone https://github.com/AMCeScience/Rosemary-Vanilla.git Rosemary-Vanilla
	# Or
	git pull 
	# Then
	cd Rosemary-Vanilla
	```

1. If you have deployed the application previously first do a cleanup.
Note that the following commands are also available in the provided [cleanup.sh](/cleanup.sh) script.

  ``` bash
  # Clean-up front-end dependencies
  rm -vfr node_modules
  rm -vfr public/bower
  # Clean-up compiled back-end code
  activator clean
  ```
  
1. Generate a new application secret based on 
[these instructions](https://www.playframework.com/documentation/2.5.x/ApplicationSecret).

1. Review the application configuration file in [conf/application.conf](/conf/application.conf) 
and adjust settings where necessary.

1. Install dependencies using `npm` and `bower`. Then compile and stage the Play Web Application using `activator`.
Note that the following commands are also available in the provided [initialize.sh](/initialize.sh) script.

	``` bash
	# Install front-end dependencies
	npm install
	bower install
	# Compile back-end code
	activator compile
	# Run gulp to generate public/script/app.js and public/styple/app.css
  	gulp default
	# Stage the application for stand-alone execution
	activator stage
	```

1. Run the Rosemary Web Application using the following command.

  ``` bash
  nohup sudo /app/Rosemary-Vanilla/target/universal/stage/bin/rosemary-vanilla -Dhttp.port=80 -Dconfig.file=/app/Rosemary-Vanilla/conf/production.conf &> /dev/null &
  ```
  
  Note that `sudo` is necessary only if your are putting it on port 80 because of SELinux.

1. If this is the first time that you are deploying the application, you should initialize the database by 
accessing the following URL:

  ```
  http://host/initialize
  ```

  Then deactivate this URL by commenting out the route in [conf/routes](/conf/routes).

1. Remember to change the admin password in the database using the `mongo` console:

  ``` json
  db.users.update( { email: "admin@rosemary.ebioscience.amc.nl" }, { $set: {password: "testtest" } }  )
  ```

## More Information

* [Play Documentation -- Starting your application in production mode](https://www.playframework.com/documentation/2.5.x/Production)
