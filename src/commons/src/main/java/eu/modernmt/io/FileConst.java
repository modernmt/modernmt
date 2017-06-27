package eu.modernmt.io;

import java.io.File;

/**
 * Created by davide on 01/09/16.
 */
public class FileConst {

    private static final String SYSPROP_MMT_HOME = "mmt.home";

    private static File _home = null;
    private static File _resourcePath = null;
    private static File _enginesPath = null;
    private static File _runtimePath = null;
    private static File _vendorPath = null;
    private static File _libPath = null;

    private static File getHome() {
        if (_home == null) {
            String home = System.getProperty(SYSPROP_MMT_HOME);
            if (home == null)
                throw new IllegalStateException("The system property '" + SYSPROP_MMT_HOME + "' must be initialized to the path of MMT installation.");

            File homeFile = new File(home).getAbsoluteFile();
            if (!homeFile.isDirectory())
                throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + homeFile + " must be a valid directory.");

            _home = homeFile;
        }

        return _home;
    }

    public static File getVendorPath() {
        if (_vendorPath == null) {
            File vendor = Paths.join(getHome(), "vendor");
            if (!vendor.isDirectory())
                throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + vendor + " must be a valid directory.");

            _vendorPath = vendor;
        }

        return _vendorPath;
    }

    public static File getResourcePath() {
        if (_resourcePath == null) {
            File resources = Paths.join(getHome(), "build", "res");
            if (!resources.isDirectory())
                throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + resources + " must be a valid directory.");

            _resourcePath = resources;
        }

        return _resourcePath;
    }

    public static File getLibPath() {
        if (_libPath == null) {
            File lib = Paths.join(getHome(), "build", "lib");
            if (!lib.isDirectory())
                throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + lib + " must be a valid directory.");

            _libPath = lib;
        }

        return _libPath;
    }

    public static File getEngineRoot(String engine) {
        if (_enginesPath == null) {
            File engines = new File(getHome(), "engines");
            if (!engines.isDirectory())
                throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + engines + " must be a valid directory.");

            _enginesPath = engines;
        }

        return new File(_enginesPath, engine);
    }

    public static File getEngineRuntime(String engine) {
        if (_runtimePath == null) {
            File runtime = new File(getHome(), "runtime");
            if (!runtime.isDirectory())
                throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + runtime + " must be a valid directory.");

            _runtimePath = runtime;
        }

        return new File(_runtimePath, engine);
    }

}
