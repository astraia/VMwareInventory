package com.astraia.vmware

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by Ruben on 23.05.2017.
 */
class InstanceTools {
    final private static Logger logger = LoggerFactory.getLogger(InstanceTools.class);

    public static File getVmxFile(File inDir) {
        if(!inDir.isDirectory())
            throw new IllegalStateException("${inDir} does not exist");

        File[] res = inDir.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                if(name.endsWith(".vmx"))
                    return true;
                return false
            }
        })
        if(res.length==0) {
            throw new VmxFileNotFound("VMX file not found in ${inDir}")
        } else if(res.length>1) {
            throw new MultipleVmxFiles("multiple VMX files found for ${inDir}")
        }
        return res[0];
    }

    public static File findFirstVM(File dir) {
        Optional<Path> p = Files.walk(dir.toPath()).filter({
            Path path ->
                File f = path.toFile();
                if(f.isDirectory()) {
                    try {
                        getVmxFile(f);
                        logger.trace("vm found in ${f.getAbsolutePath()}")
                        return true;
                    } catch(VmxFileNotFound e) {
                        //nothing, continue
                    } catch(MultipleVmxFiles m) {
                        logger.error("Multiple VMX files inside ${f}")
                    }
                }
                return false;
        }).findFirst()
        return p.get().toFile();
    }

    public static boolean isMoidAutostart(int moid) {
    }

    public static class VmxFileNotFound extends Exception {
        public VmxFileNotFound(String msg) {super(msg)}
    }

    public static class MultipleVmxFiles extends Exception {
        public MultipleVmxFiles(String msg) {super(msg)}
    }
}
