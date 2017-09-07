package com.astraia.vmware

/**
 * Created by Ruben on 30.05.2017.
 */
interface Inventory {
    public Set<VMwareInstance> getInventory();
    public boolean removeMachine(File vmxFile);
    public void write();
}