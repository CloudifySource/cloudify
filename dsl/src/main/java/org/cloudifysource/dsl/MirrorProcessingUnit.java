package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name="mirrorProcessingUnit", clazz=MirrorProcessingUnit.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class MirrorProcessingUnit extends StatelessProcessingUnit{

	
}
