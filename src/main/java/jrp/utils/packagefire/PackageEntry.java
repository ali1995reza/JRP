package jrp.utils.packagefire;


public class PackageEntry {

    private String path;
    private String entryName;
    private FileFilter fileFilter;

    public PackageEntry(String entryName,String path)
    {
        this.path = path;
        this.entryName = entryName;
    }

    public PackageEntry(String entryName,String path , FileFilter fileFilter)
    {
        this.path = path;
        this.entryName = entryName;
        this.fileFilter = fileFilter;
    }

    public void setFileFilter(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public String path() {
        return path;
    }

    public String name() {
        return entryName;
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    @Override
    public String toString() {
        return "[name:"+entryName+" , path:"+path+"]";
    }
}
