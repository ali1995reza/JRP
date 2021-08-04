package jrp.api;

public interface JRPEventListener {

    JRPEventListener EMPTY = new JRPEventListener() {
        @Override
        public void onSessionOpened(JRPSession session) {

        }

        @Override
        public void onSessionClosed(JRPSession closedSession) {

        }
    };

    void onSessionOpened(JRPSession session);

    void onSessionClosed(JRPSession closedSession);

}
