1) in ServiceConfiguration the attribute osFamily should be set osFamily=2 (instead of osFamily=1)
2) A recent build should be uploaded to azure and the link should be updated in ServiceConfiguration (the current version does not support invoke with arguments)
3) look at iisproxy-service for custom commands
4) tomcat_post-start and post-stop have usage example invoking the commands from another service 
5) the rewrite_outbound_add doesn't seem to work on azure.
