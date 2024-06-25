package eu.virtualparadox.springembedder;

/**
 * Util class for calculating the method execution elapsed time.
 */
import java.util.concurrent.TimeUnit;

public class TimeWatch {
    private final TickSupplier nanoTimeSupplier;

    private long starts;

    private TimeWatch(final TickSupplier nanoTimeSupplier) {
        this.nanoTimeSupplier = nanoTimeSupplier;

        this.reset();
    }

    public static TimeWatch start() {
        return new TimeWatch(System::nanoTime);
    }

    static TimeWatch start(final TickSupplier nanoTimeSupplier) {
        return new TimeWatch(nanoTimeSupplier);
    }

    public TimeWatch reset() {
        this.starts = nanoTimeSupplier.nanoTime();
        return this;
    }

    public long time() {
        final long ends = nanoTimeSupplier.nanoTime();
        return ends - this.starts;
    }

    public long time(final TimeUnit unit) {
        return unit.convert(this.time(), TimeUnit.NANOSECONDS);
    }

    public String toMinuteSeconds() {
        return String.format("%d min, %d sec", this.time(TimeUnit.MINUTES),
                this.time(TimeUnit.SECONDS) - (this.time(TimeUnit.MINUTES) * 60));
    }

    public String toMilliSeconds() {
        return String.format("%d ms", this.time(TimeUnit.MILLISECONDS));
    }

    public String toSeconds() {
        return String.format("%d sec", this.time(TimeUnit.SECONDS));
    }

    public interface TickSupplier {
        long nanoTime();
    }
}