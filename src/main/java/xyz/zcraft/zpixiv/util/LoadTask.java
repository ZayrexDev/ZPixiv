package xyz.zcraft.zpixiv.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.zcraft.zpixiv.ui.Main;

import java.util.LinkedList;
import java.util.function.Consumer;

@Getter
@RequiredArgsConstructor
public class LoadTask {
    private final String name;
    private final LinkedList<Consumer<Double>> changeListeners = new LinkedList<>();
    private volatile double progress = -1;
    private volatile boolean failed = false;
    private volatile boolean finished = false;

    public void setProgress(double progress) {
        this.progress = progress;
        fireChanged();
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
        fireChanged();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        fireChanged();
    }

    public void addListener(Consumer<Double> lis) {
        changeListeners.add(lis);
    }

    private void fireChanged() {
        Main.getTpe().submit(() -> changeListeners.forEach(e -> e.accept(progress)));
    }

    @Override
    public String toString() {
        return "LoadTask{" +
                "name='" + name + '\'' +
                ", progress=" + progress +
                ", failed=" + failed +
                ", finished=" + finished +
                '}';
    }
}
