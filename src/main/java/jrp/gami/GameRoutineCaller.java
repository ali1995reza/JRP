package jrp.gami;

import jrp.gami.components.Game;

import java.util.concurrent.*;

public class GameRoutineCaller {

    private final ScheduledExecutorService executorService;
    private final ConcurrentHashMap<Game, ScheduledFuture> futures = new ConcurrentHashMap<>();

    public GameRoutineCaller(int size) {
        this.executorService = Executors.newScheduledThreadPool(size);
    }

    public void register(Game game) {
        ScheduledFuture future = this.executorService.scheduleWithFixedDelay(game::routine,
                1, 1, TimeUnit.SECONDS);
        futures.put(game, future);
    }

    public void deregister(Game game) {
        ScheduledFuture future = futures.remove(game);
        if(future!=null) {
            future.cancel(false);
        }
    }

}
