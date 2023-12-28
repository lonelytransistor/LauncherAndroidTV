package net.lonelytransistor.launcher;

import androidx.annotation.NonNull;

import java.util.Objects;

public class BadgeCard extends Card {
    public String title;
    public Object badgeImage;
    public String badgeText;
    public Object mainImage;
    public int badgeColor;
    public int backgroundColor;

    public BadgeCard(String title,
                     String badgeText, Object badgeImage, int badgeColor, int backgroundColor,
                     Object mainImage, Callback cb) {
        this.title = title;
        this.mainImage = mainImage;
        this.badgeImage = badgeImage;
        this.badgeText = badgeText;
        this.badgeColor = badgeColor;
        this.backgroundColor = backgroundColor;
        this.cb = cb;
    }
    public BadgeCard(String title,
                     String badgeText, Object badgeImage, int badgeColor, int backgroundColor, Callback cb) {
        this(title, badgeText, badgeImage, badgeColor, backgroundColor, null, cb);
    }
    public BadgeCard(String title, Object mainImage, Callback cb) {
        this(title, null, null, 0, 0, mainImage, cb);
    }
    public BadgeCard(BadgeCard c) {
        this.title = c.title;
        this.mainImage = c.mainImage;
        this.badgeImage = c.badgeImage;
        this.badgeText = c.badgeText;
        this.badgeColor = c.badgeColor;
        this.backgroundColor = c.backgroundColor;
        this.cb = c.cb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BadgeCard card = (BadgeCard) o;
        return Objects.equals(title, card.title) && Objects.equals(badgeImage, card.badgeImage) && Objects.equals(badgeText, card.badgeText) && Objects.equals(mainImage, card.mainImage) && Objects.equals(cb, card.cb);
    }
    @Override
    public int hashCode() {
        return Objects.hash(title, badgeImage, badgeText, mainImage, cb);
    }
    @NonNull
    @Override
    protected BadgeCard clone() {
        return new BadgeCard(this);
    }
}
