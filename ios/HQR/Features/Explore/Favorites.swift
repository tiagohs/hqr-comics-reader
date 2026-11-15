import Shared
import SwiftUI

/// RF-115: the favorite sources (their latest works are in the feed) and the star that adds or removes one.
@MainActor
final class Favorites: ObservableObject {
    @Published private(set) var ids: Set<String> = []
    /// The feed already has 5 items: the star could not add the source.
    @Published var feedFull = false

    private let observer = Core.graph.favoriteSourceIds()
    private var subscription: Shared.Subscription?

    init() {
        ids = Set((observer.current as? [String]) ?? [])
        subscription = observer.observe { [weak self] value in
            MainActor.assumeIsolated { self?.ids = Set((value as? [String]) ?? []) }
        }
    }

    deinit { subscription?.cancel() }

    func toggle(_ sourceId: String) {
        launch {
            let result = try await Core.graph.toggleFavoriteSource(sourceId: sourceId)
            if result == "full" { self.feedFull = true }
        }
    }
}

/// The star of a source: filled in the tint when it is a favorite.
struct FavoriteStar: View {
    let name: String
    let on: Bool
    let toggle: () -> Void

    var body: some View {
        Button(action: toggle) {
            Image(systemName: on ? "star.fill" : "star")
                .foregroundStyle(on ? AnyShapeStyle(.tint) : AnyShapeStyle(Palette.onSurfaceVariant))
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(on ? L("source_unfavorite", name) : L("source_favorite", name))
    }
}

/// "há 12 min": the age of what the feed shows (RF-116).
func ago(_ epochMs: Int64?) -> String {
    guard let epochMs else { return L("time_just_now") }
    let minutes = max(0, Int((Date().timeIntervalSince1970 * 1000 - Double(epochMs)) / 60_000))
    switch minutes {
    case ..<1: return L("time_just_now")
    case ..<60: return L("time_minutes_ago", minutes)
    case ..<(60 * 24): return L("time_hours_ago", minutes / 60)
    default: return L("time_days_ago", minutes / (60 * 24))
    }
}

#Preview("FavoriteStar · Light") { FavoriteStar(name: "MangaDex", on: true) {}.padding().preferredColorScheme(.light) }
#Preview("FavoriteStar · Dark") { FavoriteStar(name: "MangaDex", on: false) {}.padding().preferredColorScheme(.dark) }
