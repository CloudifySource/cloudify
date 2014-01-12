CloudStack 4.x Cloud Driver 

This cloud driver has been tested with Cloudify 2.7.0 available for download [here](http://cloudifysource.org/http://www.cloudifysource.org/downloads/get_cloudify).

# Installation 

To install this driver following the following steps (make sure you have git client installed): 

* Clone the repo and copy the cloud driver folder to the right location in the cloudify distro: 
```
git clone git@github.com:CloudifySource/cloudify-cloud-drivers.git
cp -r cloudstack/ <cloudify root>/clouds
```
* Edit the file `cloudstack-cloud.properties` and add your cloud credentials instead of the place holders

* Bootstrap the cloud: 
```
cd <cloudify root>/bin
./cloudify.sh
```

```

  .oooooo.   oooo                              .o8   o8o   .o88o.             
 d8P'  `Y8b  `888                             "888   `"'   888 `"             
888           888   .ooooo.  oooo  oooo   .oooo888  oooo  o888oo  oooo    ooo 
888           888  d88' `88b `888  `888  d88' `888  `888   888     `88.  .8'  
888           888  888   888  888   888  888   888   888   888      `88..8'   
`88b    ooo   888  888   888  888   888  888   888   888   888       `888'    
 `Y8bood8P'  o888o `Y8bod8P'  `V88V"V8P' `Y8bod88P" o888o o888o       .8'     
                                                                  .o..P'      
                                                                  `Y8P'

  GigaSpaces Cloudify Shell.  

  Note for Windows Users:
   The Cloudify shell does not currently support the back-slash character ('\')
   as file separator. Instead, use the forward-slash character ('/') when
   specifying file paths.

Hit '<tab>' for a list of available commands.
Hit '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or 'exit' to exit the console.

Cloudify version: 2.7.0


cloudify@default> bootstrap-cloud -timeout 20 cloudstack
```

