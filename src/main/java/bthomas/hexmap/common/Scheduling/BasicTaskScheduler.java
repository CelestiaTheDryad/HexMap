package bthomas.hexmap.common.Scheduling;

import bthomas.hexmap.Main;
import bthomas.hexmap.logging.HexmapLogger;

import java.util.PriorityQueue;

/**
 This class allows for tasks to be scheduled for execution at arbitrary times in the future.
 */
public class BasicTaskScheduler
{
    private static final class TimerItem implements Comparable<TimerItem>
    {
        public final Runnable runner;
        public final long executionTime;

        public TimerItem(Runnable task, long executionTime)
        {
            this.runner = task;
            this.executionTime = executionTime;
        }

        @Override
        public int compareTo(TimerItem o)
        {
            return Long.compare(executionTime, o.executionTime);
        }
    }

    private boolean cancelled = false;

    private final PriorityQueue<TimerItem> items = new PriorityQueue<>();

    public BasicTaskScheduler()
    {
        Thread workerThread = new Thread(this::runWorker);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void cancel()
    {
        this.cancelled = true;
        synchronized(items)
        {
            items.notifyAll();
            items.clear();
        }
    }

    public void schedule(Runnable task, long executionTime)
    {
        if(task == null)
        {
            throw new NullPointerException("Attempt to schedule null task.");
        }
        if(executionTime < System.currentTimeMillis())
        {
            throw new IllegalArgumentException("Attempt to schedule task in the past.");
        }

        synchronized(items)
        {
            items.add(new TimerItem(task, executionTime));
            items.notifyAll();
        }
    }

    private void runWorker()
    {
        while(!cancelled)
        {
            TimerItem toExecute = null;
            synchronized(items)
            {
                while(toExecute == null || toExecute.executionTime > System.currentTimeMillis())
                {
                    toExecute = items.peek();
                    if(toExecute == null)
                    {
                        try
                        {
                            items.wait();
                        }
                        catch(InterruptedException e)
                        {
                            cancel();
                            break;
                        }
                    }
                    else
                    {
                        try
                        {
                            items.wait(toExecute.executionTime - System.currentTimeMillis());
                        }
                        catch(InterruptedException e)
                        {
                            cancel();
                            break;
                        }
                    }
                }
                toExecute = items.poll();
            }
            if(toExecute != null && !cancelled)
            {
                try
                {
                    toExecute.runner.run();
                }
                catch(Exception e)
                {
                    Main.logger.log(HexmapLogger.ERROR, HexmapLogger.getStackTraceString(e));
                }
            }
        }
    }
}
