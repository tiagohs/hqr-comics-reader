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
            .clipShape(Rectangle())
            // ADR-026: every cover is a comic panel with an ink frame.
            .overlay(Rectangle().stroke(Palette.onSurface, lineWidth: 2))
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
                .overlay(alignment: .topTrailing) {
                    // Unread count in a burst; "Na biblioteca" in an ink tag.
                    VStack(alignment: .trailing, spacing: 4) {
                        if let badge { Burst(text: badge, size: 34) }
                        if inLibrary {
                            Text(L("browse_in_library")).font(ComicFont.bangers(13, relativeTo: .caption2))
                                .padding(.horizontal, 6).padding(.vertical, 1)
                                .background(Palette.onSurface).foregroundStyle(Palette.background)
                        }
                    }
                    .padding(4)
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
    /// A second way out, such as "Abrir no navegador".
    var secondary: (String, () -> Void)? = nil

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.circle").foregroundStyle(Palette.error)
            VStack(alignment: .leading, spacing: 8) {
                Text(message).font(.subheadline).foregroundStyle(Palette.onSurface)
                HStack(spacing: 16) {
                    if let action, let onAction {
                        Button(action, action: onAction).font(.subheadline.weight(.semibold))
                    }
                    if let secondary {
                        Button(secondary.0, action: secondary.1).font(.subheadline.weight(.semibold))
                    }
                }
                .buttonStyle(.borderless)
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

/// A whole screen with nothing to show yet: the system's empty state with the app's words. With an onomatopoeia, a
/// tilted comic panel with halftone takes the symbol's place (ADR-026).
struct EmptyState: View {
    let title: String
    let message: String
    var systemImage: String = "books.vertical"
    var onomatopoeia: String? = nil

    var body: some View {
        if let onomatopoeia {
            VStack(spacing: 16) {
                ZStack {
                    Halftone()
                    Text(onomatopoeia)
                        .font(ComicFont.bangers(30))
                        .foregroundStyle(Palette.onSurface)
                        .padding(.horizontal, 6)
                        .background(Palette.surface)
                        .rotationEffect(.degrees(-4))
                }
                .frame(width: 150, height: 110)
                .inkPanel(border: 3, shadow: 5)
                .rotationEffect(.degrees(-3))
                .accessibilityHidden(true)
                Text(title).font(.title2.weight(.semibold)).multilineTextAlignment(.center)
                Text(message).foregroundStyle(Palette.onSurfaceVariant).multilineTextAlignment(.center)
            }
            .padding(32)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ContentUnavailableView(title, systemImage: systemImage, description: Text(message))
        }
    }
}

/// The error that blocks a whole screen: the "OPS!" burst, the message and "Try again".
struct ErrorState: View {
    let kind: ErrorKind
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Burst(text: "OPS!", size: 112, color: Palette.error, textColor: Palette.onError)
                .accessibilityHidden(true)
            Label(kind.message, systemImage: kind == .offline ? "wifi.slash" : "exclamationmark.triangle")
                .labelStyle(.titleOnly)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Button(L("action_retry"), action: retry).buttonStyle(.borderedProminent)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
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
#Preview("EmptyState panel · Light") {
    EmptyState(title: L("library_empty_title"), message: L("library_empty_message"), onomatopoeia: L("onomatopoeia_empty")).preferredColorScheme(.light)
}
#Preview("EmptyState panel · Dark") {
    EmptyState(title: L("library_empty_title"), message: L("library_empty_message"), onomatopoeia: L("onomatopoeia_empty")).preferredColorScheme(.dark)
}
#Preview("ErrorState · Light") { ErrorState(kind: .offline, retry: {}).preferredColorScheme(.light) }
#Preview("ErrorState · Dark") { ErrorState(kind: .offline, retry: {}).preferredColorScheme(.dark) }
#Preview("CoverImage · Light") { CoverImage(manga: PreviewData.manga, width: 120).padding().preferredColorScheme(.light) }
#Preview("CoverImage · Dark") { CoverImage(manga: PreviewData.manga, width: 120).padding().preferredColorScheme(.dark) }
#Preview("CoverCard · Light") { CoverCard(manga: PreviewData.manga, badge: "3").frame(width: 120).padding().preferredColorScheme(.light) }
#Preview("CoverCard · Dark") { CoverCard(manga: PreviewData.manga, badge: "3").frame(width: 120).padding().preferredColorScheme(.dark) }
#Preview("CoverGrid · Light") { ScrollView { CoverGrid(items: [1, 2, 3, 4]) { _ in CoverCard(manga: PreviewData.manga) } }.preferredColorScheme(.light) }
#Preview("CoverGrid · Dark") { ScrollView { CoverGrid(items: [1, 2, 3, 4]) { _ in CoverCard(manga: PreviewData.manga) } }.preferredColorScheme(.dark) }
