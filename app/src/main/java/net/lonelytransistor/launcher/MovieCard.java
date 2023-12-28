package net.lonelytransistor.launcher;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import net.lonelytransistor.launcher.repos.AllRepos;
import net.lonelytransistor.launcher.repos.ApkRepo;
import net.lonelytransistor.launcher.repos.JustWatch;
import net.lonelytransistor.launcher.repos.MovieTitle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MovieCard extends Card {
    public String title;
    public String desc;
    public Drawable statusIcon;
    public Uri mainImage;
    public int progressBar;
    public int score;
    public int popularity;
    public int popularityDelta;

    public MovieCard(MovieCard c) {
        title = c.title;
        desc = c.desc;
        statusIcon = c.statusIcon;
        mainImage = c.mainImage;
        progressBar = c.progressBar;
        score = c.score;
        popularity = c.popularity;
        popularityDelta = c.popularityDelta;
        clickIntent = c.clickIntent;
    }

    Intent clickIntent;
    public MovieCard(MovieTitle m) {
        title = m.title;
        if (m.originalTitle != null &&
                !m.originalTitle.isEmpty() &&
                JustWatch.jaccardSimilarity(title, m.originalTitle) < 90) {
            title += " - ";
            title += m.originalTitle;
        }
        desc = m.description;

        ApkRepo.Platform offer = ApkRepo.Platform.ERROR;
        for (ApkRepo.Platform platform : m.offers.keySet()) {
            offer = platform;
            break;
        }
        ApkRepo.App app = ApkRepo.getPlatformApp(offer);

        switch (m.system.state) {
            case WATCHING:
                progressBar = (int) (100 * m.system.timeWatched / (m.system.runtime > 0 ? m.system.runtime : 1));
                statusIcon = AllRepos.pausedIcon;
                break;
            case NEW:
                statusIcon = AllRepos.newIcon;
                break;
            case NEXT:
                statusIcon = AllRepos.nextIcon;
                break;
            case WATCHED:
                statusIcon = AllRepos.tickIcon;
                break;
            default:
                statusIcon = app != null ? app.icon : null;
                break;
        }
        mainImage = Uri.parse(m.imagePath);
        score = (int) (m.imdbScore*10);
        popularity = m.popularity;
        popularityDelta = m.popularityDelta > 0 ? R.drawable.popularity_up
                : m.popularityDelta < 0 ? R.drawable.popularity_down
                : R.drawable.popularity_same;

        clickIntent = new Intent();
        clickIntent.setAction(Intent.ACTION_VIEW);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            clickIntent.setData(Uri.parse(m.offers.get(offer)));
        } catch (Exception e) {
            Log.w("MovieCard", "Click intent failed: " + e + " : " + m.offers);
        }
    }
    public MovieCard(int id) {
        title = "title " + id + " lorem ipsum dolor";
        desc = id % 2 == 1 ? "desc\nrip\ntion\n" + id + "a quick\nbrown fox jumps over the lazy dog." : "aaa\nbbbb";
        statusIcon = null;
        mainImage = Uri.parse( "file:///data/data/net.lonelytransistor.launcher/test.png");
        progressBar = (id % 5) * 100 / 5;
        score = 80;
        popularity = 100;
        popularityDelta = R.drawable.popularity_same;

        clickIntent = new Intent();
    }
    public static List<MovieCard> from(List<MovieTitle> titles) {
        List<MovieCard> ret = new ArrayList<>();
        for (MovieTitle title : titles) {
            MovieCard card = new MovieCard(title);
            ret.add(card);
        }
        return ret;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieCard movieCard = (MovieCard) o;
        return progressBar == movieCard.progressBar && score == movieCard.score && popularity == movieCard.popularity && Objects.equals(title, movieCard.title) && Objects.equals(desc, movieCard.desc) && Objects.equals(statusIcon, movieCard.statusIcon) && Objects.equals(mainImage, movieCard.mainImage) && popularityDelta == movieCard.popularityDelta && Objects.equals(clickIntent, movieCard.clickIntent);
    }
    @Override
    public int hashCode() {
        return Objects.hash(title, desc, statusIcon, mainImage, progressBar, score, popularity, popularityDelta, clickIntent);
    }
    @NonNull
    @Override
    protected MovieCard clone() {
        return new MovieCard(this);
    }
}
