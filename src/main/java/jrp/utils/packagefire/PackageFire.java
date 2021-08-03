package jrp.utils.packagefire;

import jrp.utils.packagefire.exceptions.PackageBurnedException;
import jrp.utils.packagefire.exceptions.PackageLoadException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

public class PackageFire {

    private PackageFireClassLoader classLoader;
    private boolean burned;
    private Object _sync = new Object();

    public PackageFire(ClassLoader parent , PackageLoadMode loadMode , PackageEntry ... entries) throws PackageLoadException {
        if(entries==null)
            throw new IllegalArgumentException("null entries set to load !");
        if(entries.length<0)
            throw new IllegalArgumentException("at least one entry needed to load !");

        for(int i=0;i<entries.length;i++)
        {
            PackageEntry entry = entries[i];
            for(int j=i+1;j<entries.length;j++)
            {
                PackageEntry otherEntry = entries[j];
                if(entry.name().equals(otherEntry.name()))
                {
                    throw new PackageLoadException("2 packages with the same name <"+entry.name()+"> could not to load !");
                }

                if(entry.path().equals(otherEntry.path()))
                {
                    throw new PackageLoadException("2 packages <"+entry.name()+"> and <"+otherEntry.name()+
                            "> have same path : "+entry.path());
                }
            }
        }

        URL[] urls = new URL[entries.length];
        for(int i=0;i<entries.length;i++)
        {
            try {
                urls[i] = new File(entries[i].path()).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new PackageLoadException("package <"+entries[i].name()+"> has a bad path : "+entries[i].path());
            }
        }
        classLoader = new PackageFireClassLoader(urls,parent, entries,loadMode);
    }

    public PackageFire(PackageLoadMode loadMode , PackageEntry ... entries) throws PackageLoadException {
        this(null,loadMode,entries);
    }


    public <T> Class<T> loadClass(String className)throws PackageBurnedException,ClassNotFoundException
    {
        synchronized (_sync)
        {
            if(burned)
            {
                throw new PackageBurnedException("this package burned !");
            }

            Class<T> tClass = (Class<T>)classLoader.loadClass(className);
            return tClass;
        }
    }

    public PackageFile getFile(String packageName , String filePath)throws FileNotFoundException,PackageBurnedException
    {
        synchronized (_sync) {
            if(burned)
            {
                throw new PackageBurnedException("this package burned !");
            }
            filePath = (filePath.startsWith("/")?filePath:"/"+filePath);
            return classLoader.getFile(packageName, filePath);
        }
    }


    public void burn()throws PackageBurnedException
    {
        synchronized (_sync)
        {
            if(burned)
                throw new PackageBurnedException("this package burned already !");
            burned = true;
            classLoader.closeIt();
        }
    }

    public URLClassLoader classLoader() {
        return classLoader;
    }

    public Set<String> loadedClasses()
    {
        return classLoader.loadedClasses();
    }

    public Set<String> loadedClasses(String entryName)
    {
        return classLoader.loadedClasses(entryName);
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            burn();
        }catch (PackageBurnedException e)
        {

        }
        super.finalize();
    }
}
