# NUODB Card Fraud Prevention Demo - Create the RTFAP Node.js/D3 ReST Server

## Install Node.js
On RHEL/Centos, 

$ sudo yum install -y gcc-c++ make

$ sudo curl -sL https://rpm.nodesource.com/setup_13.x | sudo -E bash -

$ sudo yum install -y nodejs

Testing your node and npm installation:

```
$ node -v
> v13.3.0

$ npm -v
> 6.13.1
```
## Install Express
We can now use npm to install all the Node.js tools needed, including Express.

NPM installs modules at the project-level. Command-line tools can be installed globally.

Because Express is a project module and a command line tool we can install it in both scopes, so we will install it globally.

Create a Project folder
```
$ mkdir restNCFP
$ cd restNCFP
```

This creates a package .json file:
```
$ npm init
```

This creates a directory called node_modules in the working directory and install : <<<<< double install of node_modules????
```
$ npm install express --save
$ npm install connect serve-static
```
Install the application generator:
```
$ sudo npm install express-generator -g
/usr/bin/express -> /usr/lib/node_modules/express-generator/bin/express-cli.js
+ express-generator@4.16.1
updated 1 package in 0.526s
```
Create the NCFP application:

$ express NCFP

  warning: the default view engine will not be jade in future releases
  warning: use `--view=jade' or `--help' for additional options


   create : NCFP/
   create : NCFP/public/
   create : NCFP/public/javascripts/
   create : NCFP/public/images/
   create : NCFP/public/stylesheets/
   create : NCFP/public/stylesheets/style.css
   create : NCFP/routes/
   create : NCFP/routes/index.js
   create : NCFP/routes/users.js
   create : NCFP/views/
   create : NCFP/views/error.jade
   create : NCFP/views/index.jade
   create : NCFP/views/layout.jade
   create : NCFP/app.js
   create : NCFP/package.json
   create : NCFP/bin/
   create : NCFP/bin/www

   change directory:
     $ cd NCFP

   install dependencies:
     $ npm install

   run the app:
     $ DEBUG=ncfp:* npm start


$ cd NCFP
$ npm install

npm WARN deprecated jade@1.11.0: Jade has been renamed to pug, please install the latest version of pug instead of jade
npm WARN deprecated constantinople@3.0.2: Please update to at least constantinople 3.1.1
npm WARN deprecated transformers@2.1.0: Deprecated, use jstransformer
npm notice created a lockfile as package-lock.json. You should commit this file.
added 99 packages from 139 contributors and audited 194 packages in 2.735s
found 4 vulnerabilities (3 low, 1 critical)
  run `npm audit fix` to fix them, or `npm audit` for details


$ DEBUG=myapp:* npm start

The process is running on port 3000:

$ sudo netstat -tulpn | grep 3000
tcp6       0      0 :::3000                 :::*                    LISTEN      20952/node

$ ps -eaf | grep 20952
ec2-user 20952 20941  0 17:03 pts/1    00:00:00 node ./bin/www

Ensure the firewall or security rules allow incoming traffic on port 3000.

Connect to the server at http://[server]:3000 - you should see a message:

Express
Welcome to Express


The generated app has the following directory structure:

.
├── app.js
├── bin
│   └── www
├── package.json
├── public
│   ├── images
│   ├── javascripts
│   └── stylesheets
│       └── style.css
├── routes
│   ├── index.js
│   └── users.js
└── views
    ├── error.pug
    ├── index.pug
    └── layout.pug

7 directories, 9 files


========== Examples =========
The default web server definition file is app.js - this displays a simple Hello message:

$ vi app.js

var express = require('express');
var app = express();

app.get('/', function (req, res) {
  res.send('Hello World!');
});

app.listen(3000, function () {
  console.log('Example app listening on port 3000!');
});

Start the simple node server app:

$ node app.js

Example app listening on port 3000!

In your browser go to http://127.0.0.1:3000/

The browser displays:

HelloWorld!
Kill the server (press ^C).

If you simply want to serve static content (e.g. HTML files) you could try this:

$ node server - runs server.js file
This provides a simple server on port 8000 e.g. http://localhost:8000/chartdse/public/index.html

The server.js file is very simple:

var connect = require('connect');
var serveStatic = require('serve-static');
connect().use(serveStatic(__dirname)).listen(8000);

if no start script is in package .json it will default to this server.js


See also http://expressjs.com/en/starter/basic-routing.html

=============================



## Install Node NuoDB Driver

You need the Node NuoDB driver to allow the Node web application to communicate with the NuoDB database.

In the NCFP directory, download the NuoDB Node.js driver:

```
$ curl  https://registry.npmjs.org/db-nuodb/-/db-nuodb-2.0.2.tgz -o db-nuodb-2.0.2.tgz
$ tar zxvf db-nuodb-2.0.2.tgz
```

```
$ cd package

$ npm install
```
db-nuodb.js
try {
  binding = require("./build/default/nuodb_bindings");
} catch (error) {
  binding = require("./build/Release/nuodb_bindings");
}

$ node
Welcome to Node.js v13.3.0.
Type ".help" for more information.
> var mod = require('./db-nuodb');
Thrown:
Error: Cannot find module './build/Release/nuodb_bindings'
Require stack:
- /home/ec2-user/NuoDB-Card-Fraud-Prevention/restNCFP/NCFP/db-nuodb/db-nuodb.js
- <repl>
    at Function.Module._resolveFilename (internal/modules/cjs/loader.js:961:17)
    at Function.Module._load (internal/modules/cjs/loader.js:854:27)
    at Module.require (internal/modules/cjs/loader.js:1023:19)
    at require (internal/modules/cjs/helpers.js:72:18) {
  code: 'MODULE_NOT_FOUND',
  requireStack: [
    '/home/ec2-user/NuoDB-Card-Fraud-Prevention/restNCFP/NCFP/db-nuodb/db-nuodb.js',
    '<repl>'
  ]
}

============ STUCK HERE ===========

$ mv package db-nuodb
$ npm install package

> db-nuodb@2.0.2 uninstall /home/ec2-user/NuoDB-Card-Fraud-Prevention/restNCFP/NCFP/node_modules/db-nuodb
> make clean

# remove module dependencies
node-gyp clean
rm -rf build
+ package@1.0.1
added 1 package from 1 contributor, removed 275 packages and audited 195 packages in 3.08s
found 4 vulnerabilities (3 low, 1 critical)
  run `npm audit fix` to fix them, or `npm audit` for details







## Test Access and data retrieval

Navigate to the restNCFP directory in the repo:
```
$ cd restNCFP
```

Start the Node http server using the command ```DEBUG=ncfp:* npm start``` 
Alternatively use the simple shell script provided ```./run.sh```

Output is logged to the screen. 

> Don't exit the terminal session - keep it open for diagnostic output and to preserve the web service.

You should see the console display the following:

```
> restrtfap@0.0.0 start /Users/johndoe/Documents/My Projects/GitHub/RTFAP2/restRTFAP
> node ./bin/www

  restrtfap:server Listening on port 3000 +0ms
```

Now go to the service URL: http://localhost:3000/

At this point you will be able to run some (but not all) of the Solr queries.

The web page demonstrates the use of SQL queries, but the roll-up tables have not been populated yet so these will return no data at this point.

You can find more detailed instructions for installing Node, D3, and jquery using my example at https://github.com/simonambridge/chartDSE

