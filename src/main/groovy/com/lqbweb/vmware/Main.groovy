package com.lqbweb.vmware

import com.adarshr.args.ArgsEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.naming.OperationNotSupportedException

/**
 * Created by Ruben on 23.05.2017.
 */
class Main {
    final private static Logger logger = LoggerFactory.getLogger(Main.class);

    public final static int WARNING = 3;
    public final static int NOTHING_TO_DO = 2;
    public final static int ERROR = 1;
    public final static int SUCCESS = 0;


    private static void printHelp() {
        println "Main.class      -m|-f     -c|-r|-s|-e|-l     -a|-g"
        println "-m,--machine      Specified the exact directory for the VMware directory that you want to operate with"
        println "-f,--find         Instead of specifying an exact path with -m, this will look recursively for the first machine directory"

        println "-c,--create       Adds the specified machine with -m or -f to the VMware Inventory"
        println "-r,--remove       Removes the specified machine with -m or -f from the VMware Inventory"
        println "-s,--share        Moves one machine from the normal pool of machines (inventory.vmls), to Shared Machines (vmAutoStart.xml)"

        println "-a,--autostart    Used only with -s, tells to also add the moved machine to the vmAutoStart.xml, so that it will start automatically"
        println "-e,--echo         Just prints out the machine found with -m or -f"
        println "-l,--list         Lists all the machines in inventory.vmls, or in  vmInventory.xml when --global is specified"
        println "-g,--global       The operation will be performed against vmAutostart.xml in the Shared Machines pool"
    }

    public static void main(String[] args) {
        int exitCode=processMain(args)
        //println "exitCode ${exitCode}"
        System.exit(exitCode);
    }

    private static boolean assertExists(File vmDirPath) {
        if (vmDirPath == null || !vmDirPath.exists()) {
            println "could not find any virtual machine in the directory provided";
            return false;
        }
        return true;
    }

    public static int processMain(String[] args) {
        ArgsEngine engine = new ArgsEngine();
        // Configure the switches/options. Use true for valued options.
        engine.add("-m", "--machine", true);
        engine.add("-f", "--find", true);
        engine.add("-c", "--create", false);    //inserts the machine in to the properties repository, or the global one if --global
        engine.add("-r", "--remove", false);    //removes the specified machine
        engine.add("-g", "--global", false);    //any operation with this machine will be done against the global repository (xml)
        engine.add("-s", "--share", false);     //removes a machine from the properties inventory and add it to the xml inventory (global)
        engine.add("-a", "--autostart", false); //used together with --share, to add the shared machine to the vmAutoStart.xml file
        engine.add("-e", "--echo", false);      //just echoes the path of the vmx file (specially useful with --find)
        engine.add("-l", "--list", false);      //list the machines, if --global then list the global inventory, if not the properties inventory
        engine.add("-h", "--help", false);

        engine.parse(args);

        if (engine.getBoolean("-h")) {
            printHelp();
            return NOTHING_TO_DO;
        }

        File vmxFile;
        String findPath = engine.getString("-f");
        String path = engine.getString("-m");
        if (findPath != null && !findPath.isEmpty()) {
            List<File> vmxs = InstanceTools.findVM(new File(findPath));
            if(vmxs.size()>1) {
                println "multiple VMs found in the directory provided"
                return WARNING;
            } else if(vmxs.isEmpty()) {
                println "no VMs found"
                return WARNING;
            }
            vmxFile = vmxs.iterator().next();
        } else if (path != null && !path.isEmpty()) {
            vmxFile = new File(path);
        }

        GlobalInventory globalInventory = new GlobalInventory()
        PropertiesInventory propsInventory = new PropertiesInventory();
        Autostart autostart = new Autostart();

        //ECHO
        if (engine.getBoolean("-e")) {
            if(!assertExists(vmxFile))
                return WARNING;

            println vmxFile.toString();
        } else if (engine.getBoolean("-l")) {
            Inventory selected = propsInventory;
            if (engine.getBoolean("-g")) {
                selected = globalInventory;
            }
            selected.getInventory().forEach({ VMwareInstance inst ->
                println inst.getVmxFile().getAbsolutePath()
            })

            return SUCCESS;
        } else if (engine.getBoolean("-c")) {
            if(!assertExists(vmxFile))
                return WARNING;   //already exists

            if (engine.getBoolean("-g")) {
                globalInventory.addInstance(new VMwareInstance(vmxFile));
                logger.info("instance added into the global repository");
                globalInventory.write();
            } else {
                throw new OperationNotSupportedException();
            }

            // REMOVE
        } else if (engine.getBoolean("-r")) {
            if (engine.getBoolean("-g")) {
                //from global repository
                VMwareInstance instance = globalInventory.findInstance(vmxFile);
                if (instance != null) {
                    if (globalInventory.removeInstance(instance) == null) {
                        logger.error("Machine could not be found");
                        return ERROR;
                    }
                    logger.info("machine removed from the global repository");
                    globalInventory.write();

                    if(engine.getBoolean("-a")) {
                        autostart.removeInstance(instance.getObjId());
                        autostart.write();
                    }
                } else {
                    logger.error("machine with vmx path ${vmxFile.getAbsolutePath()} not found in the global repository");
                    return NOTHING_TO_DO;
                }
            } else {
                //throw new OperationNotSupportedException("not implemented yet")
                Integer id = propsInventory.findMachineIndex(vmxFile);
                if (id >= 0) {
                    if (!propsInventory.removeMachine(id)) {
                        logger.error("machine could NOT be found in the properties inventory");
                        return ERROR;
                    }
                    logger.info("machine removed from the properties inventory")
                    propsInventory.write();
                } else {
                    logger.warn("Machine not found in the repository");
                    return NOTHING_TO_DO;
                }
            }

            // SHARE
        } else if (engine.getBoolean("-s")) {
            if(!assertExists(vmxFile))
                return WARNING;

            Integer id = propsInventory.findMachineIndex(vmxFile);
            if (id >= 0) {
                logger.trace("machine found in propertiesInventory");
                VMwareInstance inst = propsInventory.getMachine(id)
                if (inst == null)
                    throw NullPointerException();
                globalInventory.addInstance(inst);
                globalInventory.write();
                if (propsInventory.removeMachine(id)) {
                    logger.trace("machine removed from propertiesInventory");
                    propsInventory.write();

                    //--AUTOSTART
                    if (engine.getBoolean("-a")) {
                        if (autostart.isMoidIn(inst.getObjId())) {
                            throw new IllegalStateException("MOID already existed in the autoStart file");
                        } else {
                            autostart.addInstance(inst);
                            autostart.write();
                        }
                    }
                }
                logger.info("done!")
            } else {
                logger.error("machine not found! specified path ${vmxFile.getAbsolutePath()}");
                return ERROR;
            }
        }
        return SUCCESS;
    }
}
