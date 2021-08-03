package jrp.utils.packagefire;

public interface FileFilter {

    boolean accept(PackageEntry entry, String filePath);
}
