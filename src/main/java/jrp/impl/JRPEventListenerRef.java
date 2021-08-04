package jrp.impl;

import jrp.api.JRPEventListener;
import jrp.api.JRPSession;

public class JRPEventListenerRef implements JRPEventListener {


    private JRPEventListener wrapped = EMPTY;

    public void setRef(JRPEventListener wrapped) {
        this.wrapped = wrapped == null ? EMPTY : wrapped;
    }

    @Override
    public void onSessionOpened(JRPSession session) {
        wrapped.onSessionOpened(session);
    }

    @Override
    public void onSessionClosed(JRPSession closedSession) {
        wrapped.onSessionClosed(closedSession);
    }
}
