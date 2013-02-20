userInterface {
	metricGroups = [
		metricGroup{
			name = "process"
			metrics = ["cpu", "memory"]
		}
	]
	widgetGroups = [
		widgetGroup{
			name  ="cpu"
			widgets = [
				balanceGauge{metric = "cpu"},
				barLineChart{metric = "cpu"}
			]
		},
		widgetGroup {
			name = "memory"
			widgets = [
				balanceGauge { metric = "memory" },
				barLineChart{ metric = "memory"
					axisYUnit Unit.PERCENTAGE
					}
			]
		}
	]
}