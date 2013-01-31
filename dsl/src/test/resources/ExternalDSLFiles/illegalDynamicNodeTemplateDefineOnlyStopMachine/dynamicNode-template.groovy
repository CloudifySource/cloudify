[
	SMALL_LINUX : computeTemplate{
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