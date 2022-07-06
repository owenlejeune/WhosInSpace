package com.owenlejeune.whosinspace.extensions

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll

suspend fun Any.awaitAll(vararg jobs: Job) {
    awaitAll(jobs.asList())
}

suspend fun Any.awaitAll(jobs: List<Job>) {
    jobs.joinAll()
}