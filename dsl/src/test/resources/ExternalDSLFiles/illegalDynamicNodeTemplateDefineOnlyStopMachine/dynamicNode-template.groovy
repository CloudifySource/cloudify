[
	SMALL_LINUX : template{
							machineMemoryMB 1600
							remoteDirectory "/tmp/gs-files"
							username "username"
							password "password"
							localDirectory "upload"
							custom ([
										"stopMachine" : {
															System.setProperty("stopMachine", "stopped")
														}
									])				
							privileged true
				}
]