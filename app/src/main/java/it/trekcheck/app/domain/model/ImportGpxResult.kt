package it.trekcheck.app.domain.model

enum class ImportErrorCode {
    READ_FAILED,
    FILE_TOO_LARGE,
    UNSAFE_XML,
    MALFORMED_XML,
    INVALID_COORDINATES,
    INSUFFICIENT_POINTS,
    TOO_MANY_POINTS,
    STORAGE_FAILED,
}

sealed class GpxImportError(val code: ImportErrorCode) {
    data object ReadFailed : GpxImportError(ImportErrorCode.READ_FAILED) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }

    data object FileTooLarge : GpxImportError(ImportErrorCode.FILE_TOO_LARGE) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }

    data object UnsafeXml : GpxImportError(ImportErrorCode.UNSAFE_XML) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }

    data object MalformedXml : GpxImportError(ImportErrorCode.MALFORMED_XML) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }

    data object InvalidCoordinates : GpxImportError(ImportErrorCode.INVALID_COORDINATES) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }

    data object InsufficientPoints : GpxImportError(ImportErrorCode.INSUFFICIENT_POINTS) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }

    data object TooManyPoints : GpxImportError(ImportErrorCode.TOO_MANY_POINTS) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }

    data object StorageFailed : GpxImportError(ImportErrorCode.STORAGE_FAILED) {
        override fun toString(): String = "GpxImportError(code=${code.name})"
    }
}

sealed interface GpxParseResult {
    data class Success(val route: ParsedGpxRoute) : GpxParseResult
    data class Failure(val error: GpxImportError) : GpxParseResult
}

sealed interface ImportGpxResult {
    data class Success(
        val plan: SavedPlanSummary,
        val hasElevation: Boolean,
        val hasTimestamps: Boolean,
    ) : ImportGpxResult

    data class Failure(
        val error: GpxImportError,
    ) : ImportGpxResult
}
