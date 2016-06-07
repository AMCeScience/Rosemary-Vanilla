#!/bin/bash

echo 'Cleaning up ...'

rm -vfr node_modules
rm -vfr public/bower

activator clean

echo 'Now initialize it with ./initialize.sh ...'
