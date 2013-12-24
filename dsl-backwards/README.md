DSL For Backwards Compatability Support
=======================================

The purpose of this module is to provide support for recipes written for cloudify 2.6 and below, to be able to run on the latest cloudify installation.

If your recipe contains any one of the following imports:

```
import org.cloudifysource.dsl.context.ServiceContext
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.context.Service
import org.cloudifysource.dsl.context.ServiceInstance
```

Than this module is required and should be placed under the ${CLOUDIFY_HOME}/lib/platform/cloudify folder.
