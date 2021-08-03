package jrp.utils.packagefire.exceptions;

public class PackageException extends Exception {

    public PackageException(String e)
    {
        super(e);
    }
    public PackageException(Exception e)
    {
        super(e);
    }
}
