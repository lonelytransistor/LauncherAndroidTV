package net.lonelytransistor.launcher;

import androidx.annotation.NonNull;

import java.util.Objects;

public class BadgeCard extends Card {
    public String title;
    public Object badgeImage;
    public String badgeText;
    public Object mainImage;
    public Object statusIcon;
    public int badgeColor;
    public int backgroundColor;

    public BadgeCard(String title,
                     String badgeText, Object badgeImage, int badgeColor, int backgroundColor,
                     Object mainImage, Object statusIcon, Callback cb) {
        this.title = title;
        this.statusIcon = statusIcon;
        this.mainImage = mainImage;
        this.badgeImage = badgeImage;
        this.badgeText = badgeText;
        this.badgeColor = badgeColor;
        this.backgroundColor = backgroundColor;
        this.cb = cb;
    }
    public BadgeCard(String title,
                     String badgeText, Object badgeImage, int badgeColor, int backgroundColor, Callback cb) {
        this(title, badgeText, badgeImage, badgeColor, backgroundColor, null, null, cb);
    }
    public BadgeCard(String title, Object mainImage, Callback cb) {
        this(title, null, null, 0, 0, mainImage, null, cb);
    }
    public BadgeCard(String title, Object mainImage, Object statusIcon, Callback cb) {
        this(title, null, null, 0, 0, mainImage, statusIcon, cb);
    }
    public BadgeCard(BadgeCard c) {
        this.title = c.title;
        this.statusIcon = c.statusIcon;
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
        BadgeCard badgeCard = (BadgeCard) o;
        return badgeColor == badgeCard.badgeColor && backgroundColor == badgeCard.backgroundColor && Objects.equals(title, badgeCard.title) && Objects.equals(badgeImage, badgeCard.badgeImage) && Objects.equals(badgeText, badgeCard.badgeText) && Objects.equals(mainImage, badgeCard.mainImage) && Objects.equals(statusIcon, badgeCard.statusIcon);
    }
    @Override
    public int hashCode() {
        return Objects.hash(title, badgeImage, badgeText, mainImage, statusIcon, badgeColor, backgroundColor);
    }
    @NonNull
    @Override
    protected BadgeCard clone() {
        return new BadgeCard(this);
    }
}
