package fr.gaellalire.vestige.core.function;

public class TwoValues<L, R> {

    private L left;

    private R right;

    public TwoValues(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    public void setLeft(final L left) {
        this.left = left;
    }

    public void setRight(final R right) {
        this.right = right;
    }

}
