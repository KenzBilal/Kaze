package com.kaze.search

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig

/**
 * AppSearch document wrapping WatchItem fields for on-device global search.
 * Indexed fields: title (high relevance), year, type, isWatched.
 * posterUrl and imdbId stored but not indexed (used for navigation on result tap).
 */
@Document
data class WatchItemDocument(

    @Document.Namespace
    val namespace: String = "watchlater",

    @Document.Id
    val id: String,

    @Document.Score
    val score: Int = 1,

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val title: String,

    @Document.LongProperty
    val year: Long,

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
    val mediaType: String,   // "MOVIE" or "SERIES"

    @Document.BooleanProperty
    val isWatched: Boolean,

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_NONE)
    val imdbId: String,

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_NONE)
    val posterUrl: String
)
