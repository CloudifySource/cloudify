package org.cloudifysource.domain;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;


@CloudifyDSLEntity(name = "balanceGauge", clazz = MetricGroup.class, allowInternalNode = true,
allowRootNode = false, parent = "widgetGroup")
public class BalanceGauge extends AbstractBasicWidget implements Widget {

}
