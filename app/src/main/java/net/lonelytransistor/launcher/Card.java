package net.lonelytransistor.launcher;

public class Card {
    public Callback cb;
    public interface Callback {
        void onClicked(Card card);
        void onHovered(Card card, boolean hovered);
    }
}
