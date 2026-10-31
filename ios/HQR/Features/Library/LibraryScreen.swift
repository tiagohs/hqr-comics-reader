import Combine
import Shared
import SwiftUI

/// SCR-LIB-001: category tabs, the works as grid or list, "Continue reading", the search, filter/sort/display and
/// picking several works (RF-52 to RF-58).
struct LibraryScreen: View {
    @EnvironmentObject private var navigator: Navigator
    @StateObject private var models = LibraryModels()
    @State private var query = ""
    @State private var removed: LibraryRemoval?
    @State private var removedMany: [LibraryRemoval] = []
    @State private var showOptions = false
    @State private var moving = false

    private var model: ScreenModel<LibraryViewModel, LibraryViewModel.State> { models.screen }
    private var picked: Set<Int64> { Set(models.picked.value.compactMap { ($0 as? KotlinLong)?.int64Value }) }

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if state.tabs.count > 1 { tabs(state) }
                if let entry = state.continueReading, query.isEmpty, picked.isEmpty { continueCard(entry) }
                if !state.loading && state.items.isEmpty {
                    if state.isEmpty {
                        EmptyState(title: L("library_empty_title"), message: L("library_empty_message"), onomatopoeia: L("onomatopoeia_empty"))
                            .frame(minHeight: 360)
                    } else {
                        EmptyState(title: L("library_filtered_title"), message: L("library_filtered_message"),
                                   systemImage: "line.3.horizontal.decrease.circle")
                            .frame(minHeight: 360)
                    }
                } else if state.options.display == .list {
                    LazyVStack(alignment: .leading, spacing: 8) {
                        ForEach(Array(state.items.enumerated()), id: \.element.manga.id) { index, item in
                            work(item, options: state.options) { listRow(item, options: state.options) }
                                .cascadeIn(index, since: models.opened)
                        }
                    }
                    .padding(.horizontal, Metrics.spaceMarginCompact)
                } else {
                    CoverGrid(items: state.items) { item in
                        work(item, options: state.options) { cell(item, options: state.options) }
                    }
                }
            }
            .padding(.vertical, 8)
        }
        .screenBackground()
        .navigationTitle(picked.isEmpty ? L("nav_library") : L("library_picked", picked.count))
        .searchable(text: $query, prompt: L("library_search_hint"))
        .onChange(of: query) { _, text in model.viewModel.search(text: text) }
        .refreshable { model.viewModel.updateLibrary() }
        .toolbar {
            if picked.isEmpty { mainToolbar } else { pickedToolbar(state) }
        }
        .sheet(isPresented: $showOptions) {
            LibraryOptionsSheet(state: model.state) { model.viewModel.setOptions(value: $0) }
        }
        .sheet(isPresented: $moving) {
            CategoryPicker(categories: state.categories, assigned: []) { ids in
                IosBridge.shared.moveSelected(viewModel: model.viewModel, categoryIds: ids.map { KotlinLong(value: $0) })
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let removal = removed {
                UndoBar(text: L("library_removed")) {
                    model.viewModel.undoRemove(removal: removal)
                    removed = nil
                } dismiss: { removed = nil }
                .id(ObjectIdentifier(removal))
            } else if !removedMany.isEmpty {
                UndoBar(text: L("library_removed_many", removedMany.count)) {
                    model.viewModel.undoRemoveAll(removals: removedMany)
                    removedMany = []
                } dismiss: { removedMany = [] }
            }
        }
    }

    @ToolbarContentBuilder private var mainToolbar: some ToolbarContent {
        ToolbarItemGroup(placement: .primaryAction) {
            Button { showOptions = true } label: {
                Image(systemName: model.state.options.hasFilters ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                    .accessibilityLabel(L("library_options"))
            }
            Menu {
                Button { navigator.open(.categories) } label: { Label(L("library_categories"), systemImage: "folder") }
                Button { navigator.open(.downloads) } label: { Label(L("settings_downloads"), systemImage: "arrow.down.circle") }
                Button { model.viewModel.updateLibrary() } label: {
                    Label(L("library_update"), systemImage: "arrow.clockwise")
                }
            } label: {
                Image(systemName: "ellipsis.circle").accessibilityLabel(L("action_more"))
            }
        }
    }

    /// RF-57: what can be done with the picked works.
    @ToolbarContentBuilder
    private func pickedToolbar(_ state: LibraryViewModel.State) -> some ToolbarContent {
            ToolbarItem(placement: .cancellationAction) {
                Button { model.viewModel.clearSelection() } label: {
                    Image(systemName: "xmark").accessibilityLabel(L("library_clear_selection"))
                }
            }
            ToolbarItemGroup(placement: .primaryAction) {
                Menu {
                    Button { model.viewModel.selectAll() } label: { Label(L("library_select_all"), systemImage: "checkmark.circle") }
                    if !state.categories.isEmpty {
                        Button { moving = true } label: { Label(L("library_move"), systemImage: "folder") }
                    }
                    Button { model.viewModel.markSelectedRead() } label: { Label(L("library_mark_read"), systemImage: "eye") }
                    Button { model.viewModel.downloadSelectedUnread() } label: {
                        Label(L("library_download_unread"), systemImage: "arrow.down.circle")
                    }
                    Button(role: .destructive) {
                        model.viewModel.removeSelected { removals in removedMany = removals }
                    } label: {
                        Label(L("library_remove_picked"), systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle").accessibilityLabel(L("action_more"))
                }
            }
    }

    /// RF-57: a long press picks a work; while some are picked, a tap picks or unpicks.
    private func work(_ item: Shared.LibraryItem, options: LibraryOptions, @ViewBuilder label: () -> some View) -> some View {
        let isPicked = picked.contains(item.manga.id)
        return Button {
            if picked.isEmpty {
                navigator.openWork(item.manga.id, from: item.manga.source)
            } else {
                model.viewModel.toggleSelected(mangaId: item.manga.id)
            }
        } label: {
            label()
                .padding(isPicked ? 4 : 0)
                .overlay { if isPicked { Rectangle().stroke(Palette.primary, lineWidth: 4) } }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isPicked ? .isSelected : [])
        .contextMenu {
            Button { model.viewModel.toggleSelected(mangaId: item.manga.id) } label: {
                Label(L("manga_select"), systemImage: "checkmark.circle")
            }
            Button(role: .destructive) {
                model.viewModel.remove(item: item) { removal in removed = removal }
            } label: {
                Label(L("manga_remove_library"), systemImage: "trash")
            }
        }
    }

    /// RF-54: the badges chosen in "Exibir".
    private func badge(_ item: Shared.LibraryItem, options: LibraryOptions) -> String? {
        options.unreadBadge && item.unread > 0 ? "\(item.unread)" : nil
    }

    @ViewBuilder
    private func cell(_ item: Shared.LibraryItem, options: LibraryOptions) -> some View {
        if options.display == .compactGrid {
            CoverImage(manga: item.manga.source)
                .coverSource(coverTransitionId(sourceId: item.manga.source.sourceKey, key: item.manga.source.key))
                .overlay(alignment: .topTrailing) { badges(item, options: options).padding(4) }
                .overlay(alignment: .bottomLeading) {
                    // ADR-026: the title in a yellow caption box, like a comic panel's narration.
                    Text(item.manga.source.title)
                        .font(ComicFont.bangers(14, relativeTo: .caption))
                        .lineLimit(2)
                        .foregroundStyle(Self.captionInk)
                        .padding(.horizontal, 6).padding(.top, 4).padding(.bottom, 2)
                        .background(Self.caption)
                        .overlay(Rectangle().stroke(Self.captionInk, lineWidth: 2))
                        .padding(.trailing, 16)
                }
                .accessibilityElement(children: .combine)
        } else {
            CoverCard(manga: item.manga.source, badge: badge(item, options: options))
                .overlay(alignment: .topLeading) { sideBadges(item, options: options).padding(4) }
        }
    }

    private func listRow(_ item: Shared.LibraryItem, options: LibraryOptions) -> some View {
        HStack(spacing: 14) {
            CoverImage(manga: item.manga.source, width: 56)
                .coverSource(coverTransitionId(sourceId: item.manga.source.sourceKey, key: item.manga.source.key))
            VStack(alignment: .leading, spacing: 4) {
                Text(item.manga.source.title).font(.headline).lineLimit(2).foregroundStyle(Palette.onSurface)
                Text(item.unread > 0 ? P("library_unread", Int(item.unread)) : L("library_up_to_date"))
                    .font(.subheadline).foregroundStyle(Palette.onSurfaceVariant)
                sideBadges(item, options: options)
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }

    private func badges(_ item: Shared.LibraryItem, options: LibraryOptions) -> some View {
        VStack(alignment: .trailing, spacing: 4) {
            if let count = badge(item, options: options) { Burst(text: count, size: 34) }
            sideBadges(item, options: options)
        }
    }

    @ViewBuilder
    private func sideBadges(_ item: Shared.LibraryItem, options: LibraryOptions) -> some View {
        HStack(spacing: 4) {
            if options.downloadedBadge && item.downloaded > 0 {
                Image(systemName: "arrow.down.circle.fill").foregroundStyle(Palette.primary)
                    .background(Palette.surface, in: Circle())
                    .accessibilityLabel(L("library_filter_downloaded"))
            }
            if options.sourceBadge {
                Text(sourceDisplayName(item.manga.source.sourceKey))
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 6).padding(.vertical, 1)
                    .background(Palette.onSurface).foregroundStyle(Palette.surface)
            }
        }
    }

    private static let caption = Color(red: 1, green: 0xF1 / 255, blue: 0xA8 / 255)
    private static let captionInk = Color(red: 0x16 / 255, green: 0x16 / 255, blue: 0x1A / 255)

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
/// The library's ViewModel with the works picked by a long press (RF-57).
@MainActor
final class LibraryModels: ObservableObject {
    let screen: ScreenModel<LibraryViewModel, LibraryViewModel.State>
    let picked: FlowModel<NSSet>
    /// RF-118: the works cascade in as the library opens.
    let opened = Date()
    private var changes: [AnyCancellable] = []

    init() {
        let viewModel = Core.graph.libraryViewModel()
        screen = ScreenModel(viewModel) { Screen<LibraryViewModel.State>(viewModel: $0, flow: $0.state) }
        picked = FlowModel(viewModel.picked)
        changes = [
            screen.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
            picked.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
        ]
    }
}

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
