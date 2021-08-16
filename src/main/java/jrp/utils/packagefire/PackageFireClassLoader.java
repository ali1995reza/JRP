package jrp.utils.packagefire;

import jrp.utils.packagefire.exceptions.PackageLoadException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class PackageFireClassLoader extends URLClassLoader {

    private PackageLoadMode loadMode;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, PackageFile>> otherFiles;
    private PackageEntry[] entries;
    private ConcurrentHashMap<String, Set<String>> packageClasses;
    private Set<String> classes;

    protected PackageFireClassLoader(URL[] urls, ClassLoader parent, PackageEntry[] entries, PackageLoadMode mode) throws PackageLoadException {
        super(urls, parent);

        if (urls == null || urls.length < 1)
            throw new PackageLoadException("no jar file or class path set to load classes !");

        if (mode == null)
            throw new PackageLoadException("can not load packages without load mode . load mode is null !");

        loadMode = mode;
        this.entries = entries;
        packageClasses = new ConcurrentHashMap<>();
        classes = new CloseableHashSet<>();

        if (loadMode == PackageLoadMode.LOAD_TO_RAM) {
            otherFiles = new ConcurrentHashMap<>();

            for (PackageEntry packageEntry : entries) {
                FileFilter fileFilter = packageEntry.getFileFilter();
                CloseableHashSet<String> classSet = new CloseableHashSet<>();
                if (packageEntry.path().endsWith(".jar")) {
                    ConcurrentHashMap<String, PackageFile> entryFiles = new ConcurrentHashMap<>();
                    otherFiles.put(packageEntry.name(), entryFiles);
                    JarFile jarFile = null;
                    try {
                        jarFile = new JarFile(packageEntry.path());
                    } catch (IOException e) {
                        closeIt();
                        throw new PackageLoadException("no specific path find for package <" + packageEntry.name() + "> in path : " + packageEntry.path());
                    }
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry entry = jarEntries.nextElement();
                        if (!entry.isDirectory()) {
                            if (entry.getName().endsWith(".class")) {
                                String className = entry.getName().replaceAll("/", ".");
                                className = className.substring(0, className.length() - 6);
                                try {
                                    checkClassExistOrNot(className, packageEntry);
                                    loadClass(className);
                                    classSet.add(className);
                                } catch (ClassNotFoundException e) {
                                    try {
                                        jarFile.close();
                                    } catch (IOException e1) {
                                    }
                                    closeIt();
                                    throw new PackageLoadException("class " + className + " not found in package <"
                                            + packageEntry.name() + "> in path : " + packageEntry.path());
                                } catch (Exception e) {
                                    try {
                                        jarFile.close();
                                    } catch (IOException e1) {
                                    }
                                    closeIt();
                                    throw new PackageLoadException("an exception throw when load class " + className +
                                            " in package <" + packageEntry.name() + "> in path : " + packageEntry.path() + "\n exception : " + e.getMessage());
                                } catch (NoClassDefFoundError e) {

                                    try {
                                        jarFile.close();
                                    } catch (IOException e1) {
                                    }
                                    closeIt();
                                    throw new PackageLoadException("an 'Exception' throw when load class " + className +
                                            " in package <" + packageEntry.name() + "> in path : " + packageEntry.path() + "\n error : " + e.getMessage());
                                }
                            } else {
                                try {
                                    if (fileFilter != null) {
                                        if (!fileFilter.accept(packageEntry, entry.getName())) {
                                            continue;
                                        }
                                    }
                                    InputStream stream = jarFile.getInputStream(entry);
                                    byte[] fileData = readAllData(stream);
                                    String entryName = entry.getName();
                                    entryName = (entryName.startsWith("/") ? entryName : "/" + entryName);
                                    entryFiles.put(entryName, new PackageFile(entryName, fileData));
                                    stream.close();
                                } catch (IOException e) {
                                    try {
                                        jarFile.close();
                                    } catch (IOException e1) {
                                    }
                                    closeIt();

                                    throw new PackageLoadException("an error occurs when wants to load the file with name "
                                            + entry.getName() + " in package <" + packageEntry.name() +
                                            "> in path : " + packageEntry.path());
                                }
                            }
                        }
                    }

                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
                } else {
                    File root = new File(packageEntry.path());
                    if (!root.exists()) {
                        closeIt();
                        throw new PackageLoadException("path not exist for package "+packageEntry);
                    }

                    if (!root.isDirectory()) {
                        closeIt();
                        throw new PackageLoadException("bad path for package "+packageEntry);
                    }


                    otherFiles.put(packageEntry.name(), new ConcurrentHashMap<>());

                    try {
                        handleFile(root.getPath(), root, fileFilter, packageEntry,classSet);
                    } catch (PackageLoadException e) {
                        this.closeIt();
                        throw e;
                    }
                }
                classSet.close();
                packageClasses.put(packageEntry.name() , classSet);
                classes.addAll(classSet);
            }

            ((CloseableHashSet)classes).close();


            try {
                this.close();
            } catch (IOException e) {
            }
        } else {
            closeIt();
            throw new IllegalStateException("not implemented");
        }
    }

    public PackageFile getFile(String packageName, String filePath) throws FileNotFoundException {
        if (loadMode == PackageLoadMode.LOAD_TO_RAM) {
            ConcurrentHashMap<String, PackageFile> filesInPackage = otherFiles.get(packageName);
            if (filesInPackage == null) {
                throw new FileNotFoundException("no file saved for this package , or package not loaded ! !");
            }

            PackageEntry entry = null;
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].name().equals(packageName)) {
                    entry = entries[i];
                    break;
                }
            }

            if (entry == null)
                throw new FileNotFoundException("package <" + packageName + "> not loaded !");

            if (entry.getFileFilter() != null) {
                if (!entry.getFileFilter().accept(entry, filePath))
                    throw new FileNotFoundException("file not found because filtered as not accepted file !");
            }

            PackageFile packageFile = filesInPackage.get(filePath);
            if (packageFile == null) {
                throw new FileNotFoundException("file not found in package files !");
            } else {
                return packageFile;
            }

        } else {
            PackageEntry entry = null;
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].name().equals(packageName)) {
                    entry = entries[i];
                    break;
                }
            }

            if (entry == null)
                throw new FileNotFoundException("package <" + packageName + "> not loaded !");

            if (entry.path().endsWith(".jar")) {
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(entry.path());
                } catch (IOException e) {
                    throw new FileNotFoundException("jar file in package <" + packageName + "> not found in path : " + entry.path());
                }

                InputStream stream = null;
                byte[] fileData;
                try {
                    stream = jarFile.getInputStream(new JarEntry(filePath));
                    fileData = new byte[stream.available()];
                } catch (IOException e) {
                    throw new FileNotFoundException("file <" + filePath + "> not found in jar file in package <" + packageName + "> not found in path : " + entry.path());
                }

                int offset = 0;
                int len = fileData.length;
                try {
                    while (offset < len) {
                        int now = stream.read(fileData, offset, len - offset);
                        offset += now;
                    }
                } catch (IOException e) {
                    throw new FileNotFoundException("an exception occurs when reading file <" + filePath + "> in package <" +
                            entry.name() + "> in path : " + entry.path());
                }

                try {
                    jarFile.close();
                } catch (IOException e) {
                }

                try {
                    stream.close();
                } catch (IOException e) {
                }

                return new PackageFile(filePath, fileData);


            } else {
                File root = new File(entry.path());
                File thisFile = new File(filePath);
                File requestedFile = new File(root.getPath() + "\\" + thisFile.getPath());

                if (!requestedFile.exists())
                    throw new FileNotFoundException("file not found in package files !");

                if (entry.getFileFilter() != null) {
                    if (!entry.getFileFilter().accept(entry, filePath))
                        throw new FileNotFoundException("file not found because filtered as not accepted file !");
                }

                try {
                    byte[] bytes = Files.readAllBytes(Paths.get(requestedFile.getPath()));
                    PackageFile file = new PackageFile(filePath, bytes);
                    return file;
                } catch (IOException e) {
                    throw new FileNotFoundException("and exception occurs when read file !");
                }
            }
        }
    }


    protected void closeIt() {
        try {
            this.close();
        } catch (IOException e) {
        }

        if (otherFiles != null) {
            otherFiles.clear();
        }

        if (entries != null) {
            entries = null;
        }
    }


    private final void handleFile(String rootPath, File file, FileFilter fileFilter, PackageEntry packageEntry , Set<String> classSet) throws PackageLoadException {
        if (!file.exists()) {
            throw new PackageLoadException("file <" + file + "> not exist in package <" + packageEntry.name() + "> !");
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File newFile : files) {
                    handleFile(rootPath, newFile, fileFilter, packageEntry,classSet);
                }
            }
        } else {
            if (file.getName().endsWith(".class")) {
                String className = file.getPath().replace(rootPath + "\\", "").replaceAll("\\\\", ".");

                className = className.substring(0, className.length() - 6);
                try {
                    checkClassExistOrNot(className, packageEntry);
                    loadClass(className);
                    classSet.add(className);
                } catch (ClassNotFoundException e) {
                    throw new PackageLoadException("class <" + className + "> in package <" + packageEntry.name() + "> not found" +
                            " in path : " + packageEntry.path());
                } catch (Exception e) {
                    throw new PackageLoadException("an exception throw when load class " + className +
                            " in package <" + packageEntry.name() + "> in path : " + packageEntry.path() + "\n exception : " + e.getMessage());
                } catch (NoClassDefFoundError e) {
                    throw new PackageLoadException("an 'Error' throw when load class " + className +
                            " in package <" + packageEntry.name() + "> in path : " + packageEntry.path() + "\n error : " + e.getMessage());
                }
            } else {
                if (fileFilter != null) {
                    if (!fileFilter.accept(packageEntry, file.getPath())) {
                        return;
                    }
                }

                String fileName = file.getPath().replace(rootPath, "").replaceAll("\\\\", "/");

                try {
                    byte[] fileData = Files.readAllBytes(Paths.get(file.toURI()));
                    ConcurrentHashMap<String, PackageFile> packageFiles = otherFiles.get(packageEntry.name());

                    packageFiles.put(fileName, new PackageFile(fileName, fileData));
                } catch (IOException e) {
                    throw new PackageLoadException("can not load file <" + fileName + "> in package <" + packageEntry.name() + "> in path : " +
                            packageEntry.path());
                }
            }
        }

    }

    @Override
    protected void finalize() throws Throwable {
        closeIt();
        super.finalize();
    }


    public Set<String> loadedClasses()
    {
        return classes;
    }

    public Set<String> loadedClasses(String packageEntry)
    {
        Set<String> loadedClasses = packageClasses.get(packageEntry);
        return loadedClasses;
    }


    private byte[] readAllData(InputStream fileStream) throws IOException {
        byte[] fileData = new byte[fileStream.available()];
        int len = fileData.length;
        int offset = 0;
        while (offset < len) {
            int now = fileStream.read(fileData, offset, len - offset);
            offset += now;
        }

        return fileData;
    }

    private void checkClassExistOrNot(String className, PackageEntry loader) {
        for (String entryName : packageClasses.keySet()) {
            Set<String> classes = packageClasses.get(entryName);
            for (String otherEntryClassName : classes) {
                if (className.equals(otherEntryClassName)) {
                    throw new IllegalStateException("entry <"
                            + loader.name()
                            + "> try to load class <"
                            + className
                            + "> when a class already loaded with this name by entry <"
                            + entryName
                            + "> ");
                }
            }
        }
    }
}


