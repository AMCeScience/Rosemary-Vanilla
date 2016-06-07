#!/bin/bash

echo 'Initializing ...'

# Download and install the JS packages
npm install
bower install

#git submodule init 
#git submodule update

# Compile the back-end
activator compile

# Generate project files for eclipse IDE
#activator eclipse

# Compile the front-end (less and coffee)
gulp default

echo 'Now run it with "activator run" for development or "activator stage" for production'
