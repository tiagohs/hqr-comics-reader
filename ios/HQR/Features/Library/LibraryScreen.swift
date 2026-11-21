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
    @State private var updateNotice: String?
    // RF-126: one work opens the sheet; several, only all or none.
    @State private var marking: MarkReadingModel?
    @State private var markingSheet = false
    @State private var markingMany = false
    @State private var markNotice: String?
    @State private var markedMany: LibraryViewModel.Marked?
    @State private var editingCharacters = false
    @Environment(\.scenePhase) private var scenePhase

    private var model: ScreenModel<LibraryViewModel, LibraryViewModel.State> { models.screen }
    private var picked: Set<Int64> { Set(models.picked.value.compactMap { ($0 as? KotlinLong)?.int64Value }) }

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let days = models.account.viewModel.staleDays(state: models.account.state)?.intValue {
                    SyncStaleNotice(days: days).padding(.horizontal, Metrics.spaceMarginCompact)
                }
                if let progress = models.update.value.firstObject as? UpdateProgress { updating(progress) }
                if !state.isEmpty { areas(state) }
                if state.tabs.count > 1 { tabs(state) }
                if !state.isEmpty && query.isEmpty && picked.isEmpty { tracking(state) }
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
        .refreshable {
            model.viewModel.updateLibrary()
            // RF-122: pulling also syncs the account.
            models.account.viewModel.syncNow()
        }
        .toolbar {
            if picked.isEmpty { mainToolbar } else { pickedToolbar(state) }
        }
        // RF-58: leaving the app stops the update; what arrived stays.
        .onChange(of: scenePhase) { _, phase in if phase == .background { model.viewModel.stopUpdate() } }
        .onChange(of: models.updated.value.count) { _, _ in
            guard let result = models.updated.value.firstObject as? UpdateProgress else { return }
            model.viewModel.updateShown()
            let found = result.newChapters == 0 ? L("library_new_none")
                : result.newChapters == 1 ? L("library_new_one") : L("library_new_many", Int(result.newChapters))
            updateNotice = result.failed > 0 ? found + " · " + L("library_update_failed", Int(result.failed)) : found
        }
        .alert(updateNotice ?? "", isPresented: Binding(get: { updateNotice != nil }, set: { if !$0 { updateNotice = nil } })) {
            Button(L("action_ok"), role: .cancel) {}
        }
        .sheet(isPresented: $editingCharacters) {
            CharactersSheet(
                names: IosBridge.shared.characterList(options: model.state.options),
                onAdd: { model.viewModel.setOptions(value: model.state.options.withCharacter(name: $0)) },
                onRemove: { model.viewModel.setOptions(value: model.state.options.withoutCharacter(name: $0)) }
            )
        }
        .sheet(isPresented: $showOptions) {
            LibraryOptionsSheet(state: model.state) { model.viewModel.setOptions(value: $0) }
        }
        .sheet(isPresented: $markingSheet) {
            if let marking {
                MarkReadingSheet(model: marking) { saved in markNotice = markReadingDone(saved) }
            }
        }
        .confirmationDialog(L("mark_many_title", picked.count), isPresented: $markingMany, titleVisibility: .visible) {
            Button(L("mark_many_read")) { markMany(read: true) }
            Button(L("mark_many_none")) { markMany(read: false) }
            Button(L("action_cancel"), role: .cancel) {}
        } message: {
            Text(L("mark_many_one_hint"))
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
            } else if let notice = markNotice {
                UndoBar(text: notice) {
                    marking?.viewModel.undo()
                    markNotice = nil
                } dismiss: { markNotice = nil }
            } else if let marked = markedMany {
                UndoBar(text: L(marked.read ? "mark_many_done_read" : "mark_many_done_unread", Int(marked.works))) {
                    model.viewModel.undoMark(marked: marked)
                    markedMany = nil
                } dismiss: { markedMany = nil }
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
                    Button {
                        if picked.count == 1, let id = picked.first {
                            marking = MarkReadingModel(mangaId: id)
                            markingSheet = true
                            model.viewModel.clearSelection()
                        } else {
                            markingMany = true
                        }
                    } label: { Label(L("mark_reading"), systemImage: "book") }
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

    private func markMany(read: Bool) {
        model.viewModel.markSelected(read: read) { marked in markedMany = marked }
    }

    /// RF-128: "Tudo", "HQs" and "Mangás", with their counts.
    private func areas(_ state: LibraryViewModel.State) -> some View {
        Picker(L("library_area_label"), selection: Binding(get: { state.area }, set: { model.viewModel.selectArea(area: $0) })) {
            ForEach([LibraryArea.all, LibraryArea.comics, LibraryArea.manga], id: \.self) { area in
                Text("\(L(Self.areaKey(area))) \((state.areaCounts[area] as? KotlinInt)?.intValue ?? 0)").tag(area)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, Metrics.spaceMarginCompact)
    }

    static func areaKey(_ area: LibraryArea) -> String {
        switch area {
        case .comics: "library_area_comics"
        case .manga: "library_area_manga"
        default: "library_area_all"
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
                .overlay { ReadingOnCover(item: item) }
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
                        // RF-129: above the reading bar.
                        .padding(.bottom, Self.showsReadBar(item) ? 6 : 0)
                }
                .accessibilityElement(children: .combine)
        } else {
            CoverCard(manga: item.manga.source, badge: badge(item, options: options), reading: item)
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

    static func showsReadBar(_ item: Shared.LibraryItem) -> Bool {
        let read = Int(IosBridge.shared.readCount(item: item))
        return item.totalChapters > 0 && read > 0 && read < Int(item.totalChapters)
    }

    private static let caption = Color(red: 1, green: 0xF1 / 255, blue: 0xA8 / 255)
    private static let captionInk = Color(red: 0x16 / 255, green: 0x16 / 255, blue: 0x1A / 255)

    /// RF-58: which work is being asked, out of how many.
    private func updating(_ progress: UpdateProgress) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            ProgressView(value: Double(progress.done), total: Double(max(progress.total, 1)))
            Text(L("library_updating", min(Int(progress.done) + 1, Int(progress.total)), Int(progress.total), progress.current ?? ""))
                .font(.caption).foregroundStyle(Palette.onSurfaceVariant).lineLimit(1)
        }
        .padding(.horizontal, Metrics.spaceMarginCompact)
        .accessibilityElement(children: .combine)
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

    /// RF-132 to RF-134: personagens (HQs), progresso, continuar, saiu capítulo and the shelf by situation.
    @ViewBuilder
    private func tracking(_ state: LibraryViewModel.State) -> some View {
        let names = IosBridge.shared.characterNames(state: state)
        if state.area == .comics && !names.isEmpty {
            CharacterButtons(
                names: names,
                counts: IosBridge.shared.characterCounts(state: state).map { Int(truncating: $0) },
                selected: state.character,
                onSelect: { model.viewModel.selectCharacter(name: $0) },
                onEdit: { editingCharacters = true }
            )
        }
        let all = state.area == .all
        ProgressCard(
            progress: state.progress,
            comics: all ? IosBridge.shared.summaryOf(perArea: state.perArea, area: .comics) : nil,
            manga: all ? IosBridge.shared.summaryOf(perArea: state.perArea, area: .manga) : nil,
            selected: state.shelfState,
            onSelect: { model.viewModel.selectShelfState(value: $0) }
        )
        .padding(.horizontal, Metrics.spaceMarginCompact)
        if !state.recent.isEmpty {
            Text(L("library_continue")).font(.title3.weight(.bold)).accessibilityAddTraits(.isHeader)
                .padding(.horizontal, Metrics.spaceMarginCompact).padding(.top, 8)
            ContinueCarousel(entries: state.recent) { entry in
                navigator.read(mangaId: entry.manga.id, chapterId: entry.chapter.id, page: entry.chapter.lastPage)
            }
        }
        if !state.newChapters.isEmpty && state.shelfState == nil {
            NewChaptersSection(
                items: state.newChapters,
                onOpen: { navigator.openWork($0.manga.id, from: $0.manga.source) },
                onSeeAll: { model.viewModel.selectShelfState(value: .theNewChapters) }
            )
            .padding(.horizontal, Metrics.spaceMarginCompact)
        }
        Text(L("library_shelf")).font(.title3.weight(.bold)).accessibilityAddTraits(.isHeader)
            .padding(.horizontal, Metrics.spaceMarginCompact).padding(.top, 8)
        StateButtons(
            parts: shelfOrder.compactMap { s in
                let n = Int(IosBridge.shared.stateCount(progress: state.progress, state: s))
                return n > 0 ? (s, n) : nil
            },
            selected: state.shelfState,
            allCount: Int(state.progress.works),
            onSelect: { model.viewModel.selectShelfState(value: $0) }
        )
        .padding(.horizontal, Metrics.spaceMarginCompact)
    }
}

/// "Removed · Undo" at the bottom of a screen (every removal can be undone, docs/design.md).
/// The library's ViewModel with the works picked by a long press (RF-57).
@MainActor
final class LibraryModels: ObservableObject {
    let screen: ScreenModel<LibraryViewModel, LibraryViewModel.State>
    let picked: FlowModel<NSSet>
    /// RF-58: the running update and the last finished one, as zero or one item.
    let update: FlowModel<NSArray>
    let updated: FlowModel<NSArray>
    /// RF-125: the account, for the notice of a sync failing for more than a day.
    let account: ScreenModel<AccountViewModel, AccountViewModel.State>
    /// RF-118: the works cascade in as the library opens.
    let opened = Date()
    private var changes: [AnyCancellable] = []

    init() {
        let viewModel = Core.graph.libraryViewModel()
        screen = ScreenModel(viewModel) { Screen<LibraryViewModel.State>(viewModel: $0, flow: $0.state) }
        picked = FlowModel(viewModel.picked)
        update = FlowModel(observer: IosBridge.shared.libraryUpdate(viewModel: viewModel))
        updated = FlowModel(observer: IosBridge.shared.libraryUpdated(viewModel: viewModel))
        account = ScreenModel(Core.graph.accountViewModel()) { Screen<AccountViewModel.State>(viewModel: $0, flow: $0.state) }
        changes = [
            account.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
            update.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
            updated.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
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
