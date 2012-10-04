package org.cloudifysource.restDoclet.annotations;



public @interface PossibleResponseStatuses {
	public int[] codes();
	public String[] descriptions();
}
