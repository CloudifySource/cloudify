package org.openspaces.cloud.azure;

public enum AzureSlot {
    Staging("staging"), Production("production");
    
    private String slot;
    
    private AzureSlot(String slot) {
        this.slot = slot;
    }
    
    public String getSlot() {
        return slot;
    }
    
    public static AzureSlot fromString(String slot) {
        for (AzureSlot azureSlot : values()) {
            if (azureSlot.getSlot().equals(slot)) {
                return azureSlot;
            }
        }
        throw new IllegalArgumentException("Invalid azure slot: " + slot);
    }
    
}
