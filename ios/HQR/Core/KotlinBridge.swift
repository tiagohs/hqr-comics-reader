import Shared
import SwiftUI

// Small conveniences over the Objective-C export of the Kotlin core: ids as strings, Kotlin boxes as Swift values and
// the texts of the core's enums.

extension SourceManga {
    /// The source this work comes from, as text (the Kotlin id is a value class).
    var sourceKey: String { IosBridge.shared.sourceOf(manga: self) }
    /// Unique across sources: what lists use as identity.
    var listId: String { sourceKey + "/" + key }
}

extension Chapter {
    var numberText: String { IosBridge.shared.chapterNumber(chapter: self) }

    /// "Chapter 10" or "Chapter 10 · Title".
    var label: String {
        let base = L("manga_chapter_label", numberText)
        guard let title = source.title, !title.isEmpty else { return base }
        return base + " · " + title
    }
}

extension ErrorKind {
    /// The message the shared screens show for this failure (same keys as Android and the web).
    var message: String {
        switch self {
        case .offline: L("error_offline")
        case .network: L("error_network")
        case .server: L("error_server")
        case .parse: L("error_parse")
        case .notFound: L("error_not_found")
        case .auth: L("error_auth")
        case .challenge: L("error_challenge")
        case .unavailable: L("error_unavailable")
        case .certificate: L("error_certificate")
        case .bridge: L("error_bridge")
        default: L("error_unknown")
        }
    }
}

/// The source's name in the app language: the local folder is a word, the others are names (RNF-14).
func sourceDisplayName(_ sourceId: String) -> String {
    sourceId == "local" ? L("source_local") : IosBridge.shared.sourceName(sourceId: sourceId)
}

extension Set where Element == KotlinLong {
    var int64s: Set<Int64> { Set<Int64>(map { $0.int64Value }) }
}

extension Array where Element == Int64 {
    var kotlin: [KotlinLong] { map { KotlinLong(value: $0) } }
}

/// Runs a Kotlin suspend call from SwiftUI, on the main actor (Kotlin's suspend export expects it).
@MainActor
func launch(_ work: @escaping @MainActor () async throws -> Void) {
    Task { @MainActor in try? await work() }
}
