import Combine
import Shared
import SwiftUI

/// SCR-OBR-001: cover, details, the main read button, chapters with read and download marks, and the cover's color as
/// the tint (RF-29 to RF-35, RF-62, RF-98).
struct MangaScreen: View {
    let mangaId: Int64
    @EnvironmentObject private var navigator: Navigator
    @Environment(\.colorScheme) private var scheme
    @StateObject private var models: MangaModels
    @State private var expanded = false
    /// The bar shows the work's name only after its title scrolled under it; over the panel it stays clear.
    @State private var titleUnderBar = false
    /// RF-73: the source this work came from, when this app doesn't have it.
    @State private var missingSource: String?
    /// RF-53: choosing the work's categories.
    @State private var picking = false
    /// RF-118: the panel draws itself in as the work opens.
    @State private var heroIn = 0.0
    /// RF-118: the chapters cascade in once, as the work opens.
    @State private var opened = Date()

    init(mangaId: Int64) {
        self.mangaId = mangaId
        // One ViewModel per screen, made once: the autoclosure runs only when SwiftUI first creates the state.
        _models = StateObject(wrappedValue: MangaModels(mangaId: mangaId))
    }

    private var model: ScreenModel<MangaViewModel, MangaViewModel.State> { models.screen }
    private var downloads: FlowModel<NSDictionary> { models.downloads }
    private var selection: FlowModel<NSSet> { models.selection }
    private var categories: [Shared.Category] { IosBridge.shared.categoriesOf(choice: models.categories.value) }

    private var selected: Set<Int64> {
        Set(selection.value.compactMap { ($0 as? KotlinLong)?.int64Value })
    }

    var body: some View {
        let state = model.state
        List {
            if let manga = state.manga {
                Section { header(manga, state: state) }
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                    .listRowSeparator(.hidden)
            }
            // RF-117: a failure of the details stays here; a failure of the list stays in the list.
            if let error = state.error, state.chaptersError == nil {
                // What was saved stays on screen; only the refresh failed.
                ErrorBox(message: state.manga == nil ? error.message : L("manga_refresh_failed"), action: L("action_retry")) {
                    model.viewModel.refresh()
                }
                    .listRowBackground(Color.clear)
            }
            Section {
                if state.chaptersLoading {
                    ForEach(0..<4, id: \.self) { skeletonRow($0) }
                }
                if let error = state.chaptersError, let manga = state.manga {
                    ErrorBox(message: sourceErrorMessage(error, source: sourceDisplayName(manga.source.sourceKey)),
                             action: L("action_retry"),
                             onAction: { model.viewModel.refresh() },
                             secondary: manga.source.url.flatMap(URL.init(string:)).map { url in (L("action_open_browser"), { UIApplication.shared.open(url) }) })
                        .listRowBackground(Color.clear)
                }
                ForEach(Array(state.chapters.enumerated()), id: \.element.id) { index, chapter in
                    row(chapter, state: state).cascadeIn(index, since: opened)
                }
            } header: {
                HStack {
                    Text(state.chaptersLoading ? L("manga_chapters", 0).filter { !$0.isNumber }.trimmingCharacters(in: .whitespaces)
                                               : L("manga_chapters", Int(state.totalChapters)))
                        .font(ComicFont.bangers(24, relativeTo: .headline))
                        .foregroundStyle(Palette.onSurface)
                    Spacer()
                    if state.chaptersLoading, let manga = state.manga {
                        HStack(spacing: 8) {
                            DotsLoader()
                            Text(L("manga_searching_source", sourceDisplayName(manga.source.sourceKey)))
                                .font(.caption.weight(.semibold)).foregroundStyle(Palette.onSurfaceVariant)
                        }
                    } else {
                        filterMenu(state)
                    }
                }
                .textCase(nil)
            }
            if state.chapters.isEmpty && !state.chaptersLoading && state.chaptersError == nil && state.error == nil {
                Text(L("manga_no_chapters")).foregroundStyle(Palette.onSurfaceVariant)
            }
        }
        .listStyle(.plain)
        .listRowSeparatorTint(Palette.onSurface)
        .screenBackground()
        .refreshable { model.viewModel.refresh() }
        .navigationTitle(titleUnderBar ? (state.manga?.source.title ?? "") : "")
        .navigationBarTitleDisplayMode(.inline)
        .tint(tint(state.manga))
        .toolbar { toolbar(state) }
        .zoomFromCover(navigator.coverIds[mangaId])
        .task(id: state.manga?.source.sourceKey) {
            if let id = state.manga?.source.sourceKey { missingSource = try? await Core.graph.unavailableSourceName(sourceId: id) }
        }
        .sheet(isPresented: $picking) {
            CategoryPicker(categories: categories,
                           assigned: Set(IosBridge.shared.assignedOf(choice: models.categories.value).map { $0.int64Value })) { ids in
                IosBridge.shared.setCategories(viewModel: model.viewModel, categoryIds: ids.map { KotlinLong(value: $0) })
            }
        }
        .overlay {
            if state.manga == nil && state.refreshing { ProgressView() }
        }
    }

    // MARK: Header

    private static let panelReach: CGFloat = 600
    /// Status bar plus the inline navigation bar, where the list scrolls under.
    private static let barHeight: CGFloat = 100

    private func header(_ manga: Manga, state: MangaViewModel.State) -> some View {
        let source = manga.source
        let vertical = source.alternativeTitles.map { $0.trimmingCharacters(in: .whitespaces) }.first { $0.isJapanese }
        return VStack(alignment: .leading, spacing: 0) {
            // The work's panel (ADR-026): tilted cover over speed lines (manga) or Ben-Day dots (comics).
            VStack(spacing: 16) {
                CoverImage(manga: source, width: 140)
                    .overlay(Rectangle().stroke(Palette.onSurface, lineWidth: 2))
                    .background(Palette.onSurface.offset(x: 6, y: 6))
                    .rotationEffect(.degrees(-2.5 * heroIn))
                    .frame(maxWidth: .infinity)
                    .overlay(alignment: .topTrailing) {
                        if let vertical { VerticalTitle(text: vertical).padding(.trailing, 16) }
                    }
                if !source.genres.isEmpty {
                    FlowLayout(spacing: 8) {
                        ForEach(Array(source.genres.prefix(4)), id: \.self) { genre in
                            Text(genre)
                                .font(ComicFont.bangers(15, relativeTo: .caption))
                                .padding(.horizontal, 10).padding(.vertical, 3)
                                .background(Palette.background)
                                .overlay(Rectangle().stroke(Palette.onSurface, lineWidth: 2))
                        }
                    }
                    .padding(.horizontal, 16)
                }
            }
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity)
            .background {
                ZStack {
                    // The panel goes on under the clear bar (and the pull to refresh), up to the top of the screen.
                    Color.accentColor.opacity(0.18).padding(.top, -Self.panelReach)
                    if vertical != nil {
                        // Centered on the cover, kept inside the panel.
                        SpeedLines(progress: heroIn).clipped()
                    } else {
                        Halftone(color: Color.accentColor.opacity(0.4), step: 9, radius: 2.4, progress: heroIn)
                            .padding(.top, -Self.panelReach)
                    }
                }
            }
            .modifier(Entrance(progress: $heroIn))
            Rectangle().fill(Palette.onSurface).frame(height: 3)
            VStack(alignment: .leading, spacing: 12) {
                ComicTitle(text: source.title)
                    .onGeometryChange(for: Bool.self) { $0.frame(in: .scrollView).maxY < Self.barHeight } action: { titleUnderBar = $0 }
                let people = Array(Set(source.authors + source.artists)).sorted().prefix(2)
                if let missingSource {
                    Text(L("source_unavailable", missingSource)).font(.subheadline.weight(.semibold)).foregroundStyle(Palette.error)
                }
                MetaTags(author: people.isEmpty ? nil : people.joined(separator: " · "),
                         status: statusLabel(source.status),
                         statusColor: statusColor(source.status),
                         source: sourceDisplayName(source.sourceKey),
                         extra: [source.language?.uppercased()].compactMap { $0 })
                if let description = source.description_, !description.isEmpty {
                    let long = description.count > 600
                    Text(description)
                        .font(.subheadline)
                        .lineLimit(expanded || !long ? nil : 8)
                        .onTapGesture { if long { withAnimation { expanded.toggle() } } }
                        .accessibilityHint(long ? (expanded ? L("action_less_text") : L("action_more_text")) : "")
                }
                HStack(spacing: 12) {
                    readButton(state)
                    if manga.inLibrary {
                        Button { model.viewModel.toggleLibrary() } label: {
                            Image(systemName: "bookmark.fill").frame(width: 32, height: 32)
                        }
                        .buttonStyle(.borderedProminent)
                        .buttonBorderShape(.circle)
                        .accessibilityLabel(L("manga_in_library"))
                    }
                }
                if !manga.inLibrary {
                    Button {
                        model.viewModel.toggleLibrary()
                        // RF-53: putting it in asks for the categories when there are any.
                        if !categories.isEmpty { picking = true }
                    } label: {
                        Label(L("manga_add_library"), systemImage: "plus").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .controlSize(.large)
                }
            }
            .padding(Metrics.spaceMarginCompact)
        }
    }

    private func statusLabel(_ status: PublicationStatus) -> String? {
        switch status {
        case .ongoing: L("status_ongoing")
        case .completed: L("status_completed")
        case .cancelled: L("status_cancelled")
        case .hiatus: L("status_hiatus")
        default: nil
        }
    }

    private func statusColor(_ status: PublicationStatus) -> Color {
        switch status {
        case .ongoing: Color(red: 0.18, green: 0.49, blue: 0.20)
        case .completed: .accentColor
        case .hiatus: Color(red: 0.70, green: 0.42, blue: 0)
        case .cancelled: Palette.error
        default: Palette.outline
        }
    }

    /// RF-117: a soft row where a chapter is still on its way.
    private func skeletonRow(_ index: Int) -> some View {
        HStack(spacing: 14) {
            Skeleton().frame(width: 40, height: 28)
            VStack(alignment: .leading, spacing: 8) {
                Skeleton().frame(width: CGFloat(200 - index * 24), height: 14)
                Skeleton().frame(width: 110, height: 10)
            }
        }
        .padding(.vertical, 8)
        .listRowBackground(Color.clear)
    }

    @ViewBuilder
    private func readButton(_ state: MangaViewModel.State) -> some View {
        let bridge = IosBridge.shared
        if let chapter = bridge.actionChapter(action: state.action) {
            let kind = bridge.actionKind(action: state.action)
            let page = bridge.actionPage(action: state.action)
            Button {
                navigator.read(mangaId: mangaId, chapterId: chapter.id, page: page)
            } label: {
                Group {
                    switch kind {
                    case "continue": Text(L("manga_continue_short", chapter.numberText))
                    case "again": Text(L("action_read_again"))
                    default: Text(L("manga_read_chapter", chapter.numberText))
                    }
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .buttonBorderShape(.capsule)
            .controlSize(.large)
            // RF-117: the button waits for the chapters.
            .disabled(state.chaptersLoading)
        }
    }

    // MARK: Chapters

    private func row(_ chapter: Chapter, state: MangaViewModel.State) -> some View {
        let download = downloads.value[KotlinLong(value: chapter.id)] as? DownloadItem
        let isSelected = selected.contains(chapter.id)
        return Button {
            if !selected.isEmpty {
                model.viewModel.toggleSelected(id: chapter.id)
            } else if let external = chapter.source.externalUrl, let url = URL(string: external) {
                UIApplication.shared.open(url)
            } else {
                navigator.read(mangaId: mangaId, chapterId: chapter.id, page: chapter.read ? 0 : chapter.lastPage)
            }
        } label: {
            HStack(spacing: 12) {
                if !selected.isEmpty {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle").foregroundStyle(.tint)
                }
                Text(chapter.numberText)
                    .font(ComicFont.bangers(26, relativeTo: .title2))
                    .foregroundStyle(chapter.read ? AnyShapeStyle(Palette.onSurfaceVariant) : AnyShapeStyle(.tint))
                    .frame(minWidth: 44)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(chapter.label).font(.body.weight(chapter.read ? .regular : .semibold)).lineLimit(1)
                        if state.newChapterIds.contains(KotlinLong(value: chapter.id)) { Stamp(text: L("manga_new")) }
                    }
                    .foregroundStyle(chapter.read ? Palette.onSurfaceVariant : Palette.onSurface)
                    subtitle(chapter).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                }
                Spacer()
                downloadMark(chapter, item: download)
            }
        }
        .buttonStyle(.plain)
        .contextMenu { menu(chapter, download: download) }
        .swipeActions(edge: .leading) {
            Button { IosBridge.shared.setRead(viewModel: model.viewModel, chapterIds: [KotlinLong(value: chapter.id)], read: !chapter.read) } label: {
                Image(systemName: chapter.read ? "eye.slash" : "eye")
            }
        }
    }

    private func subtitle(_ chapter: Chapter) -> Text {
        var parts: [String] = []
        if let group = chapter.source.group { parts.append(group) }
        if let date = chapter.source.uploadedAtEpochMs?.int64Value {
            parts.append(Date(timeIntervalSince1970: Double(date) / 1000).formatted(date: .abbreviated, time: .omitted))
        }
        if !chapter.read && chapter.lastPage > 0, let count = chapter.pageCount?.intValue {
            parts.append(L("manga_page_of", Int(chapter.lastPage) + 1, count))
        }
        if chapter.source.externalUrl != nil { parts.append(L("manga_external")) }
        if chapter.removedAtSource { parts.append(L("manga_removed_from_source")) }
        return Text(parts.joined(separator: " · "))
    }

    @ViewBuilder
    private func downloadMark(_ chapter: Chapter, item: DownloadItem?) -> some View {
        if chapter.downloaded {
            Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint).accessibilityLabel(L("manga_downloaded"))
        } else if let item {
            if item.pagesTotal > 0 {
                ProgressView(value: Double(item.pagesDone), total: Double(item.pagesTotal)).progressViewStyle(.circular)
                    .frame(width: 22, height: 22)
            } else {
                Image(systemName: "clock").foregroundStyle(Palette.onSurfaceVariant)
            }
        } else if model.viewModel.canDownload && chapter.source.externalUrl == nil {
            Button {
                IosBridge.shared.download(viewModel: model.viewModel, chapterIds: [KotlinLong(value: chapter.id)])
            } label: {
                Image(systemName: "arrow.down.circle").accessibilityLabel(L("manga_download"))
            }
            .buttonStyle(.borderless)
        }
    }

    @ViewBuilder
    private func menu(_ chapter: Chapter, download: DownloadItem?) -> some View {
        let ids = [KotlinLong(value: chapter.id)]
        Button { IosBridge.shared.setRead(viewModel: model.viewModel, chapterIds: ids, read: !chapter.read) } label: {
            Label(chapter.read ? L("manga_mark_unread") : L("manga_mark_read"), systemImage: chapter.read ? "eye.slash" : "eye")
        }
        Button { model.viewModel.markPreviousAsRead(chapter: chapter) } label: {
            Label(L("manga_mark_previous"), systemImage: "arrow.up.to.line")
        }
        Button { model.viewModel.toggleSelected(id: chapter.id) } label: {
            Label(L("manga_select"), systemImage: "checkmark.circle")
        }
        if chapter.downloaded || download != nil {
            Button(role: .destructive) { IosBridge.shared.removeDownloads(viewModel: model.viewModel, chapterIds: ids) } label: {
                Label(L("manga_delete_download"), systemImage: "trash")
            }
        }
    }

    private func filterMenu(_ state: MangaViewModel.State) -> some View {
        Menu {
            Picker(L("manga_order"), selection: Binding(get: { state.order }, set: { model.viewModel.setOrder(value: $0) })) {
                Text("↓").tag(MangaViewModel.Order.newestFirst)
                Text("↑").tag(MangaViewModel.Order.oldestFirst)
            }
            Picker("", selection: Binding(get: { state.filter }, set: { model.viewModel.setFilter(value: $0) })) {
                Text(L("manga_filter_all")).tag(MangaViewModel.ChapterFilter.all)
                Text(L("manga_filter_unread")).tag(MangaViewModel.ChapterFilter.unread)
                Text(L("manga_downloaded")).tag(MangaViewModel.ChapterFilter.downloaded)
            }
            if state.groups.count > 1 {
                Picker(L("manga_open_group"), selection: Binding(get: { state.group ?? "" },
                                                                set: { model.viewModel.setGroup(value: $0.isEmpty ? nil : $0) })) {
                    Text(L("manga_filter_all")).tag("")
                    ForEach(state.groups, id: \.self) { Text($0).tag($0) }
                }
            }
        } label: {
            Image(systemName: "line.3.horizontal.decrease.circle").accessibilityLabel(L("manga_order"))
        }
    }

    @ToolbarContentBuilder
    private func toolbar(_ state: MangaViewModel.State) -> some ToolbarContent {
        if !selected.isEmpty {
            ToolbarItemGroup(placement: .bottomBar) {
                let ids = selected.map { KotlinLong(value: $0) }
                Button(L("manga_mark_read")) { IosBridge.shared.setRead(viewModel: model.viewModel, chapterIds: ids, read: true) }
                Spacer()
                if model.viewModel.canDownload {
                    Button(L("manga_download")) { IosBridge.shared.download(viewModel: model.viewModel, chapterIds: ids) }
                    Spacer()
                }
                Button(L("manga_clear_selection")) { model.viewModel.clearSelection() }
            }
        }
        ToolbarItem(placement: .primaryAction) {
            Menu {
                if model.viewModel.canDownload {
                    Button { model.viewModel.downloadUnread(count: KotlinInt(value: 1)) } label: {
                        Label(L("manga_download_next", 1), systemImage: "arrow.down")
                    }
                    Button { model.viewModel.downloadUnread(count: nil) } label: {
                        Label(L("manga_download_unread"), systemImage: "arrow.down.circle")
                    }
                }
                Button { model.viewModel.refresh() } label: {
                    Label(L("manga_refresh"), systemImage: "arrow.clockwise")
                }
                if state.manga?.inLibrary == true && !categories.isEmpty {
                    Button { picking = true } label: { Label(L("manga_edit_categories"), systemImage: "folder") }
                }
                Button { navigator.open(.migrate(mangaId)) } label: {
                    Label(L("migrate_action"), systemImage: "arrow.left.arrow.right")
                }
                if let link = state.manga?.source.url, let url = URL(string: link) {
                    Link(destination: url) { Label(L("action_open_browser"), systemImage: "safari") }
                    ShareLink(item: url)
                }
            } label: {
                Image(systemName: "ellipsis.circle").accessibilityLabel(L("action_more"))
            }
        }
    }

    // MARK: Cover color

    /// RF-98: the work takes the cover's primary color; without one the app's violet stays.
    private func tint(_ manga: Manga?) -> Color {
        let enabled = (Core.graph.preferences().coverColor.value as? KotlinBoolean)?.boolValue ?? true
        guard enabled, let seed = manga?.coverColor?.int32Value else {
            return Palette.primary
        }
        let roles = CoverColor.shared.roles(seed: seed, dark: scheme == .dark)
        return Color(argb: roles.primary)
    }
}

/// The work's ViewModel and the two other flows it shows (download marks, selection), alive while the screen is.
@MainActor
final class MangaModels: ObservableObject {
    let screen: ScreenModel<MangaViewModel, MangaViewModel.State>
    let downloads: FlowModel<NSDictionary>
    let selection: FlowModel<NSSet>
    let categories: FlowModel<KotlinPair<NSArray, NSSet>>
    private var changes: [AnyCancellable] = []

    init(mangaId: Int64) {
        let viewModel = Core.graph.mangaViewModel(mangaId: mangaId)
        screen = ScreenModel(viewModel) { Screen<MangaViewModel.State>(viewModel: $0, flow: $0.state) }
        downloads = FlowModel(viewModel.downloadStates)
        selection = FlowModel(viewModel.selected)
        categories = FlowModel(viewModel.categoryChoice)
        changes = [
            categories.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
            screen.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
            downloads.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
            selection.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() },
        ]
    }
}

extension Color {
    /// An ARGB int from the core (cover colors).
    init(argb: Int32) {
        let value = UInt32(bitPattern: argb)
        self.init(.sRGB,
                  red: Double((value >> 16) & 0xFF) / 255,
                  green: Double((value >> 8) & 0xFF) / 255,
                  blue: Double(value & 0xFF) / 255,
                  opacity: Double((value >> 24) & 0xFF) / 255)
    }
}

#Preview("MangaScreen · Light") { MangaScreen(mangaId: 1).previewScreen().preferredColorScheme(.light) }
#Preview("MangaScreen · Dark") { MangaScreen(mangaId: 1).previewScreen().preferredColorScheme(.dark) }
