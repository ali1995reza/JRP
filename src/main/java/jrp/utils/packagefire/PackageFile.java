package jrp.utils.packagefire;

public class PackageFile {
    String fileName;
    byte[] bytesOfFile;

    public PackageFile(String fileName , byte[] bytesOfFile)
    {
        this.fileName = fileName;
        this.bytesOfFile = bytesOfFile;
    }

    public byte[] bytes() {
        return bytesOfFile.clone();
    }

    public String name() {
        return fileName;
    }
}
