# VMwareInventory

A tool that is capable of editing the VMware Inventory (vmAutoStart.xml, vmInventory.xml and inventory.vmls) from the Command line.

The tool can move a machine to the Shared Machines inventory, and set it to autostart. It can also remove machines from the inventory.vmls or from the vmInventory.xml.

Main.class      -m|-f     -c|-r|-s|-e|-l     -a|-g

-m,--machine      Specified the exact directory for the VMware directory that you want to operate with

-f,--find         Instead of specifying an exact path with -m, this will look recursively for the first machine directory

-c,--create       Adds the specified machine with -m or -f to the VMware Inventory

-r,--remove       Removes the specified machine with -m or -f from the VMware Inventory

-s,--share        Moves one machine from the normal pool of machines (inventory.vmls), to Shared Machines (vmAutoStart.xml)

-a,--autostart    Used only with -s, tells to also add the moved machine to the vmAutoStart.xml, so that it will start automatically

-e,--echo         Just prints out the machine found with -m or -f

-l,--list         Lists all the machines in inventory.vmls, or in  vmInventory.xml when --global is specified

-g,--global       The operation will be performed against vmAutostart.xml in the Shared Machines pool
