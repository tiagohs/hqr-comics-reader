#if DEBUG
import Shared
import SwiftUI

/// What the previews share: every view has a light and a dark preview in its own file (scripts/ios/check_previews.py).
extension View {
    /// The core started (in memory, like the UI tests) and a Navigator that goes nowhere: what every screen expects.
    @MainActor func previewEnvironment() -> some View {
        Core.start()
        let presenter = ReaderPresenter()
        return environmentObject(Navigator(presenter: presenter)).environmentObject(presenter)
    }

    /// A screen in its navigation stack, as the app shows it.
    @MainActor func previewScreen() -> some View {
        NavigationStack { self }.previewEnvironment()
    }
}

/// Fixed sample data, so components preview without network.
@MainActor
enum PreviewData {
    static let manga = IosBridge.shared.sampleManga(title: "Steel Ball Run", sourceId: "mangadex")
    static let work = IosBridge.shared.sampleWork(id: 1, title: "Steel Ball Run")
    static let chapters = (1...5).map { IosBridge.shared.sampleChapter(id: Int64($0), mangaId: 1, number: Double($0), read: $0 < 3) }
}
#endif
