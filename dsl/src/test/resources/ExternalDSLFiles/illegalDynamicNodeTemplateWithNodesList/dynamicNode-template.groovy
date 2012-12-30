[
	SMALL_LINUX : template{
							machineMemoryMB 1600
							remoteDirectory "/tmp/gs-files"
							username "username"
							password "password"
							localDirectory "upload"
							custom ([
										"startMachine" : {
														return "pc-lab110"
													},
										"stopMachine" : {
															System.setProperty("stopMachine", "stopped")
														},
										"nodesList" : 	([
															([
																"id" : "1",
																"host-list" : "pc-lab1"
															])
														])
									])				
							privileged true
				}
]