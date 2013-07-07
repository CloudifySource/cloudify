package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


@CloudifyDSLEntity(name = "balanceGauge", clazz = MetricGroup.class, allowInternalNode = true,
allowRootNode = false, parent = "widgetGroup")
public class BalanceGauge extends AbstractBasicWidget implements Widget {

}
