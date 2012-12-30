[
	SMALL_LINUX : template{
							machineMemoryMB 1600
							remoteDirectory "/tmp/gs-files"
							username "username"
							password "password"
							localDirectory "upload"
							custom ([
										"start-machine" : {
														return "pc-lab110"
													},
										"stop-machine" : { ip ->
															System.setProperty("stopMachine", ip)
															return true
														}
									])				
							privileged true
				}
]