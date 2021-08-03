package jrp.utils.packagefire;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

class CloseableHashSet<T> extends HashSet<T> {

    private boolean closed = false;


    @Override
    public boolean add(T t) {
        if(closed)
            throw new IllegalStateException("impossible to add more element to set");
        return super.add(t);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if(closed)
            throw new IllegalStateException("impossible to add more element to set");
        return super.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
        if(closed)
            throw new IllegalStateException("impossible to remove element from set");
        return super.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if(closed)
            throw new IllegalStateException("impossible to remove element from set");
        return super.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        if(closed)
            throw new IllegalStateException("impossible to remove element from set");
        return super.removeIf(filter);
    }

    @Override
    public void clear() {
        if(closed)
            throw new IllegalStateException("impossible clear elements");
        super.clear();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if(closed)
            throw new IllegalStateException("impossible to retain elements");
        return super.retainAll(c);
    }

    public void close()
    {
        closed = true;
    }
}
