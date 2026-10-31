import Shared
import SwiftUI

/// A cover in the 2:3 frame of docs/design.md, with the token radius; a placeholder while it loads or when it fails.
struct CoverImage: View {
    let manga: SourceManga
    var width: CGFloat? = nil
    @StateObject private var loader = ImageLoader()

    var body: some View {
        Color.clear
            .aspectRatio(2.0 / 3.0, contentMode: .fit)
            .frame(width: width)
            .overlay {
                switch loader.phase {
                case .done(let image):
                    Image(uiImage: image).resizable().scaledToFill()
                case .loading:
                    Palette.surfaceHigh
                case .failed:
                    ZStack {
                        Palette.surfaceHigh
                        Image(systemName: "book.closed").foregroundStyle(Palette.onSurfaceVariant)
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: Metrics.shapeCoverRadius, style: .continuous))
            .task(id: manga.coverUrl) { await loader.load(.cover(manga), maxPixel: 600) }
            .accessibilityHidden(true)
    }
}

/// A cover with its title under it, as the grids show works.
struct CoverCard: View {
    let manga: SourceManga
    var badge: String? = nil
    var inLibrary: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            CoverImage(manga: manga)
                .overlay(alignment: .topLeading) {
                    HStack(spacing: 4) {
                        if let badge { Chip(text: badge) }
                        if inLibrary { Chip(text: L("browse_in_library")) }
                    }
                    .padding(6)
                }
            Text(manga.title)
                .font(.subheadline.weight(.medium))
                .lineLimit(2)
                .foregroundStyle(Palette.onSurface)
        }
        .accessibilityElement(children: .combine)
    }
}

/// A small label on a cover ("3 unread", "In library").
struct Chip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(Palette.primaryContainer, in: RoundedRectangle(cornerRadius: Metrics.shapeChip, style: .continuous))
            .foregroundStyle(Palette.onPrimaryContainer)
    }
}

/// The grid of covers every list of works uses: columns follow the width.
struct CoverGrid<Item: Hashable, Cell: View>: View {
    let items: [Item]
    @ViewBuilder let cell: (Item) -> Cell

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 104, maximum: 180), spacing: Metrics.spaceCoverGap, alignment: .top)],
                  spacing: Metrics.spaceCoverGap * 1.5) {
            ForEach(items, id: \.self) { item in cell(item) }
        }
        .padding(.horizontal, Metrics.spaceMarginCompact)
    }
}

/// An error inside the content (docs/design.md): surface, a bar in the error color, the message and the fix.
struct ErrorBox: View {
    let message: String
    var action: String? = nil
    var onAction: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.circle").foregroundStyle(Palette.error)
            VStack(alignment: .leading, spacing: 8) {
                Text(message).font(.subheadline).foregroundStyle(Palette.onSurface)
                if let action, let onAction {
                    Button(action, action: onAction).font(.subheadline.weight(.semibold))
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
        .background(alignment: .leading) {
            ZStack(alignment: .leading) {
                Palette.surfaceHigh
                Palette.error.frame(width: 4)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: Metrics.shapeChip, style: .continuous))
    }
}

/// A whole screen with nothing to show yet: the system's empty state with the app's words.
struct EmptyState: View {
    let title: String
    let message: String
    var systemImage: String = "books.vertical"

    var body: some View {
        ContentUnavailableView(title, systemImage: systemImage, description: Text(message))
    }
}

/// The error that blocks a whole screen, with "Try again".
struct ErrorState: View {
    let kind: ErrorKind
    let retry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(kind.message, systemImage: kind == .offline ? "wifi.slash" : "exclamationmark.triangle")
        } actions: {
            Button(L("action_retry"), action: retry).buttonStyle(.borderedProminent)
        }
    }
}

extension View {
    /// The app background behind a scrolling screen.
    func screenBackground() -> some View {
        scrollContentBackground(.hidden).background(Palette.background)
    }
}

#Preview("Chip · Light") { Chip(text: "3 unread").padding().preferredColorScheme(.light) }
#Preview("Chip · Dark") { Chip(text: "3 unread").padding().preferredColorScheme(.dark) }
#Preview("ErrorBox · Light") {
    ErrorBox(message: ErrorKind.network.message, action: L("action_retry"), onAction: {}).padding().preferredColorScheme(.light)
}
#Preview("ErrorBox · Dark") {
    ErrorBox(message: ErrorKind.network.message, action: L("action_retry"), onAction: {}).padding().preferredColorScheme(.dark)
}
#Preview("EmptyState · Light") { EmptyState(title: L("library_empty_title"), message: L("library_empty_message")).preferredColorScheme(.light) }
#Preview("EmptyState · Dark") { EmptyState(title: L("library_empty_title"), message: L("library_empty_message")).preferredColorScheme(.dark) }
#Preview("ErrorState · Light") { ErrorState(kind: .offline, retry: {}).preferredColorScheme(.light) }
#Preview("ErrorState · Dark") { ErrorState(kind: .offline, retry: {}).preferredColorScheme(.dark) }
#Preview("CoverImage · Light") { CoverImage(manga: PreviewData.manga, width: 120).padding().preferredColorScheme(.light) }
#Preview("CoverImage · Dark") { CoverImage(manga: PreviewData.manga, width: 120).padding().preferredColorScheme(.dark) }
#Preview("CoverCard · Light") { CoverCard(manga: PreviewData.manga, badge: "3").frame(width: 120).padding().preferredColorScheme(.light) }
#Preview("CoverCard · Dark") { CoverCard(manga: PreviewData.manga, badge: "3").frame(width: 120).padding().preferredColorScheme(.dark) }
#Preview("CoverGrid · Light") { ScrollView { CoverGrid(items: [1, 2, 3, 4]) { _ in CoverCard(manga: PreviewData.manga) } }.preferredColorScheme(.light) }
#Preview("CoverGrid · Dark") { ScrollView { CoverGrid(items: [1, 2, 3, 4]) { _ in CoverCard(manga: PreviewData.manga) } }.preferredColorScheme(.dark) }
