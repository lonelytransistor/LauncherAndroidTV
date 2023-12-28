package net.lonelytransistor.launcher.repos;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class MovieTitlePriv {
    int year;
    String imagePath;
    String originalTitle;
    JustWatch.AgeRating ageRating;
    String[] productionCountries;
    Map<String,String> actors = new HashMap<>();
    int popularity;
    int popularityDelta;
    JustWatch.Genre[] genres;
    Map<ApkRepo.Platform, String> offers = new HashMap<>();
    int id;
    JustWatch.Type type;
    String title;
    String description;
    String imageUrl;
    int runtime;
    double imdbScore;

    @Override
    public String toString() {
        return "TitlePriv{" +
                "originalTitle='" + originalTitle + '\'' +
                ", ageRating=" + ageRating +
                ", productionCountries=" + Arrays.toString(productionCountries) +
                ", actors=" + actors +
                ", popularity=" + popularity +
                ", popularityDelta=" + popularityDelta +
                ", genres=" + Arrays.toString(genres) +
                ", offers=" + offers +
                ", id=" + id +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", runtime=" + runtime +
                ", imdbScore=" + imdbScore +
                '}';
    }
}
