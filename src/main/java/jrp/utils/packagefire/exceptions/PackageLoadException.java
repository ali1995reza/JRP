package jrp.utils.packagefire.exceptions;

public class PackageLoadException extends PackageException {
    public PackageLoadException(String e) {
        super(e);
    }


    public PackageLoadException(Exception e) {
        super(e);
    }
}
