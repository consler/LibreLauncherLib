package net.consler.librelauncherlib.integration.modrinth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Builder for the parameters of GET /search: query, facets, index, offset and limit.
 */
public class SearchQuery
{
    /** Maximum page size the API allows. */
    public static final int MAX_LIMIT = 100;

    private static final Gson FACET_GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** Sort order of the results. */
    public enum Index
    {
        RELEVANCE, DOWNLOADS, FOLLOWS, NEWEST, UPDATED;

        /** @return The API identifier for this sort order. */
        public String getId()
        {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Facet operators. */
    public enum Operator
    {
        EQ(":"), NE("!="), GTE(">="), GT(">"), LTE("<="), LT("<");

        private final String symbol;

        Operator(String symbol)
        {
            this.symbol = symbol;
        }

        /** @return The facet comparison symbol. */
        public String getSymbol()
        {
            return symbol;
        }
    }

    /** Values of the "environment" facet. */
    public enum Environment
    {
        CLIENT_AND_SERVER, CLIENT_ONLY, CLIENT_ONLY_SERVER_OPTIONAL, SINGLEPLAYER_ONLY, SERVER_ONLY,
        SERVER_ONLY_CLIENT_OPTIONAL, DEDICATED_SERVER_ONLY, CLIENT_OR_SERVER, CLIENT_OR_SERVER_PREFERS_BOTH, UNKNOWN;

        /** @return The API identifier for this environment. */
        public String getId() { return name().toLowerCase(Locale.ROOT); }
    }

    /** Values of the "disclosure_types" facet. */
    public enum Disclosure
    {
        AI_CONTENT, AI_CONTENT_CODE, AI_CONTENT_ASSETS, AI_CONTENT_TEXT, AI_FUNCTIONALITY, ADVERTISEMENTS,
        EPILEPSY_TRIGGERS, SYSTEM_INTERACTIONS, TELEMETRY, TELEMETRY_OPT_IN, TELEMETRY_OPT_OUT,
        TELEMETRY_ALWAYS_ACTIVE, DERIVATIVE_WORK, PAID_FEATURES, ARCHIVED;

        /** @return The API identifier for this disclosure type. */
        public String getId() { return name().toLowerCase(Locale.ROOT); }
    }

    private String query;
    private Index index;
    private Integer offset;
    private Integer limit;
    private final List<List<String>> facets = new ArrayList<>();

    /** Creates an empty search query. */
    public SearchQuery() {}

    /** Creates a search query with the given text. */
    public SearchQuery(String query)
    {
        this.query = query;
    }

    /** Sets the text searched by Modrinth. */
    public SearchQuery query(String query)
    {
        this.query = query;
        return this;
    }

    /** Sort order (server default: relevance). */
    public SearchQuery index(Index index)
    {
        this.index = index;
        return this;
    }

    /** Number of results to skip. */
    public SearchQuery offset(int offset)
    {
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
        this.offset = offset;
        return this;
    }

    /** Results per request, 1-100 (server default: 10). */
    public SearchQuery limit(int limit)
    {
        if (limit < 1 || limit > MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        this.limit = limit;
        return this;
    }

    /**
     * Adds one facet group. Values are OR-ed together, the group is AND-ed with all other groups.
     * Example: facet("versions", Operator.EQ, "1.20.1", "1.20.4") = supports 1.20.1 OR 1.20.4.
     */
    public SearchQuery facet(String field, Operator operator, String... anyOf)
    {
        if (anyOf == null || anyOf.length == 0) throw new IllegalArgumentException("at least one value required");
        List<String> group = new ArrayList<>();
        for (String value : anyOf)
        {
            group.add(field + operator.getSymbol() + value);
        }
        facets.add(group);
        return this;
    }

    /** Adds one raw OR group, e.g. anyOf("categories:fabric", "categories:quilt"). */
    public SearchQuery anyOf(String... rawFacets)
    {
        if (rawFacets == null || rawFacets.length == 0) throw new IllegalArgumentException("at least one facet required");
        facets.add(new ArrayList<>(Arrays.asList(rawFacets)));
        return this;
    }

    /** Restricts results to the given project types; values in this call are OR-ed. */
    public SearchQuery projectType(ProjectType... anyOf)
    {
        return facet("project_type", Operator.EQ, Arrays.stream(anyOf).map(ProjectType::getId).toArray(String[]::new));
    }

    /** Restricts results to projects whose versions match any of the given project types. */
    public SearchQuery allProjectTypes(ProjectType... anyOf)
    {
        return facet("all_project_types", Operator.EQ, Arrays.stream(anyOf).map(ProjectType::getId).toArray(String[]::new));
    }

    /** Categories and loaders (loaders are lumped in with categories), e.g. "fabric", "technology". */
    public SearchQuery category(String... anyOf)
    {
        return facet("categories", Operator.EQ, anyOf);
    }

    /** Minecraft versions, e.g. "1.20.1". */
    public SearchQuery gameVersion(String... anyOf)
    {
        return facet("versions", Operator.EQ, anyOf);
    }

    /** Excludes projects supporting the given game version. */
    public SearchQuery excludeGameVersion(String version)
    {
        return facet("versions", Operator.NE, version);
    }

    /** Restricts results by open-source status. */
    public SearchQuery openSource(boolean openSource)
    {
        return facet("open_source", Operator.EQ, String.valueOf(openSource));
    }

    /** Restricts results to any of the given client/server environments. */
    public SearchQuery environment(Environment... anyOf)
    {
        return facet("environment", Operator.EQ, Arrays.stream(anyOf).map(Environment::getId).toArray(String[]::new));
    }

    /** Restricts results to any of the given disclosure types. */
    public SearchQuery disclosure(Disclosure... anyOf)
    {
        return facet("disclosure_types", Operator.EQ, Arrays.stream(anyOf).map(Disclosure::getId).toArray(String[]::new));
    }

    /** Excludes projects with the given disclosure type. */
    public SearchQuery excludeDisclosure(Disclosure disclosure)
    {
        return facet("disclosure_types", Operator.NE, disclosure.getId());
    }

    /** Matches an exact project title. */
    public SearchQuery title(String title)
    {
        return facet("title", Operator.EQ, title);
    }
    /** Matches an exact project author. */
    public SearchQuery author(String author)
    {
        return facet("author", Operator.EQ, author);
    }
    /** Restricts results to projects with any of the given IDs. */
    public SearchQuery projectId(String... anyOf)
    {
        return facet("project_id", Operator.EQ, anyOf);
    }
    /** Restricts results to any of the given SPDX license identifiers. */
    public SearchQuery license(String... spdxIds)
    {
        return facet("license", Operator.EQ, spdxIds);
    }

    /** Restricts results by download count. */
    public SearchQuery downloads(Operator operator, long value)
    {
        return facet("downloads", operator, String.valueOf(value));
    }
    /** Restricts results by follower count. */
    public SearchQuery follows(Operator operator, long value)
    {
        return facet("follows", operator, String.valueOf(value));
    }

    /** Unix timestamp (seconds). */
    public SearchQuery createdTimestamp(Operator operator, long unixSeconds)  { return facet("created_timestamp", operator, String.valueOf(unixSeconds)); }

    /** Unix timestamp (seconds). */
    public SearchQuery modifiedTimestamp(Operator operator, long unixSeconds) { return facet("modified_timestamp", operator, String.valueOf(unixSeconds)); }


    int getOffset()
    {
        return offset == null ? 0 : offset;
    }

    /** Independent copy, used for paging without mutating the caller's builder. */
    SearchQuery copy()
    {
        SearchQuery c = new SearchQuery(query);
        c.index = index;
        c.offset = offset;
        c.limit = limit;
        for (List<String> group : facets) c.facets.add(new ArrayList<>(group));
        return c;
    }

    /** Builds the query string (without the leading '?'). */
    String toQueryString()
    {
        List<String> params = new ArrayList<>();

        if (query != null && !query.isEmpty()) params.add("query=" + enc(query));

        if (!facets.isEmpty())
        {
            JsonArray and = new JsonArray();
            for (List<String> group : facets)
            {
                JsonArray or = new JsonArray();
                group.forEach(or::add);
                and.add(or);
            }
            params.add("facets=" + enc(FACET_GSON.toJson(and)));
        }

        if (index != null) params.add("index=" + index.getId());
        if (offset != null) params.add("offset=" + offset);
        if (limit != null) params.add("limit=" + limit);

        return String.join("&", params);
    }

    private static String enc(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}