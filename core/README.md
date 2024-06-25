# core
Primary repository for Optio3 code

# Pre-requisites

Install Docker on your machine.

Login into the Docker Registry:

	sudo docker login repo.dev.optio3.io:5000

It will ask for username (your optio3 email without the @optio3.com part) and password

# Configuration

To begin working with the Optio3 codebase, the first thing you have to do is to execute the 'setenv.sh' script:

    . setenv.sh

After that, the `o3` script will be available:

    Welcome to the Optio3 command line!
    
    Available commands:
       cred
       db
       ldap
       mvn
       sdk
       site
       tooling
