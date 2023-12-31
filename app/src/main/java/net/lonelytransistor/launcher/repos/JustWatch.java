package net.lonelytransistor.launcher.repos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;

import net.lonelytransistor.launcher.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

public class JustWatch {
    private static final String TAG = "JustWatch";
    private static final String IMAGES_URL = "https://images.justwatch.com";
    private static final String GRAPHQL_URL = "https://apis.justwatch.com/graphql";
    private static final String GetPopularTitlesSmall_graphql = """
            query GetPopularTitles(
                $country: Country!
                $titlesFilter: TitleFilter
                $sortBy: PopularTitlesSorting! = POPULAR
                $offset: Int! = 0
                $first: Int! = 40
                $language: Language! = "en"
            ) {
                popularTitles(
                    country: $country
                    filter: $titlesFilter
                    after: ""
                    offset: $offset
                    sortBy: $sortBy
                    first: $first
                    sortRandomSeed: 0
                ) {
                    edges {
                        ...PopularTitleGraphql
                    }
                }
            }
            fragment PopularTitleGraphql on PopularTitlesEdge {
                node {
                    objectId
                    objectType
                    content(
                        country: $country
                        language: $language
                    ) {
                        isReleased
                    }
                    popularityRank(
                        country: $country
                    ) {
                        rank
                        trend
                        trendDifference
                    }
                }
            }""";
    private static final String GetPopularTitles_graphql = """
            query GetPopularTitles(
                $country: Country!
                $titlesFilter: TitleFilter
                $offersFilter: OfferFilter!
                $sortBy: PopularTitlesSorting! = POPULAR
                $offset: Int! = 0
                $first: Int! = 40
                $language: Language!
            ) {
                popularTitles(
                    country: $country
                    filter: $titlesFilter
                    after: ""
                    offset: $offset
                    sortBy: $sortBy
                    first: $first
                    sortRandomSeed: 0
                ) {
                    edges {
                        ...PopularTitleGraphql
                    }
                }
            }
            fragment SimilarTitle on MovieOrShow {
                objectId
                objectType
                content(
                    country: $country
                    language: $language
                ) {
                    isReleased
                    title
                    originalReleaseYear
                    runtime
                    scoring {
                        imdbScore
                        tmdbScore
                    }
                    genres {
                        shortName
                    }
                    fullPosterUrl: posterUrl(
                        profile: S718
                        format: JPG
                    )
                    ageCertification
                }
            }
            fragment PopularTitleGraphql on PopularTitlesEdge {
                node {
                    objectId
                    objectType
                    content(
                        country: $country
                        language: $language
                    ) {
                        isReleased
                        title
                        shortDescription
                        originalReleaseYear
                        runtime
                        scoring {
                            imdbScore
                            tmdbScore
                        }
                        genres {
                            shortName
                        }
                        fullPosterUrl: posterUrl(
                            profile: S718
                            format: JPG
                        )
                        ... on MovieOrShowContent {
                            ageCertification
                            originalTitle
                            productionCountries
                            starring: credits(
                                role: ACTOR
                                first: 5
                            ) {
                                name
                                characterName
                            }
                        }
                    }
                    popularityRank(
                        country: $country
                    ) {
                        rank
                        trend
                        trendDifference
                    }
                    offers(
                        country: $country
                        platform: WEB
                        filter: $offersFilter
                    ) {
                        standardWebURL
                        package {
                            shortName
                        }
                    }
                    ... on MovieOrShow {
                        similarTitlesV2(
                            country: $country
                            filter: $titlesFilter
                        ) {
                            edges {
                                node {
                                    ...SimilarTitle
                                }
                            }
                        }
                    }
                    ... on Show {
                        totalSeasonCount
                        recentEpisodes: episodes(
                            sortDirection: DESC
                            limit: 3
                            releasedInCountry: $country
                        ) {
                            content(
                                country: $country
                                language: $language
                            ) {
                                title
                                shortDescription
                                episodeNumber
                                seasonNumber
                                isReleased
                            }
                        }
                    }
                }
            }""";

    public enum SortOrder {
        RANDOM,
        TRENDING,
        POPULAR,
        ALPHABETICAL,
        RELEASE_YEAR,
        TMDB_POPULARITY,
        IMDB_SCORE
    }
    private static String[] SortOrderStr = null;
    private static String getSortOrderStr(SortOrder order) {
        return SortOrderStr[order.ordinal()];
    }
    private static String getSortOrderJSON(SortOrder order) {
        return order.name();
    }
    private static final OkHttpClient httpClient = new OkHttpClient();

    public enum Genre {
        ROMANCE,         WESTERN,    DRAMA,
        THRILLER,        REALITY_TV, HORROR,
        EUROPEAN,        MUSICAL,    ACTION_ADVENTURE,
        FANTASY,         COMEDY,     SPORT,
        CRIME,           FAMILY,     DOCUMENTARY,
        SCIENCE_FICTION, ANIMATED,   HISTORICAL,
        WAR,
        ERROR
    }
    private static String[] GenreStr = null;
    private static Genre getGenre(String genreStr) {
        List<String> data = Arrays.asList(
                "rma", "wsn", "drm",
                "trl", "rly", "hrr",
                "eur", "msc", "act",
                "fnt", "cmy", "spt",
                "crm", "fml", "doc",
                "scf", "ani", "hst",
                "war",
                "ERROR"
        );
        for (Genre genre : Genre.values()) {
            if (data.indexOf(getGenreCode(genre)) == data.indexOf(genreStr)) {
                return genre;
            } else if (getGenreStr(genre).equals(genreStr)) {
                return genre;
            }
        }
        return Genre.ERROR;
    }
    private static String getGenreStr(Genre genre) {
        return GenreStr[genre.ordinal()];
    }
    private static String getGenreCode(Genre genre) {
        String[] data = {
                "rma", "wsn", "drm",
                "trl", "rly", "hrr",
                "eur", "msc", "act",
                "fnt", "cmy", "spt",
                "crm", "fml", "doc",
                "scf", "ani", "hst",
                "war",
                "ERROR"
        };
        return data[genre.ordinal()];
    }
    private static JSONArray getGenreJSON(Genre[] genres) {
        JSONArray ret = new JSONArray();
        for (Genre genre : genres) {
            ret.put(getGenreCode(genre));
        }
        return ret;
    }

    public enum AgeRating {
        G, PG_13, MA_16, NC_17, R,
        ERROR
    }
    private static AgeRating getAgeRating(String ratingStr) {
        switch (ratingStr) {
            case "G":
            case "PG":
                return AgeRating.G;
            case "PG-13":
                return AgeRating.PG_13;
            case "16":
                return AgeRating.MA_16;
            case "NC-17":
                return AgeRating.NC_17;
            case "R":
            case "18":
                return AgeRating.R;
        }
        List<String> data = Arrays.asList("0+", "13+", "16+", "17+", "18+", "ERROR");
        for (AgeRating rating : AgeRating.values()) {
            if (getAgeRatingStr(rating).equals(ratingStr)) {
                return rating;
            }
        }
        return AgeRating.ERROR;
    }
    private static String getAgeRatingStr(AgeRating rating) {
        String[] data = {"0+", "13+", "16+", "17+", "18+", "ERROR"};
        return data[rating.ordinal()];
    }
    private static JSONArray getAgeRatingJSON(AgeRating[] ratings) {
        JSONArray ret = new JSONArray();
        for (AgeRating rating : ratings) {
            switch (rating) {
                case G -> {
                    ret.put("G");
                    ret.put("PG");
                }
                case PG_13 -> ret.put("PG-13");
                case MA_16 -> ret.put("16");
                case NC_17 -> ret.put("NC-17");
                case R -> {
                    ret.put("18");
                    ret.put("R");
                }
            }
        }
        return ret;
    }

    public enum Type {
        MOVIE,
        SERIES,
        ERROR
    }
    private static String[] TypeStr = null;
    private static Type getType(String type) {
        if (type.equals(TypeStr[0]) || type.equals("MOVIE")) {
            return Type.MOVIE;
        } else if (type.equals(TypeStr[1]) || type.equals("SERIES") || type.equals("SHOW")) {
            return Type.SERIES;
        }
        return Type.ERROR;
    }
    private static String getTypeStr(Type type) {
        return TypeStr[type.ordinal()];
    }
    private static JSONArray getTypeJSON(Type[] types) {
        JSONArray ret = new JSONArray();
        for (Type type : types) {
            switch (type) {
                case MOVIE -> ret.put("MOVIE");
                case SERIES -> ret.put("SHOW");
            }
        }
        return ret;
    }


    private static final List<String> PlatformCodes = Arrays.asList(
            "dnp", "nfx", "itu", "atp", "ply",
            "prv", "vpl", "mbi", "hrz", "yot",
            "cts", "chi", "dsv", "plp", "gdc",
            "sfx", "wow", "ipl", "mgl", "bhd",
            "fmz", "dkk", "trs", "daf", "hoc",
            "vip", "eve", "ctx", "hbm", "hmf",
            "flb", "tak", "snx", "cla", "sst",
            "cru", "amz", "aft", "sha", "amg",
            "ERROR"
    );
    private static ApkRepo.Platform getPlatform(String code) {
        return ApkRepo.Platform.values()[PlatformCodes.indexOf(code)];
    }
    private static String getPlatformCode(ApkRepo.Platform platform) {
        return PlatformCodes.get(platform.ordinal());
    }
    private static JSONArray getPlatformJSON(ApkRepo.Platform[] platforms) {
        JSONArray ret = new JSONArray();
        for (ApkRepo.Platform platform : platforms) {
            ret.put(getPlatformCode(platform));
        }
        return ret;
    }

    static void init(Context ctx) {
        if (SortOrderStr == null) {
            List<String> lst = new ArrayList<>();
            int[] data = {
                    R.string.random, R.string.trending, R.string.popular, R.string.alphabetical,
                    R.string.release_year, R.string.tmdb_popularity, R.string.imdb_score
            };
            for (int res : data) {
                lst.add(ctx.getString(res));
            }
            lst.add("ERROR");
            SortOrderStr = lst.toArray(new String[]{});
        }
        if (GenreStr == null) {
            List<String> lst = new ArrayList<>();
            int[] data = {
                    R.string.romance, R.string.western, R.string.drama,
                    R.string.thriller, R.string.reality_tv, R.string.horror,
                    R.string.european, R.string.musical, R.string.action_adventure,
                    R.string.fantasy, R.string.comedy, R.string.sport,
                    R.string.crime, R.string.family, R.string.documentary,
                    R.string.science_fiction, R.string.animated, R.string.historical,
                    R.string.war
            };
            for (int res : data) {
                lst.add(ctx.getString(res));
            }
            lst.add("ERROR");
            GenreStr = lst.toArray(new String[]{});
        }
        if (TypeStr == null) {
            List<String> lst = new ArrayList<>();
            int[] data = {
                    R.string.movie, R.string.series
            };
            for (int res : data) {
                lst.add(ctx.getString(res));
            }
            lst.add("ERROR");
            TypeStr = lst.toArray(new String[]{});
        }
    }

    public static class Config {
        public String query = "";

        public String country = "PL";
        public String language = "en";

        public int offset = 0;
        public int count = 40;
        public SortOrder sortOrder = SortOrder.TRENDING;
        public SortOrder sortPostOrder = SortOrder.IMDB_SCORE;

        public Type[] type = {};
        public double minScore = 1.0;
        public int minYear = 1900;
        public int maxYear = 2100;
        public AgeRating[] ageRating = {};
        public Genre[] genre = {};
        public ApkRepo.Platform[] platform = {};
        public Config(){}
        @NonNull
        public Config clone() {
            return new Config(this);
        }
        public Config(Config c) {
            query = c.query;
            country = c.country;
            language = c.language;
            offset = c.offset;
            count = c.count;
            sortOrder = c.sortOrder;
            sortPostOrder = c.sortPostOrder;
            type = Arrays.asList(c.type).toArray(new Type[]{});
            minScore = c.minScore;
            minYear = c.minYear;
            maxYear = c.maxYear;
            ageRating = Arrays.asList(c.ageRating).toArray(new AgeRating[]{});
            genre = Arrays.asList(c.genre).toArray(new Genre[]{});
            platform = Arrays.asList(c.platform).toArray(new ApkRepo.Platform[]{});
        }
    }

    public interface Callback {
        void onFailure(String error);
        void onSuccess(List<MovieTitle> titles);
    }

    private static void parseResponseSimple(JSONArray titlesJSON, Callback cb, SortOrder postOrder) {
        List<MovieTitle> titles = new ArrayList<>();
        try {
            for (int ix = 0; ix < titlesJSON.length(); ix++) {
                JSONObject obj = titlesJSON.getJSONObject(ix).getJSONObject("node");
                JSONObject content = obj.getJSONObject("content");
                if (!content.getBoolean("isReleased"))
                    continue;

                MovieRepo.ID id = new MovieRepo.ID(getType(obj.getString("objectType")), obj.getInt("objectId"));
                MovieTitle title = MovieRepo.get(id);
                if (title == null) {
                    cb.onFailure("No title " + obj.getString("objectType") + "=" + id.type + "_" + id.id + " in repo, must populate!");
                    return;
                }
                titles.add(title);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        sortTitles(titles, postOrder);
        if (titles.isEmpty()) {
            cb.onFailure("Empty response!");
        } else {
            cb.onSuccess(titles);
        }
    }
    public static void downloadImage(String url, File image, Callback cb) {
        Request request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .build();
        } catch (Exception e) {
            Log.w(TAG, "Request failed: " + e);
            cb.onFailure(e.toString());
            return;
        }
        if (Drawable.createFromPath(image.getAbsolutePath()) == null) {
            try {
                image.createNewFile();
                image.setWritable(true);
            } catch (Exception ignored) {}
            httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    cb.onFailure(e.toString());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    BufferedSink sink = Okio.buffer(Okio.sink(image));
                    sink.writeAll(response.body().source());
                    sink.close();
                    Bitmap bmp = BitmapFactory.decodeFile(image.getAbsolutePath());
                    if (bmp != null) {
                        bmp = Bitmap.createScaledBitmap(bmp, 150, (int) (bmp.getHeight() * 150f / bmp.getWidth()), true);
                        try (FileOutputStream out = new FileOutputStream(image.getAbsolutePath())) {
                            bmp.compress(Bitmap.CompressFormat.PNG, 75, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    cb.onSuccess(null);
                }
            });
        } else {
            Log.i(TAG, "Read from cache: " + url + " : " + image.getAbsolutePath());
            cb.onSuccess(null);
        }
    }
    private static void parseResponse(JSONArray titlesJSON, Callback cb, SortOrder postOrder) {
        List<MovieTitle> titles = new ArrayList<>();
        AtomicInteger requests = new AtomicInteger(0);
        ReentrantLock mutex = new ReentrantLock();
        try {
            for (int ix = 0; ix < titlesJSON.length(); ix++) {
                JSONObject obj = titlesJSON.getJSONObject(ix).getJSONObject("node");
                JSONObject content = obj.getJSONObject("content");
                if (!content.getBoolean("isReleased"))
                    continue;
                JSONObject score = content.getJSONObject("scoring");
                JSONArray genres = content.getJSONArray("genres");
                JSONArray starring = content.getJSONArray("starring");
                JSONObject rank = obj.getJSONObject("popularityRank");
                JSONArray offers = obj.getJSONArray("offers");
                JSONArray countries = content.getJSONArray("productionCountries");

                MovieTitlePriv title = new MovieTitlePriv();
                MovieRepo.ID id = new MovieRepo.ID(getType(obj.getString("objectType")), obj.getInt("objectId"));
                title.id = obj.getInt("objectId");
                title.type = getType(obj.getString("objectType"));
                title.title = content.getString("title");
                title.originalTitle = content.getString("originalTitle");
                title.description = content.getString("shortDescription");
                title.runtime = content.getInt("runtime");
                title.imdbScore = score.getDouble("imdbScore");
                title.imageUrl = IMAGES_URL + content.getString("fullPosterUrl");
                title.ageRating = getAgeRating(content.getString("ageCertification"));
                for (int iix = 0; iix < starring.length(); iix++) {
                    JSONObject actor = starring.optJSONObject(iix);
                    if (actor == null)
                        continue;

                    title.actors.put(actor.getString("name"), actor.getString("characterName"));
                }
                title.popularity = rank.getInt("rank");
                title.popularityDelta = (rank.getString("trend").equals("DOWN") ? -1 : 1) * rank.getInt("trendDifference");
                List<Genre> genresList = new ArrayList<>();
                for (int iix = 0; iix < genres.length(); iix++) {
                    JSONObject genre = genres.optJSONObject(iix);
                    if (genre == null)
                        continue;

                    genresList.add(getGenre(genre.getString("shortName")));
                }
                title.genres = genresList.toArray(new Genre[]{});
                List<String> countriesList = new ArrayList<>();
                for (int iix = 0; iix < countries.length(); iix++) {
                    String country = countries.optString(iix);
                    if (country == null || country.equals(""))
                        continue;

                    countriesList.add(country);
                }
                title.productionCountries = countriesList.toArray(new String[]{});
                for (int iix = 0; iix < offers.length(); iix++) {
                    JSONObject offer = offers.optJSONObject(iix);
                    if (offer == null)
                        continue;

                    ApkRepo.Platform platform = getPlatform(offer.getJSONObject("package").getString("shortName"));
                    if (ApkRepo.getPlatformApp(platform) != null) {
                        title.offers.put(platform, offer.getString("standardWebURL"));
                    }
                }
                title.year = content.getInt("originalReleaseYear");

                File image = new File(AllRepos.CACHE_DIR, title.type.name() + "_" + title.id);
                title.imagePath = image.getAbsolutePath();
                if (Drawable.createFromPath(title.imagePath) == null && (image.exists() || image.createNewFile()) && image.setWritable(true)) {
                    requests.incrementAndGet();
                    Request request = new Request.Builder()
                            .url(title.imageUrl)
                            .build();
                    httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            cb.onFailure(e.toString());
                        }
                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            BufferedSink sink = Okio.buffer(Okio.sink(image));
                            sink.writeAll(response.body().source());
                            sink.close();
                            title.imagePath = image.getAbsolutePath();
                            Bitmap bmp = BitmapFactory.decodeFile(title.imagePath);
                            if (bmp != null) {
                                bmp = Bitmap.createScaledBitmap(bmp, 150, (int) (bmp.getHeight() * 150f / bmp.getWidth()), true);
                                try (FileOutputStream out = new FileOutputStream(title.imagePath)) {
                                    bmp.compress(Bitmap.CompressFormat.PNG, 75, out);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            MovieRepo.put(id, new MovieTitle(title));
                            titles.add(MovieRepo.get(id));

                            mutex.lock();
                            if (requests.decrementAndGet() == 0) {
                                if (titles.isEmpty()) {
                                    cb.onFailure("Empty response!");
                                } else {
                                    sortTitles(titles, postOrder);
                                    cb.onSuccess(titles);
                                }
                            }
                            mutex.unlock();
                        }
                    });
                } else {
                    Log.i(TAG, "exists: " + image.getAbsolutePath());
                    MovieRepo.put(id, new MovieTitle(title));
                    titles.add(MovieRepo.get(id));
                }
            }
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
        mutex.lock();
        if (requests.get() == 0) {
            if (titles.isEmpty()) {
                cb.onFailure("Empty response!");
            } else {
                sortTitles(titles, postOrder);
                MovieRepo.save();
                cb.onSuccess(titles);
            }
        }
        mutex.unlock();
    }
    public static void getPopularTitles(Config cfg, Callback cb) {
        try {
            JSONObject body = buildGetPopularTitles(cfg);
            body.put("query", GetPopularTitlesSmall_graphql);
            getPopularTitles(body, cfg.sortPostOrder, new Callback() {
                @Override
                public void onFailure(String error) {
                    Log.w(TAG, error);
                    try {
                        body.put("query", GetPopularTitles_graphql);
                        getPopularTitles(body, cfg.sortPostOrder, cb, false);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                @Override
                public void onSuccess(List<MovieTitle> titles) {
                    cb.onSuccess(titles);
                }
            }, true);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private static void getPopularTitles(JSONObject body, SortOrder sortPostOrder, Callback cb, boolean simple) {
        Request request = new Request.Builder()
                .url(GRAPHQL_URL)
                .method("POST", RequestBody.create(
                        body.toString(),
                        MediaType.get("application/json")))
                .build();
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onFailure(e.toString());
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        JSONArray titlesJSON = new JSONObject(response.body().string())
                                .getJSONObject("data")
                                .getJSONObject("popularTitles")
                                .getJSONArray("edges");
                        if (simple) {
                            parseResponseSimple(titlesJSON, cb, sortPostOrder);
                        } else {
                            parseResponse(titlesJSON, cb, sortPostOrder);
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    ResponseBody body = response.body();
                    cb.onFailure("Status " + response.code() + (body != null ? ": " + body.string() : ""));
                }
            }
        });
    }
    private static List<MovieTitle> sortTitles(List<MovieTitle> titles, SortOrder order) {
        titles.sort((o1, o2) -> {
            switch (order) {
                case POPULAR:
                case TRENDING:
                case TMDB_POPULARITY:
                    return o1.popularity < o2.popularity ? 1 : -1;
                case ALPHABETICAL:
                    return o1.title.compareToIgnoreCase(o2.title);
                case IMDB_SCORE:
                    return o1.imdbScore < o2.imdbScore ? 1 : -1;
                case RELEASE_YEAR:
                    return o1.year < o2.year ? 1 : -1;
            }
            return 0;
        });
        return titles;
    }
    private static JSONObject buildGetPopularTitles(Config cfg) {
        JSONObject json = new JSONObject();
        try {
            JSONObject vars = new JSONObject();
            vars.put("country", cfg.country);
            vars.put("language", cfg.language);
            vars.put("offset", cfg.offset);
            vars.put("first", cfg.count);

            JSONObject offersFilter = new JSONObject();
            offersFilter.put("monetizationTypes", new JSONArray("[\"FLATRATE\"]"));
            offersFilter.put("packages", getPlatformJSON(cfg.platform));
            vars.put("offersFilter", offersFilter);

            vars.put("sortBy", getSortOrderJSON(cfg.sortOrder));
            {
                JSONObject titlesFilter = new JSONObject();
                titlesFilter.put("searchQuery", cfg.query);
                titlesFilter.put("monetizationTypes", new JSONArray("[\"FLATRATE\"]"));
                titlesFilter.put("packages", getPlatformJSON(cfg.platform));
                titlesFilter.put("excludeGenres", new JSONArray());
                titlesFilter.put("excludeIrrelevantTitles", false);
                titlesFilter.put("excludeProductionCountries", new JSONArray());
                titlesFilter.put("presentationTypes", new JSONArray());
                titlesFilter.put("productionCountries", new JSONArray());
                titlesFilter.put("objectTypes", getTypeJSON(cfg.type));
                titlesFilter.put("ageCertifications", getAgeRatingJSON(cfg.ageRating));
                titlesFilter.put("genres", getGenreJSON(cfg.genre));
                {
                    JSONObject score = new JSONObject();
                    score.put("min", cfg.minScore);
                    titlesFilter.put("imdbScore", score);
                }{
                    JSONObject year = new JSONObject();
                    year.put("min", cfg.minYear);
                    year.put("max", cfg.maxYear);
                    titlesFilter.put("releaseYear", year);
                }
                vars.put("titlesFilter", titlesFilter);
            }
            json.put("variables", vars);
            json.put("operationName", "GetPopularTitles");
        } catch (JSONException ignored) {}
        return json;
    }

    public static int jaccardSimilarity(String searchQuery, String realTitle) {
        searchQuery = searchQuery.toLowerCase().replaceAll("[^\\w\\s]", "").trim();
        realTitle = realTitle.toLowerCase().replaceAll("[^\\w\\s]", "").trim();

        Set<String> queryWordSet = new HashSet<>(Arrays.asList(searchQuery.split("\\s+")));
        Set<String> titleWordSet = new HashSet<>(Arrays.asList(realTitle.split("\\s+")));

        Set<String> intersection = new HashSet<>(queryWordSet);
        intersection.retainAll(titleWordSet);
        Set<String> union = new HashSet<>(queryWordSet);
        union.addAll(titleWordSet);

        return 100*intersection.size() / union.size();
    }
    private static boolean isSimilar(String title, String query) {
        return jaccardSimilarity(title, query) > 75;
    }
    public static void findMovieTitle(String title, JustWatch.Callback cb) {
        JustWatch.Config cfg = new JustWatch.Config();
        cfg.count = 1;
        cfg.query = title;
        JustWatch.getPopularTitles(cfg, new JustWatch.Callback() {
            @Override
            public void onFailure(String error) {
                MovieRepo.putAlias(cfg.query, null);
                MovieRepo.save();
                cb.onFailure(error);
            }

            @Override
            public void onSuccess(List<MovieTitle> titles) {
                MovieTitle title = titles.get(0);
                MovieRepo.putAlias(cfg.query,
                        isSimilar(title.title, cfg.query) ? new MovieRepo.ID(title.type, title.id) : null);
                MovieRepo.save();
                cb.onSuccess(titles);
            }
        });
    }
}
