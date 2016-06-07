# Basic development installation

This whole guide is targeted for OS X installation. This because all development has taken place on OS X.

## Installation using HomeBrew

This assumes a clean OS X installation (with Java SDK). Skip steps accordingly to your own configuration:

### Steps

* Install [Homebrew](http://brew.sh) (alternatively, you can install [MacPorts](https://www.macports.org))

	```
	ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
	```

* Install NodeJS (with NPM), MongoDB and Scala (through the Typesafe Activator)

	This instruction is tested with typesafe-activator version __1.3.9__, Scala version __2.11.7__ and Java version __1.8.0_92__.

	```
	brew install node mongodb typesafe-activator
	```
	<sub>**Optional:** Look in the comments on how to enable MongoDB, or just run directly using:<sub>	
	```
	mongod --config /usr/local/etc/mongod.conf
	```


* Install Bower & Gulp

	```
	npm install -g bower gulp
	```

* Clone Rosemary using git(hub)

	```
	git clone https://github.com/AMCeScience/Rosemary.git Rosemary
	```

* Install dependencies

	```
	# Navigate into Rosemary directory
	npm install
	bower install
	```
	
## Run!

Now, we have a fully enabled development environment. To start, just run the following command:

```
activator run
```

and navigate to **http://localhost:9000/**.

<sub>Make sure you have disabled your caches to enable proper live reload functionality.</sub>
