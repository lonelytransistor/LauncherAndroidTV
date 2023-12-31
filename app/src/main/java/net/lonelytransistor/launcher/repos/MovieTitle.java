package net.lonelytransistor.launcher.repos;


import android.content.Context;
import android.graphics.drawable.Drawable;

import net.lonelytransistor.launcher.R;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MovieTitle implements Serializable {
    final public int id;
    final public JustWatch.Type type;

    final public String title;
    final public String originalTitle;
    final public String description;
    final public String imagePath;
    final public String imageUrl;
    final public int runtime;
    final public JustWatch.AgeRating ageRating;
    final public JustWatch.Genre[] genres;

    final public String[] productionCountries;
    final public Map<String,String> actors;
    final public double imdbScore;
    final public int popularity;
    final public int popularityDelta;
    final public int year;

    final public Map<ApkRepo.Platform, String> offers;

    public static class System implements Serializable {
        public String pkgName = "";
        public long lastWatched = 0; //in milliseconds
        public long timeWatched = 0;
        public long runtime = 0;
        public enum State {
            NONE,
            NEW,
            NEXT,
            WATCHING,
            WATCHED
        }
        public State state = State.NONE;

        @Override
        public String toString() {
            return "System{" +
                    "pkgName='" + pkgName + '\'' +
                    ", lastWatched=" + lastWatched +
                    ", timeWatched=" + timeWatched +
                    ", runtime=" + runtime +
                    ", state=" + state +
                    '}';
        }
    }
    final public System system = new System();

    private static Drawable POPULARITY_UP = null;
    private static Drawable POPULARITY_SAME = null;
    private static Drawable POPULARITY_DOWN = null;
    static void init(Context ctx) {
        if (POPULARITY_UP == null) {
            POPULARITY_UP = ctx.getDrawable(R.drawable.popularity_up);
        }
        if (POPULARITY_SAME == null) {
            POPULARITY_SAME = ctx.getDrawable(R.drawable.popularity_same);
        }
        if (POPULARITY_DOWN == null) {
            POPULARITY_DOWN = ctx.getDrawable(R.drawable.popularity_down);
        }
    }
    public Drawable getPopularityDeltaImage() {
        return popularityDelta > 0 ? POPULARITY_UP : (popularityDelta < 0 ? POPULARITY_DOWN : POPULARITY_SAME);
    }
    public Drawable getImage() {
        return Drawable.createFromPath(imagePath);
    }
    public int getTimeWatched() {
        return 0;
    }

    public MovieTitle(MovieTitle priv) {
        id = priv.id;
        type = priv.type;
        title = priv.title;
        originalTitle = priv.originalTitle;
        description = priv.description;
        imagePath = priv.imagePath;
        imageUrl = priv.imageUrl;
        runtime = priv.runtime;
        ageRating = priv.ageRating;
        genres = priv.genres != null ? priv.genres.clone() : new JustWatch.Genre[]{};
        productionCountries = priv.productionCountries != null ? priv.productionCountries.clone() : new String[]{};
        actors = priv.actors;
        imdbScore = priv.imdbScore;
        popularity = priv.popularity;
        popularityDelta = priv.popularityDelta;
        year = priv.year;
        offers = priv.offers;
    }
    public MovieTitle(MovieTitlePriv priv) {
        id = priv.id;
        type = priv.type;
        title = priv.title;
        originalTitle = priv.originalTitle;
        description = priv.description;
        imagePath = priv.imagePath;
        imageUrl = priv.imageUrl;
        runtime = priv.runtime;
        ageRating = priv.ageRating;
        genres = priv.genres;
        productionCountries = priv.productionCountries;
        actors = priv.actors;
        imdbScore = priv.imdbScore;
        popularity = priv.popularity;
        popularityDelta = priv.popularityDelta;
        year = priv.year;
        offers = priv.offers;
    }

    @Override
    public String toString() {
        return "MovieTitle{" +
                "id=" + id +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", originalTitle='" + originalTitle + '\'' +
                ", description='" + description + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", runtime=" + runtime +
                ", ageRating=" + ageRating +
                ", genres=" + Arrays.toString(genres) +
                ", productionCountries=" + Arrays.toString(productionCountries) +
                ", actors=" + actors +
                ", imdbScore=" + imdbScore +
                ", popularity=" + popularity +
                ", popularityDelta=" + popularityDelta +
                ", year=" + year +
                ", offers=" + offers +
                ", system=" + system +
                '}';
    }

    public MovieTitle(int id) {
        this.id = id;
        type = JustWatch.Type.ERROR;
        title = "title" + id;
        originalTitle = "originalTitle" + id;
        description = "description" + id;
        imagePath = "";
        imageUrl = "" + id;
        runtime = 3600;
        ageRating = JustWatch.AgeRating.values()[id % JustWatch.AgeRating.values().length];
        genres = new JustWatch.Genre[]{};
        productionCountries = new String[]{};
        actors = new HashMap<>();
        imdbScore = 0;
        popularity = 0;
        popularityDelta = 0;
        year = 1900;
        offers = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieTitle that = (MovieTitle) o;
        return id == that.id && type == that.type && Objects.equals(title, that.title) && Objects.equals(originalTitle, that.originalTitle) && Objects.equals(description, that.description);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id, type, title, originalTitle, description);
    }
}
