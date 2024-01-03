package com.ukhamitov.dartimportsorter.model

data class ImportInfo(
    val library: String,
    val alias: String? = null,
    val show: List<String>? = null,
    val hide: List<String>? = null
)
