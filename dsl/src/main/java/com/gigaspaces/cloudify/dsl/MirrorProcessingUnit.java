package com.gigaspaces.cloudify.dsl;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name="mirrorProcessingUnit", clazz=MirrorProcessingUnit.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class MirrorProcessingUnit extends StatelessProcessingUnit{

	
}
