import Shared
import SwiftUI

/// SCR-LIB-001: category tabs, the grid, "Continue reading" and the search (RF-52 to RF-58).
struct LibraryScreen: View {
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var model = ScreenModel(Core.graph.libraryViewModel()) {
        Screen<LibraryViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var query = ""
    @State private var removed: LibraryRemoval?

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if state.tabs.count > 1 { tabs(state) }
                if let entry = state.continueReading, query.isEmpty { continueCard(entry) }
                if !state.loading && state.items.isEmpty {
                    if query.isEmpty {
                        EmptyState(title: L("library_empty_title"), message: L("library_empty_message"))
                            .frame(minHeight: 360)
                    } else {
                        EmptyState(title: L("library_filtered_title"), message: L("library_filtered_message"),
                                   systemImage: "magnifyingglass")
                            .frame(minHeight: 360)
                    }
                } else {
                    CoverGrid(items: state.items) { item in
                        Button { navigator.open(.manga(item.manga.id)) } label: {
                            CoverCard(manga: item.manga.source, badge: item.unread > 0 ? "\(item.unread)" : nil)
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            Button(role: .destructive) {
                                model.viewModel.remove(item: item) { removal in removed = removal }
                            } label: {
                                Label(L("manga_remove_library"), systemImage: "trash")
                            }
                        }
                    }
                }
            }
            .padding(.vertical, 8)
        }
        .screenBackground()
        .navigationTitle(L("nav_library"))
        .searchable(text: $query, prompt: L("library_search_hint"))
        .onChange(of: query) { _, text in model.viewModel.search(text: text) }
        .refreshable { model.viewModel.updateLibrary() }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button { navigator.open(.categories) } label: { Label(L("library_categories"), systemImage: "folder") }
                    Button { model.viewModel.updateLibrary() } label: {
                        Label(L("library_update"), systemImage: "arrow.clockwise")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle").accessibilityLabel(L("action_more"))
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let removal = removed {
                UndoBar(text: L("library_removed")) {
                    model.viewModel.undoRemove(removal: removal)
                    removed = nil
                } dismiss: { removed = nil }
                .id(ObjectIdentifier(removal))
            }
        }
    }

    private func tabs(_ state: LibraryViewModel.State) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(state.tabs, id: \.id) { tab in
                    let selected = tab.id == state.selectedTab
                    Button { model.viewModel.selectTab(id: tab.id) } label: {
                        Text("\(tabName(tab)) \(tab.count)")
                            .font(.subheadline.weight(selected ? .semibold : .regular))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(selected ? Palette.primaryContainer : Palette.surfaceHigh, in: Capsule())
                            .foregroundStyle(selected ? Palette.onPrimaryContainer : Palette.onSurface)
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(selected ? .isSelected : [])
                }
            }
            .padding(.horizontal, Metrics.spaceMarginCompact)
        }
    }

    private func tabName(_ tab: LibraryViewModel.Tab) -> String {
        if !tab.name.isEmpty { return tab.name }
        return tab.id == LibraryViewModel.Companion.shared.ALL ? L("library_tab_all") : L("library_tab_uncategorized")
    }

    private func continueCard(_ entry: HistoryEntry) -> some View {
        Button {
            navigator.read(mangaId: entry.manga.id, chapterId: entry.chapter.id, page: entry.chapter.lastPage)
        } label: {
            HStack(spacing: 12) {
                CoverImage(manga: entry.manga.source, width: 52)
                VStack(alignment: .leading, spacing: 4) {
                    Text(L("library_continue_reading")).font(ComicFont.bangers(15, relativeTo: .caption))
                        .foregroundStyle(Palette.onSurfaceVariant)
                    ComicTitle(text: entry.manga.source.title, size: 24).lineLimit(2)
                    Text(L("manga_continue_chapter", entry.chapter.numberText, Int(entry.chapter.lastPage) + 1))
                        .font(.subheadline).foregroundStyle(Palette.onSurfaceVariant)
                }
                Spacer()
                Image(systemName: "play.fill").foregroundStyle(Palette.primary)
            }
            .padding(12)
            // ADR-026: "Continuar lendo" as a speech balloon: an ink panel with its tail.
            .inkPanel()
            .overlay(alignment: .bottomLeading) {
                Path { path in
                    path.move(to: .zero)
                    path.addLine(to: CGPoint(x: 18, y: 0))
                    path.addLine(to: CGPoint(x: 0, y: 18))
                    path.closeSubpath()
                }
                .fill(Palette.onSurface)
                .frame(width: 18, height: 18)
                .offset(x: 24, y: 18)
            }
            .padding(.bottom, 18)
        }
        .buttonStyle(.plain)
        .padding(.horizontal, Metrics.spaceMarginCompact)
    }
}

/// "Removed · Undo" at the bottom of a screen (every removal can be undone, docs/design.md).
struct UndoBar: View {
    let text: String
    let undo: () -> Void
    let dismiss: () -> Void

    var body: some View {
        HStack {
            Text(text).font(.subheadline)
            Spacer()
            Button(L("action_undo"), action: undo).font(.subheadline.weight(.semibold))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Palette.surfaceHighest, in: RoundedRectangle(cornerRadius: Metrics.shapeChip, style: .continuous))
        .padding(.horizontal, Metrics.spaceMarginCompact)
        .padding(.bottom, 8)
        .task {
            try? await Task.sleep(nanoseconds: 5_000_000_000)
            dismiss()
        }
    }
}

#Preview("LibraryScreen · Light") { LibraryScreen().previewScreen().preferredColorScheme(.light) }
#Preview("LibraryScreen · Dark") { LibraryScreen().previewScreen().preferredColorScheme(.dark) }
#Preview("UndoBar · Light") { UndoBar(text: L("library_removed"), undo: {}, dismiss: {}).preferredColorScheme(.light) }
#Preview("UndoBar · Dark") { UndoBar(text: L("library_removed"), undo: {}, dismiss: {}).preferredColorScheme(.dark) }
