package com.tiagohs.hqr.sources.mangadex

/** Answers in the shape of the MangaDex API v5, trimmed, with fictional works. */
internal object Fixtures {
    const val TAGS = """
    { "result": "ok", "response": "collection", "data": [
      { "id": "t-romance", "type": "tag", "attributes": { "name": { "en": "Romance" }, "group": "genre" } },
      { "id": "t-horror", "type": "tag", "attributes": { "name": { "en": "Horror" }, "group": "genre" } },
      { "id": "t-action", "type": "tag", "attributes": { "name": { "en": "Action" }, "group": "genre" } }
    ], "limit": 100, "offset": 0, "total": 3 }
    """

    private const val MANGA_A1 = """
    {
      "id": "a1", "type": "manga",
      "attributes": {
        "title": { "en": "Blade of the North" },
        "altTitles": [ { "pt-br": "Lâmina do Norte" }, { "ja": "北の刃" } ],
        "description": { "en": "A smith crosses the ice.", "pt-br": "Uma ferreira atravessa o gelo." },
        "originalLanguage": "ja", "status": "ongoing", "contentRating": "safe",
        "tags": [
          { "id": "t1", "type": "tag", "attributes": { "name": { "en": "Action", "pt-br": "Ação" }, "group": "genre" } },
          { "id": "t2", "type": "tag", "attributes": { "name": { "en": "Fantasy", "pt-br": "Fantasia" }, "group": "genre" } }
        ],
        "year": 2021
      },
      "relationships": [
        { "id": "au1", "type": "author", "attributes": { "name": "Rui Takeda" } },
        { "id": "ar1", "type": "artist", "attributes": { "name": "Mei Lin" } },
        { "id": "c1", "type": "cover_art", "attributes": { "fileName": "capa.jpg" } }
      ]
    }"""

    private const val MANGA_B2 = """
    {
      "id": "b2", "type": "manga",
      "attributes": {
        "title": { "en": "Kaiju Express" }, "altTitles": [], "description": [],
        "originalLanguage": "ko", "status": "completed", "tags": []
      },
      "relationships": []
    }"""

    const val MANGA_LIST = """
    { "result": "ok", "response": "collection", "data": [$MANGA_A1, $MANGA_B2],
      "limit": 20, "offset": 20, "total": 100 }"""

    const val MANGA = """{ "result": "ok", "response": "entity", "data": $MANGA_A1 }"""

    const val FEED = """
    { "result": "ok", "response": "collection", "limit": 2, "offset": 0, "total": 3, "data": [
      { "id": "ch12", "type": "chapter",
        "attributes": { "volume": "2", "chapter": "12", "title": "O farol", "translatedLanguage": "pt-br",
          "externalUrl": null, "publishAt": "2026-10-20T12:00:00+00:00", "pages": 3 },
        "relationships": [ { "id": "g1", "type": "scanlation_group", "attributes": { "name": "Scan Aurora" } } ] },
      { "id": "ch11", "type": "chapter",
        "attributes": { "volume": "2", "chapter": "11.5", "title": "", "translatedLanguage": "pt-br",
          "externalUrl": "https://example.org/c", "publishAt": "2026-10-10T12:00:00+00:00", "pages": 0 },
        "relationships": [] }
    ] }"""

    const val FEED_LAST = """
    { "result": "ok", "response": "collection", "limit": 2, "offset": 2, "total": 3, "data": [
      { "id": "ch1", "type": "chapter",
        "attributes": { "volume": null, "chapter": "1", "title": null, "translatedLanguage": "pt-br", "pages": 20,
          "publishAt": "2026-01-01T00:00:00+00:00" },
        "relationships": [] }
    ] }"""

    const val AT_HOME = """
    { "result": "ok", "baseUrl": "https://node.example.net",
      "chapter": { "hash": "hash1", "data": ["p1.png", "p2.png"], "dataSaver": ["p1.jpg", "p2.jpg"] } }"""
}
